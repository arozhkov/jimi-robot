package com.opshack.jimi.sources;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.MetricGroups;
import com.opshack.jimi.writers.Writer;


public abstract class JmxSource implements Runnable{
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());	
	
	private String host;
	private int port;
	private String username;
	private String password;
	private String prefix;
	private String suffix; 
	private List<String> metricGroupsList;
	
	private Writer writer;
	private MetricGroups metricGroups;
	
	private boolean broken = false;
	private boolean definitlyBroken = false;
	
	protected MBeanServerConnection mbeanServerConnection;
	private ScheduledExecutorService metricExecutor;
	
	private HashSet <ScheduledFuture<?>> tasks;
	
	private String label; 
	
	
	public abstract void setMBeanServerConnection() throws InterruptedException;
	
	public MBeanServerConnection getMBeanServerConnection() {
		return this.mbeanServerConnection;
	}
	
	public void run() {
		
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
		
		
		if (this.isConnected() && !this.isBroken()) {
			
			for (String list: this.metricGroupsList) {
				
				List<Map> metrics = this.metricGroups.get(list);
				if (metrics != null && metrics.size() > 0) {
					
					for (Map metric: metrics) {
						
						try {
							
							JmxMetric jmxMetric = new JmxMetric(this, metric); 	// create JMX metric
							
							tasks.add( 											// schedule JMX metric
									metricExecutor.scheduleAtFixedRate(jmxMetric,
									0,
									Long.valueOf((Integer) metric.get("rate")), 
									TimeUnit.SECONDS)
							);

						} catch (IOException e) { 								// if IO exception occurred
							
							log.warn(this + " " + e.getMessage()); 				// print warning message
							
							if (log.isDebugEnabled()) {
								e.printStackTrace();
							}
							
							this.setBroken(true); 								// break the source
							break; 												// break the loop
							
						} catch (Exception e) { 								// if not an IO exception just skip metric creation
							log.error(this + " " + e.getMessage());
						}
					}
				}
			}
			
			log.info(this + " tasks are initiated");
		}
	}
	
	
	// TODO make object validation during "init" and return real status of
	// execution
	public boolean init(Writer writer, MetricGroups metrics, ScheduledExecutorService taskExecutor) {

		
		StringBuffer buffer = new StringBuffer();
		
		if (this.prefix != null) {
			buffer.append(prefix + ".");
		}
		
		buffer.append(host.replaceAll("\\.", "_") + "_" + port);
		
		if (this.suffix != null) {
			buffer.append("." + suffix);
		}
		
		this.setLabel(buffer.toString());

		this.writer = writer;
		this.metricGroups = metrics;
		
		metricExecutor = taskExecutor;
		this.setBroken(false);
		
		tasks = new HashSet<ScheduledFuture<?>>();
		log.info(this + " initialized");

		return true;
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
		return this.getLabel();
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

	public synchronized String getPrefix() {
		return prefix;
	}
	public synchronized void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public synchronized String getSuffix() {
		return suffix;
	}
	public synchronized void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public synchronized String getLabel() {
		return label;
	}
	public synchronized void setLabel(String label) {
		this.label = label;
	}


	public synchronized Writer getWriter() {
		return writer;
	}	
	
	
}
