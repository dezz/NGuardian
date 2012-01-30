/*
 * Dumbster - a dummy SMTP server
 * Copyright 2004 Jason Paul Kitchen
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dumbster.smtp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Container for a complete SMTP message - headers and message body.
 */
public class SmtpMessage {
	/** Headers: Map of List of String hashed on header name. */
	private Map<String, List<String> > headers;
	/** Message body. */
//	private StringBuffer body;
	private ByteArrayOutputStream body;
	private Date reservedSendDate;
	private List<String> recipients;

	/**
	 * Constructor. Initializes headers Map and body buffer.
	 */
	public SmtpMessage() {
		headers = new HashMap<String, List<String> >(10);
//		body = new StringBuffer();
		body = new ByteArrayOutputStream();
		recipients = new ArrayList<String>();
	}
	
	private String lastHeaderName = null;

	/**
	 * Update the headers or body depending on the SmtpResponse object and line
	 * of input.
	 * 
	 * @param response
	 *            SmtpResponse object
	 * @param params
	 *            remainder of input line after SMTP command has been removed
	 */
	public void store(SmtpState state, String params, byte[] rawBytes) {
		if (params != null) {
			if (SmtpState.DATA_HDR.equals(state)) {
				int headerNameEnd = params.indexOf(':');
				if (headerNameEnd >= 0) {
					String name = params.substring(0, headerNameEnd).trim();
					String value = params.substring(headerNameEnd + 1).trim();
					addHeader(name, value);
					lastHeaderName = name;
					
				} else {
					// multiline value
					if (lastHeaderName != null && (params.startsWith(" ") || params.startsWith("\t"))) {
						List<String> values = (List<String>)this.headers.get(lastHeaderName);
						int lastIndex = values.size() - 1;
						String value = (String)values.get(lastIndex);
						String newValue = value + params;
						values.remove(lastIndex);
						values.add(newValue);
					}
					
				}
				
			} else if (SmtpState.DATA_BODY == state) {
				if (body.size() > 0) {
					body.write((byte)'\r');
					body.write((byte)'\n');
				}
				body.write(rawBytes, 0, rawBytes.length);
				
			} else if (SmtpState.RCPT == state) {
				recipients.add(params);
			}
		}
	}
	
	private List<String> extractBccFromRecipients() {
		try {
			String[] to = getHeaderValues("To");
			String[] cc = getHeaderValues("Cc");
			
			List<InternetAddress> toAddr = new ArrayList<InternetAddress>();
			List<InternetAddress> ccAddr = new ArrayList<InternetAddress>();
			
			for (String t : to) {
				InternetAddress[] adr = InternetAddress.parse(t);
				for (InternetAddress a : adr) {
					toAddr.add(a);
				}
			}
			for (String t : cc) {
				InternetAddress[] adr = InternetAddress.parse(t);
				for (InternetAddress a : adr) {
					ccAddr.add(a);
				}
			}
			
			// bcc = rcpt ^ (to | cc)
			List<InternetAddress> bccAdr = new ArrayList<InternetAddress>();
			for (String r : recipients) {
				InternetAddress adr = new InternetAddress(r);
				
				if (toAddr.contains(adr) || ccAddr.contains(adr)) {
					continue;
				} else {
					bccAdr.add(adr);
				}
			}
			
			List<String> bccStr = new ArrayList<String>();
			for (InternetAddress bcc : bccAdr) {
				bccStr.add(bcc.getAddress());
			}
			return bccStr;
			
		} catch (AddressException e) {
			throw new RuntimeException("Failed to parse mail addresses.", e);
		}
	}

	/**
	 * Get an Iterator over the header names.
	 * 
	 * @return an Iterator over the set of header names (String)
	 */
	public Iterator<String> getHeaderNames() {
		Set<String> nameSet = headers.keySet();
		return nameSet.iterator();
	}

