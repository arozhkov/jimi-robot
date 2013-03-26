package com.opshack.jimi.sources;

import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Weblogic extends Source {
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());
	private JMXConnector jmxConnector;
	
	
	@Override
	public synchronized void setMBeanServerConnection() throws InterruptedException {
		
		if (!super.isConnected()) {
			
			try {
				
				JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:t3://" 
						+ this.getHost() + ":" 
						+ this.getPort() + "/jndi/weblogic.management.mbeanservers.runtime");
				
				log.debug(this + " serviceURL " + serviceURL);
				
				Map<String,Object> h = new HashMap<String, Object>();
				
				h.put(Context.SECURITY_PRINCIPAL, this.getUsername());
				h.put(Context.SECURITY_CREDENTIALS, this.getPassword());
				h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
				h.put("jmx.remote.x.request.waiting.timeout", Long.valueOf(10000));
				
				this.jmxConnector = JMXConnectorFactory.newJMXConnector(serviceURL, h);
				this.jmxConnector.connect();
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
