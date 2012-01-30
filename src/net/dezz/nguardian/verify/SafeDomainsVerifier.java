package net.dezz.nguardian.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import net.dezz.nguardian.UTF8Control;

public class SafeDomainsVerifier implements IVerifyImplementation<String> {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("net.dezz.nguardian.messages"); //$NON-NLS-1$
	private static final String SAFE_DOMAIN_PATTERN = "^[a-zA-Z0-9\\._-]+$";
	
	public String[] verify(String input) {
		List<String> errors = new ArrayList<String>();
		
		if (input == null || input.length() == 0) {
//			errors.add(BUNDLE.getString("Common.null.not.allowed"));
			
		} else {
			String ls = System.getProperty("line.separator");
			String[] domains = input.split(ls);
			if (domains.length == 0) {
				// empty input?
				errors.add(BUNDLE.getString("Common.null.not.allowed"));
				
			} else {
				// Verify each line
				for (int i = 0; i < domains.length; i++) {
					String domain = domains[i];
					if (!domain.matches(SAFE_DOMAIN_PATTERN)) {
						// The domain name at line 'i' is invalid
						errors.add(String.format(BUNDLE.getString("SafeDomainsVerifier.invalid.input"), i + 1));
					}
				}
			}
			
		}
		
		return errors.toArray(new String[errors.size()]);
	}
}
