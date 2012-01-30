package net.dezz.nguardian;

import java.util.ResourceBundle;

import net.dezz.nguardian.verify.ControlVerifier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Shell;

public class ConfigDialogPresenter {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("net.dezz.nguardian.messages");
	private String smtpHost;
	private int smtpPort;
	private String safeDomains;
	private int listenPort;
	private boolean outputLogWhenSendMail;
	private int defaultAutoSendExpireMinutes;
	private boolean dirty;
	private ConfigDialogView view;
	
	public ConfigDialogPresenter(Shell parent) {
		// load config
		Configuration config = Application.getInstance().getConfig();
		this.smtpHost = config.getProperty(Application.class, "smtp.host");
		this.smtpPort = Integer.valueOf(config.getProperty(Application.class, "smtp.port"));
		this.listenPort = Integer.valueOf(config.getProperty(Application.class, "server.port"));
		this.safeDomains = csvToMultiLine(config.getProperty(Application.class, "safe_domains"));
		
		this.outputLogWhenSendMail = Boolean.valueOf(config.getProperty(Application.class, "outputLogWhenSendMail"));
		
		view = new ConfigDialogView(this, parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		view.createContents();
		
	}
	
	public Object open() {
		return view.open();
	}
	
	public void onOKButtonClicked(SelectionEvent e) {
		// Save to Configuration object
		if (!areAllControlsValid()) {
			return;
		}
		
		Configuration config = Application.getInstance().getConfig();
		config.setProperty(Application.class, "smtp.host", smtpHost);
		config.setProperty(Application.class, "smtp.port", String.valueOf(smtpPort));
		config.setProperty(Application.class, "server.port", String.valueOf(listenPort));
		config.setProperty(Application.class, "safe_domains", multiLineToCsv(safeDomains));
		config.setProperty(Application.class, "outputLogWhenSendMail", String.valueOf(outputLogWhenSendMail));
		config.setProperty(Application.class, "defaultAutoSendExpireMinutes", String.valueOf(defaultAutoSendExpireMinutes));
		
		this.view.shell.close();
		this.view.result = ConfigDialogView.RESULT_OK;
	}
	
	private String csvToMultiLine(String csv) {
		String[] lines = csv.split(",");
		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");
		for (String l : lines) {
			if (sb.length() > 0) {
				sb.append(ls);
			}
			sb.append(l);
		}
		
		return sb.toString();
	}
	
	private String multiLineToCsv(String multiline) {
		String ls = System.getProperty("line.separator");
		String[] lines = multiline.split(ls);
		StringBuilder sb = new StringBuilder();
		for (String l : lines) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(l);
		}
		
		return sb.toString();
		
	}
	
	private boolean areAllControlsValid() {
		for (ControlVerifier<?> verifier : this.view.getVerifiers().values()) {
			verifier.verify();
			if (!verifier.isValid()) {
				return false;
			}
		}
		return true;
	}
	
	public void onCloseDialog() {
	}
	
	public void onVerifySafeDomains(VerifyEvent e) {
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}

	public int getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(int smtpPort) {
		this.smtpPort = smtpPort;
	}

	public String getSafeDomains() {
		return safeDomains;
	}

	public void setSafeDomains(String safeDomains) {
		this.safeDomains = safeDomains;
	}

	public int getListenPort() {
		return listenPort;
	}

	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public int getAutoDefaultSendExpireMinutes() {
		return defaultAutoSendExpireMinutes;
	}

	public void setDefaultAutoSendExpireMinutes(int autoSendExpireMinutes) {
		this.defaultAutoSendExpireMinutes = autoSendExpireMinutes;
	}

	public boolean isOutputLogWhenSendMail() {
		return outputLogWhenSendMail;
	}

	public void setOutputLogWhenSendMail(boolean outputLogWhenSendMail) {
		this.outputLogWhenSendMail = outputLogWhenSendMail;
	}
}
