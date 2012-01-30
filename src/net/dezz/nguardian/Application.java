package net.dezz.nguardian;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import com.dumbster.smtp.SimpleSmtpServer;

public class Application {
	
	private static final String PROPERTY_FILENAME = "nguardian.properties";
	public static int CTRL_EQUIVALENT_KEY;

	private NGuardianPresenter presenter;
	private SimpleSmtpServer smtpServer;
	private static Application instance;
	private Configuration config;
	private List<String> safeDomains;
	private Image largeAppIcon;
	private Image smallAppIcon;
	private static Logger logger = Logger.getLogger(Application.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (SWT.getPlatform().equals("cocoa")) {
			// OSX platform
			CTRL_EQUIVALENT_KEY = SWT.COMMAND;
		} else {
			// Windows/Linux
			CTRL_EQUIVALENT_KEY = SWT.CTRL;
		}
		
		instance = new Application();
		instance.run(args);
	}
	
	private void run(String[] args) {
		// Initialize log4j configuration
		URL log4jconf = ClassLoader.getSystemResource("log4j.xml");
		DOMConfigurator.configure(log4jconf);
		
		// load properties
		try {
			this.config = new Configuration(PROPERTY_FILENAME);
			
		} catch (Exception e) {
			logger.error("Failed to load configuration.", e);
		}
		
		// create main window
		Display display = Display.getDefault();
		Realm.runWithDefault(SWTObservables.getRealm(display), new Runnable() {
			public void run() {
				Tray tray = null;
				TrayItem trayItem = null;
				
				try {
					// load icons
					InputStream smallIconStream = null;
					InputStream largeIconStream = null;
					try {
						smallIconStream = ClassLoader.getSystemClassLoader().getResourceAsStream("3_20x20.png");
						smallAppIcon = new Image(Display.getDefault(), smallIconStream);
						
						largeIconStream = ClassLoader.getSystemClassLoader().getResourceAsStream("3_128x128.png");
						largeAppIcon = new Image(Display.getDefault(), largeIconStream);
						
					} finally {
						if (smallIconStream != null) {
							smallIconStream.close();
						}
						
						if (largeIconStream != null) {
							largeIconStream.close();
						}
					}
		
					presenter = new NGuardianPresenter();
					Shell shell = presenter.createApplicationWindow();
					
					// Load configuration and Start SMTP server
					applyConfig();
					
					// add icon to tasktray
					tray = Display.getDefault().getSystemTray();
					trayItem = new TrayItem(tray, SWT.ICON);
					trayItem.setText("NGuardian");
					trayItem.setToolTipText("NGuardian");
					trayItem.setImage(smallAppIcon);
					trayItem.addSelectionListener(new SelectionListener() {
						public void widgetSelected(SelectionEvent arg0) {
							presenter.onTaskItemSelected(arg0);
						}
						public void widgetDefaultSelected(SelectionEvent arg0) {
						}
					});
		
					// open window
					Display display = Display.getDefault();
					shell.setImage(largeAppIcon);
					shell.open();
					shell.layout();
					while (!shell.isDisposed()) {
						try {
							if (!display.readAndDispatch()) {
								display.sleep();
							}
						} catch (Exception e) {
							logger.error("Unexpected erorr occured.", e);
							ErrorDialog dlg = new ErrorDialog(shell, "error", e.getMessage(),
									throwableToStatus(e), IStatus.ERROR);
							dlg.open();
						}
					}	
					
					trayItem.dispose();
					display.dispose();
					
				} catch (Exception e) {
					e.printStackTrace();
					
				} finally {
					if (smtpServer != null) {
						// stop server
						smtpServer.stop();
					}
					
					if (trayItem != null) {
						trayItem.dispose();
					}
				}
			}
		});
		
		
		// save configurations
		saveConfig();
	}
	
	private void saveConfig() {
		try {
			this.config.saveToFile(PROPERTY_FILENAME);
		} catch (Exception e) {
			logger.error("Failed to save configuration file.", e);
		}
	}
	
	private Status throwableToStatus(Throwable e) {
		Throwable t = e;
		ArrayList<IStatus> list = new ArrayList<IStatus>();
		do {
			Status s = new Status(IStatus.ERROR, "NGuardian", t.getMessage());
			list.add(s);
		} while ((t = t.getCause()) != null);
		
		return new MultiStatus("NGuardian", 0, list.toArray(new IStatus[list.size()]), e.getMessage(), e);
	}
	
	/**
	 * 
	 * @param domains Comma separated domains list.
	 * @return
	 */
	private List<String> loadSafeDomains(String domains) {
		List<String> rv = new ArrayList<String>();
		if (domains == null || domains.length() == 0) {
			return rv;
		}
		
		String[] domainDevided = domains.split(",");
		for (String domain : domainDevided) {
			rv.add(domain.trim());
		}
		
		return rv;
	}
	
	public boolean isSafeAddress(InternetAddress address) {
		if (safeDomains.size() == 0) {
			// no need to check
			return false;
		}
		
		for (String domain : safeDomains) {
			if (address.getAddress().endsWith(domain)) {
				return true;
			}
		}
		
		return false;
	}

	public SimpleSmtpServer getSmtpServer() {
		return smtpServer;
	}
	
	private void restartSmtpServer() {
		if (smtpServer != null) {
			smtpServer.stop();
		}
		
		SimpleSmtpServer oldServer = smtpServer;
		smtpServer = new SimpleSmtpServer(Integer.valueOf(config.getProperty(Application.class, "server.port")));
		
		if (oldServer != null) {
			smtpServer.copyUnsentMailsFrom(oldServer);
		}
		
		smtpServer.setMailListListener(presenter);
		
		// create new thread to run server
		Thread thread = new Thread(smtpServer);
		thread.start();
	}
	
	public void applyConfig() {
		// save properties
		saveConfig();
		
		// re-construct MailSender instance
		MailSender.recreateInstance();
		
		// load and parse safe domains
		String csvDomains = config.getProperty(Application.class, "safe_domains");
		safeDomains = loadSafeDomains(csvDomains);

		// start smtp server
		restartSmtpServer();
	}
	
	public static Application getInstance() {
		return instance;
	}

	public Image getLargeAppIcon() {
		return largeAppIcon;
	}

	public Image getSmallAppIcon() {
		return smallAppIcon;
	}

	public Configuration getConfig() {
		return config;
	}
}
