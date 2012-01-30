package net.dezz.nguardian;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeUtility;

public class MimeMessageInfoExtractor {
	
	private MimeMessage message;

	/**
	 * 
	 * 
	 * @param message MIME message for extracting
	 */
	public MimeMessageInfoExtractor(MimeMessage message) {
		this.message = message;
	}
	
	public String[] getTo() throws IOException, MessagingException {
		Address[] addresses = this.message.getRecipients(RecipientType.TO);
		return decodeAddresses(addresses);
	}
	
	public String[] getCc() throws IOException, MessagingException {
		Address[] addresses = this.message.getRecipients(RecipientType.CC);
		return decodeAddresses(addresses);
	}
	
	public String[] getBcc() throws IOException, MessagingException {
		Address[] addresses = this.message.getRecipients(RecipientType.BCC);
		return decodeAddresses(addresses);
	}
	
	public String[] getFrom() throws IOException, MessagingException {
		Address[] addresses = this.message.getFrom();
		return decodeAddresses(addresses);
	}
	
	private String[] decodeAddresses(Address[] addresses) throws UnsupportedEncodingException {
		ArrayList<String> list = new ArrayList<String>();
		if (addresses != null) {
			for (Address adr : addresses) {
				String str = MimeUtility.decodeText(adr.toString());
				list.add(str);
			}
		}
		return list.toArray(new String[list.size()]);
	}
	
	/**
	 * Return main body text of a message.
	 * 
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws MessagingException
	 */
	public String getMainBody() throws IOException, MessagingException {
		Object o = message.getContent();
		if (o instanceof String) {
			// single part mail
			return (String)o;
			
		} else if (o instanceof Multipart) {
			// this mail may have attachments or html part
			
			Multipart mp = (Multipart)o;
			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart bp = mp.getBodyPart(i);
				if (Part.ATTACHMENT.equals(bp.getDisposition())) {
					// this part is attachment
					continue;
					
				} else {
					if (bp.isMimeType("text/*")) {
						return (String)bp.getContent();
						
					} else if (bp.isMimeType("multipart/alternative")) {
						// this message is formed by multipart/alternative
						// find text/plain part
						Multipart mpal = (Multipart)bp.getContent();
						for (int j = 0; j < mpal.getCount(); j++) {
							BodyPart mpalbp = mpal.getBodyPart(i);
							if (mpalbp.isMimeType("text/plain")) {
								return (String)mpalbp.getContent();
							}
						}
						
						// omg, message doesn't contain text/plain part
						throw new MessagingException("Message doesn't contain text/plain part in spite of formed by multipart/alternative.");
					} else {
						// This mail doesn't contain text message part
						return "";
					}
				}
			}
		}
		
		// This mail doesn't contain text message part
		return "";
	}
	
	/**
	 * Return subject of a message.
	 * 
	 * @return
	 * @throws IOException
	 * @throws MessagingException
	 */
	public String getSubject() throws IOException, MessagingException {
		String subject = this.message.getSubject();
		return (subject != null) ? subject : "";
	}
	
	/**
	 * Return all attachments of a message.
	 * 
	 * @return All attachments as BodyPart array
	 * @throws IOException
	 * @throws MessagingException
	 */
	public BodyPart[] getAttachments() throws IOException, MessagingException {
		Object content = this.message.getContent();
		ArrayList<BodyPart> list = new ArrayList<BodyPart>();
		if (content instanceof Multipart) {
			Multipart mp = (Multipart)content;
			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart bp = mp.getBodyPart(i);
				if (Part.ATTACHMENT.equals(bp.getDisposition())) {
					list.add(bp);
				}
			}
			
			return list.toArray(new BodyPart[list.size()]);
			
		} else {
			// this mail has no attachment
			return new BodyPart[] {};
		}
	}
}
