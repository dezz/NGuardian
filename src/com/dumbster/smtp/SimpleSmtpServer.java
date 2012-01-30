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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Dummy SMTP server for testing purposes.
 * 
 * @todo constructor allowing user to pass preinitialized ServerSocket
 */
public class SimpleSmtpServer implements Runnable {
	public static final String MESSAGE_ID_HEADER_NAME = "Message-ID";
	public static final String MESSAGE_ID_GEN_SEED = "NGuardian+Dumpster+Collabo";
	
	/**
	 * Stores all of the email received since this instance started up.
	 */
	private ArrayList<SmtpMessage> receivedMail;

	/**
	 * Listener for when the e-mails list are changed.
	 */
	private SmtpMessageListListener mailListListener;

	/**
	 * Default SMTP port is 25.
	 */
	public static final int DEFAULT_SMTP_PORT = 25;

	/**
	 * Indicates whether this server is stopped or not.
	 */
	private volatile boolean stopped = true;

	/**
	 * Handle to the server socket this server listens to.
	 */
	private ServerSocket serverSocket;

	/**
	 * Port the server listens on - set to the default SMTP port initially.
	 */
	private int port = DEFAULT_SMTP_PORT;

	/**
	 * Timeout listening on server socket.
	 */
	private static final int TIMEOUT = 500;

	/**
	 * Constructor.
	 * 
	 * @param port
	 *            port number
	 */
	public SimpleSmtpServer(int port) {
		receivedMail = new ArrayList<SmtpMessage>();
		this.port = port;
	}

