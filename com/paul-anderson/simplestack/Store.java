package com.paul-anderson.simplestack;

public class Store {

	
	public static final int METHOD_HASHTABLE_SIZE = 8192;
	public static final int METHOD_HASHTABLE_HASH = 8191;
	
	public static final long GOLDEN_RATIO = 2654435761L;
	public static final int NULL = 0;
	public static final long NULLL = 0;


	public static final int DEFAULT_ALL_STACKS_SIZE = 1024;
	
	public static final int DEFAULT_METHOD_STACKS_SIZE = 16;
	public static final int DEFAULT_INDEX_SIZE = 1024;
	public static final int DEFAULT_GRAPHS = 1024;
	public static final int DEFAULT_GRAPH_SIZE = 3600;
	
	public static final int METHOD_HASH_NODE_LENGTH = 2;
	public static final int METHOD_HASH_KEY = 0;
	public static final int METHOD_HASH_METHOD_PTR = 1;

	public static final int METHOD_DEFAULT_CHAIN_LENGTH = 8;
	public static final int METHOD_DEFAULT_SIZE = 16384;

	public static final int STRINGS_DEFAULT_SIZE = 32768;
	
	public static final int METHOD_NODE_LENGTH = 13;
	public static final int METHOD_ID = 0;
	public static final int METHOD_HITS = 1;
	public static final int METHOD_WAITING_HITS = 2;
	public static final int METHOD_RUNNING_HITS = 3;
	public static final int METHOD_CLASSNAME_PTR = 4;
	public static final int METHOD_NAME_PTR = 5;
	public static final int METHOD_SIGNATURE_PTR = 6;
	public static final int METHOD_HASHBUCKET_PTR = 7;
	public static final int METHOD_HASHENTRY_PTR = 8;
	public static final int METHOD_STACKS_PTR = 9;
	public static final int METHOD_GRAPH_PTR = 10;
	public static final int METHOD_GRAPH_POS = 11;
	public static final int METHOD_IGNORE = 12;

	public static final int METHOD_BASE = 1;
	public static final int STRINGS_BASE = 1;
	public static final int STACKS_BASE = 1;
	public static final int GRAPHS_BASE = 1;

	public long[] methods;
	public int methodsPointer;
	public long[][] methodHashtable;
	public char[][] strings;
	public int stringsPointer;
	public long[][][] stacks;
	public int stacksPointer;
	public float[][] graphs;
	public int graphsPointer;

	public int[] index;
	public int indexPointer;
	public int[] waitingIndex;
	public int waitingIndexPointer;
	public int[] runningIndex;
	public int runningIndexPointer;

	
	public static int totalHits = 0;
	public static int totalWaitingHits = 0;
	public static int totalRunningHits = 0;
	
	
	public static int hitCounter=0;
	
	public static long hash(long key) {
		
		return key *= GOLDEN_RATIO;
		
	}
	
