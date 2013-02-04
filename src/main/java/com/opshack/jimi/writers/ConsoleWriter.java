package com.opshack.jimi.writers;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public class ConsoleWriter extends Writer {
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());
	final private Logger writer = LoggerFactory.getLogger("ConsoleWriter");
	
	private String format;
	private MessageFormat message; 
	
	@Override
	public boolean init() {
		
		String[] vars = {"$id","$source","$metric","$value","$timestamp"};
		String[] places = {"{0}","{1}","{2}","{3}","{4}"};
		
		for (int i=0; i<5; i++) {
			String str = this.format.replace(vars[i], places[i]);
			this.format = str;
		}
		
		message = new MessageFormat(this.format);
		return true;
	}
	
	
	@Override
	public void write(Event event) {

		Object[] args = { event.getId(), event.getSource(), event.getMetric(), event.getValue(), String.valueOf(event.getTs()) };
		writer.info(message.format(args));

	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		
		log.info("Event format: " + format);
		this.format = format;
	}
	
	
}
