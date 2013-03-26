package com.opshack.jimi;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import com.opshack.jimi.sources.Source;

public class Event implements Serializable{

	private final UUID id;
	public final Source source;
	private final String metric;
	public final String value;
	public final long ts;
	
	public Event(Source source, String metric, String value) {
		
		this.id = UUID.randomUUID();
		this.source = source;
		this.metric = metric;
		this.value = value;
		this.ts = System.currentTimeMillis();
	}
	
	public UUID getId() {
		return this.id;
	}
	

	public Source getSource() {
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