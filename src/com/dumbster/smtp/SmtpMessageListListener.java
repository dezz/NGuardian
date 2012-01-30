package com.dumbster.smtp;

import java.util.List;

public interface SmtpMessageListListener {
	
	List<SmtpMessage> onAdded(SimpleSmtpServer server, List<SmtpMessage> messages);
	void onRemoved(SimpleSmtpServer server);
	void onChanged(SimpleSmtpServer server);
}
