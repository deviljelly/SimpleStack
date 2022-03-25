package com.paul-anderson.simplestack;



import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class SimpleStack {

	
	

	static String value;

	static Receiver receiver= null;

	
	protected static MethodList methodList = null;
	protected static RunningMethodList runningMethodList = null;
	protected static WaitingMethodList waitingMethodList = null;
	protected static ServerStatus serverStatus = null;
	protected static MethodDetail methodDetail = null;
	protected static MethodGraph  methodGraph = null;
	protected static StacksList stacksList = null;
	protected static StackDetail stackDetail = null;

	protected static Color BACKGROUND = new Color(null, 0, 0, 0);
	protected static Color FOREGROUND = new Color(null, 255, 203, 32);
	protected static Color HIGHLIGHT = new Color(null, 255, 255, 255);
//	protected static Font LABEL_FONT = new Font(null, "Arial", 12, SWT.BOLD | SWT.ITALIC);
	protected static Font LABEL_FONT = new Font(null, "Arial", 10, SWT.NORMAL);

	
	protected static int selectedMethod=0;
	protected static long[] selectedStack = null;
	protected static int PADDING = 6;
	protected static int HALF_PADDING = PADDING/2;

	public static void main(String[] args) {
		
		
		final Display display = Display.getCurrent();
		Shell shell = new Shell(display);
		Layout layout = new FillLayout();
		shell.setLayout(layout);
		shell.setBackground(BACKGROUND);		
		
		
		SashForm outerSashForm = new SashForm(shell, SWT.HORIZONTAL);
		outerSashForm.setBackground(SimpleStack.BACKGROUND);
		outerSashForm.setSashWidth(2);


		
		
		SashForm leftSashForm = new SashForm(outerSashForm, SWT.VERTICAL);
		SashForm rightSashForm = new SashForm(outerSashForm, SWT.VERTICAL);
		
		leftSashForm.setBackground(BACKGROUND);
		leftSashForm.setForeground(FOREGROUND);
		
		rightSashForm.setBackground(BACKGROUND);
		rightSashForm.setForeground(FOREGROUND);

		
		
		Composite c = new Composite(leftSashForm, SWT.NONE);
		c.setLayout(new FillLayout());
		
		Button b1=  new Button(c, SWT.NONE);
		Button b2 =  new Button(c, SWT.NONE);
		b1.setBackground(BACKGROUND);
		b1.setForeground(FOREGROUND);
		b1.setText("Slower");
		b2.setBackground(BACKGROUND);
		b2.setForeground(FOREGROUND);
		b2.setText("Faster");

		b1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				receiver.slower();
			}
		});
	
		b2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				receiver.faster();
			}
		});

		serverStatus = new ServerStatus(leftSashForm, SWT.NONE);
		
		
		
		methodList = new MethodList(leftSashForm, SWT.NONE);
		runningMethodList = new RunningMethodList(leftSashForm, SWT.NONE);
		waitingMethodList = new WaitingMethodList(leftSashForm, SWT.NONE);

		methodDetail = new MethodDetail(rightSashForm, SWT.NONE);
//		methodGraph = new MethodGraph(rightSashForm, SWT.NONE);
		stacksList = new StacksList(rightSashForm, SWT.NONE);
//		ScrolledComposite sc = new ScrolledComposite(rightSashForm,  SWT.H_SCROLL | SWT.V_SCROLL);
//		Composite c = new Composite(sc, SWT.NONE);
//		c.setLayout(new FillLayout());
//		c.setSize(500,500);
		stackDetail = new StackDetail(rightSashForm, SWT.NONE);
//		sc.setExpandHorizontal(true);
//		sc.setExpandVertical(true);
//		stackDetail.setSize(500,500);
//		sc.setContent(c);
//		sc.setMinHeight(100);
		


		
	//	outerSashForm.setWeights(new int[] { 2, 1 });
	//	sashForm.setWeights(new int[] { 1, 4 });
		
		leftSashForm.setSashWidth(2);
		

		leftSashForm.setWeights(new int[] {1, 2 ,6 ,6 ,6});
		rightSashForm.setWeights(new int[] {2 ,6 ,6});

		receiver = new Receiver();

		
		
		
		Thread receiverThread = new Thread(receiver);

		receiverThread.start();

//		shell.setBounds(10, 10, 1290, 1100);
//		final ThreadSnapshot canvas = new ThreadSnapshot(shell, SWT.NONE);
//		method.setBounds(10, 10, 1250,1090);
		
		
		display.timerExec(100, new Runnable() {
			public void run() {
				if (methodList.isDisposed())
					return;
					methodList.redraw();
					methodList.update();
				display.timerExec(250, this);
			}
		});
		display.timerExec(100, new Runnable() {
			public void run() {
				if (waitingMethodList.isDisposed())
					return;
					waitingMethodList.redraw();
					waitingMethodList.update();
				display.timerExec(250, this);
			}
		});
		display.timerExec(100, new Runnable() {
			public void run() {
				if (runningMethodList.isDisposed())
					return;
					runningMethodList.redraw();
					runningMethodList.update();
				display.timerExec(250, this);
			}
		});
		
		display.timerExec(500, new Runnable() {
			public void run() {
				if (serverStatus.isDisposed())
					return;
					serverStatus.redraw();
					serverStatus.update();
				display.timerExec(250, this);
			}
		});
		

		display.timerExec(100, new Runnable() {
			public void run() {
				if (stackDetail.isDisposed())
					return;
					stackDetail.redraw();
					stackDetail.update();
				display.timerExec(1000, this);
			}
		});


		display.timerExec(250, new Runnable() {
			public void run() {
				if (stacksList.isDisposed())
					return;
					stacksList.redraw();
					stacksList.update();
				display.timerExec(1000, this);
			}
		});

		display.timerExec(250, new Runnable() {
			public void run() {
				if (methodDetail.isDisposed())
					return;
					methodDetail.redraw();
					methodDetail.update();
				display.timerExec(1000, this);
			}
		});


	//	display.timerExec(10000, new Runnable() {
	//		public void run() {
				//	Receiver.store.updateAllGraphs();
	//			display.timerExec(10000, this);
	//		}
	//	});

		
	/*	display.timerExec(500, new Runnable() {
			public void run() {
				if (serverStatus.isDisposed())
					return;
					methodGraph.redraw();
					methodGraph.update();
				display.timerExec(500, this);
			}
		});
*/
		
		
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		receiver.connected = false;
	}

}
