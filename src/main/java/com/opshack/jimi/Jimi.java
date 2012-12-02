package com.opshack.jimi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.opshack.jimi.sources.JmxSource;
import com.opshack.jimi.sources.Jvm;
import com.opshack.jimi.sources.Weblogic;
import com.opshack.jimi.sources.WeblogicDomain;
import com.opshack.jimi.writers.ConsoleWriter;
import com.opshack.jimi.writers.GraphiteTCPWriter;
import com.opshack.jimi.writers.GraphiteWriter;
import com.opshack.jimi.writers.Writer;

public class Jimi {

	final private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private Writer writer;
	private ArrayList<JmxSource> sources;
	private MetricGroups metricGroups;
	

	public void start() throws Exception {
		
		if (writer.init()) { 				// setup writer 
			new Thread(writer).start(); 	// start writer
			
		} else {
			log.error("Can't initialze writer.");
			System.exit(1);	
		}
		
		compileSources();
		
		boolean startedSources = false;		// is there any running source?
		for (JmxSource source: sources) {
			if (source.init(writer, metricGroups)) {
				new Thread(source).start();
				startedSources = true;		// yes
			}
		}
		
		if (!startedSources) {				// if not, stop execution
			log.info("There is nothing to do, check configuration.");
			System.exit(0);
			
		} else {
			
			int counter = 0;
			while (true) {
				
				for (JmxSource source: sources) {
					
					if (source.isBroken()) { 		// check source state
						log.warn(source + " is broken.");
						source.shutdown();			// shutdown, cleanup if broken
						
						if (source.init(writer, metricGroups)) { 
							new Thread(source).start(); 	// restart
						}
					}
				}
				
				try {
					
					Thread.sleep(1000); // sleep for 1 second then re-check
					
				} catch (InterruptedException e) {
					log.error("Thread interrupted.");
					e.printStackTrace();
					System.exit(1);
				} 
				
				if (Thread.interrupted()) {
			        log.info("Shutdown.");
			        System.exit(0);
			    }
				counter++;
				if (counter == 300) {
					log.info("Jimi's running well.");
					counter = 0;
				}
			}

		}
	}
	
	private void compileSources() throws Exception {
		
		ArrayList<JmxSource> compiledSources = new ArrayList<JmxSource>();
		
		for (JmxSource source: sources) {
			
			if (source instanceof WeblogicDomain) {
				compiledSources.addAll( ((WeblogicDomain)source).getSources() );
			} else {
				compiledSources.add(source);
			}
		}
		
		this.sources = new ArrayList<JmxSource>();
		this.sources.addAll(compiledSources);
		
	}
	
    public static void main( String[] args ) throws Exception {
    	
    	final Logger log = LoggerFactory.getLogger(Jimi.class);
    	
    	if (args.length < 2) {
    		log.error("Missing parameters!\n\n java -cp ... com.opshack.jimi.Jimi path/to/config.yaml path/to/metrics.yaml\n\n");
    		System.exit(1);
    	}
    	
    	InputStream configFile = null;
    	InputStream metricsFile = null;
    	
    	Jimi jimi = new Jimi();
    	
		try {
			
			// TODO better cli integration
			log.info("Read configuration");
	        configFile = new FileInputStream(new File(args[0]));
	        metricsFile = new FileInputStream(new File(args[1]));
			
	        log.info("Load configuration");
	        Constructor configConstructor = new Constructor(Jimi.class);
	        configConstructor.addTypeDescription(new TypeDescription(Weblogic.class, "!weblogic"));
	        configConstructor.addTypeDescription(new TypeDescription(WeblogicDomain.class, "!weblogicDomain"));
	        configConstructor.addTypeDescription(new TypeDescription(Jvm.class, "!jvm"));
	        configConstructor.addTypeDescription(new TypeDescription(GraphiteWriter.class, "!graphite"));
	        configConstructor.addTypeDescription(new TypeDescription(GraphiteTCPWriter.class, "!graphiteTCP"));
	        configConstructor.addTypeDescription(new TypeDescription(ConsoleWriter.class, "!console"));
			Yaml configYaml = new Yaml(configConstructor);
	        jimi = (Jimi) configYaml.load(configFile);
	        
	        Constructor metricConstructor = new Constructor(MetricGroups.class);
	        Yaml metricsYaml = new Yaml(metricConstructor);
	        jimi.setMetricGroups((MetricGroups) metricsYaml.load(metricsFile));
	        
		} catch (FileNotFoundException e) {
			
			log.error("FileNotFoundException occured. Please check configuration file's paths.");
			e.printStackTrace();
			System.exit(1);
			
		}  catch (Exception e) {
			
			log.error(e.getClass() + " occured. Please check configuration files.");
			e.printStackTrace();
			System.exit(1);
		}
 
		if (jimi != null && jimi.getMetricGroups() != null) {
	
			jimi.start();
			
		}
    }
	
	
	public Writer getWriter() {
		return writer;
	}
	public void setWriter(Writer writer) {
		this.writer = writer;
	}
	
	public ArrayList<JmxSource> getSources() {
		return sources;
	}
	public void setSources(ArrayList<JmxSource> sources) {
		this.sources = sources;
	}

	public MetricGroups getMetricGroups() {
		return metricGroups;
	}
	public void setMetricGroups(MetricGroups metricGroups) {
		this.metricGroups = metricGroups;
	}
}
