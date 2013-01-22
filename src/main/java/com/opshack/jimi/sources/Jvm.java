package com.opshack.jimi.sources;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jvm  extends JmxSource {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private JMXConnector jmxConnector;


	@Override
	public synchronized void setMBeanServerConnection()	throws InterruptedException {

		if (!super.isConnected()) {

			try {
				
				JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" 
						+ this.getHost() + ":" 
						+ this.getPort() + "/jmxrmi");
				
				log.debug(this + " serviceURL " + serviceURL);
				
				this.jmxConnector =  JMXConnectorFactory.newJMXConnector(serviceURL, null);
				this.jmxConnector.connect();
				this.mbeanServerConnection = this.jmxConnector.getMBeanServerConnection();

			} catch (Exception e) {

				if (log.isDebugEnabled()) {
					e.printStackTrace();
				}
				
				throw new InterruptedException(e.getMessage() + "; occurred during connection to JMX server");
				
			}
			
			log.info(this + " is connected");
		}
		
		log.debug(this + " already connected");
	}
}
