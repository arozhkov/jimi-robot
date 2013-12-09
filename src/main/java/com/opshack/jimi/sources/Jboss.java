package com.opshack.jimi.sources;

import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Jboss extends Source {
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());

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

				log.debug(this + " connecting... ");
				this.jmxConnector = JMXConnectorFactory.connect(serviceURL, h);
				this.mbeanServerConnection = this.jmxConnector.getMBeanServerConnection();

			} catch (Exception e) {
				
				this.setSourceState(SourceState.BROKEN);
				
				if (log.isDebugEnabled()) {
					e.printStackTrace();
				}

				this.mbeanServerConnection = null;

				if (this.jmxConnector != null) {
					
					try {
						this.jmxConnector.close();
						
					} catch (Exception e1) {

						e1.printStackTrace();
					}
				}
				
				throw new InterruptedException(e.getMessage());
			}

			this.setSourceState(SourceState.CONNECTED);
			
		} else {
			
			log.warn(this + " is already connected");
		}
	}
}
