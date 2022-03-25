package com.paul-anderson.simplestack;



import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class StacksList extends Canvas {

	private static Font LABEL_FONT = SimpleStack.LABEL_FONT;
	private static int PADDING = SimpleStack.PADDING;
	private static int HALF_PADDING = SimpleStack.HALF_PADDING;
	
	private static int SPACING = 16;
	
	
	public StacksList(Composite parent, int style) {
		super(parent, style);
		super.setBackground(SimpleStack.BACKGROUND);
		super.setForeground(SimpleStack.FOREGROUND);

		addMouseListener(new MouseListener() {
			
			public void mouseUp(MouseEvent arg0) {
			}

			public void mouseDown(MouseEvent me) {
				
				int stackPosition = ((me.y-PADDING)/SPACING)-1;
				
				if(stackPosition>=0){
				
				long[][][] stacks = Receiver.store.stacks;
				
				int stackPointer = (int)Receiver.store.methods[SimpleStack.selectedMethod+Store.METHOD_STACKS_PTR];

				long[][] methodStacks = stacks[stackPointer];
				
				long[] stack = methodStacks[stackPosition];
				SimpleStack.selectedStack = stack;
				SimpleStack.stacksList.redraw();
				SimpleStack.stackDetail.redraw();
				}
			}
			
			public void mouseDoubleClick(MouseEvent arg0) {
			}
		});

		
		
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				StacksList.this.paintControl(e);
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
			
			gc.drawString("Stacks: ", PADDING, y);
			y+=SPACING;


			long[] methods = Receiver.store.methods;

			long[][][] stacks = Receiver.store.stacks;
			
			int stackPointer = (int)methods[selectedMethod+Store.METHOD_STACKS_PTR];

			if((stackPointer!=Store.NULL)||(stacks[stackPointer]!=null)) {
				
				long[][] methodStacks = stacks[stackPointer];
				
				for(int p=0;p<methodStacks.length;p++) {
					if(methodStacks[p]!=null) {
						
						long[] theStack = methodStacks[p];
						StringBuilder sb = new StringBuilder();
							sb.append(theStack[0]);
						sb.append(": ");
						for(int d=1;d<theStack.length;d++) {
							long methodID = theStack[d]; 
							int method = Receiver.store.getMethod(methodID);
							sb.append(Receiver.store.strings[(int)Receiver.store.methods[method+Store.METHOD_NAME_PTR]]);
							if(d<(theStack.length-1)) sb.append("\u2190");
						}
						
						if(theStack.equals(SimpleStack.selectedStack))  {
							gc.setForeground(SimpleStack.HIGHLIGHT);
						}
						gc.drawString(sb.toString() , PADDING, y);
						if(theStack.equals(SimpleStack.selectedStack))  {
							gc.setForeground(SimpleStack.FOREGROUND);
						}
					y+=SPACING;
					
					}
					
				}
				
			}
			

			
			
			
		}
		
		
		gc.drawRoundRectangle(HALF_PADDING, HALF_PADDING, size.x - PADDING, size.y - (PADDING), PADDING, PADDING);
		gc.setAntialias(SWT.OFF);
		gc.dispose();
	}

}
