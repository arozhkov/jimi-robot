package com.opshack.jimi.sources;

import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jboss extends Source {
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());
	private JMXConnector jmxConnector;
	
	
	@Override
	public synchronized void setMBeanServerConnection() throws InterruptedException {
		
		if (!super.isConnected()) {
			
			try {
				
				JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:remoting-jmx://" 
						+ this.getHost() + ":" 
						+ this.getPort());
				
				log.debug(this + " serviceURL " + serviceURL);
				
				Map<String,Object> h = new HashMap<String, Object>();
				
				String[] credentials = new String[2];
				credentials[0] = this.getUsername();
				credentials[1] = this.getPassword();
		        
				h.put(JMXConnector.CREDENTIALS, credentials);
				h.put("jmx.remote.x.request.waiting.timeout", Long.valueOf(10000));
				
				this.jmxConnector = JMXConnectorFactory.connect(serviceURL, h);
				//this.jmxConnector.connect();
				this.mbeanServerConnection = this.jmxConnector.getMBeanServerConnection();

			} catch (Exception e) {
				
				if (log.isDebugEnabled()) {
					e.printStackTrace();
				}
				
				throw new InterruptedException(e.getMessage() + "; occurred during connection to JMX server");
				
			}

			log.info(this + " is connected");
			
		} else {
			log.warn(this + " is already connected");
		}
	}
}
