package net.dezz.nguardian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.dumbster.smtp.SmtpMessageListListener;
import com.ibm.icu.text.SimpleDateFormat;

public class NGuardianPresenter implements SmtpMessageListListener {
	
	private MimeMessage selectedMessage = null;
	private List<MimeMessage> receivedEmails;
	private NGuardianView window;
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("net.dezz.nguardian.messages"); //$NON-NLS-1$
	private static Logger logger = Logger.getLogger(NGuardianPresenter.class);
	
	public static class MessageTableContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}

		@Override
		public Object[] getElements(Object arg0) {
			@SuppressWarnings("unchecked")
			List<MimeMessage> list = (List<MimeMessage>)arg0;
			return list.toArray();
		}
		
	}
	
	
	public static class MessageTableLabelProvider implements ITableLabelProvider {
		private static final int COLUMN_NUMBER = 0;
		private static final int COLUMN_SUBJECT = 1;
		private static final int COLUMN_BODY = 2;

		@Override
		public void addListener(ILabelProviderListener arg0) {
			
		}

		@Override
		public void dispose() {
			
		}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) {
			
		}

		@Override
		public Image getColumnImage(Object arg0, int arg1) {
			return null;
		}

		@Override
		public String getColumnText(Object arg0, int arg1) {
			try {
				MimeMessage msg = (MimeMessage)arg0;
				MimeMessageInfoExtractor mmie = new MimeMessageInfoExtractor(msg);
				
				switch (arg1) {
				case COLUMN_NUMBER:
					Date date = msg.getSentDate();
					if (date != null) {
						SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss");
						return sdf.format(date);
					} else {
						return "";
					}
				case COLUMN_SUBJECT:
					return mmie.getSubject();
				case COLUMN_BODY:
					return mmie.getMainBody();
				}
			} catch (IOException e) {
				return "error";
				
			} catch (MessagingException e) {
				return "error";
			}
			
			return "error";
		}

	}
	
	public Shell createApplicationWindow() {
		// create window
		this.window = new NGuardianView(this);
		
		// add Quit listener
		Display.getDefault().addListener(SWT.Close, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				// if trapped e-mails exist, Ask user to quit.
				if (receivedEmails != null && receivedEmails.size() > 0) {
					MessageBox mb = new MessageBox(window.getShell(), SWT.YES | SWT.NO);
					mb.setText(BUNDLE.getString("NGuradianPresenter.quit.confirm.text"));
					mb.setMessage(BUNDLE.getString("NGuradianPresenter.quit.confirm.message"));
					int result = mb.open();
					if (result != SWT.YES) {
						arg0.doit = false;
					}
				}
				
				// save
				saveSizeAndLocation();
			}
		});
		
		return this.window.getShell();
	}
	
	public void onAbout() {
		MessageBox mb = new MessageBox(this.window.getShell());
		mb.setText(BUNDLE.getString("NGuardianPresenter.about.title"));
		mb.setMessage(BUNDLE.getString("NGuardianPresenter.about.message"));
		mb.open();
	}
	
	public void onTableDoubleClicked(MouseEvent e) {
		openMailViewWithSelectedItem();
	}
	
	public void onTableKeyPressed(KeyEvent e) {
		if (e.keyCode == SWT.CR) {
			openMailViewWithSelectedItem();
		}
	}
	
	private void openMailViewWithSelectedItem() {
		if (selectedMessage != null) {
			try {
				openMailView(this.selectedMessage.getMessageID());
				
			} catch (MessagingException e) {
				throw new RuntimeException("Failed to get Message-ID from the selected mail.", e);
			}
		}
	}
	
	public void onMailTableSectionChanged(Object selected) {
		this.selectedMessage = (MimeMessage)selected;
	}
	
	private void openMailView(String msgId) {
		try {
			// Checks if the mail is already sent or purged
			SimpleSmtpServer smtpServer = Application.getInstance().getSmtpServer();
			SmtpMessage smtpMsg = smtpServer.getReceivedEmailById(msgId);
			if (smtpMsg == null) {
				// Already sent or purged
				return;
			}
			
			MimeMessage msg = smtpMessageToMimeMessage(smtpMsg);
			MailView mv = new MailView(this.window.getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			Object result = mv.open(msg);
			if (MailView.RESULT_CLOSE_TO_SEND.equals(result)) {
				// send mail
				MailSender sender = MailSender.getInstance();
				try {
					sendMail(sender, msg);
					// remove mail
					removeMailInUIThread(smtpServer, msg);
					
				} finally {
					CloseableUtility.safeClose(sender);
				}
				
			} else if (MailView.RESULT_CLOSE_TO_PURGE.equals(result)) {
				// remove mail
				removeMailInUIThread(smtpServer, msg);
				
			} else {
				// canceled
				
			}
			
		} catch (Exception e) {
			throw new RuntimeException("An error occured while open MailView or sending/purging a mail.", e);
		}
	}

	private void removeMailInUIThread(SimpleSmtpServer server, MimeMessage message) throws MessagingException {
		server.removeReceivedEmailById(message.getMessageID());
		this.window.getTableViewer().getTable().setSelection(-1);
	}
	
	private MimeMessage smtpMessageToMimeMessage(SmtpMessage smtpMsg) throws IOException, MessagingException {
		MailSender sender = MailSender.getInstance();
		return new MimeMessage(sender.getSession(), smtpMsg.toMessageStream());
	}
	
	private static class NotifyDialogMouseEventListener extends MouseAdapter {
		private NGuardianPresenter presenter;
		private String messageId;
		
		public NotifyDialogMouseEventListener(NGuardianPresenter presenter, String messageId) {
			this.presenter = presenter;
			this.messageId = messageId;
		}
		
		public void mouseDown(MouseEvent e) {
			presenter.openMailView(messageId);
		}
	}
	
	@Override
	public List<SmtpMessage> onAdded(final SimpleSmtpServer server, final List<SmtpMessage> messages) {
		// Send safe domain mails
		final List<SmtpMessage> unsafeMails = sendSafeDomainMails(messages);
		
		// show notify dialog
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				for (SmtpMessage smtpMessage : unsafeMails) {
					try {
						MimeMessage mimeMessage = smtpMessageToMimeMessage(smtpMessage);
						MimeMessageInfoExtractor mmie = new MimeMessageInfoExtractor(mimeMessage);
						NotifyDialog nd = new NotifyDialog(window.getShell(), SWT.DIALOG_TRIM);
						nd.setMouseEventListener(new NotifyDialogMouseEventListener(NGuardianPresenter.this, mimeMessage.getMessageID()));
						nd.open(mmie.getSubject(), mmie.getMainBody());
						
					} catch (IOException e) {
						logger.error("Failed to open Mail View.", e);
						
					} catch (MessagingException e) {
						logger.error("Failed to open Mail View.", e);
					}
				}
			}
		});
		
		return unsafeMails;
	}

	@Override
	public void onRemoved(SimpleSmtpServer server) {
	}

	@Override
	public void onChanged(final SimpleSmtpServer server) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					List<SmtpMessage> msgs = server.createCopyOfReceivedEmails();
					List<MimeMessage> mimeMsg = new ArrayList<MimeMessage>();
					for (SmtpMessage dumpsterMsg : msgs) {
						MimeMessage mmsg = smtpMessageToMimeMessage(dumpsterMsg);
						mimeMsg.add(mmsg);
					}
					receivedEmails = mimeMsg;
					window.getTableViewer().setInput(mimeMsg);
					
				} catch (Exception e) {
					throw new RuntimeException("Cannot show received e-mail table.", e);
				}
			}
		});
		
	}
	
	private List<SmtpMessage> sendSafeDomainMails(List<SmtpMessage> messages) {
		List<SmtpMessage> unsafeMails = new ArrayList<SmtpMessage>();
		MailSender sender = MailSender.getInstance();
		try {
			for (SmtpMessage message : messages) {
				try {
					boolean send = true;
					MimeMessage mimeMessage = smtpMessageToMimeMessage(message);
					Application app = Application.getInstance();
					Address[] recipients = mimeMessage.getAllRecipients();
					for (Address recipient : recipients) {
						if (!app.isSafeAddress((InternetAddress)recipient)) {
							send = false;
						}
					}
					
					if (send) {
						sendMail(sender, mimeMessage);
						
					} else {
						// this mail contains unsafe domain address
						unsafeMails.add(message);
					}
					
				} catch (IOException e) {
					logger.error("Failed to send mail.", e);
					
				} catch (MessagingException e) {
					logger.error("Failed to send mail.", e);
				}
			}
		} finally {
			CloseableUtility.safeClose(sender);
		}
		
		return unsafeMails;
		
	}
	
	public void onShellClosed(ShellEvent e) {
		saveSizeAndLocation();
		
		// prevent closing
		e.doit = false;
		
		// hide shell
		minimizeAndHide();
	}
	
	private void minimizeAndHide() {
//		this.window.getShell().setMinimized(true);
		this.window.getShell().setVisible(false);
	}
	
	private void restoreAndShow() {
		this.window.getShell().setVisible(true);
		this.window.getShell().setMinimized(false);
		this.window.getShell().forceActive();
	}

	private void saveSizeAndLocation() {
		// save size & location
		Configuration config = Application.getInstance().getConfig();
		Point size = this.window.getShell().getSize();
		Point location = this.window.getShell().getLocation();
		config.setObjectProperty(NGuardianView.class, "size", size);
		config.setObjectProperty(NGuardianView.class, "location", location);
	}
	
	private void sendMail(MailSender sender, MimeMessage message) throws NoSuchProviderException, MessagingException {
		sender.send(message);
	}
	
	public void onTaskItemSelected(SelectionEvent e) {
		// show shell
		restoreAndShow();
	}
	
	public void onWindowCloseSelected(SelectionEvent e) {
		minimizeAndHide();
	}
	
	public void onToolOptionSelected(SelectionEvent e) {
		ConfigDialogPresenter dialog = new ConfigDialogPresenter(this.window.shell);
		Object result = dialog.open();
		
		if (result == ConfigDialogView.RESULT_OK) {
			Application app = Application.getInstance();
			// Reload configuration
			app.applyConfig();
		}
	}
}
