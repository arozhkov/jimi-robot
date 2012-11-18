package com.opshack.jimi.writers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public class ConsoleWriter extends Writer {
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());
	final private Logger writer = LoggerFactory.getLogger("ConsoleWriter");
	
	private String format;
	
	@Override
	public boolean init() {
		// TODO Auto-generated method stub
		return true;
	}
	
	
	@Override
	public void write(Event event) {
		writer.info(event.getId() 
				+ " " + event.getSource() + "." + event.getLabel() 
				+ " " + event.getValue() 
				+ " " + event.getTs());

	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		
		log.info("Event format: " + format);
		this.format = format;
	}
	
	
}
