package com.opshack.jimi.writers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public class Graphite extends Writer {

	final private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private String host;
	private int port;
	private String format;
	
	InetAddress address;
	private DatagramSocket datagramSocket;
	
	
	@Override
	public boolean init() {
		
		try {
			address = InetAddress.getByName(this.host);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
		
		try {
			this.datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			return false;
		}
		
		log.info(this.host + ":" + this.port + ", format " + this.format);
		return true;
	}
	
	
	@Override
	public void write(Event event) {
		
		if ( valide(event) ) {
			
			VelocityContext velocityContext = getVelocityContext(event);
	        String message =  getVelocityString(velocityContext, this.getFormat());
	        
			log.debug("Graphite: " + event.getId() + " " + message + " to " + this.address + ":" + this.port);
			
			byte[] byteMessage = message.getBytes();
			setEventsSize(byteMessage.length);
			
			DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, this.address, this.port);
			
			try {
				this.datagramSocket.send(packet);
			} catch (Exception e) {
				log.error("Graphite: " + event.getId() + " " + message + " to " + this.address + ":" + this.port);
				e.printStackTrace();
			}
		}
	}

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

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}
	
	
}
