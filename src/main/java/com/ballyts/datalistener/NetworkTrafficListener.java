package com.ballyts.datalistener;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Rfc1349Tos;
import org.pcap4j.packet.Packet;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ballyts.datalistener.PacketCaptureInfo.IPDirection;
import com.ballyts.datalistener.util.PropertyReader;

@Component("NetworkTrafficListener")
@Scope("prototype")
public class NetworkTrafficListener implements DataListenerInterface, Runnable {
	
	private static Logger logger = LogManager.getLogger(NetworkTrafficListener.class);
	
	//private static PropertyReader propertyReader = PropertyReader.getPropertyReader();
	
	private PcapNetworkInterface nic;
	
	private PcapHandle handle;
	
	private PacketListener packetListener;
	
	private PacketCaptureInfo captureInfo;
	
	private boolean stopCapture = false;
	
	private Thread captureThread;
	
	private Timestamp lastCaptured;
	
	private int failedTries=0;
	
	public NetworkTrafficListener(PacketCaptureInfo captureInfo) throws UnknownHostException{
		this.captureInfo = captureInfo;
		initialize(this.captureInfo);
	}
	
	/**
	 * Get Network Interface by IP Address. If no InetAddress is provided, 
	 * auto select the first interface.
	 * 
	 * @param ipAddress
	 * @return PcapNetworkInterface. Null if interface cannot be obtained. 
	 */
	public PcapNetworkInterface getNetworkInterface(InetAddress ipAddress){
		try {
			PcapNetworkInterface nif;
			if (ipAddress == null) {
				logger.info("No IP Address provided, auto selecting one..");
				List<PcapNetworkInterface> nifList = Pcaps.findAllDevs();
				if (nifList == null || nifList.isEmpty()){
					logger.error("No network interfaces available");
					return null;
				}
				nif = nifList.get(0);
				logger.info("Choosing interface with MAC address "+nif.getLinkLayerAddresses().get(0)); //can have multiple IPs associated, including IPv6
				return nif;
			} else
				nif = Pcaps.getDevByAddress(ipAddress);
				if (nif == null)
					logger.info("Couldnt locate network interface for IP: "+ipAddress.getHostAddress());
				else
					logger.debug("Choosing interface with MAC address "+nif.getLinkLayerAddresses().get(0));
				return nif;
		} catch (PcapNativeException pne) {
			logger.error("Error obtaining Network Interface Device. "+pne.getMessage());
			return null;
		}
	}
	
	public PcapHandle getDeviceHandle(PacketCaptureInfo captureInfo){
		PcapHandle handle;
		try {
			handle = nic.openLive(captureInfo.getSnaplen(), captureInfo.getMode(), captureInfo.getCaptureTimeout());
			logger.debug("Obtained handle ");
			String filter = captureInfo.getFilter();
/*			
			if (captureInfo.getIpAddress() != null && !captureInfo.getIpAddress().isEmpty())
				filter = ((captureInfo.getDirection() != null)? captureInfo.getDirection()+" ":"")+
							"host "+captureInfo.getIpAddress();
			if (captureInfo.getPort() != 0)
				filter = (filter.isEmpty())? "port "+captureInfo.getPort(): filter+" && port "+captureInfo.getPort();
*/				
			if (filter!=null) {
				handle.setFilter(filter, BpfCompileMode.OPTIMIZE);
				logger.debug("Filter set as: "+filter);
			}
			return handle;
		} catch (PcapNativeException pne) {
			logger.error("Error obtaining handle to the NIC."+pne.getMessage());
			return null;
		} catch (NotOpenException noe) {
			logger.error("Error setting filter for the NIC handle."+noe.getMessage());
			return null;
		}
	}
	
	public int capturePackets(PacketCaptureInfo captureInfo){
		int capturedCount=0;
		try {
System.out.println("***Starting dispatch****");
			capturedCount = handle.dispatch(-1, packetListener);
			//handle.loop(-1, packetListener);
			lastCaptured = handle.getTimestamp();
System.out.println("***returned from dispatch****"+lastCaptured);
		} catch (PcapNativeException e) {
			logger.error("Pcap Native Error while capturing packets: "+e.getMessage());
			return -1;
		} catch (InterruptedException e) {
			logger.error("Interrupted Error while capturing packets: "+e.getMessage());
			return -2;
		} catch (NotOpenException e) {
			logger.error("Not Open Error while capturing packets: "+e.getMessage());
			return -1;
		}
		return capturedCount;
	}
	
