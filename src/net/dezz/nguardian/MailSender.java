package net.dezz.nguardian;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.print.attribute.standard.MediaSize.Other;

import org.apache.log4j.Logger;

public class MailSender implements Closeable {
	private static MailSender instance;
	private Session session;
	private Transport transport;
	private static Logger logger = Logger.getLogger(MailSender.class);
	
	public MailSender() {
		Configuration config = Application.getInstance().getConfig();
		String smtpHost = config.getProperty(Application.class, "smtp.host");
		String smtpPort = config.getProperty(Application.class, "smtp.port");
		
		Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.host", smtpHost);
		props.put("mail.smtp.port", smtpPort);
		this.session = Session.getInstance(props);
	}
	
	public void connect() throws NoSuchProviderException, MessagingException {
		transport = session.getTransport("smtp");
		transport.connect();
	}
	
	public void send(MimeMessage message) throws NoSuchProviderException, MessagingException {
		Configuration config = Application.getInstance().getConfig();
		boolean outputLog = Boolean.valueOf(config.getProperty(Application.class, "outputLogWhenSendMail"));
		
		outputInfoLog(outputLog, "Start send a mail: " + message.getSubject());
		
		// If message has no recipients
		if (message.getAllRecipients().length == 0) {
			// ignore this message
			outputInfoLog(outputLog, "Sending mail has been canceled. The mail has no recipients");
			return;
		}
		
		if (transport == null) {
			connect();
		}
		transport.sendMessage(message, message.getAllRecipients());
		
		outputInfoLog(outputLog, "Sending mail has been completed.");
	}
	
	private void outputInfoLog(boolean flag, String message) {
		if (flag) {
			logger.info(message);
		}
	}
	
	public void close() throws IOException {
		try {
			if (transport != null) {
				transport.close();
				transport = null;
			}
		} catch (MessagingException e) {
			throw new IOException(e);
		}
	}
	
	public static synchronized MailSender getInstance() {
		if (instance == null) {
			instance = new MailSender();
		}
		return instance;
	}
	
	public static synchronized void recreateInstance() {
		instance = new MailSender();
	}

	public Session getSession() {
		return this.session;
	}
}
