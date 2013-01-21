package com.opshack.jimi.sources;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jvm  extends JmxSource {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private JMXConnector jmxConnector;


	@Override
	public void setMBeanServerConnection()	throws InterruptedException {

		if (!super.isConnected()) {

			JMXServiceURL serviceURL = null;

			try {
				serviceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + 
						this.getHost() + ":" + this.getPort() + "/jmxrmi");

			} catch (MalformedURLException e) {

				log.error(this + " MalformedURLException : " + "service:jmx:rmi:///jndi/rmi://" 
						+ this.getHost() + ":"  + this.getPort() + "/jmxrmi");
				throw new InterruptedException();

			}

			try {
				this.jmxConnector =  JMXConnectorFactory.newJMXConnector(serviceURL, null);
				this.jmxConnector.connect();
				this.mbeanServerConnection = this.jmxConnector.getMBeanServerConnection();

			} catch (IOException e) {

				log.warn(this + " IO Exception occurred during connection to JMX server");
				Thread.sleep(30000); // sleep 30 seconds then mark thread as broken and interrupt it

				this.setBroken(true);
				throw new InterruptedException("IO Exception occurred during connection to JMX server");	
				
			}  catch (Exception ee) {
				
				log.warn(this + " Non-IO Exception occurred during connection to JMX server");
				ee.printStackTrace();
				
				Thread.sleep(60000); // sleep 60 seconds then mark thread as broken and interrupt it
				
				this.setBroken(true);
				throw new InterruptedException("Non-IO Exception occurred during connection to JMX server");
			}
		}

		log.info(this + " connected");
	}
}
