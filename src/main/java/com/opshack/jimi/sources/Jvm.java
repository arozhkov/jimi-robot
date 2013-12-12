package com.opshack.jimi.sources;

import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jvm  extends Source {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public synchronized boolean setMBeanServerConnection() {

		try {

			JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" 
					+ this.getHost() + ":" 
					+ this.getPort() + "/jmxrmi");

			log.debug(this + " serviceURL " + serviceURL);

			Map<String,Object> h = new HashMap<String, Object>();
			h.put("jmx.remote.x.request.waiting.timeout", Long.valueOf(this.jimi.getSourceConnectionTimeout()));

			this.jmxConnector =  JMXConnectorFactory.newJMXConnector(serviceURL, h);
			this.jmxConnector.connect();
			this.mbeanServerConnection = this.jmxConnector.getMBeanServerConnection();

		} catch (Exception e) {

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

			return false;
		}
		return true;

	}
}
