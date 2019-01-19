package com.ballyts.datalistener;

public class ShutdownThread extends Thread {
	
	DataListener listenerApp;

	public ShutdownThread(DataListener listenerApp){
		this.listenerApp = listenerApp;
	}
	
	public void run(){
		listenerApp.stopListening();
	}
}
