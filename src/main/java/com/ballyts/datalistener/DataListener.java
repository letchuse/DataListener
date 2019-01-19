package com.ballyts.datalistener;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;

@Component
public class DataListener 
{
	private Logger logger = LogManager.getLogger(DataListener.class);
	
	private ExecutorService listenerThreads;
	
	public DataListener(){
		listenerThreads = Executors.newFixedThreadPool(5);
	}
	
	private List<PacketCaptureInfo> loadNetworkProperties() throws ConfigurationException{
		Configurations configs = new Configurations();
		XMLConfiguration xmlConfig = configs.xml("DataListenerProperties.xml");
		List<HierarchicalConfiguration<ImmutableNode>> subConfigs = xmlConfig.configurationsAt("Network");
		System.out.println("Network list size: "+ subConfigs.size());
		List<PacketCaptureInfo> packetCaptureList = new ArrayList<PacketCaptureInfo>();
		for(HierarchicalConfiguration<ImmutableNode> subConfig:subConfigs){
			PacketCaptureInfo captureInfo = new PacketCaptureInfo();
			captureInfo.setCaptureTimeout(30000);
			captureInfo.setIpAddress(subConfig.getString("[@IP_Address]",null));
			String direction = subConfig.getString("[@Direction]",null);
			if (direction!=null)
				captureInfo.setDirection(PacketCaptureInfo.IPDirection.valueOf(direction));
			captureInfo.setPort(subConfig.getInt("[@Port]",0));
			packetCaptureList.add(captureInfo);
		}
		return packetCaptureList;
	}
	
	@Lookup
	public NetworkTrafficListener getNetworkTrafficListener(PacketCaptureInfo captureInfo){
		return null;
	}
	
	@PostConstruct
	public void startListening(){
		try {
			List<PacketCaptureInfo> packetCaptureList = loadNetworkProperties();
			for(PacketCaptureInfo captureInfo:packetCaptureList){
				try {
					//listenerThreads.submit(new NetworkTrafficListener(captureInfo));
					listenerThreads.submit(getNetworkTrafficListener(captureInfo));
					logger.debug("Started Thread");
				//} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
				//	e.printStackTrace();
				} catch (Exception e){
//TODO				Add more detail on which thread
					logger.error("Error launching listener Thread "+e.getMessage());
				}
			}
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void stopListening(){
		try {
			logger.debug("Initiating shutdown sequence.");
			boolean terminated = listenerThreads.awaitTermination(30000, TimeUnit.MILLISECONDS);
			if (!terminated){
				logger.debug("Could not wait any longer, forcefully shutting down");
				listenerThreads.shutdownNow();
			}			
		} catch (InterruptedException ie) {
			// TODO Auto-generated catch block
			logger.error("Interrupted while shutting down. "+ie.getMessage());
		}
		
	}
	
    public static void main( String[] args ){
    	DataListener trafficListener = new DataListener();
    	trafficListener.startListening();
    	Runtime.getRuntime().addShutdownHook(new ShutdownThread(trafficListener));
    }
}
