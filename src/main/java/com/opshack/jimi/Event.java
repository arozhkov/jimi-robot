package com.opshack.jimi;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public class Event implements Serializable{

	private final UUID id;
	public final String source;
	private final String metric;
	public final String value;
	public final long ts;
	
	public Event(String source, Map metric, String value) {
		
		this.id = UUID.randomUUID();
		this.source = source;
		this.metric = (String) metric.get("label");
		this.value = value;
		this.ts = System.currentTimeMillis();
	}
	
	public Event(String source, String label, String value) {
		
		this.id = UUID.randomUUID();
		this.source = source;
		this.metric = label;
		this.value = value;
		this.ts = System.currentTimeMillis();
	}
	
	public UUID getId() {
		return this.id;
	}
	

	public String getSource() {
		return this.source;
	}

	public synchronized String getMetric() {
		return metric;
	}

	public String getValue() {
		return this.value;
	}

	public long getTs() {
		return this.ts;
	}
}