	public Store() {
		
		methodHashtable = new long[METHOD_HASHTABLE_SIZE][];
		methods = new long[METHOD_DEFAULT_SIZE];
		strings = new char[STRINGS_DEFAULT_SIZE][];
		methodsPointer = METHOD_BASE;
		stringsPointer = STRINGS_BASE;
		stacks = new long[DEFAULT_ALL_STACKS_SIZE][][];
		stacksPointer = STACKS_BASE;
		graphs = new float[DEFAULT_GRAPHS][];
		graphsPointer = GRAPHS_BASE;

		index = new int[DEFAULT_INDEX_SIZE];
		waitingIndex = new int[DEFAULT_INDEX_SIZE];
		runningIndex = new int[DEFAULT_INDEX_SIZE];
		
		
	}
	
	
	public int getMethod(long methodID) {
		
		
		
		int bucket = (int) (hash(methodID) & METHOD_HASHTABLE_HASH);
		
		long[] chain = methodHashtable[bucket];

        if(chain==null) return NULL;
			
        int chainLength= chain.length;
        
		for(int i=0;i<chainLength;i+=METHOD_HASH_NODE_LENGTH) {
			
			if(chain[i+METHOD_HASH_KEY]== methodID)
				{
//				System.err.println("getting method " + (int) chain[i+METHOD_HASH_METHOD_PTR] + " hash " + bucket);
				return (int) chain[i+METHOD_HASH_METHOD_PTR];
				
				}
			
		}

		return NULL;
		
	}

	
	public void updateAllGraphs() {
		
		int method = METHOD_BASE;

//		Store store = Receiver.store;

		float onePercent = Receiver.stacksSampled/100.0f;

		while(methods[method+Store.METHOD_ID]!=0) {
			
			
			int hits = (int)methods[method+Store.METHOD_HITS];
			float percentage = (hits/onePercent);

			updateGraph(method, percentage);
			
			method+=METHOD_NODE_LENGTH;
			
		}
	
	}

	
	public void updateGraph(int method, float percentage) {
		
		int graphPointer = (int)methods[method+METHOD_GRAPH_PTR];

		if((graphPointer==NULL)||(graphs[graphPointer]==null)) {
			
			graphPointer = graphsPointer++;
			if(graphsPointer>graphs.length) {
				float[][] tempGraphs = new float[graphs.length*2][];
				System.arraycopy(graphs, 0, tempGraphs, 0, graphs.length);
				graphs = tempGraphs;
			}
			
			graphs[graphPointer] = new float[DEFAULT_GRAPH_SIZE];
			
			methods[method+METHOD_GRAPH_PTR] = graphPointer;
			methods[method+METHOD_GRAPH_POS] = 0;
		}
		
		float[] graph = graphs[graphPointer];
		int graphPos = (int) methods[method+METHOD_GRAPH_POS];

		if(graphPos>=graphs.length) {
			float[] tempGraph = new float[graph.length*2];
			System.arraycopy(graph, 0, tempGraph, 0, graph.length);
			graph = tempGraph;
			graphs[graphPointer] = graph;
		}
		
		
		
		graph[graphPos] = percentage;
		graphPos++;
		methods[method+METHOD_GRAPH_POS] = graphPos;
	
	}


	public void sort(int method) {
		

		int methodIndex = 0;
		
		for(int i=0;i<indexPointer;i++) {
			
			if(index[i]==method) {
				methodIndex=i;
				break;
			}
		}
			if(methodIndex!=0) {
				int previousIndex = methodIndex-1;
				while(methods[index[methodIndex]+METHOD_HITS]>methods[index[previousIndex]+METHOD_HITS]) {
					
					int temp = index[previousIndex];
					index[previousIndex] = index[methodIndex];
					index[methodIndex] = temp; 					
					methodIndex--;
					previousIndex--;
					
					if(previousIndex<0) break;
					
				}
			}
		
		
	 //		System.out.println("total");
	//	for(int i=0;i<indexPointer;i++) {
			
//			if(methods[index[i]+METHOD_HITS]==0) break;
//			System.out.println("   " + i + ": " + methods[index[i]+METHOD_HITS] + "(" + index[i] + ")");

//		}
		
		
	}
	public void sortWaiters(int method) {
		

		int methodIndex = 0;
		
		for(int i=0;i<waitingIndexPointer;i++) {
			
			if(waitingIndex[i]==method) {
				methodIndex=i;
				break;
			}
		}
			if(methodIndex!=0) {
				int previousIndex = methodIndex-1;
				while(methods[waitingIndex[methodIndex]+METHOD_WAITING_HITS]>methods[waitingIndex[previousIndex]+METHOD_WAITING_HITS]) {
					
					int temp = waitingIndex[previousIndex];
					waitingIndex[previousIndex] = waitingIndex[methodIndex];
					waitingIndex[methodIndex] = temp; 					
					methodIndex--;
					previousIndex--;
					
					if(previousIndex<0) break;
					
				}
			}
		
		//	System.out.println("Waiters");
//		for(int i=0;i<waitingIndexPointer;i++) {
			
//			if(methods[waitingIndex[i]+METHOD_WAITING_HITS]==0) break;
//			System.out.println("   " + i + ": " + methods[waitingIndex[i]+METHOD_WAITING_HITS] + "(" + waitingIndex[i] + ")");

//		}
		
		
	}

	
	public void sortRunners(int method) {
		

		int methodIndex = 0;
		
		for(int i=0;i<runningIndexPointer;i++) {
			
			if(runningIndex[i]==method) {
				methodIndex=i;
				break;
			}
		}
			if(methodIndex!=0) {
				int previousIndex = methodIndex-1;
				while(methods[runningIndex[methodIndex]+METHOD_RUNNING_HITS]>methods[runningIndex[previousIndex]+METHOD_RUNNING_HITS]) {
					
					int temp = runningIndex[previousIndex];
					runningIndex[previousIndex] = runningIndex[methodIndex];
					runningIndex[methodIndex] = temp; 					
					methodIndex--;
					previousIndex--;
					
					if(previousIndex<0) break;
					
				}
			}
		
//			System.out.println("Runners");
		
	//	for(int i=0;i<runningIndexPointer;i++) {
			
	//		if(methods[runningIndex[i]+METHOD_RUNNING_HITS]==0) break;
	//		System.out.println("   " + i + ": " + methods[runningIndex[i]+METHOD_RUNNING_HITS] + "(" + runningIndex[i] + ")");

	//	}
		
		
	}

