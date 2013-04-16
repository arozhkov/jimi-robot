package com.opshack.jimi.writers;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public abstract class Writer implements Runnable{

	final private Logger log = LoggerFactory.getLogger(this.getClass());
	private LinkedBlockingQueue<Event> writerQueue = new LinkedBlockingQueue<Event>(5000);
	private long eventCounter = 0;
	private long eventsSize = 0;
	private ArrayList<String> filter;
	
	public void run() {
		
	    try {
	    	
	      while (true) {
	    	  
	        Event event = writerQueue.take();
	        write(event); // call method implemented by real writer
	        
	        if (Thread.interrupted()) {
	        	throw new InterruptedException();
	        }
	        
	      }
	      
	    } catch (InterruptedException ex) {
	    	log.info("Writer iterrupted.");
		} 
	}
	
	public boolean valide(Event event) {
		
		if (this.filter == null) return true;
		
		for (String token: this.filter) {
			
			String metricLabel = (String) event.getMetric().get("label");
			
			if ( metricLabel.contains(token) ) return true;
			log.debug("Event is invalid: " + metricLabel);
		}

		return false;
	}
	
	public abstract void write(Event event);
	public abstract boolean init();
	
	public void put(Event event) throws InterruptedException {
		writerQueue.put(event);
		eventCounter++;
	}

	public long getEventCounter() {
		return eventCounter;
	}

	public long getEventsSize() {
		return eventsSize;
	}

	public void setEventsSize(long eventSize) {
		this.eventsSize = this.eventsSize + eventSize;
	}
	
	public ArrayList<String> getFilter() {
		return filter;
	}

	public void setFilter(ArrayList<String> filter) {
		this.filter = filter;
	}
}
