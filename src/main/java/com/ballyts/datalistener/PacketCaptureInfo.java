package com.ballyts.datalistener;

import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;

public class PacketCaptureInfo {
	
	private String ipAddress="";
	
	public static enum IPDirection {
		src, dst
	};
	
	private IPDirection direction;
	
	private String protocol;
	
	private int port=0;
	
	private int snaplen=65535;
	
	private PromiscuousMode mode = PromiscuousMode.NONPROMISCUOUS; 
	
	private int captureTimeout=10000;
	
	private String filter;
	
	private int maxCaptureFailedAttempts;

	/**
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * @return the direction
	 */
	public IPDirection getDirection() {
		return direction;
	}

	/**
	 * @param direction the direction to set
	 */
	public void setDirection(IPDirection direction) {
		this.direction = direction;
	}

	/**
	 * @return the protocol
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * @param protocol the protocol to set
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the snaplen
	 */
	public int getSnaplen() {
		return snaplen;
	}

	/**
	 * @param snaplen the snaplen to set
	 */
	public void setSnaplen(int snaplen) {
		this.snaplen = snaplen;
	}

	

	/**
	 * @return the mode
	 */
	public PromiscuousMode getMode() {
		return mode;
	}

	/**
	 * @param mode the mode to set
	 */
	public void setMode(PromiscuousMode mode) {
		this.mode = mode;
	}

	/**
	 * @return the captureTimeout
	 */
	public int getCaptureTimeout() {
		return captureTimeout;
	}

	/**
	 * @param captureTimeout the captureTimeout to set
	 */
	public void setCaptureTimeout(int captureTimeout) {
		this.captureTimeout = captureTimeout;
	}
	
	/**
	 * 
	 * @return the capture filter string in Berkley Packet Filter Format.
	 */
	public String getFilter(){
//TODO	Limited filter construction, does not support protocol or || symbols etc.		
		if (direction != null)
			filter = direction.toString();
		if (ipAddress != null && !ipAddress.isEmpty())
			filter = ((filter != null)?" ":"")+ "host "+ ipAddress;
		if ( port != 0){
			if (filter != null) {
				if (filter.equals(direction.toString())){
					filter = filter + " port " + port;
				} else {
					filter = filter + " && port " + port;
				}
			} else
				filter = "port "+port;
		}
		return filter;
	}

	/**
	 * @return the maxCaptureFailedAttempts
	 */
	public int getMaxCaptureFailedAttempts() {
		return maxCaptureFailedAttempts;
	}

	/**
	 * @param maxCaptureFailedAttempts the maxCaptureFailedAttempts to set
	 */
	public void setMaxCaptureFailedAttempts(int maxCaptureFailedAttempts) {
		this.maxCaptureFailedAttempts = maxCaptureFailedAttempts;
	}

}