	public void sortStacks(long[][] methodStacks, int pointer) {
		

		
			if(pointer!=0) {
				int previousPointer = pointer-1;
				while(methodStacks[pointer][0] > methodStacks[previousPointer][0]) {
					
					long[] temp = methodStacks[previousPointer];
					methodStacks[previousPointer] = methodStacks[pointer];
					methodStacks[pointer] = temp;
					pointer--;
					previousPointer--;
					
					if(previousPointer<0) break;
					
				}
			}
		
	}
	
	
	public void hit(int method, long[] stack, long state) {
		
		// stacks first

		
//		char[] methodName = Receiver.store.strings[(int)methods[method+METHOD_NAME_PTR]];
		
		
//		String m = new String(methodName);
		
//		if(m.equals("waitSemaphore")) {
		
//		System.out.println(Long.toHexString(state));
//		}
		

		int stackPointer = (int)methods[method+METHOD_STACKS_PTR];

		if((stackPointer==NULL)||(stacks[stackPointer]==null)) {
			
			stackPointer = stacksPointer++;
			
			if(stacksPointer>stacks.length) {
				long[][][] tempStacks = new long[stacks.length*2][][];
				System.arraycopy(stacks, 0, tempStacks, 0, stacks.length);
				stacks = tempStacks;
			}
			
			stacks[stackPointer] = new long[DEFAULT_METHOD_STACKS_SIZE][];
			
			methods[method+METHOD_STACKS_PTR] = stackPointer;
		}
		
		long[][] methodStacks = stacks[stackPointer];
		
		int paddedLength = stack.length + 1;
		
		int pointer=0;
		
		boolean found = false;
		while((pointer<methodStacks.length)&&(methodStacks[pointer]!=null)&&(!found)) {
			
			long[] theStack = methodStacks[pointer];
			
			if(theStack.length == paddedLength) {
				
				boolean match=true;
				for(int i=0;i<stack.length;i++) {
					if(stack[i]!=theStack[i+1]) {
						match=false;
						break;
					}
				}

				if(match) {
					methodStacks[pointer][0]++;
					found=true;
					sortStacks(methodStacks, pointer);
				} 
				
			}
			
			pointer++;
		}
		

		if(!found) {
			
//			int j=0;
//			while(methodStacks[j]!=null) j++;
			
			if(pointer==methodStacks.length) { // expand if required

				long[][] tempMethodStacks = new long[methodStacks.length*2][];
				System.arraycopy(methodStacks, 0, tempMethodStacks, 0, methodStacks.length);
				methodStacks = tempMethodStacks;
				stacks[stackPointer] = methodStacks;
				
			}
			//add
			//System.err.println("adding new stack of Length "+ stack.length);
				methodStacks[pointer] = new long[paddedLength];
				System.arraycopy(stack, 0, methodStacks[pointer], 1, stack.length);
				methodStacks[pointer][0]++;
				sortStacks(methodStacks, pointer);
	
		}
		
		
		
		
//		System.out.println("S: " + Long.toHexString(state));
//		if(methods[method+METHOD_HITS]==0) {
//			System.err.println("here");
//		}
		
		if(methods[method+METHOD_IGNORE]==0) {

			methods[method+METHOD_HITS] = methods[method+METHOD_HITS]+1;
			
			totalHits++;
			sort(method);
			if((state&Receiver.JVMTI_THREAD_WAITING) >0) {
//				System.out.println("w: " + Long.toHexString(state));
				methods[method+METHOD_WAITING_HITS] = methods[method+METHOD_WAITING_HITS]+1;
				totalWaitingHits++;
				sortWaiters(method);
			} else {
		//		System.out.println("R: " + Long.toHexString(state));
				methods[method+METHOD_RUNNING_HITS]++;
				totalRunningHits++;
				sortRunners(method);
			}
		
		
		}

		
		
		
		
		/*

		long hits = methods[method+METHOD_HITS];
		
		
		if(method!=METHOD_BASE) {

			
			int previousMethod = method-METHOD_NODE_LENGTH;

			if(hits > methods[previousMethod+METHOD_HITS]) {

				long save1 = methods[method+METHOD_ID];
				long save2 = methods[method+METHOD_HITS];
				long save3 = methods[method+METHOD_WAITING_HITS];
				long save4 = methods[method+METHOD_RUNNING_HITS];
				long save5 = methods[method+METHOD_CLASSNAME_PTR];
				long save6 = methods[method+METHOD_NAME_PTR];
				long save7 = methods[method+METHOD_SIGNATURE_PTR];
				long save8 = methods[method+METHOD_HASHBUCKET_PTR];
				long save9 = methods[method+METHOD_HASHENTRY_PTR];
				long save10 = methods[method+METHOD_STACKS_PTR];
				long save11 = methods[method+METHOD_GRAPH_PTR];
				long save12 = methods[method+METHOD_GRAPH_POS];

				
			while((hits > methods[previousMethod+METHOD_HITS])) {
				
//				System.err.println(hits + " is greater than " + methods[previousMethod+METHOD_HITS]);
				
//				System.err.println("moving "+methods[previousMethod+METHOD_ID] +" to " + method);
				
				
				methods[method+METHOD_ID] = methods[previousMethod+METHOD_ID];
				methods[method+METHOD_HITS] = methods[previousMethod+METHOD_HITS];
				methods[method+METHOD_WAITING_HITS] = methods[previousMethod+METHOD_WAITING_HITS];
				methods[method+METHOD_RUNNING_HITS] = methods[previousMethod+METHOD_RUNNING_HITS];
				methods[method+METHOD_CLASSNAME_PTR] = methods[previousMethod+METHOD_CLASSNAME_PTR];
				methods[method+METHOD_NAME_PTR] = methods[previousMethod+METHOD_NAME_PTR];
				methods[method+METHOD_SIGNATURE_PTR] = methods[previousMethod+METHOD_SIGNATURE_PTR];
				methods[method+METHOD_HASHBUCKET_PTR] = methods[previousMethod+METHOD_HASHBUCKET_PTR];
				methods[method+METHOD_HASHENTRY_PTR] = methods[previousMethod+METHOD_HASHENTRY_PTR];
				methods[method+METHOD_STACKS_PTR] = methods[previousMethod+METHOD_STACKS_PTR];
				methods[method+METHOD_GRAPH_PTR] = methods[previousMethod+METHOD_GRAPH_PTR];
				methods[method+METHOD_GRAPH_POS] = methods[previousMethod+METHOD_GRAPH_POS];
				int bucket = (int)methods[method+METHOD_HASHBUCKET_PTR];
				int entry =(int)methods[method+METHOD_HASHENTRY_PTR]; 
				
				//System.err.println("changing hash entry from " + methodHashtable[bucket][entry+METHOD_HASH_METHOD_PTR] + " to " + method);
				methodHashtable[bucket][entry+METHOD_HASH_METHOD_PTR] = method;
				
				
				
				method-=METHOD_NODE_LENGTH;
				previousMethod-=METHOD_NODE_LENGTH;
				
				if(previousMethod<METHOD_BASE) break;

			}

			previousMethod+=METHOD_NODE_LENGTH;
				
			
			methods[previousMethod+METHOD_ID] = save1;
			methods[previousMethod+METHOD_HITS] = save2;
			methods[previousMethod+METHOD_WAITING_HITS] = save3;
			methods[previousMethod+METHOD_RUNNING_HITS] = save4;
			methods[previousMethod+METHOD_CLASSNAME_PTR] = save5;
			methods[previousMethod+METHOD_NAME_PTR] = save6;
			methods[previousMethod+METHOD_SIGNATURE_PTR] = save7;
			methods[previousMethod+METHOD_HASHBUCKET_PTR] = save8;
			methods[previousMethod+METHOD_HASHENTRY_PTR] = save9;
			methods[previousMethod+METHOD_STACKS_PTR] = save10;
			methods[previousMethod+METHOD_GRAPH_PTR] = save11;
			methods[previousMethod+METHOD_GRAPH_POS] = save12;
			int bucket = (int)methods[previousMethod+METHOD_HASHBUCKET_PTR];
			int entry =(int)methods[previousMethod+METHOD_HASHENTRY_PTR]; 
			
			
			//System.err.println("changing hash entry from " + methodHashtable[bucket][entry+METHOD_HASH_METHOD_PTR] + " to " + previousMethod);
			methodHashtable[bucket][entry+METHOD_HASH_METHOD_PTR] = previousMethod;

			//			methodHashtable[(int)save6][(int)save7+METHOD_HASH_METHOD_PTR] = previousMethod;
//			methodHashtable[previousMethod+METHOD_HASHBUCKET_PTR][previousMethod+METHOD_HASHENTRY_PTR+METHOD_HASH_METHOD_PTR] = previousMethod;
			}
				
				
			
		}
		*/
	}

	
	public static final char[] translateClassName(char[] className) {
		
		
		int length = className.length;
		
		char[] translatedName = new char[length];
		
		int pointer = 0;
		
		boolean firstLRemoved = false;
		
		for(int i=0;i<length;i++) {
			
			switch (className[i]) {
			case 'L':
				if(firstLRemoved) {
				translatedName[pointer++] = className[i];
				} else {
					firstLRemoved = true;
				}
				
				break;
			case '/':
				translatedName[pointer++] = '.';
				break;

			case '[':
			case ';':
				break;

			default:
				translatedName[pointer++] = className[i];
				break;
			}
			
		}
		
		char[] finalName = new char[pointer];
		System.arraycopy(translatedName, 0, finalName, 0, pointer);
		
		return finalName;
		
	}

