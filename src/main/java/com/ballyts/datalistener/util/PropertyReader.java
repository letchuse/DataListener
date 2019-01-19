package com.ballyts.datalistener.util;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class PropertyReader {

	private static Properties properties;
	private static PropertyReader propertyReader = null;
	private static Logger logger = LogManager.getLogger(PropertyReader.class);

	static {
		try {
			properties = new Properties();
			properties.load(ClassLoader.getSystemClassLoader().getResourceAsStream("DataListenerInterface.properties"));
			logger.info("Properties loaded");
		}catch(Exception e){
			logger.error("Error loading DataListenerInterface.properties file. "+e.getMessage());
		}
	}

	private PropertyReader(){
	}

	public static PropertyReader getPropertyReader(){
		if (propertyReader == null){
			propertyReader = new PropertyReader();
		}
		return propertyReader;
	}

	public String getValue(String key){
		return properties.getProperty(key);
	}

	public int getIntValue(String key){
		return (Integer.parseInt(properties.getProperty(key)));
	}

}
