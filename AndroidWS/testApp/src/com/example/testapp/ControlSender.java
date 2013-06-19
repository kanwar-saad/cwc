package com.example.testapp;

public class ControlSender extends Thread {

	private static final String TAG = "CtrlRxThread";
	private static final int maxCtrlMsgLen = 1024;
	
	private UDPConnection CtrlChannel;
	private UDPConnection LogServer;
	
	public void run() {}
	
}
