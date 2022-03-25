package com.paul-anderson.simplestack;

import java.util.EmptyStackException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class ThreadSnapshot extends Canvas {

	Font font;
	int x=0;

	Color waiting;
	Color running;
	
	public ThreadSnapshot(Composite parent, int style) {
		super(parent, style);
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				ThreadSnapshot.this.paintControl(e);
			}
		});
	}

	public synchronized void paintControl(PaintEvent pe) {

		GC gc = pe.gc;

		if (font == null) {
			font = new Font(gc.getDevice(), "Arial", 8, SWT.NORMAL);
		}

		if(waiting==null) {
//			waiting = new Color(SWT.COLOR_BLACK);
		}
		
		gc.setFont(font);

		long[][] threadsToDraw = null;

		try {
		//	threadsToDraw = SimpleStack.receiver.drawStack.pop();
		} catch (EmptyStackException ese) {
			return;
		}

		if (threadsToDraw != null) {

			int height = 0;

			for (int i = 0; i < threadsToDraw.length; i++) {

				StringBuilder sb1 = new StringBuilder();

				
				sb1.append(threadsToDraw[i][0]);
				
				sb1.append(": ");
				
				Device display = gc.getDevice();
				if((threadsToDraw[i][1]&Receiver.JVMTI_THREAD_STATE_RUNNABLE)==Receiver.JVMTI_THREAD_STATE_RUNNABLE) {
					gc.setForeground(display.getSystemColor(SWT.COLOR_GREEN));
				} else if((threadsToDraw[i][1]&Receiver.JVMTI_THREAD_STATE_SLEEPING)==Receiver.JVMTI_THREAD_STATE_SLEEPING) {
					gc.setForeground(display.getSystemColor(SWT.COLOR_YELLOW));
				} else if((threadsToDraw[i][1]&Receiver.JVMTI_THREAD_STATE_WAITING)==Receiver.JVMTI_THREAD_STATE_WAITING) {
					gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
				} else {
					gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
				}
				
				gc.drawString(sb1.toString(), 0, height, true);
				
				gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
				StringBuilder sb = new StringBuilder();

				long[] theStack = threadsToDraw[i];
				

				if (theStack != null) {
					
					int nextToLast = theStack.length;
					int j = 2;
					
					for (; j < nextToLast; j++) {
	//					SimpleMethod theMethod = SimpleStack.receiver.methods.get(new Long(theStack[j]));
						
		//				if (theMethod != null) {
		//					sb.append(theMethod.methodName);
		//					if(j<(nextToLast-1)) sb.append(" - ");
		//				} else {
		//					sb.append("null");
		//					sb.append(" - ");

		//				}
					}
				//	 sb.append(FirstUI.receiver.methods.get(new Long(theStack[nextToLast])).methodName);
				}
				gc.drawString(sb.toString(), 30, height, true);
				height+=14;
			}

			gc.drawString(Integer.toString(x++), 0, height, true);
			height+=14;
		}

	}

}
