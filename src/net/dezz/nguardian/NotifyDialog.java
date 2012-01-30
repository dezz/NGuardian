package net.dezz.nguardian;

import java.io.InputStream;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.events.MouseAdapter;


public class NotifyDialog extends Dialog {
	
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("net.dezz.nguardian.messages"); //$NON-NLS-1$
	protected Object result;
	protected Shell shell;
	
	private Image mailImage;
	private ShellFader fadeInTimer;
	private ShellFader fadeOutTimer;
	private static int instanceCount = 0;
	
	private MouseAdapter mouseEventListener;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public NotifyDialog(Shell parent, int style) {
		super(parent, style);
		setText(BUNDLE.getString("NotifyDialog.this.text")); //$NON-NLS-1$
		instanceCount++;
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open(String title, String body) {
		// create preview text
		String preview = createPreviewText(title, body);
		
		createContents(preview);
		shell.open();
		shell.setImage(Application.getInstance().getSmallAppIcon());
		shell.layout();
		// set foreground
		shell.forceActive();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}
	
	private String createPreviewText(String title, String body) {
		StringBuilder sb = new StringBuilder();
		String rt = System.getProperty("line.separator");
		sb.append(BUNDLE.getString("NotifyDialog.preview.label") + rt);
		sb.append(rt);
		sb.append(BUNDLE.getString("NotifyDialog.preview.label.title") + title + rt);
		sb.append("-----------------------------" + rt);
		sb.append(BUNDLE.getString("NotifyDialog.preview.label.body") + rt + body);
		
		return sb.toString();
	}
	
	/**
	 * Create contents of the dialog.
	 */
	private void createContents(String previewText) {
		shell = new Shell(getParent(), getStyle());
		shell.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				instanceCount--;
				fadeInTimer.stop();
				fadeOutTimer.stop();
			}
		});
		shell.setSize(350, 150);
		shell.setText(getText());
		shell.setLayout(new GridLayout(2, false));
		
		// move to right-bottom
		Rectangle screenBounds = Display.getDefault().getClientArea();
		
		int widgetHeight = shell.getSize().y;
		int widgetWidth = shell.getSize().x;
		int y = (screenBounds.y + screenBounds.height) - (widgetHeight * (instanceCount));
		int x = (screenBounds.x + screenBounds.width) - widgetWidth;
		shell.setBounds(x, y, widgetWidth, widgetHeight);
		
		// prepare MouseTrackAdapter for fade-in/out
		MouseTrackAdapter mouseTrackForFade = new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				fadeInTimer.stop();
				fadeOutTimer.stop();
				shell.setAlpha(255);
			}
			
			@Override
			public void mouseHover(MouseEvent e) {
				mouseEnter(e);
			}
			
			@Override
			public void mouseExit(MouseEvent e) {
				fadeOutTimer.start();
			}
		};
		shell.addMouseTrackListener(mouseTrackForFade);
		
		Canvas canvas = new Canvas(shell, SWT.NONE);
		canvas.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent arg0) {
				ImageData imgd = mailImage.getImageData();
				int scaledW = arg0.width;
				int scaledH = (int)((float)imgd.height * (float)((float)scaledW / (float)imgd.width));
				arg0.gc.drawImage(mailImage, 0, 0, imgd.width, imgd.height, 0, 0, scaledW, scaledH);
			}
		});
		canvas.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		// Preview label
		Label lblNewLabel = new Label(shell, SWT.WRAP);
		lblNewLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		lblNewLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				// close
				shell.close();
				
				// Run listener when preview label clicked
				if (mouseEventListener != null) {
					mouseEventListener.mouseDown(e);
				}
			}
		});
		lblNewLabel.addMouseTrackListener(mouseTrackForFade);
		lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
		lblNewLabel.setSize(59, 14);
		lblNewLabel.setText(previewText);

		// prepare icons
		InputStream imgs = ClassLoader.getSystemClassLoader().getResourceAsStream("3_64x64.png");
		try {
			mailImage = new Image(shell.getDisplay(), imgs);
		} finally {
			try {
				imgs.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// fade-in
		fadeInTimer = new ShellFader(shell, 0, 0, 10, new Runnable() {
			@Override
			public void run() {
				// fade-out after 5 seconds
				fadeOutTimer.start();
			}
		});
		fadeOutTimer = new ShellFader(shell, 5000, 255, -10, new Runnable() {
			@Override
			public void run() {
				shell.close();
				shell.dispose();
			}
		});
		
		fadeInTimer.start();
		shell.setAlpha(0);
	}

	public MouseAdapter getMouseEventListener() {
		return mouseEventListener;
	}

	public void setMouseEventListener(MouseAdapter mouseEventListener) {
		this.mouseEventListener = mouseEventListener;
	}
}
