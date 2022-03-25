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

public class MethodGraph extends Canvas {

	private static Font LABEL_FONT = SimpleStack.LABEL_FONT;
	private static int PADDING = SimpleStack.PADDING;
	private static int HALF_PADDING = SimpleStack.HALF_PADDING;
	
	private static int SPACING = 16;
	
	
	public MethodGraph(Composite parent, int style) {
		super(parent, style);
		super.setBackground(SimpleStack.BACKGROUND);
		super.setForeground(SimpleStack.FOREGROUND);

		
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				MethodGraph.this.paintControl(e);
			}
		});

	}

	public void paintControl(PaintEvent pe) {
		GC gc = pe.gc;
		Point size = getSize();

		
		gc.setFont(LABEL_FONT);
		gc.setAntialias(SWT.ON);

		int selectedMethod = SimpleStack.selectedMethod;
		
		if(selectedMethod!=0) {
			
			int doublePadding = PADDING *2;
			int availableWidth = size.x - doublePadding; 
			int availableHeight = size.y - doublePadding;

			gc.drawLine(doublePadding, doublePadding, doublePadding, availableHeight);
			gc.drawLine(doublePadding, availableHeight, availableWidth, availableHeight);
			

//			int[] methods = Receiver.storet
			
//			int numberOfPoints = 
			
			
			
		}
		
		
		gc.setLineWidth(3);
		gc.drawRoundRectangle(HALF_PADDING, HALF_PADDING, size.x - PADDING, size.y - (PADDING), PADDING, PADDING);
		gc.setAntialias(SWT.OFF);
		gc.dispose();
	}

}
