package com.opshack.jimi.sources;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeblogicDomain extends Source {
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());
	private String filter = "";
	
	private static MBeanServerConnection connection;
	private static JMXConnector connector;
	private static final ObjectName service;
	
	
	static {
		try {
			service = new ObjectName(
					"com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean");
		} catch (MalformedObjectNameException e) {
			throw new AssertionError(e.getMessage());
		}
	}
	
	
	@Override
	public boolean setMBeanServerConnection() {
		
		String protocol = "t3";
		int port = this.getPort();
		String jndiroot = "/jndi/";
		String mserver = "weblogic.management.mbeanservers.domainruntime";
		JMXServiceURL serviceURL;
		
		try {
			serviceURL = new JMXServiceURL(protocol, this.getHost(), port, jndiroot + mserver);
			
		} catch (MalformedURLException e) {
			
			log.error(this + " MalformedURLException : " + protocol + "://" + this.getHost() + ":"  + this.getPort() + mserver);
			return false;		
		}
		
		Hashtable h = new Hashtable();
		h.put(Context.SECURITY_PRINCIPAL, this.getUsername());
		h.put(Context.SECURITY_CREDENTIALS, this.getPassword());
		h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
		
		try {
			this.connector = JMXConnectorFactory.connect(serviceURL, h);
			this.connection = this.connector.getMBeanServerConnection();
			
		} catch (IOException e) {
			
			log.warn(this + " IO Exception occurred during connection to Weblogic Administration server");
			return false;	
		}
		return true;		
	}
	
	public static ObjectName[] getServerRuntimes() throws Exception {
	      return (ObjectName[]) connection.getAttribute(service, "ServerRuntimes");
	}
	
	public ArrayList<Source> getSources() throws Exception {
		
		ArrayList<Source> sources = new ArrayList<Source>();
		
		this.setMBeanServerConnection();
		ObjectName[] serverRT = getServerRuntimes();
		
		for (ObjectName server: serverRT) {
			
			Source source = new Weblogic();
			
			String name = (String) connection.getAttribute(server, "Name");
			log.debug("Weblogic name: " + name);
			
			if (name.contains(this.getFilter())) {
				
				String defaultUrl = (String) connection.getAttribute(server, "DefaultURL");
				log.debug(defaultUrl);
				URL url = new URL(defaultUrl.replace("t3", "http")); // replace t3 with http to avoid "MalformedURLException: unknown protocol: t3"
				
				source.setHost((String) url.getHost());
				log.debug("Weblogic host: " + source.getHost());
				
				source.setPort((Integer) url.getPort());
				log.debug("Weblogic port: " + source.getPort());
				
				source.setUsername(this.getUsername());
				source.setPassword(this.getPassword());
				
				source.setProps(new HashMap<String, Object>(this.getProps()));
				source.setPropsMBean(this.getPropsMBean());
				
				source.setMetrics(this.getMetrics());
				
				sources.add(source);
			}
			
		}
		
		this.connector.close();
		return sources;
	}

	
	public synchronized String getFilter() {
		return filter;
	}
	
	public synchronized void setFilter(String filter) {
		this.filter = filter;
	}
}
