package net.dezz.nguardian.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import net.dezz.nguardian.UTF8Control;

public class HostNameVerifier implements IVerifyImplementation<String> {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("net.dezz.nguardian.messages"); //$NON-NLS-1$
	private static final String HOSTNAME_PATTERN = "^[a-zA-Z0-9\\._-]+$";

	@Override
	public String[] verify(String input) {
		List<String> errors = new ArrayList<String>();
		
		if (input == null || input.length() == 0) {
			errors.add(BUNDLE.getString("Common.null.not.allowed"));
			
		} else {
			if (!input.matches(HOSTNAME_PATTERN)) {
				errors.add(BUNDLE.getString("HostNameVerifier.invalid.input"));
			}
		}
		
		return errors.toArray(new String[errors.size()]);
	}

}
