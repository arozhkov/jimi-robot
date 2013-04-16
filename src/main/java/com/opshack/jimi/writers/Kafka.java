package com.opshack.jimi.writers;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import kafka.javaapi.producer.Producer;
import kafka.javaapi.producer.ProducerData;
import kafka.producer.ProducerConfig;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opshack.jimi.Event;

public class Kafka extends Writer {

	final private Logger log = LoggerFactory.getLogger(this.getClass());
	
	
	Producer<String, String> kafkaProducer;
	
	private String topic;
	private String message;
	private Map<String, String> props;	
	
	
	@Override
	public boolean init() {

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
		
			VelocityContext velocityContext = getVelocityContext(event);
	        String stringTopic =  getVelocityString(velocityContext, this.getTopic());
	        String stringMessage =  getVelocityString(velocityContext, this.getMessage());
	        
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
