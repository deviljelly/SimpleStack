package com.paul-anderson.simplestack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Stack;

public class Receiver implements Runnable {

	public static boolean littleEndian = false;
	public static final int PROTOCOL_DEFINING_METHOD = 1;
	public static final int PROTOCOL_BEGIN_STACKS = 2;
	public static final int PROTOCOL_THREAD_STACK = 3;
	public static final int PROTOCOL_END_STACKS = 4;
	public static final int PROTOCOL_SIZEOF_METHODID = 5;
	public static final int PROTOCOL_SIZEOF_THREADID = 6;
	public static final int PROTOCOL_DELAY = 7;
	public static final int PROTOCOL_HERTZ = 8;

	public static final int COMMAND_FASTER = 10;
	public static final int COMMAND_SLOWER = 11;
	public static final int COMMAND_STOP = 12;
	public static final int COMMAND_START = 13;
	public static final int COMMAND_DISCONNECT = 14;
	
	public static final int JVMTI_THREAD_STATE_ALIVE = 0x0001;
	public static final int JVMTI_THREAD_STATE_TERMINATED = 0x0002;
	public static final int JVMTI_THREAD_STATE_RUNNABLE = 0x0004;
	public static final int JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 0x0400;
	public static final int JVMTI_THREAD_STATE_WAITING = 0x0080;
	public static final int JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 0x0010;
	public static final int JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 0x0020;
	public static final int JVMTI_THREAD_STATE_SLEEPING = 0x0040;
	public static final int JVMTI_THREAD_STATE_IN_OBJECT_WAIT = 0x0100;
	public static final int JVMTI_THREAD_STATE_PARKED = 0x0200;
	public static final int JVMTI_THREAD_STATE_SUSPENDED = 0x100000;
	public static final int JVMTI_THREAD_STATE_INTERRUPTED = 0x200000;
	public static final int JVMTI_THREAD_STATE_IN_NATIVE = 0x400000;
	public static final int JVMTI_THREAD_STATE_VENDOR_1 = 0x10000000;
	public static final int JVMTI_THREAD_STATE_VENDOR_2 = 0x20000000;
	public static final int JVMTI_THREAD_STATE_VENDOR_3 = 0x40000000;

	public static final int JVMTI_THREAD_WAITING = JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_INDEFINITELY | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT | JVMTI_THREAD_STATE_SLEEPING | JVMTI_THREAD_STATE_IN_OBJECT_WAIT  | JVMTI_THREAD_STATE_IN_NATIVE |JVMTI_THREAD_STATE_SUSPENDED|JVMTI_THREAD_STATE_PARKED| JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER;

	public static int sizeOfJMethodID = 0;
	public static int stacksSampled;
	public static int eventsConsumed;
	public static int numberOfThreads;
	public static long delay;
	public static int hertz;
	public OutputStream os;
	
//	public Stack<long[][]> drawStack = new Stack<long[][]>();

	public static Store store = null;
	
//	public static CallStackNode rootNode = new CallStackNode();

	boolean connected = true;

	private final static void read(InputStream is, byte[] buffer) throws IOException {

		is.read(buffer);
	}

	private final static int readInt8(InputStream is) throws IOException {

		return is.read();
	}

	private final static int readInt16(InputStream is) throws IOException {

		int byte0;
		int byte1;

		byte0 = is.read();
		byte1 = is.read();

		int value;

		if (littleEndian) {
			value = ((byte1 & 0xff) << 8) | (byte0 & 0xff);
		} else {
			value = ((byte0 & 0xff) << 8) | (byte1 & 0xff);
		}

		return value;

	}

	private final static int readInt32(InputStream is) throws IOException {

		int byte0;
		int byte1;
		int byte2;
		int byte3;

		byte0 = is.read();
		byte1 = is.read();
		byte2 = is.read();
		byte3 = is.read();

		int value;

		if (littleEndian) {
			value = (((byte3 & 0xff) << 24) | ((byte2 & 0xff) << 16) | ((byte1 & 0xff) << 8) | (byte0 & 0xff));
		} else {
			value = ((byte0 & 0xff) << 24) | ((byte1 & 0xff) << 16) | ((byte2 & 0xff) << 8) | ((byte3 & 0xff));
		}

		return value;

	}

	private final static long readUInt32(InputStream is) throws IOException {

		int byte0;
		int byte1;
		int byte2;
		int byte3;

		byte0 = is.read();
		byte1 = is.read();
		byte2 = is.read();
		byte3 = is.read();

		long value;

		if (littleEndian) {
			value = ((byte3 & 0xffL) << 24) | ((byte2 & 0xffL) << 16) | ((byte1 & 0xffL) << 8) | (byte0 & 0xffL);
		} else {
			value = ((byte0 & 0xffL) << 24) | ((byte1 & 0xffL) << 16) | ((byte2 & 0xffL) << 8) | ((byte3 & 0xffL));
		}

		return value;

	}

