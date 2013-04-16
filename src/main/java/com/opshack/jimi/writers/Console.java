package com.opshack.jimi.writers;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public class Console extends Writer {
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());
	final private Logger writer = LoggerFactory.getLogger("Console");
	
	private String format;
	
	
	@Override
	public boolean init() {
		
		return true;
	}
	
	
	@Override
	public void write(Event event) {
		
		if ( valide(event) ) {
			
			VelocityContext velocityContext = getVelocityContext(event);
	        String message =  getVelocityString(velocityContext, this.getFormat());
	        
	        setEventsSize(message.toString().getBytes().length);
	        writer.info(message.toString());
		}
	}

	
	public String getFormat() {
		return format;
	}

	
	public void setFormat(String format) {
		
		log.info("Event format: " + format);
		this.format = format;
	}

}
