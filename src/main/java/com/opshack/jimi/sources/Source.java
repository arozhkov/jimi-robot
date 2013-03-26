package com.opshack.jimi.sources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Jimi;
import com.opshack.jimi.Metric;
import com.opshack.jimi.writers.Writer;


public abstract class Source implements Runnable{
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());	
	
	private String host;
	private int port;
	private String username;
	private String password;
	private LinkedHashMap<String, Object> props = new LinkedHashMap<String, Object>();
	private String propsMBean;
	private List<String> metrics;
	private String label;
	
	protected Jimi jimi;
	
	private boolean broken = false;
	private boolean definitlyBroken = false;
	
	protected MBeanServerConnection mbeanServerConnection;
	
	//protected HashMap<String,Object> props = new HashMap<String, Object>();
	
	private HashSet <ScheduledFuture<?>> tasks;
	
	public abstract void setMBeanServerConnection() throws InterruptedException;
	
	public MBeanServerConnection getMBeanServerConnection() {
		return this.mbeanServerConnection;
	}
	
	public void run() {
	
			
		for (String group: this.metrics) {

			ArrayList<Map> metricDefs = (ArrayList) this.jimi.metricGroups.get(group);
			if (metricDefs != null && metricDefs.size() > 0) {

				for (Map metricDef: metricDefs) {

					if (this.isConnected() && !this.isBroken()) {				// 
						
						try {

							Metric metric = new Metric(this, metricDef); 		// create JMX metric

							this.tasks.add( 									// schedule JMX metric
									this.jimi.taskExecutor.scheduleAtFixedRate(metric,
											10,
											Long.valueOf((Integer) metricDef.get("rate")), 
											TimeUnit.SECONDS)
									);

						} catch (IOException e) { 								// if IO exception occurred

							log.warn(this + " IOException: " + e.getMessage()); // print warning message

							if (log.isDebugEnabled()) {
								e.printStackTrace();
							}

							this.setBroken(true); 								// break the source
							break; 												// break the loop

						} catch (Exception e) { 								// if not an IO exception just skip metric creation
							log.error(this + " non-IOException: " + e.getMessage());
						}
						
					}
				}
			}
		}
			
			log.info(this + " tasks are initiated");
		}
	
	public boolean init(Jimi jimi) {

		this.setLabel();
		this.jimi = jimi;
		this.setBroken(false);
		
		try {
			
			setMBeanServerConnection(); 										// connect to source
		
		// TODO handle non IO exceptions as definitlyBroken	
		} catch (Exception e) { 												// if failed 
			
			if (log.isDebugEnabled()) {
				e.printStackTrace();
			}
			
			log.warn(this + " " + e.getMessage()); 								// print warning message
			try {
				
				Thread.sleep(30000); 											// block thread for 30 seconds
				
			} catch (InterruptedException e1) {}								// do nothing, anyway it's going to exit
			
			this.setBroken(true);												// break the source
		}

		
		if (this.getPropsMBean() != null && !this.getPropsMBean().isEmpty()) {
			setProperties();
		}
		
		this.tasks = new HashSet<ScheduledFuture<?>>();

		return true;
	}
	
	
	protected void setProperties() {

		try {
			
			ObjectName objectName = new ObjectName(this.getPropsMBean());

			Set<ObjectInstance> objectInstances = this.getMBeanServerConnection().queryMBeans(objectName, null);

			if (objectInstances!= null && !objectInstances.isEmpty()) {

				for (ObjectInstance obj: objectInstances) {
					
					log.info(this + " set properties from MBean: " + obj.getObjectName());
					MBeanAttributeInfo[] attributes = this.getMBeanServerConnection().getMBeanInfo(obj.getObjectName()).getAttributes();
					
					for (MBeanAttributeInfo attribute: attributes) {
						
						String attributeName = attribute.getName();
						Object value = this.getMBeanServerConnection().getAttribute(obj.getObjectName(), attributeName);
						
						this.props.put(attributeName, value);
						log.debug(this + " " + attributeName + " = " + value);
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

	public void shutdown() {
	
		for (ScheduledFuture<?> task: tasks) {
			task.cancel(true);
			log.debug(this + " task cancelation status is " + task.isCancelled());
		}
		
		this.mbeanServerConnection = null;
		log.info(this + " tasks are canceled");
	}
	
	public boolean isConnected() {
		
		if (this.mbeanServerConnection == null) {
			return false;
		}
		return true;
	}
	
	public String toString() {
		return this.label;
	}
	

	// getters and setters, boring staff
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public List<String> getMetrics() {
		return metrics;
	}
	public void setMetrics(List<String> metrics) {
		this.metrics = metrics;
	}


	public synchronized boolean isBroken() {
		return broken;
	}
	public synchronized void setBroken(boolean broken) {
		this.broken = broken;
	}

	public synchronized boolean isDefinitlyBroken() {
		return definitlyBroken;
	}
	public synchronized void setDefinitlyBroken(boolean definitlyBroken) {
		this.definitlyBroken = definitlyBroken;
	}

	public String getPropsMBean() {
		return propsMBean;
	}

	public void setPropsMBean(String propsMBean) {
		this.propsMBean = propsMBean;
	}
	
	public LinkedHashMap<String, Object> getProps() {
		return props;
	}
	
	public Object get(String property) {
		return props.get(property);
	}

	public void setProps(LinkedHashMap<String, Object> props) {
		this.props = props;
	}

	public void setLabel() {

		this.label = this.getHost().replaceAll("\\.", "_") + "_" + this.getPort();
	}

	public String getLabel() {
		return label;
	}

	public ArrayList<Writer> getWriters() {
		return this.jimi.getWriters();
	}
}
