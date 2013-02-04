package com.opshack.jimi.writers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public class GraphiteTCPWriter extends Writer {

	final private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private String host;
	private int port;
	
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
		
		return true;
	}
	
	
	@Override
	public void write(Event event) {
		
		String stringMessage = event.getSource() + "." + event.getMetric()
				+ " " + event.getValue() 
				+ " " + event.getTs()/1000; // divide by 1000 to get seconds
		
		log.debug("Graphite: " + event.getId() + " " + stringMessage + " to " + this.address + ":" + this.port);
		
		byte[] byteMessage = stringMessage.getBytes();
		
		DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, this.address, this.port);
		
		try {
			this.datagramSocket.send(packet);
		} catch (Exception e) {
			log.error("Graphite: " + event.getId() + " " + stringMessage + " to " + this.address + ":" + this.port);
			e.printStackTrace();
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
}
