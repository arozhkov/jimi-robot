package com.opshack.jimi.sources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Jimi;
import com.opshack.jimi.JmxMetric;
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
	
	private Jimi jimi;
	
	private boolean broken = false;
	private boolean definitlyBroken = false;
	
	protected MBeanServerConnection mbeanServerConnection;

	
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
		
			
		for (String group: this.metricGroupsList) {

			ArrayList<Map> metrics = (ArrayList) this.jimi.metricGroups.get(group);
			if (metrics != null && metrics.size() > 0) {

				for (Map metric: metrics) {

					if (this.isConnected() && !this.isBroken()) {				// 
						
						try {

							JmxMetric jmxMetric = new JmxMetric(this, metric); 	// create JMX metric

							this.tasks.add( 											// schedule JMX metric
									this.jimi.taskExecutor.scheduleAtFixedRate(jmxMetric,
											10,
											Long.valueOf((Integer) metric.get("rate")), 
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

		this.jimi = jimi;
		
		StringBuffer buffer = new StringBuffer();
		
		if (this.prefix != null) {
			buffer.append(prefix + ".");
		}
		
		buffer.append(host.replaceAll("\\.", "_") + "_" + port);
		
		if (this.suffix != null) {
			buffer.append("." + suffix);
		}
		
		this.setLabel(buffer.toString());

		this.setBroken(false);
		
		this.tasks = new HashSet<ScheduledFuture<?>>();
		log.info(this + " is initialized");

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

	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSuffix() {
		return suffix;
	}
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	public Writer getWriter() {
		return this.jimi.getWriter();
	}
}