	private final static long readInt64(InputStream is) throws IOException {

		int byte0;
		int byte1;
		int byte2;
		int byte3;
		int byte4;
		int byte5;
		int byte6;
		int byte7;

		byte0 = is.read();
		byte1 = is.read();
		byte2 = is.read();
		byte3 = is.read();
		byte4 = is.read();
		byte5 = is.read();
		byte6 = is.read();
		byte7 = is.read();

		long value;

		if (littleEndian) {
			value = ((byte7 & 0xffL) << 56) | ((byte6 & 0xffL) << 48) | ((byte5 & 0xffL) << 40) | ((byte4 & 0xffL) << 32) | ((byte3 & 0xffL) << 24) | ((byte2 & 0xffL) << 16) | ((byte1 & 0xffL) << 8) | (byte0 & 0xffL);
		} else {
			value = ((byte0 & 0xffL) << 56) | ((byte1 & 0xffL) << 48) | ((byte2 & 0xffL) << 40) | ((byte3 & 0xffL) << 32) | ((byte4 & 0xffL) << 24) | ((byte5 & 0xffL) << 16) | ((byte6 & 0xffL) << 8) | (byte7 & 0xffL);
		}

		return value;

	}
	
	
	public void slower() {
	
		try{
			os.write(COMMAND_SLOWER);
			os.flush();
		} catch(Exception e) {
		System.err.println(e);	
		}
		
	}
	
	public void faster() {
	
		try{
			os.write(COMMAND_FASTER);
			os.flush();
		} catch(Exception e) {
		System.err.println(e);	
		}
		
	}

	public void run() {

		store = new Store();

		Socket socket = null;
		try {
		//	socket = new Socket("wsc4.washington.ibm.com", 12346);

	//		socket = new Socket("10.3.86.2", 12346);
					
			socket = new Socket("localhost", 12346);
			

			InputStream is = socket.getInputStream();

			os = socket.getOutputStream();

			while (connected) {
				int protocolInstruction = readInt8(is);

				// System.out.println("read " + protocolInstruction);
				switch (protocolInstruction) {
				case PROTOCOL_DELAY:
					delay = readInt64(is);
					break;
				case PROTOCOL_HERTZ:
					hertz = readInt32(is);
					break;
				case PROTOCOL_SIZEOF_METHODID:
					sizeOfJMethodID = readInt32(is);
					break;
					
				case PROTOCOL_DEFINING_METHOD:

					long methodID;
					if(sizeOfJMethodID==4) {
						methodID = readUInt32(is);
					} else {
						methodID = readInt64(is);
					}

					int classSignatureLength = readInt16(is);

					byte[] classSignatureArray = new byte[classSignatureLength];
					read(is, classSignatureArray);

					int methodNameLength = readInt16(is);
					byte[] methodNameArray = new byte[methodNameLength];
					read(is, methodNameArray);

					int methodSignatureLength = readInt16(is);
					byte[] methodSignatureArray = new byte[methodSignatureLength];
					read(is, methodSignatureArray);


					store.addMethod(methodID, Store.toCharArray(classSignatureArray), Store.toCharArray(methodNameArray), Store.toCharArray(methodSignatureArray));

					// System.out.println("DefineClass: " + new String(classSignatureArray) + " " + new String(methodNameArray) + " " + new String(methodSignatureArray));

					break;

				case PROTOCOL_BEGIN_STACKS:

				{

					int numberOfThreads = readInt16(is);
					Receiver.numberOfThreads = numberOfThreads;

//					long[][] threadsToDraw = new long[numberOfThreads][];

					for (int i = 0; i < numberOfThreads; i++) {
						eventsConsumed++;

						stacksSampled++;
						
						int instruction = readInt8(is);

						long threadID = readInt64(is);

						long threadState = readUInt32(is);

						int numberOfFrames = readInt16(is);

//						long[] frames = new long[2 + numberOfFrames];
						long[] altFrames = new long[numberOfFrames];

						int j = 0;

	//					frames[j++] = threadID;
	//					frames[j++] = threadState;

						for (; j < numberOfFrames; j++) {
							if(sizeOfJMethodID==4) {

								altFrames[j] = readUInt32(is);
							} else {
								altFrames[j] = readInt64(is);
							}
						//	altFrames[j - 2] = frames[j];
						}

						if (numberOfFrames > 0) {

							long mthID = altFrames[0];
							
							int method = store.getMethod(mthID);
							store.hit(method, altFrames, threadState);

//							int k = numberOfFrames - 1;


					//		CallStackNode theNode = rootNode.getOrAdd(altFrames[k]);

//							theNode.hit();
	//						for (; k >= 0; k--) {

	//							theNode = theNode.getOrAdd(altFrames[k]);
		//						theNode.hit();
		//					}

						}


					//	threadsToDraw[i] = frames;

					}

				//	drawStack.push(threadsToDraw);

					int instruction = readInt8(is);

				}

					break;

				default:
					is.close();
					socket.close();
					connected = false;
					break;
				}

			}
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}

	}

}
