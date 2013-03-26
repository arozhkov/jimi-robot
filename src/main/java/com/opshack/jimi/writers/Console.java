package com.opshack.jimi.writers;

import java.io.StringWriter;
import java.text.MessageFormat;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public class Console extends Writer {
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());
	final private Logger writer = LoggerFactory.getLogger("Console");
	private VelocityEngine ve = new VelocityEngine();
	
	private String format;
	private MessageFormat message; 
	
	
	@Override
	public boolean init() {
		
		ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogSystem");
		ve.init();
		
		return true;
	}
	
	
	@Override
	public void write(Event event) {

		VelocityContext velocityContext = new VelocityContext();

		velocityContext.put("source", event.getSource());
		velocityContext.put("metric", event.getMetric());
		velocityContext.put("value", event.getValue());
		velocityContext.put("ts", event.getTs());
		
        StringWriter w = new StringWriter();
        ve.evaluate(velocityContext, w, "velocity", this.getFormat());
        
        writer.info(w.toString());
	}

	
	public String getFormat() {
		return format;
	}

	
	public void setFormat(String format) {
		
		log.info("Event format: " + format);
		this.format = format;
	}
}
