package com.opshack.jimi.sources;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
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
			
			if (this.getPropsMBean() != null && !this.getPropsMBean().isEmpty()) {
				setProperties();
			}

			log.info(this + " is connected");
			
		} else {
			log.warn(this + " is already connected");
		}
	}
	
	private void setProperties() {

		try {
			ObjectName objectName = new ObjectName("com.bea:Type=ServerRuntime,*");

			Set<ObjectInstance> objectInstances = this.getMBeanServerConnection().queryMBeans(objectName, null);

			if (objectInstances!= null && !objectInstances.isEmpty()) {

				for (ObjectInstance obj: objectInstances) {
					
					log.info("MBean: " + obj.getObjectName());
					MBeanAttributeInfo[] attributes = this.getMBeanServerConnection().getMBeanInfo(obj.getObjectName()).getAttributes();
					
					for (MBeanAttributeInfo attribute: attributes) {
						
						String attributeName = attribute.getName();
						Object value = this.getMBeanServerConnection().getAttribute(obj.getObjectName(), attributeName);
						
						this.props.put(attributeName, value);
						log.debug("Attribute: " + attributeName + ", Value: " + value);
					}
					
					break;
				}
			}

		} catch (MalformedObjectNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AttributeNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstanceNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MBeanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReflectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IntrospectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
