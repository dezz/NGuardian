package net.dezz.nguardian;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.services.IDisposable;

public class MailViewAttachmentSelectedAction extends SelectionAdapter implements IDisposable {
	private MailView view;
	private String filename;
	private BodyPart body;
	private List<File> temporaryFiles = new ArrayList<File>();
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("net.dezz.nguardian.messages", new UTF8Control());
	private static final Logger logger = Logger.getLogger(MailViewAttachmentSelectedAction.class);
	
	public MailViewAttachmentSelectedAction(MailView view, String filename, BodyPart body) {
		this.view = view;
		this.filename = filename;
		this.body = body;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		try {
			// create temporary file
			final File tempFile = File.createTempFile("ngd", getExt());
			this.temporaryFiles.add(tempFile);
			
			FileOutputStream fos = null;
			
			// write attachment to temporary file
			try {
				fos = new FileOutputStream(tempFile);
				
				DataHandler dh = body.getDataHandler();
				dh.writeTo(fos);
				
			} finally {
				if (fos != null) {
					CloseableUtility.safeClose(fos);
				}
			}
			
			// open attachment in default application
			Desktop.getDesktop().open(tempFile);
			
		} catch (IOException ex) {
			logger.error("Failed to open attachment.", ex);
			
			MessageBox mb = new MessageBox(this.view.shell, SWT.OK | SWT.ICON_ERROR);
			mb.setText(BUNDLE.getString("MailViewAttachmentSelectedAction.error.opening.title"));
			mb.setMessage(BUNDLE.getString("MailViewAttachmentSelectedAction.error.opening.message"));
			mb.open();
			
		} catch (MessagingException ex) {
			throw new RuntimeException("Error occured when opening attachment file.", ex);
			
		}
	}
	
	private String getExt() {
		int dotPos = this.filename.lastIndexOf(".");
		if (dotPos == -1) {
			return null;
		} else {
			return this.filename.substring(dotPos);
		}
	}
	
	public void dispose() {
		// delete temporary files
		for (File file : this.temporaryFiles) {
			file.delete();
		}
	}

}
