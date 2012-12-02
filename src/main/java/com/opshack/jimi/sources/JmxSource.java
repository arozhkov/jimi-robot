package com.opshack.jimi.sources;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.InvalidKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;
import com.opshack.jimi.MetricGroups;
import com.opshack.jimi.writers.Writer;


public abstract class JmxSource implements Runnable{
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());	
	
	private String host;
	private int port;
	private String username;
	private String password;
	private List<String> metricGroupsList;
	
	private Writer writer;
	private MetricGroups metricGroups;
	
	private boolean broken = false;
	private boolean definitlyBroken = false;
	
	private MBeanServerConnection mbeanServerConnection;
	private ScheduledExecutorService metricExecutor;
	
	private HashSet <ScheduledFuture<?>> tasks;
	
	
	private class JmxMetric implements Runnable {

		private Map metric;
		private ObjectName objectName;
		
		JmxMetric(Map metric) {

			log.debug(JmxSource.this + " " + metric + " cteat metric");
			this.metric = metric;
			try {
				this.objectName = new ObjectName((String) this.metric.get("mbean"));
				
			} catch (MalformedObjectNameException e) {
				log.error(JmxSource.this + " " + this.objectName + " " + e.getMessage());
				e.printStackTrace();
				
				try {
					throw new InterruptedException();
				} catch (InterruptedException e1) {
					log.error(JmxSource.this + " " + this.objectName + " " + e.getMessage());
				}
				
			} catch (NullPointerException e) {
				log.error(JmxSource.this + " " + this.objectName + " " + e.getMessage());
				e.printStackTrace();
				
				try {
					throw new InterruptedException();
				} catch (InterruptedException e1) {
					log.error(JmxSource.this + " " + this.objectName + " " + e.getMessage());
				}
			}
		}
		
		private String getLabel(ObjectName objectName) {
			
			String label = (String) this.metric.get("label");
			
			//TODO find a better way to manage Upper/Lower case in "name"
			String name = objectName.getKeyProperty("Name");
			if (name == null) name = objectName.getKeyProperty("name");

			log.debug("Name : " + name + "-" + objectName);
			
			if (name == null) return label;
			return label.replace("$Name", name.replaceAll("\\W", ""));
		}
		
		public void run() {
			
			if (Thread.interrupted()) {
				
			    try {
					throw new InterruptedException();
					
				} catch (InterruptedException e) {
					log.error(JmxSource.this + " " + this.objectName + " " + e.getMessage());
					e.printStackTrace();
				}
			}
			
			if (isConnected()) {
				
				log.debug(JmxSource.this + " " + this.metric + " getting mbean(s)");
				Set<ObjectInstance> beans = new HashSet<ObjectInstance>();
				
				try {
					if (this.objectName.isPattern()) {
						beans.addAll(mbeanServerConnection.queryMBeans(this.objectName, null));
					} else {
						beans.add(mbeanServerConnection.getObjectInstance(this.objectName));
					}
					log.debug(JmxSource.this + " " + this.metric + " got " + beans.size() + " bean(s)");
					
					String filter = "";
					if (this.metric.get("filter") != null) {
						filter = (String) this.metric.get("filter");
					} 

					log.debug(JmxSource.this + " " + this.metric + " apply filter '" + filter + "'");
					for (ObjectInstance bean: beans) {

						Object value = null;
						String attr = (String) this.metric.get("attr");
						
						if (bean.getObjectName().toString().contains(filter) && attr != null) {
							
							value = mbeanServerConnection.getAttribute(bean.getObjectName(), attr);
							
							if (value != null) {
								
								log.debug(JmxSource.this + " " + bean.getObjectName() + 
										" attribute " + this.metric.get("attr") +  " value is " + String.valueOf(value));
								
								if (value instanceof Long || value instanceof Integer) {
									
									JmxSource.this.writer.write(
											new Event(JmxSource.this.toString(), getLabel(bean.getObjectName()), String.valueOf(value)));
									
								} else if (value instanceof CompositeDataSupport) {
									
									Object subvalue = null;
									String subattr = (String) this.metric.get("subattr");
									
									if (subattr != null) {
										subvalue = ((CompositeDataSupport) value).get(subattr);

										if (subvalue != null && (subvalue instanceof Long || subvalue instanceof Integer)) {

											JmxSource.this.writer.write(
													new Event(JmxSource.this.toString(), getLabel(bean.getObjectName()), String.valueOf(subvalue)));
											
										} else {
											log.warn(JmxSource.this + " " + bean.getObjectName() + 
													" got UNSUPPORTED value " + String.valueOf(subvalue) );
										}
										
									} else {
										log.warn(JmxSource.this + " " + bean.getObjectName() + 
												" got CompositeData value, you have to provide subattr parameter.");
									}
								
								} else {
									log.warn(JmxSource.this + " " + bean.getObjectName() + 
											" got UNSUPPORTED value " + String.valueOf(value) );
								}
							} 
						}
					}

				} catch (AttributeNotFoundException e) {			
					log.warn(JmxSource.this + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());
					
					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
					
				} catch (InstanceNotFoundException e) {
					log.warn(JmxSource.this + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());
					
					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
					

				} catch (MBeanException e) {
					log.warn(JmxSource.this + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());
					
					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
					
				} catch (ReflectionException e) {
					log.warn(JmxSource.this + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());
					
					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
					
				} catch (IllegalStateException e) {
					log.debug(JmxSource.this + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());
					
					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
					
				}  catch (InvalidKeyException e) {
					log.warn(JmxSource.this + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());
					
					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
					
				} catch (Exception e) {
					log.error(JmxSource.this + " " + this.objectName + " " + e.getClass().getName() + ": " + e.getMessage());
					
					if (log.isDebugEnabled()) {
						e.printStackTrace();
					}
					
					JmxSource.this.setBroken(true); // source must be shutdown
					
					try {
						throw new InterruptedException("after " + e.getMessage());
					} catch (InterruptedException e1) {
						log.error(JmxSource.this + " " + this.objectName + " " + e1.getMessage());
					}
					
				}
			} else {
				log.error(JmxSource.this + " is not connected");
			}
		}
		
	}
		
	
	public void run() {
		
		log.info(this + " start");
		
		try {
			this.mbeanServerConnection = getMbeanServerConnection(); // connect to source
			
		} catch (InterruptedException e) {
			
			log.error(JmxSource.this + " " + e.getMessage());
			e.printStackTrace();
			this.setBroken(true);
			
			try {
				throw new InterruptedException();
			} catch (InterruptedException e1) {
				log.error(JmxSource.this + " " + e1.getClass());
			}
		}
		
		if (this.isConnected() && !this.isBroken()) {
			
			for (String list: this.metricGroupsList) {
				
				List<Map> metrics = this.metricGroups.get(list);
				if (metrics != null && metrics.size() > 0) {
					
					for (Map metric: metrics) {
						
						try {
							
							JmxMetric jmxMetric = new JmxMetric(metric);
							tasks.add( 
									metricExecutor.scheduleAtFixedRate(jmxMetric,
									0,
									Long.valueOf((Integer) metric.get("rate")), 
									TimeUnit.SECONDS)
							);
							
						} catch (Exception e) {
							log.error(JmxSource.this + " " + e.getMessage());
							if (log.isDebugEnabled()) {
								e.printStackTrace();
							}
							
							JmxSource.this.setBroken(true); // source must be shutdown
							
							try {
								throw new InterruptedException();
							} catch (InterruptedException e1) {
								log.error(JmxSource.this + " " + e1.getMessage());
							}
						}
					}
				}
			}
		}
		
		log.debug(this + " terminates");
	}
	
	
	// TODO make object validation during "init" and return real status of
	// execution
	public boolean init(Writer writer, MetricGroups metrics, ScheduledExecutorService taskExecutor) {

		log.info("Init " + this);
		this.writer = writer;
		this.metricGroups = metrics;
		
		metricExecutor = taskExecutor;
		this.setBroken(false);
		
		tasks = new HashSet<ScheduledFuture<?>>();

		return true;
	}

	public void shutdown() {
	
		for (ScheduledFuture<?> task: tasks) {
			task.cancel(true);
			log.info("Cancel task, result is " + task.isCancelled());
		}

		//this.metricExecutor.shutdownNow();
		this.mbeanServerConnection = null;
	}

	public abstract MBeanServerConnection getMbeanServerConnection() throws InterruptedException;
	
	public boolean isConnected() {
		
		if (this.mbeanServerConnection == null) {
			return false;
		}
		return true;
	}
	
	public String toString() {
		return host.replaceAll("\\.", "_") + "_" + port;
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
	
	public List<String> getMetricsLists() {
		return metricGroupsList;
	}
	public void setMetricsLists(List<String> metricsLists) {
		this.metricGroupsList = metricsLists;
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
	
	
}
