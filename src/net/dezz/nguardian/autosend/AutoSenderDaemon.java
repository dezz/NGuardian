package net.dezz.nguardian.autosend;

import java.util.Date;
import java.util.Iterator;

import javax.mail.internet.MimeMessage;

import net.dezz.nguardian.MailSender;

import org.apache.log4j.Logger;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

public class AutoSenderDaemon extends Thread {
	
	private static final int AUTOSEND_CHECK_INTERVAL = 1000;
	private boolean stopFlag;
	private SimpleSmtpServer simpleSmtpServer;
	private static Logger logger = Logger.getLogger(AutoSenderDaemon.class);

	public AutoSenderDaemon(SimpleSmtpServer server) {
		setDaemon(true);
		this.simpleSmtpServer = server;
	}
	
	@Override
	public void run() {
		try {
			while (!stopFlag) {
				synchronized(simpleSmtpServer) {
					for (Iterator<SmtpMessage> it = simpleSmtpServer.getReceivedEmail(); it.hasNext(); ) {
						SmtpMessage msg = it.next();
						
						Date now = new Date();
						Date receivedDate = msg.getReservedSendDate();
						
						long diffAsMilliseconds = now.getTime() - receivedDate.getTime();
						if (diffAsMilliseconds >= 0) {
							// Now it's reserved time, send mail
							MailSender sender = MailSender.getInstance();
							MimeMessage mimeMsg = new MimeMessage(sender.getSession(), msg.toMessageStream());
							sender.send(mimeMsg);
						}
					}
				}
				
				sleep(AUTOSEND_CHECK_INTERVAL);
			}
			
		} catch (Exception e) {
			logger.error("An error occured in auto-sender thread.", e);
			
		}
	}

	public boolean getStopFlag() {
		return stopFlag;
	}

	public void setStopFlag(boolean stopFlag) {
		this.stopFlag = stopFlag;
	}
	
}
