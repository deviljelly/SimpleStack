package com.paul-anderson.simplestack;
import java.util.Vector;



public class CallStackNode {

	long methodID;
//	SimpleMethod method;
	CallStackNode[] children;
	CallStackNode parent;
	int hits;

	public CallStackNode() {
		
		children = new CallStackNode[16];
		
	}
	
	public void hit() {
		hits++;
	}
	
	public CallStackNode getOrAdd(long methodID) {
		
		int i=0;
		for(;i<children.length;i++) {
			if(children[i]==null) break;
			if(children[i].methodID==methodID) return children[i];
		}
		

		CallStackNode newNode = new CallStackNode();
		newNode.methodID = methodID;
	//	newNode.method = Receiver.methods.get(methodID);
		newNode.parent = this;

		
		CallStackNode theParent = newNode;
	//	while(theParent!=Receiver.rootNode) {
	//		System.out.print(theParent.method.methodName);
	//		System.out.print("-->");
	//		theParent = theParent.parent;
	//	}
		System.out.println();
		
		
		
		if(i<children.length) {
			
			children[i] = newNode;
			
		} else {
			
			CallStackNode[] tempChildren = new CallStackNode[children.length*2];
			System.arraycopy(children, 0, tempChildren, 0, children.length);
			children = tempChildren;
			children[i] = newNode;
		}
		
		return newNode;
	}

}
