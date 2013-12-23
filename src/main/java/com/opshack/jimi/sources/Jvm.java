package com.opshack.jimi.sources;

import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jvm  extends Source {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public synchronized boolean setMBeanServerConnection() {

		JMXConnector jmxConnector = null;
		try {

			JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" 
					+ this.getHost() + ":" 
					+ this.getPort() + "/jmxrmi");

			log.debug(this + " serviceURL " + serviceURL);

			Map<String,Object> h = new HashMap<String, Object>();
			h.put("jmx.remote.x.request.waiting.timeout", Long.valueOf(this.jimi.getSourceConnectionTimeout()));

			jmxConnector =  JMXConnectorFactory.newJMXConnector(serviceURL, h);
			jmxConnector.connect();
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
