package com.opshack.jimi;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.UUID;

public class Event implements Serializable{

	private static final long serialVersionUID = -5913264590485616558L;
	private final UUID id;
	public final HashMap<String, Object> source;
	private final HashMap<String, Object> metric;
	public final String value;
	public final Timestamp ts;
	
	public Event(HashMap<String, Object> sourceProps, HashMap<String, Object> metricDef, String label, String value) {
		
		this.id = UUID.randomUUID();
		this.source = new HashMap<String, Object>(sourceProps);
		this.metric = new HashMap<String, Object>(metricDef);
		this.metric.put("label", new String(label));
		this.value = new String(value);
		this.ts = new Timestamp(System.currentTimeMillis());
	}
	
	public UUID getId() {
		return this.id;
	}

	public HashMap<String, Object> getSource() {
		return this.source;
	}

	public HashMap<String, Object> getMetric() {
		return metric;
	}

	public String getValue() {
		return this.value;
	}

	
	public Timestamp getTs() {
		return this.ts;
	}
	
	// timestamp wrapper 
	public class Timestamp {
		
		private final long ts;
		
		public Timestamp(long ts) {
			this.ts = ts;
		}
		
		// allow usage of ${ts.format('dd.MM.yy')} in writers
		public String format(String pattern) {
			
			SimpleDateFormat df = new SimpleDateFormat(pattern);
			return df.format(ts);
		}
		
		public String toString() {
			return "" + ts;
		}

		public long getTs() {
			return ts;
		}
		
		// allow usage of ${ts.s} in writers
		public long getS() {
			return ts/1000;
		}
		
		// allow usage of ${ts.ms} in writers
		public long getMs() {
			return ts;
		}
		
	}
	
}