	public void initialize(PacketCaptureInfo captureInfo) throws UnknownHostException{	
		String ipAddress = captureInfo.getIpAddress();
//TODO	More restrictive IP address validation when not null/empty		
		nic = getNetworkInterface((ipAddress == null)?null:InetAddress.getByName(ipAddress));
		handle = getDeviceHandle(captureInfo);
		
		packetListener = new PacketListener() {
			public void gotPacket(Packet packet) {
		//		IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
		//		Inet4Address destAddress = ipV4Packet.getHeader().getDstAddr();			
		//		logger.debug(handle.getTimestamp()+" "+destAddress.getHostAddress()+" "+packet);
		//System.out.println(handle.getTimestamp()+" "+destAddress.getHostAddress()+" "+packet);
		//  System.out.println("Packet processing done by: "+Thread.currentThread().getName());
			}
		};
	}
	
	public void dataFlowCheck() throws UnknownHostException {	
		while(!stopCapture){
			int capturedCount = capturePackets(captureInfo);
			switch (capturedCount){
			case -2:
				logger.info("Gracefully stopping capture");
				handle.close();
//TODO			Set exit flag?				
				break;
			case -1:
				try {
					logger.info("Encountered error while capturing packets: "+handle.getError());
				} catch (NotOpenException e) {
					logger.error("Could not recover error status as handle is already closed."+e.getMessage());
				}
				//Close and Reinitialize
				if (captureInfo.getMaxCaptureFailedAttempts() != 0 && ++failedTries > captureInfo.getMaxCaptureFailedAttempts()){
					stopCapture = true;
					logger.error("Error count exceded "+captureInfo.getMaxCaptureFailedAttempts()+" stopping capture.");
//TODO				Trigger email alert.						
				} else{
								handle.close();
								initialize(captureInfo);
				}
				break;
			default:
//				System.out.println(System.currentTimeMillis()+" "+lastCaptured.getTime()+" "+captureInfo.getCaptureTimeout());
//				if (lastCaptured == null || ((System.currentTimeMillis()-lastCaptured.getTime())> captureInfo.getCaptureTimeout())){
//				Since Timestamps between library and local dont seem to be in sync, using packet captured count. So if timeout occurred
//				after less than buffer full of packets were captured, alert will not be triggered, it will have to timeout again with 0
//				packets captured the next time to trigger the alert.				
				if (capturedCount == 0){
					logger.info("Captured packets: "+capturedCount+". No messages for the last "+
								captureInfo.getCaptureTimeout()/1000+" seconds.");
//TODO				Trigger email alert & go back to monitoring network flow.
				}
				System.out.println(capturedCount+" "+lastCaptured);
					
			}
			
		}
	}
	
	public void shutdown(){
		stopCapture = true;
		try {
			handle.breakLoop();
			logger.info("Handle has been requested to break the packet capture loop");
//			Capture can be stuck on IO wait if no packet is received. Kill after time wait			
			captureThread.join(1000);;
			if (captureThread.isAlive()) {
				captureThread.interrupt();
				logger.debug("Interrupting capture after waiting for break loop");
			}
		} catch (NotOpenException e) {
			logger.error("Open handle not available for a break request. Thread will be interrupted. "+e.getMessage());
			if (captureThread.isAlive())
				captureThread.interrupt();
		} catch (InterruptedException e) {
			logger.debug("Shutdown thread interrupted");
		}
	}
	
	public void run() {
		captureThread = Thread.currentThread();	
		captureThread.setName(captureThread.getName()+"-"+captureInfo.getFilter());
		try {
			dataFlowCheck();
		} catch (UnknownHostException e) {
			logger.error("Could not monitor network. "+e.getMessage());
		}
	}
	
	
	
	public static void main(String[] args) throws UnknownHostException, InterruptedException{
		
		PacketCaptureInfo capInfo = new PacketCaptureInfo();
		//capInfo.setIpAddress("172.30.44.16");
		//capInfo.setDirection(IPDirection.dst);
		capInfo.setIpAddress(null);
		capInfo.setCaptureTimeout(30000);
		capInfo.setSnaplen(1024);
		NetworkTrafficListener networkListener = new NetworkTrafficListener(capInfo);
		Thread capThread = new Thread(networkListener);
		capThread.start();
		System.out.println("Main thread sleeping");
		Thread.sleep(60000);
		System.out.println("Main thread awake. Stopping capture and will wait for capture thread"+new Date());
		networkListener.shutdown();
		capThread.join();
		System.out.println("Capture ended "+new Date());
		//networkListener.getNetworkInterface(InetAddress.getByName("10.115.171.109"));
		//networkListener.getNetworkInterface(null);
	}

}