	public static final char[] translateMethodSignatire(char[] methodSignature) {
		
		
		int length = methodSignature.length;
		
		char[] translatedSignature = new char[2048];
		
		int pointer = 0;
		
		boolean hasPrevious = false;
		boolean inParameters = false;
		boolean inClass = false;
		boolean arrayDeclared = false;
		int cardinality=0;
		
		for(int i=0;i<length;i++) {
			
			switch (methodSignature[i]) {
			case 'L':
				if(hasPrevious) {
					translatedSignature[pointer++] = ',';
					translatedSignature[pointer++] = ' ';
				}
				if(!inClass) {
					inClass = true;
				} else {
					translatedSignature[pointer++] = methodSignature[i];
				}
				hasPrevious = true;
				break;

			case 'I':
				if(!inClass) {
					if(hasPrevious) {
						translatedSignature[pointer++] = ',';
						translatedSignature[pointer++] = ' ';
					}

					translatedSignature[pointer++] = 'i';
					translatedSignature[pointer++] = 'n';
					translatedSignature[pointer++] = 't';
					hasPrevious = true;

				} else {
					translatedSignature[pointer++] = methodSignature[i];
				}
				break;
			case 'B':
				if(!inClass) {
					if(hasPrevious) {
						translatedSignature[pointer++] = ',';
						translatedSignature[pointer++] = ' ';
					}

					translatedSignature[pointer++] = 'b';
					translatedSignature[pointer++] = 'y';
					translatedSignature[pointer++] = 't';
					translatedSignature[pointer++] = 'e';
					hasPrevious = true;

				} else {
					translatedSignature[pointer++] = methodSignature[i];
				}
				break;
			case 'C':
				if(!inClass) {
					if(hasPrevious) {
						translatedSignature[pointer++] = ',';
						translatedSignature[pointer++] = ' ';
					}

					translatedSignature[pointer++] = 'c';
					translatedSignature[pointer++] = 'h';
					translatedSignature[pointer++] = 'a';
					translatedSignature[pointer++] = 'r';
					hasPrevious = true;

				} else {
					translatedSignature[pointer++] = methodSignature[i];
				}
				break;

			case 'S':
				if(!inClass) {
					if(hasPrevious) {
						translatedSignature[pointer++] = ',';
						translatedSignature[pointer++] = ' ';
					}

					translatedSignature[pointer++] = 's';
					translatedSignature[pointer++] = 'h';
					translatedSignature[pointer++] = 'o';
					translatedSignature[pointer++] = 'r';
					translatedSignature[pointer++] = 't';
					hasPrevious = true;

				} else {
					translatedSignature[pointer++] = methodSignature[i];
				}
				break;
			case 'D':
				if(!inClass) {
					if(hasPrevious) {
						translatedSignature[pointer++] = ',';
						translatedSignature[pointer++] = ' ';
					}

					translatedSignature[pointer++] = 'd';
					translatedSignature[pointer++] = 'o';
					translatedSignature[pointer++] = 'u';
					translatedSignature[pointer++] = 'b';
					translatedSignature[pointer++] = 'l';
					translatedSignature[pointer++] = 'e';
					hasPrevious = true;

				} else {
					translatedSignature[pointer++] = methodSignature[i];
				}
				break;
			case 'F':
				if(!inClass) {
					if(hasPrevious) {
						translatedSignature[pointer++] = ',';
						translatedSignature[pointer++] = ' ';
					}

					translatedSignature[pointer++] = 'f';
					translatedSignature[pointer++] = 'l';
					translatedSignature[pointer++] = 'o';
					translatedSignature[pointer++] = 'a';
					translatedSignature[pointer++] = 't';
					hasPrevious = true;

				} else {
					translatedSignature[pointer++] = methodSignature[i];
				}
				break;

			case 'J':
				if(!inClass) {
					if(hasPrevious) {
						translatedSignature[pointer++] = ',';
						translatedSignature[pointer++] = ' ';
					}

					translatedSignature[pointer++] = 'l';
					translatedSignature[pointer++] = 'o';
					translatedSignature[pointer++] = 'n';
					translatedSignature[pointer++] = 'g';
					hasPrevious = true;

				} else {
					translatedSignature[pointer++] = methodSignature[i];
				}
				break;
			case 'V':
				if(!inClass) {
					if(hasPrevious) {
						translatedSignature[pointer++] = ',';
						translatedSignature[pointer++] = ' ';
					}

					translatedSignature[pointer++] = 'v';
					translatedSignature[pointer++] = 'o';
					translatedSignature[pointer++] = 'i';
					translatedSignature[pointer++] = 'd';
					hasPrevious = true;

				} else {
					translatedSignature[pointer++] = methodSignature[i];
				}
				break;
			case 'Z':
				if(!inClass) {
					if(hasPrevious) {
						translatedSignature[pointer++] = ',';
						translatedSignature[pointer++] = ' ';
					}

					translatedSignature[pointer++] = 'b';
					translatedSignature[pointer++] = 'o';
					translatedSignature[pointer++] = 'o';
					translatedSignature[pointer++] = 'l';
					translatedSignature[pointer++] = 'e';
					translatedSignature[pointer++] = 'a';
					translatedSignature[pointer++] = 'n';
					hasPrevious = true;

				}
				break;
			
			case '/':
				translatedSignature[pointer++] = '.';
				break;

			case '(':
				translatedSignature[pointer++] = '(';
				inParameters=true;
				break;

			case ')':
				inParameters=false;
				hasPrevious=false;
				translatedSignature[pointer++] = ')';
				translatedSignature[pointer++] = ' ';
				break;

			case '[':
				arrayDeclared=true;
				cardinality++;
				break;
			case ';':
				if(arrayDeclared) {
					for(int x=0;x<cardinality;x++) {
						translatedSignature[pointer++] = '[';
						translatedSignature[pointer++] = ']';
					}
					arrayDeclared=false;
					cardinality = 0;
				}
				inClass=false;
				break;

			default:
				translatedSignature[pointer++] = methodSignature[i];
				break;
			}
			
		}
		
		char[] finalSignature = new char[pointer];
		System.arraycopy(translatedSignature, 0, finalSignature, 0, pointer);
		
		return finalSignature;
		
	}

	
	public int addMethod(long methodID, char[] className, char[] name, char[] signature) {
		

		
		
		className = translateClassName(className);
		signature = translateMethodSignatire(signature);
		
		if(stringsPointer+3 >= strings.length) {
			char[][] tempStrings = new char[strings.length * 2][];
			System.arraycopy(strings, 0, tempStrings, 0, strings.length);
			strings = tempStrings;
		}
		
		int classNamePointer = stringsPointer++;
		int namePointer = stringsPointer++;
		int signaturePointer = stringsPointer++;

		strings[classNamePointer] = className;
		strings[namePointer] = name;
		strings[signaturePointer] = signature;
		
		int bucket = (int) (hash(methodID) & METHOD_HASHTABLE_HASH);
		
		long[] chain = methodHashtable[bucket];

		int nextFree=0;
		
        if(chain==null) {
        	chain = new long[METHOD_DEFAULT_CHAIN_LENGTH * METHOD_HASH_NODE_LENGTH];
        	methodHashtable[bucket] = chain;
        	nextFree = 0;
        } else {

        	for(nextFree=0;nextFree<chain.length;nextFree+=METHOD_HASH_NODE_LENGTH) {
        		if(chain[nextFree]==NULLL) break;
        	}
        	
        	if(nextFree==chain.length) {
        		long[] tempChain = new long[chain.length*2];
        		System.arraycopy(chain, 0, tempChain, 0, chain.length);
        		chain = tempChain;
        		methodHashtable[bucket] = chain;
        	}


        	
        	
        }

        
    	if(methodsPointer+METHOD_NODE_LENGTH>=methods.length) {
    		long[] tempMethods = new long[methods.length*2];
    		System.arraycopy(methods, 0, tempMethods, 0, methods.length);
    		methods=tempMethods;
    	}
    	
    	int method = methodsPointer;
    	methodsPointer+= METHOD_NODE_LENGTH;
    		
    	chain[nextFree+METHOD_HASH_KEY] = methodID; 
    	chain[nextFree+METHOD_HASH_METHOD_PTR] = method; 

    	
    	methods[method + METHOD_ID] = methodID;
    	methods[method + METHOD_CLASSNAME_PTR] = classNamePointer;
    	methods[method + METHOD_NAME_PTR] = namePointer;
    	methods[method + METHOD_SIGNATURE_PTR] = signaturePointer;
    	methods[method + METHOD_HASHBUCKET_PTR] = bucket;
    	methods[method + METHOD_HASHENTRY_PTR] = nextFree;

    	String nameString = new String(name); 
    	
    	if((nameString.equals("nativeRunApplicationThread"))||(nameString.equals("waitForSignal"))||(nameString.equals("ntv_getReceiveQueue"))) {
//       	if( (nameString.equals("nativeRunApplicationThread")) || (nameString.equals("waitForSignal")) ) {
    		methods[method + METHOD_IGNORE] = 1;
    	}

    	if(indexPointer>=index.length) {
    		int[] tempIndex = new int[index.length*2];
    		System.arraycopy(index, 0, tempIndex, 0, index.length);
    		index = tempIndex;
    	}
    	index[indexPointer++] = method;

    	if(waitingIndexPointer>=waitingIndex.length) {
    		int[] tempWaitingIndex = new int[waitingIndex.length*2];
    		System.arraycopy(waitingIndex, 0, tempWaitingIndex, 0, waitingIndex.length);
    		waitingIndex = tempWaitingIndex;
    	}
    	waitingIndex[waitingIndexPointer++] = method;

    	if(runningIndexPointer>=runningIndex.length) {
    		int[] tempRunningIndex = new int[runningIndex.length*2];
    		System.arraycopy(runningIndex, 0, tempRunningIndex, 0, runningIndex.length);
    		runningIndex = tempRunningIndex;
    	}
    	runningIndex[runningIndexPointer++] = method;

//    	System.err.println("Creating method " + methodID + " at " + method + " hash " + (int) (hash(methodID) & METHOD_HASHTABLE_HASH));
    	
    	return method;
		
	}
	
	
	public static char[] toCharArray(byte[] byteArray) {
		
		int length = byteArray.length;
		char[] charArray = new char[length];
		for(int i=0;i<length;i++) charArray[i] = (char)byteArray[i];
		
		return charArray;
	}
	
}
