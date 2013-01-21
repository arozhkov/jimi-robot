package com.opshack.jimi.sources;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.InvalidKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public class JmxMetric implements Runnable {

	final private Logger log = LoggerFactory.getLogger(this.getClass());	
	
	private JmxSource source;
	private Map metric;
	private ObjectName objectName;
	private HashMap<String, ObjectInstance> beans = new HashMap<String, ObjectInstance>();
	
	JmxMetric(JmxSource source, Map metric) throws IOException {

		this.source = source;
		
		log.debug(this.source + " " + metric + " creat metric");
		this.metric = metric;
		try {
			this.objectName = new ObjectName((String) this.metric.get("mbean"));
			
		} catch (MalformedObjectNameException e) {
			log.error(this.source + " " + this.objectName + " " + e.getMessage());
			e.printStackTrace();
			
			try {
				throw new InterruptedException();
			} catch (InterruptedException e1) {
				log.error(this.source + " " + this.objectName + " " + e.getMessage());
			}
			
		} catch (NullPointerException e) {
			log.error(this.source + " " + this.objectName + " " + e.getMessage());
			e.printStackTrace();
			
			try {
				throw new InterruptedException();
			} catch (InterruptedException e1) {
				log.error(this.source + " " + this.objectName + " " + e.getMessage());
			}
		}
		
		if (this.source.isConnected() && !this.source.isBroken()) {
			
			log.debug(this.source + " " + this.metric + " getting mbean(s)");
			Set<ObjectInstance> beans = new HashSet<ObjectInstance>();
			
			if (this.objectName.isPattern()) {
				
				beans.addAll(this.source.getMBeanServerConnection().queryMBeans(this.objectName, null));
				
			} else {
				
				try {
					
					beans.add(this.source.getMBeanServerConnection().getObjectInstance(this.objectName));
					
				} catch (InstanceNotFoundException e) {
					
					log.warn(this.source + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());

					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
				}
			}
			log.debug(this.source + " " + this.metric + " got " + beans.size() + " bean(s)");
			
			String filter = "";
			if (this.metric.get("filter") != null) {
				filter = (String) this.metric.get("filter");
			}
			
			String attr = (String) this.metric.get("attr");
			if (attr != null) {
				for (ObjectInstance bean: beans) {
					
					if (bean.getObjectName().toString().contains(filter)) { // filter out beans
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

		if (Thread.interrupted()) {

			try {
				throw new InterruptedException();

			} catch (InterruptedException e) {
				log.error(this.source + " " + this.objectName + " " + e.getMessage());
				e.printStackTrace();
			}
		}

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
									log.warn(this.source + " " + bean.getObjectName() + 
											" got UNSUPPORTED value " + String.valueOf(subvalue) );
								}

							} else {
								log.warn(this.source + " " + bean.getObjectName() + 
										" attr is of CompositeData type, you have to provide subattr parameter.");
							}
				
						} else if (value instanceof Long || value instanceof Integer) {

							this.source.getWriter().write(
									new Event(this.source.toString(), label, String.valueOf(value)));

						} else  {
							log.warn(this.source + " " + bean.getObjectName() + 
									" got UNSUPPORTED value " + String.valueOf(value) );
						}
					} 
		
				} catch (AttributeNotFoundException e) {      
					log.warn(this.source + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());

					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
				} catch (MBeanException e) {
					log.warn(this.source + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());

					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}

				} catch (ReflectionException e) {
					log.warn(this.source + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());

					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}

				} catch (IllegalStateException e) {
					log.debug(this.source + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());

					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}

				}  catch (InvalidKeyException e) {
					log.warn(this.source + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());

					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
					
				} catch (Exception e) {
					log.error(this.source + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());

					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}

					this.source.setBroken(true); // source must be shutdown

					try {
						throw new InterruptedException("after " + e.getMessage());
					} catch (InterruptedException e1) {
						log.error(this.source + " " + this.objectName + " " + e1.getMessage());
					}

				}
			}
			
		} else {
			
			log.error(this.source + " is not connected");
			this.source.setBroken(true); // source must be shutdown
			
			try {
				throw new InterruptedException("Source is not connected");
			} catch (InterruptedException e) {
				log.error(this.source + " " + e.getMessage());
			}
		}
	} 
}
	
