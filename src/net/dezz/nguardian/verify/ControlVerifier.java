package net.dezz.nguardian.verify;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

public class ControlVerifier<T> {
	private static final Color errorBackgroundColor = new Color(Display.getDefault(), 255, 180, 150);
	private Control control;
	private IVerifyImplementation<T> impl;
	private Color normalBackgroundColor;
	private boolean valid = false;
	
	public ControlVerifier(Control control, IVerifyImplementation<T> impl) {
		this.control = control;
		this.normalBackgroundColor = control.getBackground();
		this.impl = impl;
		
		// Auto-validation
		if (control instanceof Text) {
			final Text t = (Text)control;
			t.addModifyListener(new ModifyListener() {
				@Override
				@SuppressWarnings("unchecked")
				public void modifyText(ModifyEvent arg0) {
					verify((T)t.getText());
				}
			});
			
			t.addFocusListener(new FocusAdapter() {
				@Override
				@SuppressWarnings("unchecked")
				public void focusLost(FocusEvent e) {
					verify((T)t.getText());
				}
			});
		}
	}
	
	public void verify(T input) {
		String[] errors = this.impl.verify(input);
		if (errors.length > 0) {
			// There are errors to show
			valid = false;
			String msg = createErrorMessage(errors);
			control.setToolTipText(msg);
			control.setBackground(errorBackgroundColor);
			
		} else {
			// No error
			valid = true;
			control.setToolTipText("");
			control.setBackground(normalBackgroundColor);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void verify() {
		if (control instanceof Text) {
			verify((T)((Text)control).getText());
		}
	}
	
	private String createErrorMessage(String[] errors) {
		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");
		for (String e : errors) {
			if (sb.length() > 0) {
				sb.append(ls);
			}
			sb.append(e);
		}
		
		return sb.toString();
	}

	public boolean isValid() {
		return valid;
	}
}
