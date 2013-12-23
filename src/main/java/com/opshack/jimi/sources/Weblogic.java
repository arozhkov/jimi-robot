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
	
	
	@Override
	public synchronized boolean setMBeanServerConnection() {
		
		JMXConnector jmxConnector = null;
		try {

			JMXServiceURL serviceURL = new JMXServiceURL(
					"service:jmx:t3://" 
					+ this.getHost() + ":" 
					+ this.getPort() + "/jndi/weblogic.management.mbeanservers.runtime");

			log.debug(this + " serviceURL " + serviceURL);

			Map<String,Object> h = new HashMap<String, Object>();

			h.put(Context.SECURITY_PRINCIPAL, this.getUsername());
			h.put(Context.SECURITY_CREDENTIALS, this.getPassword());
			h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
			h.put("jmx.remote.x.request.waiting.timeout", Long.valueOf(this.jimi.getSourceConnectionTimeout()));

			log.info(this + " open " + serviceURL);
			jmxConnector = JMXConnectorFactory.newJMXConnector(serviceURL, h);
			jmxConnector.connect();
			this.mbeanServerConnection = jmxConnector.getMBeanServerConnection();

		} catch (Exception e) {

			log.error(this + " " + e.getMessage() + " in setMBeanServerConnection");
			if (log.isDebugEnabled()) {
				e.printStackTrace();
			}

			this.mbeanServerConnection = null;
			if (jmxConnector != null) {

				try {
					jmxConnector.close();				
				} catch (Exception e1) {
					log.error(this + " " + e1.getMessage() + " after Exception in setMBeanServerConnection");
					e1.printStackTrace();
				}
			}	
			
			return false;
		}
		return true;
	}
}
