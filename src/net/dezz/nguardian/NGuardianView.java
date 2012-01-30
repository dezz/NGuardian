package net.dezz.nguardian;

import java.util.ResourceBundle;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class NGuardianView {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("net.dezz.nguardian.messages"); //$NON-NLS-1$
	protected Shell shell;
	private TableViewer tableViewer;
	private NGuardianPresenter presenter;
	private Table table;

	/**
	 * Open the window.
	 */
	public NGuardianView(NGuardianPresenter presenter) {
		this.presenter = presenter;
		createContents();
		shell.open();
		shell.layout();
	}
	
	private void loadSizeAndLocation() {
		Configuration config = Application.getInstance().getConfig();
		Point size = (Point)config.getObjectProperty(NGuardianView.class, "size");
		Point location = (Point)config.getObjectProperty(NGuardianView.class, "location");
		if (size != null && location != null) {
			shell.setSize(size);
			shell.setLocation(location);
		}
	}

	/**
	 * Create contents of the window.
	 * @wbp.parser.entryPoint
	 */
	protected void createContents() {
		shell = new Shell();
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				presenter.onShellClosed(e);
			}
		});
		shell.setSize(710, 469);
		
		// restore size and location
		loadSizeAndLocation();
		shell.setText(BUNDLE.getString("NGuardianView.shell.text")); //$NON-NLS-1$
		shell.setLayout(new FillLayout(SWT.HORIZONTAL));

		Composite container = new Composite(shell, SWT.NONE);
		container.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		tableViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent arg0) {
				IStructuredSelection sel = (IStructuredSelection)arg0.getSelection();
				Object[]  selectedObjs = sel.toArray();
				if (selectedObjs.length > 0) {
					presenter.onMailTableSectionChanged(selectedObjs[0]);
				} else {
					presenter.onMailTableSectionChanged(null);
				}
			}
		});
		table = tableViewer.getTable();
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				presenter.onTableKeyPressed(e);
			}
		});
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				presenter.onTableDoubleClicked(e);
			}
		});
		table.setHeaderVisible(true);
		
		
		TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNo = tableViewerColumn.getColumn();
		tblclmnNo.setWidth(131);
		tblclmnNo.setText(BUNDLE.getString("NGuardianView.tblclmnNo.text")); //$NON-NLS-1$
		
		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnSubject = tableViewerColumn_1.getColumn();
		tblclmnSubject.setWidth(224);
		tblclmnSubject.setText(BUNDLE.getString("NGuardianView.tblclmnSubject.text")); //$NON-NLS-1$
		
		TableViewerColumn tableViewerColumn_2 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnBody = tableViewerColumn_2.getColumn();
		tblclmnBody.setWidth(280);
		tblclmnBody.setText(BUNDLE.getString("NGuardianView.tblclmnBody.text")); //$NON-NLS-1$
		
		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);
		
		MenuItem mntmFile = new MenuItem(menu, SWT.CASCADE);
		mntmFile.setText(BUNDLE.getString("NGuardianView.mntmFile.text")); //$NON-NLS-1$
		
		Menu menu_1 = new Menu(mntmFile);
		mntmFile.setMenu(menu_1);
		
		MenuItem mntmQuit = new MenuItem(menu_1, SWT.NONE);
		mntmQuit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().close();
			}
		});
		mntmQuit.setText(BUNDLE.getString("NGuardianView.mntmQuit.text")); //$NON-NLS-1$
		mntmQuit.setAccelerator(Application.CTRL_EQUIVALENT_KEY | 'X');
		
		MenuItem mntmTool = new MenuItem(menu, SWT.CASCADE);
		mntmTool.setText(BUNDLE.getString("NGuardianView.mntmNewSubmenu.text")); //$NON-NLS-1$
		
		Menu menu_2 = new Menu(mntmTool);
		mntmTool.setMenu(menu_2);
		
		MenuItem mntmOptions = new MenuItem(menu_2, SWT.NONE);
		mntmOptions.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				presenter.onToolOptionSelected(e);
			}
		});
		mntmOptions.setText(BUNDLE.getString("NGuardianView.mntmoptions.text")); //$NON-NLS-1$
		
		MenuItem mntmWindow = new MenuItem(menu, SWT.CASCADE);
		mntmWindow.setText(BUNDLE.getString("NGuardianView.mntmWindow.text")); //$NON-NLS-1$
		
		Menu menu_4 = new Menu(mntmWindow);
		mntmWindow.setMenu(menu_4);
		
		MenuItem mntmClose = new MenuItem(menu_4, SWT.NONE);
		mntmClose.setText(BUNDLE.getString("NGuardianView.mntmClose.text")); //$NON-NLS-1$
		mntmClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				presenter.onWindowCloseSelected(e);
			}
		});
		mntmClose.setAccelerator(Application.CTRL_EQUIVALENT_KEY | 'W');
		
		MenuItem mntmHelp = new MenuItem(menu, SWT.CASCADE);
		mntmHelp.setText(BUNDLE.getString("NGuardianView.mntmHelp.text")); //$NON-NLS-1$
		
		Menu menu_3 = new Menu(mntmHelp);
		mntmHelp.setMenu(menu_3);
		
		MenuItem mntmAbout = new MenuItem(menu_3, SWT.NONE);
		mntmAbout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				presenter.onAbout();
			}
		});
		mntmAbout.setText(BUNDLE.getString("NGuardianView.mntmAbout.text")); //$NON-NLS-1$
		
		tableViewer.setContentProvider(new NGuardianPresenter.MessageTableContentProvider());
		tableViewer.setLabelProvider(new NGuardianPresenter.MessageTableLabelProvider());
	}

	public TableViewer getTableViewer() {
		return tableViewer;
	}

	public Shell getShell() {
		return shell;
	}
	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		return bindingContext;
	}
}
