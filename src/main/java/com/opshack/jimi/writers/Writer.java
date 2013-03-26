package com.opshack.jimi.writers;

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public abstract class Writer implements Runnable{

	final private Logger log = LoggerFactory.getLogger(this.getClass());
	private LinkedBlockingQueue<Event> writerQueue = new LinkedBlockingQueue<Event>(5000);
	private long eventCounter = 0;
	
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
	
	public abstract void write(Event event);
	public abstract boolean init();
	
	public void put(Event event) throws InterruptedException {
		writerQueue.put(event);
		//eventCounter++;
	}

	public long getEventCounter() {
		return eventCounter;
	}
}
