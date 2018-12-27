package com.ballyts.datalistener;

public class ShutdownThread extends Thread {
	
	DataListenerApp listenerApp;

	public ShutdownThread(DataListenerApp listenerApp){
		this.listenerApp = listenerApp;
	}
	
	public void run(){
		listenerApp.stopListening();
	}
}
