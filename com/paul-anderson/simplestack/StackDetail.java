package com.paul-anderson.simplestack;



import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class StackDetail extends Canvas {

	private static Font LABEL_FONT = SimpleStack.LABEL_FONT;
	private static int PADDING = SimpleStack.PADDING;
	private static int HALF_PADDING = SimpleStack.HALF_PADDING;
	
	private static int SPACING = 16;
	
	
	public StackDetail(Composite parent, int style) {
		super(parent, style);
		super.setBackground(SimpleStack.BACKGROUND);
		super.setForeground(SimpleStack.FOREGROUND);

		
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				StackDetail.this.paintControl(e);
			}
		});

	}

	public void paintControl(PaintEvent pe) {
		GC gc = pe.gc;
		Point size = getSize();
		gc.setLineWidth(3);

		
		gc.setFont(LABEL_FONT);
		gc.setAntialias(SWT.ON);

		long[] selectedStack = SimpleStack.selectedStack;
		
		if(selectedStack!=null) {
			
			int y = PADDING;
			
			gc.drawString("Stack: ", PADDING, y);
			y+=SPACING;


						for(int d=1;d<selectedStack.length;d++) {
							StringBuffer sb = new StringBuffer();
							long methodID = selectedStack[d]; 
							int method = Receiver.store.getMethod(methodID);
							sb.append(Receiver.store.strings[(int)Receiver.store.methods[method+Store.METHOD_CLASSNAME_PTR]]);
							sb.append('.');
							sb.append(Receiver.store.strings[(int)Receiver.store.methods[method+Store.METHOD_NAME_PTR]]);
							sb.append(Receiver.store.strings[(int)Receiver.store.methods[method+Store.METHOD_SIGNATURE_PTR]]);
							gc.drawString(sb.toString() , PADDING, y);
							y+=SPACING;
						}
					
				
			}
			

			
			
			
		
		
		
		gc.drawRoundRectangle(HALF_PADDING, HALF_PADDING, size.x - PADDING, size.y - (PADDING), PADDING, PADDING);
		gc.setAntialias(SWT.OFF);
		gc.dispose();
	}

}
