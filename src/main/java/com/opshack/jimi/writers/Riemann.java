package com.opshack.jimi.writers;

import java.io.IOException;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aphyr.riemann.client.RiemannClient;
import com.opshack.jimi.Event;

public class Riemann extends Writer {
	
	final private Logger log = LoggerFactory.getLogger(this.getClass());

	private String riemannHost;
	private int riemannPort;
	
	private String host;
	private String service;
	private String state;
	private String tags;
	private long ttl;
	
	private RiemannClient client;
	
	@Override
	public void write(Event event) {
		
		if ( valide(event) ) {
			
			VelocityContext velocityContext = getVelocityContext(event);
	        String host =  getVelocityString(velocityContext, this.getHost());
	        String service =  getVelocityString(velocityContext, this.getService());
	        String state =  getVelocityString(velocityContext, this.getState());
	        String tags =  getVelocityString(velocityContext, this.getTags());
	        
//			host	A hostname, e.g. "api1", "foo.com"
//			service	e.g. "API port 8000 reqs/sec"
//			state	Any string less than 255 bytes, e.g. "ok", "warning", "critical"
//			time	The time of the event, in unix epoch seconds
//			description	Freeform text
//			tags	Freeform list of strings, e.g. ["rate", "fooproduct", "transient"]
//			metric	A number associated with this event, e.g. the number of reqs/sec.
//			ttl		A floating-point time, in seconds, that this event is considered valid for. Expired states may be removed from the index.
			
			client.event().
				host(host).
				service(service).
				state(state).
				time(event.getTs().getS()).
				tags(tags.split(" ")).
				metric(new Double(event.getValue())).
				ttl(this.getTtl()).
				send();
	        
			log.debug("RIEMANN: " + 
					host + "-" + 
					service + "-" + 
					state + "-" + 
					event.getTs().getS() + "-" + 
					tags + "-" + 
					new Double(event.getValue()) + "-" +
					this.getTtl());
		}

	}

	@Override
	public boolean init() {
		
		try {
			this.client = RiemannClient.udp(this.riemannHost, this.riemannPort);
			this.client.connect();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		log.info("Riemann writer connected to " + this.riemannHost + ":" + this.riemannPort);
		return true;
	}

	public String getRiemannHost() {
		return riemannHost;
	}
	public void setRiemannHost(String riemannHost) {
		this.riemannHost = riemannHost;
	}

	public int getRiemannPort() {
		return riemannPort;
	}
	public void setRiemannPort(int riemannPort) {
		this.riemannPort = riemannPort;
	}

	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}

	public String getService() {
		return service;
	}
	public void setService(String service) {
		this.service = service;
	}

	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	public String getTags() {
		return tags;
	}
	public void setTags(String tags) {
		this.tags = tags;
	}
	
	public long getTtl() {
		return ttl;
	}
	public void setTtl(long ttl) {
		this.ttl = ttl;
	}

	public RiemannClient getClient() {
		return client;
	}
	public void setClient(RiemannClient client) {
		this.client = client;
	}
}
