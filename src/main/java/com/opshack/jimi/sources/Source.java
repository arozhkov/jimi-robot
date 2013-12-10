package com.opshack.jimi.sources;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Jimi;
import com.opshack.jimi.Metric;
import com.opshack.jimi.writers.Writer;


public abstract class Source implements Runnable {
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());	
	
	private String host;
	private int port;
	private String username;
	private String password;
	private HashMap<String, Object> props;
	private String propsMBean;
	private List<String> metrics;
	private String label;
	
	protected Jimi jimi;

	private boolean broken = true;
	private boolean definitlyBroken = true;
	private int countdown = 0;
	
	private SourceState sourceState = SourceState.INIT;
	
	protected JMXConnector jmxConnector;
	protected MBeanServerConnection mbeanServerConnection;
	
	private HashSet <ScheduledFuture<?>> tasks = new HashSet<ScheduledFuture<?>>();
	
	public abstract boolean setMBeanServerConnection();
	
	public MBeanServerConnection getMBeanServerConnection() {
		return this.mbeanServerConnection;
	}
	
	public void run() {
			
		if (this.getSourceState().equals(SourceState.CONNECTED)) {

			this.tasks = new HashSet<ScheduledFuture<?>>();

			for (String group: this.metrics) {

				ArrayList<Map> metricDefs = (ArrayList) this.jimi.metricGroups.get(group);
				if (metricDefs != null && metricDefs.size() > 0) {

					for (Map metricDef: metricDefs) {

						try {

							Metric metric = new Metric(this, metricDef); 		// create JMX metric

							this.tasks.add( 									// schedule JMX metric
									this.jimi.taskExecutor.scheduleAtFixedRate(metric,
											10,
											Long.valueOf((Integer) metricDef.get("rate")), 
											TimeUnit.SECONDS)
									);

						} catch (IOException e) { 								// if IO exception occurred

							this.setSourceState(SourceState.BROKEN);
							
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
			log.info(this + " metrics are scheduled");
		}
	}
	
	public void init(Jimi jimi) {

		this.setLabel();
		this.setJimi(jimi);
		this.setBroken(false);
		
		if (this.getProps() == null) {
			this.setProps(new HashMap<String, Object>());	
		}
		
		this.props.put("host", this.host);
		this.props.put("port", Integer.toString(this.port));
		this.props.put("label", this.label);

		if (this.isOnline()) {
			
			this.setSourceState(SourceState.ONLINE);
			
			if (this.setMBeanServerConnection()) {
				this.setSourceState(SourceState.CONNECTED);
				this.setPropertiesFromMBean();
			} else {
				this.setSourceState(SourceState.BROKEN);
			}

		} else {
			this.setSourceState(SourceState.OFFLINE);
		}

	}
	
	protected Boolean isOnline() {
		
		final Socket socket = new Socket();
		try {
			
			socket.connect(new InetSocketAddress(host, port), 2000);
			
		} catch (Exception e) {
			return false;
			
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
				//Ignore
			}
		}

		return true;
	}
	
	
	protected void setPropertiesFromMBean() {


		if ( this.getSourceState().equals(SourceState.CONNECTED) && this.getPropsMBean() != null && !this.getPropsMBean().isEmpty() ) {

			try {
				ObjectName objectName = new ObjectName(this.getPropsMBean());
				Set<ObjectInstance> objectInstances = this.getMBeanServerConnection().queryMBeans(objectName, null);

				if (objectInstances!= null && !objectInstances.isEmpty()) {

					for (ObjectInstance obj: objectInstances) {

						log.info(this + " set properties from " + obj.getObjectName());
						MBeanAttributeInfo[] attributes = this.getMBeanServerConnection().getMBeanInfo(obj.getObjectName()).getAttributes();

						for (MBeanAttributeInfo attribute: attributes) {

							String attributeName = attribute.getName();
							Object value = this.getMBeanServerConnection().getAttribute(obj.getObjectName(), attributeName);

							this.props.put(attributeName, value);
							log.debug(this + " set " + attributeName + " = " + value);
						}
						break;
					}
				}
			} catch (Exception e) {

				this.setSourceState(SourceState.BROKEN);
				
				if (log.isDebugEnabled()) {
					e.printStackTrace();
				}
			} 
		}
	}

	public void shutdown() {
	
		if ( !this.tasks.isEmpty() ) {
			
			for (ScheduledFuture<?> task: tasks) {
				task.cancel(true);
				log.debug(this + " task cancelation status is " + task.isCancelled());
			}
		}
		
		log.info(this + " source shutdown");
	}
	
	public boolean isConnected() {
		
		if (this.mbeanServerConnection == null) {
			return false;
		}
		return true;
	}
	
	public synchronized void setBroken(boolean broken) {
		
		this.broken = broken;
		this.mbeanServerConnection = null;
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

	public synchronized boolean isDefinitlyBroken() {
		return definitlyBroken;
	}
	public synchronized void setDefinitlyBroken(boolean definitlyBroken) {
		this.definitlyBroken = definitlyBroken;
	}

	public String getPropsMBean() {
		return this.propsMBean;
	}

	public void setPropsMBean(String propsMBean) {
		this.propsMBean = propsMBean;
	}
	
	public HashMap<String, Object> getProps() {
		return this.props;
	}

	public void setProps(HashMap<String, Object> props) {
		this.props = props;
	}

	public void setLabel() {
		this.label = this.getHost().replaceAll("\\.", "_") + "_" + this.getPort();
	}

	public String getLabel() {
		return this.label;
	}

	public ArrayList<Writer> getWriters() {
		return this.jimi.getWriters();
	}
	
	public void setJimi(Jimi jimi) {
		this.jimi = jimi;
	}

	public int getCountdown() {
		return countdown;
	}

	public void setCountdown(int countdown) {
		this.countdown = countdown;
	}

	public synchronized SourceState getSourceState() {
		return sourceState;
	}

	public synchronized void setSourceState(SourceState sourceState) {
		log.info(this + " change state to " + sourceState);
		this.sourceState = sourceState;
	}
	
	
}
