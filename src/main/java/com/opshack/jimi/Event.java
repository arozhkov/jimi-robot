package com.opshack.jimi;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.opshack.jimi.sources.Source;

public class Event implements Serializable{

	private final UUID id;
	public final Source source;
	private final Map metric;
	public final String value;
	public final Timestamp ts;
	
	public Event(Source source, Map metricDef, String label, String value) {
		
		this.id = UUID.randomUUID();
		this.source = source;
		this.metric = new HashMap(metricDef);
		this.metric.put("label", label);
		this.value = value;
		this.ts = new Timestamp(System.currentTimeMillis());
	}
	
	public UUID getId() {
		return this.id;
	}

	public Source getSource() {
		return this.source;
	}

	public Map getMetric() {
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