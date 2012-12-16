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
	private MBeanServerConnection mbeanServerConnection;


	@Override
	public MBeanServerConnection getMbeanServerConnection()	throws InterruptedException {

		log.info(this + " connecting");
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

				log.error(this + " IO Exception : " + "service:jmx:rmi:///jndi/rmi://" 
						+ this.getHost() + ":"  + this.getPort() + "/jmxrmi");

				Thread.sleep(30000); // sleep 30 seconds then mark thread as broken and interrupt it

				this.setBroken(true);
				throw new InterruptedException();	
			}
		}

		return this.mbeanServerConnection;
	}
}