	/**
	 * Get the value(s) associated with the given header name.
	 * 
	 * @param name
	 *            header name
	 * @return value(s) associated with the header name
	 */
//	public String[] getHeaderValues(String name) {
//		List<String> values = (List<String>) headers.get(name);
//		if (values == null) {
//			return new String[0];
//		} else {
//			return (String[]) values.toArray(new String[0]);
//		}
//	}

	/**
	 * Get the first values associated with a given header name.
	 * 
	 * @param name
	 *            header name
	 * @return first value associated with the header name
	 */
	public String getHeaderValue(String name) {
		List<String> values = (List<String>) headers.get(name);
		if (values == null) {
			return null;
		} else {
			Iterator<String> iterator = values.iterator();
			return (String) iterator.next();
		}
	}
	
	public String[] getHeaderValues(String name) {
		for (String key : headers.keySet()) {
			if (key.equalsIgnoreCase(name)) {
				List<String> values = headers.get(key);
				return values.toArray(new String[values.size()]);
			}
		}
		
		return new String[]{};
	}

	/**
	 * Get the message body.
	 * 
	 * @return message body
	 */
	public String getBody() {
		return body.toString();
	}

	/**
	 * Adds a header to the Map.
	 * 
	 * @param name
	 *            header name
	 * @param value
	 *            header value
	 */
	public void addHeader(String name, String value) {
		// uniform header case for Message-ID
		if (SimpleSmtpServer.MESSAGE_ID_HEADER_NAME.equalsIgnoreCase(name)) {
			name = SimpleSmtpServer.MESSAGE_ID_HEADER_NAME;
		}
		
		List<String> valueList = (List<String>)headers.get(name);
		if (valueList == null) {
			valueList = new ArrayList<String>(1);
			headers.put(name, valueList);
		}
		valueList.add(value);
	}

	/**
	 * String representation of the SmtpMessage.
	 * 
	 * @return a String
	 */
	@Deprecated
	public String toString() {
//		StringBuffer msg = new StringBuffer();
//		for (Iterator i = headers.keySet().iterator(); i.hasNext();) {
//			String name = (String) i.next();
//			List values = (List) headers.get(name);
//			for (Iterator j = values.iterator(); j.hasNext();) {
//				String value = (String) j.next();
//				msg.append(name);
//				msg.append(": ");
//				msg.append(value);
//				msg.append('\n');
//			}
//		}
		
		StringBuilder msg = getHeaderAsStringBuilder();
//		msg.append('\n');
//		msg.append(body);
//		msg.append('\n');
		
		msg.append("\r\n");
		msg.append("body is now raw bytes.");
		return msg.toString();
	}
	
	public StringBuilder getHeaderAsStringBuilder() {
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> i = headers.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			List<String> values = (List<String>) headers.get(name);
			for (Iterator<String> j = values.iterator(); j.hasNext();) {
				String value = (String) j.next();
				sb.append(name);
				sb.append(": ");
				sb.append(value);
				sb.append("\r\n");
			}
		}
		
		// bcc support
		List<String> bcc = extractBccFromRecipients();
		for (String b : bcc) {
			sb.append("Bcc: " + b + "\r\n");
		}
		
		return sb;
		
	}
	
	public InputStream toMessageStream() {
		StringBuilder sb = getHeaderAsStringBuilder();
		byte[] header = sb.toString().getBytes();
		byte[] crlf = new byte[] { 0x0D, 0x0A };
		byte[] allbytes = new byte[header.length + body.size() + 2];
		
		System.arraycopy(header, 0, allbytes, 0, header.length);
		System.arraycopy(crlf, 0, allbytes, header.length, crlf.length);
		System.arraycopy(body.toByteArray(), 0, allbytes, header.length + crlf.length, body.size());
		ByteArrayInputStream bais = new ByteArrayInputStream(allbytes);
		
		return bais;
	}

	public Date getReservedSendDate() {
		return reservedSendDate;
	}

	public void setReservedSendDate(Date reservedSendDate) {
		this.reservedSendDate = reservedSendDate;
	}
}
