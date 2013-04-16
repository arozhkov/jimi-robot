package com.opshack.jimi.writers;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import kafka.javaapi.producer.Producer;
import kafka.javaapi.producer.ProducerData;
import kafka.producer.ProducerConfig;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public class Kafka extends Writer {

	final private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private VelocityEngine ve = new VelocityEngine();
	Producer<String, String> kafkaProducer;
	
	private String topic;
	private String message;
	private Map<String, String> props;	
	
	
	@Override
	public boolean init() {

		ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogSystem");
		ve.init();

		Properties config = new Properties();
		
		Set<String> propsKeys = props.keySet();
		
		for (String key: propsKeys){
			config.setProperty(key, props.get(key).toString());
		}
		
		log.info("Kafka producer configuration: " + config.toString());
		
		ProducerConfig producerConfig = new ProducerConfig(config);
		kafkaProducer = new Producer<String, String>(producerConfig);
		
		return true;
	}
	
	
	@Override
	public void write(Event event) {
		
		if ( valide(event) ) {
		
			VelocityContext velocityContext = new VelocityContext();
	
			velocityContext.put("source", event.getSource());
			velocityContext.put("metric", event.getMetric());
			velocityContext.put("value", event.getValue());
			velocityContext.put("ts", event.getTs());
			
	        StringWriter w1 = new StringWriter();
	        ve.evaluate(velocityContext, w1, "velocity", this.getTopic());
	        
	        StringWriter w2 = new StringWriter();
	        ve.evaluate(velocityContext, w2, "velocity", this.getMessage());
	        
	        String stringTopic =  w1.toString();
	        String stringMessage =  w2.toString();
	        
			byte[] byteMessage = stringMessage.getBytes();
			setEventsSize(byteMessage.length);
	
			ProducerData<String, String> data = new ProducerData<String, String>(stringTopic, stringMessage);
			kafkaProducer.send(data);
		}
		
	}


	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}


	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}


	public Map<String, String> getProps() {
		return props;
	}

	public void setProps(Map<String, String> props) {
		this.props = props;
	}
	
}
