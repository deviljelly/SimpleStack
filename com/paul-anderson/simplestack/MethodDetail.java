package com.paul-anderson.simplestack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class MethodDetail extends Canvas {

	private static Font LABEL_FONT = SimpleStack.LABEL_FONT;
	private static int PADDING = SimpleStack.PADDING;
	private static int HALF_PADDING = SimpleStack.HALF_PADDING;
	
	private static int SPACING = 16;
	
	
	public MethodDetail(Composite parent, int style) {
		super(parent, style);
		super.setBackground(SimpleStack.BACKGROUND);
		super.setForeground(SimpleStack.FOREGROUND);

		
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				MethodDetail.this.paintControl(e);
			}
		});

	}

	public void paintControl(PaintEvent pe) {
		GC gc = pe.gc;
		Point size = getSize();
		gc.setLineWidth(3);

		
		gc.setFont(LABEL_FONT);
		gc.setAntialias(SWT.ON);


		int selectedMethod = SimpleStack.selectedMethod;
		if(selectedMethod!=0) {
			
			int y = PADDING;
			
			gc.drawString("Method: ", PADDING, y);
			y+=SPACING;


			char[] classNameArray =  Receiver.store.strings[(int)Receiver.store.methods[selectedMethod+Store.METHOD_CLASSNAME_PTR]];
			char[] methodNameArray =  Receiver.store.strings[(int)Receiver.store.methods[selectedMethod+Store.METHOD_NAME_PTR]];
			char[] methodDescriptorArray =  Receiver.store.strings[(int)Receiver.store.methods[selectedMethod+Store.METHOD_SIGNATURE_PTR]];
			
			gc.drawString("   " + new String(classNameArray) , PADDING, y);
			y+=SPACING;
			gc.drawString("   " + new String(methodNameArray) , PADDING, y);
			y+=SPACING;
			gc.drawString("   " + new String(methodDescriptorArray) , PADDING, y);
			y+=SPACING;
			y+=SPACING;

			
			float onePercent = ((float)Store.totalHits)/100.0f;
			float oneWaitingPercent = Store.totalWaitingHits/100.0f;
			float oneRunningPercent = Store.totalRunningHits/100.0f;
			
			long[] methods = Receiver.store.methods;
			
			int hits = (int)methods[selectedMethod+Store.METHOD_HITS];
			int runningHits = (int)methods[selectedMethod+Store.METHOD_RUNNING_HITS];
			int waitingHits = (int)methods[selectedMethod+Store.METHOD_WAITING_HITS];
			
			float percentage = (hits/onePercent);
			float runningPercentage = (runningHits/oneRunningPercent);
			float waitingPercentage = (waitingHits/oneWaitingPercent);

			
			StringBuilder sb = new StringBuilder();
			Formatter formatter = new Formatter(sb, Locale.US);
			formatter.format("%d (%3.2f%%)  ",(int)Receiver.store.methods[selectedMethod+Store.METHOD_HITS], percentage);
			gc.drawString("Total: " + sb.toString() , PADDING, y);
			y+=SPACING;

			sb = new StringBuilder();
			formatter = new Formatter(sb, Locale.US);
			formatter.format("%d (%3.2f%%)  ", (int)Receiver.store.methods[selectedMethod+Store.METHOD_WAITING_HITS], waitingPercentage);
			gc.drawString("Waiting: " + sb.toString() , PADDING, y);
			y+=SPACING;

			
			sb = new StringBuilder();
			formatter = new Formatter(sb, Locale.US);
			formatter.format("%d (%3.2f%%)  ",(int)Receiver.store.methods[selectedMethod+Store.METHOD_RUNNING_HITS], runningPercentage);
			gc.drawString("Running: " + sb.toString() , PADDING, y);
			y+=SPACING;

			
		//	long[] methods = Receiver.store.methods;

/*			long[][][] stacks = Receiver.store.stacks;
			
			int stackPointer = (int)methods[selectedMethod+Store.METHOD_STACKS_PTR];

			if((stackPointer!=Store.NULL)||(stacks[stackPointer]!=null)) {
				
				long[][] methodStacks = stacks[stackPointer];
				
				for(int p=0;p<methodStacks.length;p++) {
					if(methodStacks[p]!=null) {
						

						gc.drawString("Hits: " + methodStacks[p][0] , PADDING, y);
					y+=SPACING;
					
					}
					
				}
				
			}
			
*/
			
			
			
		}
		
		
		gc.drawRoundRectangle(HALF_PADDING, HALF_PADDING, size.x - PADDING, size.y - (PADDING), PADDING, PADDING);
		gc.setAntialias(SWT.OFF);
		gc.dispose();
	}

}
