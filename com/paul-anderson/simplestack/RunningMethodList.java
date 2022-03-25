package com.paul-anderson.simplestack;

import java.util.EmptyStackException;
import java.util.Formatter;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class RunningMethodList extends Canvas {


	private static Font LABEL_FONT = SimpleStack.LABEL_FONT;
	private static int PADDING = SimpleStack.PADDING;
	private static int HALF_PADDING = SimpleStack.HALF_PADDING;

	private static int SPACING = 16;

	
	public RunningMethodList(Composite parent, int style) {
		super(parent, style);
		super.setBackground(SimpleStack.BACKGROUND);
		super.setForeground(SimpleStack.FOREGROUND);

		addMouseListener(new MouseListener() {
			
			public void mouseUp(MouseEvent arg0) {
			}

			public void mouseDown(MouseEvent me) {
				
				int methodPosition = (me.y-PADDING)/SPACING;
				if((methodPosition-2)>=0){
				int method = Receiver.store.runningIndex[methodPosition-2];
				SimpleStack.selectedMethod = method;
				SimpleStack.selectedStack = null;
				SimpleStack.methodDetail.redraw();
				//SimpleStack.methodGraph.redraw();
				SimpleStack.stacksList.redraw();
				SimpleStack.stackDetail.redraw();
				}
			}
			
			public void mouseDoubleClick(MouseEvent arg0) {
			}
		});
		
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				RunningMethodList.this.paintControl(e);
			}
		});
	}

	public synchronized void paintControl(PaintEvent pe) {

		GC gc = pe.gc;
		
		Point size = getSize();
		gc.setLineWidth(3);
		
		gc.setFont(LABEL_FONT);
		gc.setAntialias(SWT.ON);
		gc.drawRoundRectangle(HALF_PADDING, HALF_PADDING, size.x - PADDING, size.y - (PADDING), PADDING, PADDING);

//		gc.setFont(font);


			
			int height = PADDING;

			gc.drawString("Ruinning:", PADDING, height, true);
			height+=16;
			height+=16;

			
			int[] index = Receiver.store.runningIndex;
			int pointer = 0;
			int method = index[pointer];
			//int method = Store.METHOD_BASE;
			
			
			 //for (int i = 0; i < 30; i++) {
			Store store = Receiver.store;

			float onePercent = ((float)Receiver.store.totalRunningHits)/100.0f;

			while(method!=0) {

				int hits = (int)store.methods[method+Store.METHOD_RUNNING_HITS];
				float percentage = (hits/onePercent);
				
				if(percentage >= 0.01) {


					StringBuilder sb = new StringBuilder();
					Formatter formatter = new Formatter(sb, Locale.US);
					
					
					formatter.format("%3.2f%%   ", percentage);
					
//					sb.append(percentage);
//					sb.append("%   ");
					sb.append(store.strings[(int)store.methods[method+Store.METHOD_CLASSNAME_PTR]]);
					sb.append(".");
					sb.append(store.strings[(int)store.methods[method+Store.METHOD_NAME_PTR]]);

					
					if(method==SimpleStack.selectedMethod) {
						gc.setForeground(SimpleStack.HIGHLIGHT);
					}
//					gc.drawString(sb.toString(), PADDING, height, true);
					gc.drawString(formatter.toString(), PADDING, height, true);
					if(method==SimpleStack.selectedMethod) {
						gc.setForeground(SimpleStack.FOREGROUND);
					}
					height+=16;
					
				}
				pointer++;
				if(pointer>=index.length) break;
				method=index[pointer];
				
			}
			gc.setAntialias(SWT.OFF);
			gc.dispose();

			
		}
	


}
