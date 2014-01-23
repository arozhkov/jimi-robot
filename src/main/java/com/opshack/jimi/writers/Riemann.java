package com.opshack.jimi.writers;

import java.io.IOException;

import com.aphyr.riemann.client.RiemannClient;
import com.opshack.jimi.Event;

public class Riemann extends Writer {

	private String host;
	private int port;
	private String format;
	
	private RiemannClient c;
	
	@Override
	public void write(Event event) {
		
//		host	A hostname, e.g. "api1", "foo.com"
//		service	e.g. "API port 8000 reqs/sec"
//		state	Any string less than 255 bytes, e.g. "ok", "warning", "critical"
//		time	The time of the event, in unix epoch seconds
//		description	Freeform text
//		tags	Freeform list of strings, e.g. ["rate", "fooproduct", "transient"]
//		metric	A number associated with this event, e.g. the number of reqs/sec.
//		ttl		A floating-point time, in seconds, that this event is considered valid for. Expired states may be removed from the index.
		
		c.event().
		  service("fridge").
		  state("running").
		  time(100).
		  tags("appliance", "cold").
		  metric(5.3).
		  ttl(10).
		  send();
	}

	@Override
	public boolean init() {
		
		try {
			c = RiemannClient.tcp(this.host, this.port);
			c.connect();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
