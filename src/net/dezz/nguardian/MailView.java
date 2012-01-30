package net.dezz.nguardian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class MailView extends Dialog {
	public static final String RESULT_CLOSE_TO_SEND = "CLOSE_TO_SEND";
	public static final String RESULT_CLOSE_TO_PURGE = "CLOSE_TO_PURGE";
	public static final String RESULT_CLOSE_TO_CANCEL = "CLOSE_TO_CANCEL";
	
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("net.dezz.nguardian.messages"); //$NON-NLS-1$
	protected Object result;
	protected Shell shell;
	private Text txtTo;
	private Text txtFrom;
	private Text txtSubject;
	private Text txtBody;
	private Text txtCc;
	private Text txtBcc;
	private List<MailViewAttachmentSelectedAction> attachmentActions = new ArrayList<MailViewAttachmentSelectedAction>();

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public MailView(Shell parent, int style) {
		super(parent, style);
		setText(BUNDLE.getString("MailView.shell.text"));
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open(MimeMessage message) throws IOException, MessagingException {
		createContents(message);
		shell.open();
		shell.setImage(Application.getInstance().getSmallAppIcon());
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
	private void createContents(MimeMessage message) throws IOException, MessagingException {
		MimeMessageInfoExtractor mmie = new MimeMessageInfoExtractor(message);
		
		final Configuration config = Application.getInstance().getConfig();
		shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				Point size = shell.getSize();
				Point pos = shell.getLocation();
				
				config.setObjectProperty(MailView.class, "size", size);
				config.setObjectProperty(MailView.class, "location", pos);
				
				// delete temporary files
				for (MailViewAttachmentSelectedAction action : attachmentActions) {
					action.dispose();
				}
			}
		});
		
		shell.setSize(594, 489);
		
		// load size & location
		Point size = (Point)config.getObjectProperty(MailView.class, "size");
		Point location = (Point)config.getObjectProperty(MailView.class, "location");
		if (size != null && location != null) {
			shell.setSize(size);
			shell.setLocation(location);
		}
		
		shell.setText(BUNDLE.getString("MailView.shell.text")); //$NON-NLS-1$
		shell.setLayout(new GridLayout(1, false));
		
		Label label = new Label(shell, SWT.NONE);
		label.setText(BUNDLE.getString("MailView.lblDesc.text")); //$NON-NLS-1$
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		composite.setLayout(new GridLayout(2, false));
		
		// TO
		Label lblTo = new Label(composite, SWT.NONE);
		lblTo.setText(BUNDLE.getString("MailView.lblTo.text"));
		lblTo.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		
		txtTo = new Text(composite, SWT.BORDER | SWT.MULTI);
		txtTo.setEditable(false);
		txtTo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtTo.setText(arrayToMultiline(mmie.getTo()));
		
		// CC
		Label lblNewLabel = new Label(composite, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText(BUNDLE.getString("MailView.lblCc.text"));
		
		txtCc = new Text(composite, SWT.BORDER | SWT.MULTI);
		txtCc.setEditable(false);
		txtCc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtCc.setText(arrayToMultiline(mmie.getCc()));
		
		// BCC
		Label lblNewLabel_1 = new Label(composite, SWT.NONE);
		lblNewLabel_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_1.setText(BUNDLE.getString("MailView.lblBcc.text"));
		
		txtBcc = new Text(composite, SWT.BORDER | SWT.MULTI);
		txtBcc.setEditable(false);
		txtBcc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtBcc.setText(arrayToMultiline(mmie.getBcc()));
		
		// FROM
		Label label_1 = new Label(composite, SWT.NONE);
		label_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		label_1.setText(BUNDLE.getString("MailView.lblFrom.text"));
		
		txtFrom = new Text(composite, SWT.BORDER | SWT.MULTI);
		txtFrom.setEditable(false);
		txtFrom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtFrom.setText(arrayToMultiline(mmie.getFrom()));
		
		// Subject
		Label label_3 = new Label(composite, SWT.NONE);
		label_3.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		label_3.setText(BUNDLE.getString("MailView.lblSubject.text"));
		
		txtSubject = new Text(composite, SWT.BORDER | SWT.MULTI);
		txtSubject.setEditable(false);
		txtSubject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtSubject.setText(mmie.getSubject());
		
		// Attachments
		Label label_4 = new Label(composite, SWT.NONE);
		label_4.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		label_4.setText(BUNDLE.getString("MailView.lblAttachments.text"));
		
		Composite composite_2 = new Composite(composite, SWT.NONE);
		composite_2.setLayout(new RowLayout(SWT.HORIZONTAL));
		composite_2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		
		for (BodyPart bp : mmie.getAttachments()) {
			Link link = new Link(composite_2, SWT.NONE);
			String filename = MimeUtility.decodeText(bp.getFileName());
			link.setText(String.format("<a>%s</a>", filename));
			MailViewAttachmentSelectedAction action = new MailViewAttachmentSelectedAction(this, filename, bp);
			link.addSelectionListener(action);
			this.attachmentActions.add(action);
		}
		
		// Body
		Label label_5 = new Label(composite, SWT.NONE);
		label_5.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
		label_5.setText(BUNDLE.getString("MailView.lblBody.text"));
		
		txtBody = new Text(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		txtBody.setEditable(false);
		txtBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		txtBody.setText(mmie.getMainBody());
		
		Composite composite_1 = new Composite(shell, SWT.NONE);
		composite_1.setLayout(new RowLayout(SWT.HORIZONTAL));
		composite_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		
		Button btnSend = new Button(composite_1, SWT.NONE);
		btnSend.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// confirm to send
				MessageBox mb = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
				mb.setText(BUNDLE.getString("MailView.confirm_to_send.title"));
				mb.setMessage(BUNDLE.getString("MailView.confirm_to_send.message"));
				
				if (mb.open() == SWT.OK) {
					// close to send
					result = MailView.RESULT_CLOSE_TO_SEND;
					shell.close();
				}
			}
		});
		btnSend.setText(BUNDLE.getString("MailView.btnSend.text"));
		
		Button btnPurge = new Button(composite_1, SWT.NONE);
		btnPurge.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// confirm to send
				MessageBox mb = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
				mb.setText(BUNDLE.getString("MailView.confirm_to_purge.title"));
				mb.setMessage(BUNDLE.getString("MailView.confirm_to_purge.message"));
				
				if (mb.open() == SWT.OK) {
					// close to purge
					result = MailView.RESULT_CLOSE_TO_PURGE;
					shell.close();
				}
			}
		});
		btnPurge.setText(BUNDLE.getString("MailView.btnPurge.text"));
		shell.setTabList(new Control[]{composite_1, composite});
		
		// set "Send" button as default
		shell.setDefaultButton(btnSend);
		
		
		Menu mb = new Menu(shell, SWT.BAR);
		shell.setMenuBar(mb);
		
		MenuItem mntmWindow = new MenuItem(mb, SWT.CASCADE);
		mntmWindow.setText(BUNDLE.getString("MailView.mntmWindow.text")); //$NON-NLS-1$
		
		Menu menu = new Menu(mntmWindow);
		mntmWindow.setMenu(menu);
		
		MenuItem mntmClose = new MenuItem(menu, SWT.NONE);
		mntmClose.setText(BUNDLE.getString("MailView.mntmClose.text")); //$NON-NLS-1$
		int CTRL_EQUIVALENT_KEY = (SWT.getPlatform().equals("cocoa")) ? SWT.COMMAND : SWT.CONTROL;
		mntmClose.setAccelerator(CTRL_EQUIVALENT_KEY | 'W');
		mntmClose.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent arg0) {
				MailView.this.result = RESULT_CLOSE_TO_CANCEL;
				shell.close();
			}
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
	}
	
	/**
	 * Convert string array to multiline text
	 * 
	 * @param array
	 * @return
	 */
	private String arrayToMultiline(String[] array) {
		String returnCode = System.getProperty("line.separator");
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			if (i != 0) {
				str.append("," + returnCode);
			}
			str.append(array[i]);
		}
		
		return str.toString();
	}
}
