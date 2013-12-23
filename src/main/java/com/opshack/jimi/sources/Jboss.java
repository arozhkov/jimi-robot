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
	public synchronized boolean setMBeanServerConnection() {

		JMXConnector jmxConnector = null;
		
		try {

			JMXServiceURL serviceURL = new JMXServiceURL(
					"service:jmx:remoting-jmx://" 
					+ this.getHost() + ":"
					+ this.getPort());

			log.debug(this + " serviceURL " + serviceURL);

			Map<String, Object> h = new HashMap<String, Object>();

			String[] credentials = new String[2];
			credentials[0] = this.getUsername();
			credentials[1] = this.getPassword();

			h.put(JMXConnector.CREDENTIALS, credentials);

			log.debug(this + " connecting... ");
			jmxConnector = JMXConnectorFactory.connect(serviceURL, h);
			this.mbeanServerConnection = jmxConnector.getMBeanServerConnection();

		} catch (Exception e) {

			if (log.isDebugEnabled()) {
				e.printStackTrace();
			}

			this.mbeanServerConnection = null;
			if (jmxConnector != null) {

				try {
					jmxConnector.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			return false;
		}
		return true;
	}
}
