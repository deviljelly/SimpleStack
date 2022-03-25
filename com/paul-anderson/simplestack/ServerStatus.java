package com.paul-anderson.simplestack;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class ServerStatus extends Canvas {

	private static Font LABEL_FONT = SimpleStack.LABEL_FONT;
	private static int PADDING = SimpleStack.PADDING;
	private static int HALF_PADDING = SimpleStack.HALF_PADDING;
	private static Date serverTime;
	private static SimpleDateFormat dateFormat;
	private static long previousEventsConsumed = 0;
	private static long previousRedrawTime = 0;
	
	public ServerStatus(Composite parent, int style) {
		super(parent, style);
		super.setBackground(SimpleStack.BACKGROUND);
		super.setForeground(SimpleStack.FOREGROUND);

		serverTime = new Date();
		dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
		previousRedrawTime = System.currentTimeMillis();
		
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				ServerStatus.this.paintControl(e);
			}
		});

	}

	public void paintControl(PaintEvent pe) {
		GC gc = pe.gc;
		Point size = getSize();
		gc.setLineWidth(3);

		long eventsConsumed = Receiver.eventsConsumed;
		
		long now = System.currentTimeMillis();
		
		long interval = now - previousRedrawTime;
		
		double eventsPerSecond = (((double)eventsConsumed - (double)previousEventsConsumed)/(double)interval)*1000.0 ;
		
		

		previousEventsConsumed = eventsConsumed; 
		previousRedrawTime = now;
//		serverTime.setTime(Threads.serverTime);
//		String serverTimeString = dateFormat.format(serverTime);
		String serverTimeString = "NaN";
		
		gc.setFont(LABEL_FONT);
		gc.setAntialias(SWT.ON);
		int height = PADDING;
		gc.drawString("Status  ||  " +  "Server Time:" + serverTimeString +  "  |  Threads:" + Receiver.numberOfThreads + "  |  Events/sec:" + (int)eventsPerSecond , PADDING, height);
		height+=16;
		gc.drawString("Total:  " + Store.totalHits + "  |  Running:" + Store.totalRunningHits +  "  |  Waiting:" + Store.totalWaitingHits , PADDING, height);
		height+=16;
		gc.drawString("Sample Overhead:  " + Receiver.delay + "\u00B5s  |  Frequency:  " + Receiver.hertz + "Hz", PADDING, height);
		gc.drawRoundRectangle(HALF_PADDING, HALF_PADDING, size.x - PADDING, size.y - (PADDING), PADDING, PADDING);
		gc.setAntialias(SWT.OFF);
		gc.dispose();
	}

}
