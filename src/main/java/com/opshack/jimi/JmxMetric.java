package com.opshack.jimi;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.sources.JmxSource;

public class JmxMetric implements Runnable {

	final private Logger log = LoggerFactory.getLogger(this.getClass());	
	
	private JmxSource source;
	private Map metric;
	private ObjectName objectName;
	private HashMap<String, ObjectInstance> beans = new HashMap<String, ObjectInstance>();
	
	
	public JmxMetric(JmxSource source, Map metric) throws IOException, MalformedObjectNameException, NullPointerException, InstanceNotFoundException {

		this.source = source;
		
		log.debug(this.source + " " + metric + " creat metric");
		this.metric = metric;
		
		this.objectName = new ObjectName((String) this.metric.get("mbean"));
		
		if (this.source.isConnected() && !this.source.isBroken()) {
			
			log.debug(this.source + " " + this.metric + " getting mbean(s)");
			Set<ObjectInstance> beans = new HashSet<ObjectInstance>();
			
			if (this.objectName.isPattern()) {
				
				beans.addAll(this.source.getMBeanServerConnection().queryMBeans(this.objectName, null));
				
			} else {
				
				beans.add(this.source.getMBeanServerConnection().getObjectInstance(this.objectName));
			}
			
			log.debug(this.source + " " + this.metric + " got " + beans.size() + " bean(s)");
			
			String filter = "";
			if (this.metric.get("filter") != null) {
				filter = (String) this.metric.get("filter");
			}
			
			String attr = (String) this.metric.get("attr");
			if (attr != null) {
				for (ObjectInstance bean: beans) {
					
					if (bean.getObjectName().toString().contains(filter)) { 	// filter out beans
						String label = getLabel(bean.getObjectName());
						this.beans.put(label, bean);
					}
				}
			}
		}
	}
	
	
	private String getLabel(ObjectName objectName) {
		
		String label = (String) this.metric.get("label");
		
		//TODO find a better way to manage Upper/Lower case in "name"
		String name = objectName.getKeyProperty("Name");
		if (name == null) name = objectName.getKeyProperty("name");

		log.debug("Name : " + name + "-" + objectName);
		
		if (name == null) {
			return label.replace("$Name", "Undefined");
		}
		
		return label.replace("$Name", name.replaceAll("\\W", ""));
	}
	
	
	public void run() {

		if (this.source.isConnected() && !this.source.isBroken()) {

			Set<String> labels = this.beans.keySet();
			for (String label: labels) {

				try {
				
					ObjectInstance bean = this.beans.get(label);	
					Object value = this.source.getMBeanServerConnection().getAttribute(bean.getObjectName(), (String) this.metric.get("attr"));

					if (value != null) {

						log.debug(this.source + " " + bean.getObjectName() + 
								" attribute " + this.metric.get("attr") +  " value is " + String.valueOf(value));

						if (value instanceof CompositeDataSupport) {

							Object subvalue = null;
							String subattr = (String) this.metric.get("subattr");

							if (subattr != null) {
								subvalue = ((CompositeDataSupport) value).get(subattr);

								if (subvalue != null && (subvalue instanceof Long || subvalue instanceof Integer)) {

									this.source.getWriter().write(
											new Event(this.source.toString(), label, String.valueOf(subvalue)));

								} else {
									log.error(this.source + " " + bean.getObjectName() + 
											" got UNSUPPORTED value " + String.valueOf(subvalue) );
								}

							} else {
								log.error(this.source + " " + bean.getObjectName() + 
										" attr is of CompositeData type, you have to provide subattr parameter.");
							}
				
						} else if (value instanceof Long || value instanceof Integer) {

							this.source.getWriter().write(
									new Event(this.source.toString(), label, String.valueOf(value)));

						} else  {
							log.error(this.source + " " + bean.getObjectName() + 
									" got UNSUPPORTED value " + String.valueOf(value) );
						}
					} 
					
				} catch (IOException e) {
					
					log.warn(this.source + " IOException: " + e.getMessage());

					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}

					this.source.setBroken(true); 								// source must be shutdown

				} catch (Exception e) {      
					
					log.warn(this.source + " non-IOException: " + e.getMessage());

					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
				}
			}
		}
	} 
}
	