	/**
	 * Main loop of the SMTP server.
	 */
	public void run() {
		stopped = false;
		try {
			try {
				serverSocket = new ServerSocket(port);
				serverSocket.setSoTimeout(TIMEOUT); // Block for maximum of 1.5
													// seconds
			} finally {
				synchronized (this) {
					// Notify when server socket has been created
					notifyAll();
				}
			}

			// Server: loop until stopped
			while (!isStopped()) {
				// Start server socket and listen for client connections
				Socket socket = null;
				try {
					socket = serverSocket.accept();
				} catch (Exception e) {
					if (socket != null) {
						socket.close();
					}
					continue; // Non-blocking socket timeout occurred: try
								// accept() again
				}

				// Get the input and output streams
//				BufferedReader input = new BufferedReader(new InputStreamReader(
//						socket.getInputStream()));
				BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
				PrintWriter out = new PrintWriter(socket.getOutputStream());

				synchronized (this) {
					/*
					 * We synchronize over the handle method and the list update
					 * because the client call completes inside the handle
					 * method and we have to prevent the client from reading the
					 * list until we've updated it. For higher concurrency, we
					 * could just change handle to return void and update the
					 * list inside the method to limit the duration that we hold
					 * the lock.
					 */
					List<SmtpMessage> msgs = handleTransaction(out, input);
					if (msgs.size() > 0) {
						if (mailListListener != null) {
							List<SmtpMessage> unsafeMessages = mailListListener.onAdded(this, msgs);
							receivedMail.addAll(unsafeMessages);
							mailListListener.onChanged(this);
						} else {
							receivedMail.addAll(msgs);
						}
					}
				}
				socket.close();
			}
		} catch (Exception e) {
			/** @todo Should throw an appropriate exception here. */
			e.printStackTrace();
		} finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Check if the server has been placed in a stopped state. Allows another
	 * thread to stop the server safely.
	 * 
	 * @return true if the server has been sent a stop signal, false otherwise
	 */
	public synchronized boolean isStopped() {
		return stopped;
	}

	/**
	 * Stops the server. Server is shutdown after processing of the current
	 * request is complete.
	 */
	public synchronized void stop() {
		// Mark us closed
		stopped = true;
		try {
			// Kick the server accept loop
			if (serverSocket != null) {
				serverSocket.close();
			}
			
		} catch (IOException e) {
			// Ignore
		}
	}

	/**
	 * Handle an SMTP transaction, i.e. all activity between initial connect and
	 * QUIT command.
	 * 
	 * @param out
	 *            output stream
	 * @param input
	 *            input stream
	 * @return List of SmtpMessage
	 * @throws IOException
	 */
//	private List<SmtpMessage> handleTransaction(PrintWriter out, BufferedReader input)
	private List<SmtpMessage> handleTransaction(PrintWriter out, BufferedInputStream input)
			throws IOException {
		// Initialize the state machine
		SmtpState smtpState = SmtpState.CONNECT;
		SmtpRequest smtpRequest = new SmtpRequest(SmtpActionType.CONNECT, "", smtpState);

		// Execute the connection request
		SmtpResponse smtpResponse = smtpRequest.execute();

		// Send initial response
		sendResponse(out, smtpResponse);
		smtpState = smtpResponse.getNextState();

		List<SmtpMessage> msgList = new ArrayList<SmtpMessage>();
		SmtpMessage msg = new SmtpMessage();

		while (smtpState != SmtpState.CONNECT) {
//			String line = input.readLine();
//			if (line == null) {
//				break;
//			}

			byte[] lineBytes = readLineAsByteArray(input);
			if (lineBytes == null) {
				break;
			}
			String line = new String(lineBytes);

			// Create request from client input and current state
			SmtpRequest request = SmtpRequest.createRequest(line, smtpState);
			// Execute request and create response object
			SmtpResponse response = request.execute();
			// Send response to client
			sendResponse(out, response);

			// Store input in message
			String params = request.getParams();
			msg.store(smtpState, params, lineBytes);

			// Move to next internal state
			smtpState = response.getNextState();
			
			// If message reception is complete save it
			if (smtpState == SmtpState.QUIT) {
				// if the message has no Message-ID header
				// generate and add Message-ID
				String msgId = msg.getHeaderValue(MESSAGE_ID_HEADER_NAME);
				if (msgId == null) {
					String generatedId = generateMessageId();
					msg.addHeader(MESSAGE_ID_HEADER_NAME, generatedId);
				}
				
				msgList.add(msg);
				msg = new SmtpMessage();
			}
		}

		return msgList;
	}
	
	private String generateMessageId() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest((MESSAGE_ID_GEN_SEED + sdf.format(date)).getBytes());
			String hexMd = byteArrayToHex(digest);
			return hexMd + "@" + "NGuardian";
			
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Failed to generate Message-ID", e);
		}
	}
	
	private String byteArrayToHex(byte[] array) {
		StringBuilder sb = new StringBuilder();
		for (byte b : array) {
			sb.append(Integer.toHexString((int)b & 0xFF));
		}
		return sb.toString();
	}
	private byte[] readLineAsByteArray(BufferedInputStream input) throws IOException {
		int n;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final int NORMAL = 0;
		final int CR = 0x0D;
		final int LF = 0x0A;
		int state = NORMAL;
		int read = 0;
		
		outer:
		while ((n = input.read()) != -1) {
			++read;
			switch (state) {
			case NORMAL:
				if (n == CR) {
					input.mark(10);
					state = CR;
					
				} else if (n == LF) {
					break outer;
					
				} else {
					baos.write((byte)n);
				}
				break;
			
			case CR:
				// now waiting LF for CRLF / waiting another char for just CR
				if (n == LF) {
					break outer;
				} else {
					// cancel reading
					input.reset();
					break outer;
				}
			
			default:
				break;
			}
		}
		
		return (read == 0) ? null : baos.toByteArray();
	}

	/**
	 * Send response to client.
	 * 
	 * @param out
	 *            socket output stream
	 * @param smtpResponse
	 *            response object
	 */
	private static void sendResponse(PrintWriter out, SmtpResponse smtpResponse) {
		if (smtpResponse.getCode() > 0) {
			int code = smtpResponse.getCode();
			String message = smtpResponse.getMessage();
			out.print(code + " " + message + "\r\n");
			out.flush();
		}
	}

	/**
	 * Get email received by this instance since start up.
	 * 
	 * @return List of String
	 */
	public synchronized Iterator<SmtpMessage> getReceivedEmail() {
		return receivedMail.iterator();
	}

	public synchronized SmtpMessage getReceivedEmailAt(int index) {
		return this.receivedMail.get(index);
	}

	public synchronized SmtpMessage getReceivedEmailById(String msgId) {
		int i = findMailByIdIgnoreHeaderNameCase(msgId);
		return (i != -1) ? receivedMail.get(i) : null;
	}
	
	private int findMailByIdIgnoreHeaderNameCase(String msgId) {
		for (int i = 0; i < receivedMail.size(); i++) {
			SmtpMessage msg = receivedMail.get(i);
			if (msgId.equals(msg.getHeaderValue(MESSAGE_ID_HEADER_NAME))) {
				return i;
			}
		}
		
		return -1;
	}

	public synchronized void removeReceivedEmailById(String msgId) {
		int i = findMailByIdIgnoreHeaderNameCase(msgId);
		if (i != -1) {
			receivedMail.remove(i);
		}
		
		if (mailListListener != null) {
			mailListListener.onRemoved(this);
			mailListListener.onChanged(this);
		}
	}

	/**
	 * Get the number of messages received.
	 * 
	 * @return size of received email list
	 */
	public synchronized int getReceivedEmailSize() {
		return receivedMail.size();
	}

	/**
	 * Creates an instance of SimpleSmtpServer and starts it. Will listen on the
	 * default port.
	 * 
	 * @return a reference to the SMTP server
	 */
	public static SimpleSmtpServer start() {
		return start(DEFAULT_SMTP_PORT);
	}

	/**
	 * Creates an instance of SimpleSmtpServer and starts it.
	 * 
	 * @param port
	 *            port number the server should listen to
	 * @return a reference to the SMTP server
	 */
	public static SimpleSmtpServer start(int port) {
		SimpleSmtpServer server = new SimpleSmtpServer(port);
		Thread t = new Thread(server);
		t.start();

		// Block until the server socket is created
		synchronized (server) {
			try {
				server.wait();
			} catch (InterruptedException e) {
				// Ignore don't care.
			}
		}
		return server;
	}
	
	public synchronized List<SmtpMessage> createCopyOfReceivedEmails() {
		List<SmtpMessage> copy = new ArrayList<SmtpMessage>();
		
		// create shallow copy
		for (SmtpMessage msg : receivedMail) {
			copy.add(msg);
		}
		return copy;
	}

	public SmtpMessageListListener getMailListListener() {
		return mailListListener;
	}

	public void setMailListListener(SmtpMessageListListener mailListListener) {
		this.mailListListener = mailListListener;
	}
	
	
	public void copyUnsentMailsFrom(SimpleSmtpServer server) {
		this.receivedMail.addAll(server.receivedMail);
	}

}
