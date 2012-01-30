package net.dezz.nguardian;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import net.dezz.nguardian.verify.ControlVerifier;
import net.dezz.nguardian.verify.HostNameVerifier;
import net.dezz.nguardian.verify.SafeDomainsVerifier;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;

public class ConfigDialogView extends Dialog {
	public static final String RESULT_OK = "OK";
	public static final String RESULT_OK_RESTART = "OK_RESTART";
	public static final String RESULT_CANCEL = "CANCEL";
	
	private DataBindingContext m_bindingContext;
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("net.dezz.nguardian.messages"); //$NON-NLS-1$

	protected Object result = RESULT_CANCEL;
	protected Shell shell;
	private Text textSmtpHost;
	private Text textSafeDomains;
	private ConfigDialogPresenter presenter;
	private Spinner spinnerSmtpPort;
	private Spinner spinnerListenPort;
	private Map<String, ControlVerifier<?>> verifiers;
	private Button btnSendMailLog;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ConfigDialogView(Shell parent, int style) {
		super(parent, style);
		setText(BUNDLE.getString("ConfigDialogView.this.text")); //$NON-NLS-1$
	}
	
	public ConfigDialogView(ConfigDialogPresenter presenter, Shell parent, int style) {
		super(parent, style);
		this.presenter = presenter;
		setText(BUNDLE.getString("ConfigDialogView.this.text")); //$NON-NLS-1$
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		
		// Initialize verifier
		verifiers = new HashMap<String, ControlVerifier<?>>();
		verifiers.put("smtpHost", new ControlVerifier<String>(getTextSmtpHost(), new HostNameVerifier()));
		verifiers.put("safeDomains", new ControlVerifier<String>(getTextSafeDomains(), new SafeDomainsVerifier()));
		
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	public void createContents() {
		shell = new Shell(getParent(), SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE);
		shell.setSize(433, 400);
		
		// load size & location
		final Configuration config = Application.getInstance().getConfig();
		Point size = (Point)config.getObjectProperty(ConfigDialogView.class, "size");
		Point location = (Point)config.getObjectProperty(ConfigDialogView.class, "location");
		if (size != null && location != null) {
			shell.setSize(size);
			shell.setLocation(location);
		}
		
		// save size & location when shell closed
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				config.setObjectProperty(ConfigDialogView.class, "size", shell.getSize());
				config.setObjectProperty(ConfigDialogView.class, "location", shell.getLocation());
				
				presenter.onCloseDialog();
			}
		});
		
		shell.setText(BUNDLE.getString("ConfigDialogView.shell.text")); //$NON-NLS-1$
		shell.setLayout(new GridLayout(1, false));
		
		Group grpSmtp = new Group(shell, SWT.NONE);
		grpSmtp.setLayout(new GridLayout(2, false));
		grpSmtp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpSmtp.setText(BUNDLE.getString("ConfigDialogView.grpSmtp.text")); //$NON-NLS-1$
		
		Label labelSmtpHost = new Label(grpSmtp, SWT.NONE);
		labelSmtpHost.setAlignment(SWT.RIGHT);
		labelSmtpHost.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		labelSmtpHost.setBounds(0, 0, 59, 14);
		labelSmtpHost.setText(BUNDLE.getString("ConfigDialogView.labelSmtpHost.text")); //$NON-NLS-1$
		
		textSmtpHost = new Text(grpSmtp, SWT.BORDER);
		textSmtpHost.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lblSmtpPort = new Label(grpSmtp, SWT.NONE);
		lblSmtpPort.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblSmtpPort.setAlignment(SWT.RIGHT);
		lblSmtpPort.setBounds(0, 0, 59, 14);
		lblSmtpPort.setText(BUNDLE.getString("ConfigDialogView.lblSmtpPort.text")); //$NON-NLS-1$
		
		spinnerSmtpPort = new Spinner(grpSmtp, SWT.BORDER);
		spinnerSmtpPort.setMaximum(65535);
		spinnerSmtpPort.setMinimum(1);
		spinnerSmtpPort.setSelection(25);
		
		Group groupDomains = new Group(shell, SWT.NONE);
		groupDomains.setLayout(new GridLayout(2, false));
		groupDomains.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		groupDomains.setText(BUNDLE.getString("ConfigDialogView.groupDomains.text")); //$NON-NLS-1$
		
		Label lblIncomingPort = new Label(groupDomains, SWT.NONE);
		lblIncomingPort.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblIncomingPort.setText(BUNDLE.getString("ConfigDialogView.lblIncomingPort.text")); //$NON-NLS-1$
		
		spinnerListenPort = new Spinner(groupDomains, SWT.BORDER);
		GridData gd_spinnerListenPort = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_spinnerListenPort.widthHint = 80;
		spinnerListenPort.setLayoutData(gd_spinnerListenPort);
		spinnerListenPort.setMaximum(65535);
		spinnerListenPort.setMinimum(1);
		spinnerListenPort.setSelection(12525);
		
		Label lblSafeDomains = new Label(groupDomains, SWT.NONE);
		lblSafeDomains.setAlignment(SWT.RIGHT);
		lblSafeDomains.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, true, 1, 1));
		lblSafeDomains.setText(BUNDLE.getString("ConfigDialogView.lblSafeDomains.text")); //$NON-NLS-1$
		
		textSafeDomains = new Text(groupDomains, SWT.BORDER | SWT.MULTI);
		textSafeDomains.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				// Ctrl-A support
				if ((e.stateMask & Application.CTRL_EQUIVALENT_KEY) != 0) {
					if (e.keyCode == 'a') {
						textSafeDomains.selectAll();
					}
				}
			}
		});
		textSafeDomains.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				presenter.onVerifySafeDomains(arg0);
			}
		});
		textSafeDomains.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Group groupOthers = new Group(shell, SWT.NONE);
		groupOthers.setLayout(new GridLayout(1, false));
		groupOthers.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		groupOthers.setText(BUNDLE.getString("ConfigDialogView.group.text")); //$NON-NLS-1$
		
		btnSendMailLog = new Button(groupOthers, SWT.CHECK);
		btnSendMailLog.setText(BUNDLE.getString("ConfigDialogView.btnLog.text"));
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new RowLayout(SWT.HORIZONTAL));
		composite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		
		Button btnOK = new Button(composite, SWT.NONE);
		btnOK.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				presenter.onOKButtonClicked(e);
			}
		});
		btnOK.setLayoutData(new RowData(96, SWT.DEFAULT));
		btnOK.setText(BUNDLE.getString("ConfigDialogView.btnOK.text")); //$NON-NLS-1$
		m_bindingContext = initDataBindings();

	}
	public Text getTextSmtpHost() {
		return textSmtpHost;
	}
	public Text getTextSafeDomains() {
		return textSafeDomains;
	}

	public Map<String, ControlVerifier<?>> getVerifiers() {
		return verifiers;
	}
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue textSmtpHostObserveTextObserveWidget = SWTObservables.observeText(textSmtpHost, SWT.Modify);
		IObservableValue presenterSmtpHostObserveValue = PojoObservables.observeValue(presenter, "smtpHost");
		bindingContext.bindValue(textSmtpHostObserveTextObserveWidget, presenterSmtpHostObserveValue, null, null);
		//
		IObservableValue spinnerSmtpPortObserveSelectionObserveWidget = SWTObservables.observeSelection(spinnerSmtpPort);
		IObservableValue presenterSmtpPortObserveValue = PojoObservables.observeValue(presenter, "smtpPort");
		bindingContext.bindValue(spinnerSmtpPortObserveSelectionObserveWidget, presenterSmtpPortObserveValue, null, null);
		//
		IObservableValue textSafeDomainsObserveTextObserveWidget = SWTObservables.observeText(textSafeDomains, SWT.Modify);
		IObservableValue presenterSafeDomainsObserveValue = PojoObservables.observeValue(presenter, "safeDomains");
		bindingContext.bindValue(textSafeDomainsObserveTextObserveWidget, presenterSafeDomainsObserveValue, null, null);
		//
		IObservableValue spinnerListenPortObserveSelectionObserveWidget = SWTObservables.observeSelection(spinnerListenPort);
		IObservableValue presenterListenPortObserveValue = PojoObservables.observeValue(presenter, "listenPort");
		bindingContext.bindValue(spinnerListenPortObserveSelectionObserveWidget, presenterListenPortObserveValue, null, null);
		//
		IObservableValue btnSendMailLogObserveSelectionObserveWidget = SWTObservables.observeSelection(btnSendMailLog);
		IObservableValue presenterOutputLogWhenSendMailObserveValue = PojoObservables.observeValue(presenter, "outputLogWhenSendMail");
		bindingContext.bindValue(btnSendMailLogObserveSelectionObserveWidget, presenterOutputLogWhenSendMailObserveValue, null, null);
		//
		return bindingContext;
	}
}
