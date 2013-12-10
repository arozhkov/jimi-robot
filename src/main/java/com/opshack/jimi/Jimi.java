package com.opshack.jimi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.opshack.jimi.sources.Jboss;
import com.opshack.jimi.sources.Source;
import com.opshack.jimi.sources.Jvm;
import com.opshack.jimi.sources.SourceState;
import com.opshack.jimi.sources.Weblogic;
import com.opshack.jimi.sources.WeblogicDomain;
import com.opshack.jimi.writers.Console;
import com.opshack.jimi.writers.Graphite;
import com.opshack.jimi.writers.Kafka;
import com.opshack.jimi.writers.Writer;

public class Jimi {

	final private Logger log = LoggerFactory.getLogger(this.getClass());

	private ArrayList<Source> sources;
	private int executorThreadPoolSize = 2;
	private int sourceConnectionTimeout = 3000;
	

	private ArrayList<Writer> writers;
	public ScheduledExecutorService taskExecutor;
	ExecutorService sourceExecutor;
	public HashMap metricGroups = new HashMap();

	Jimi() throws FileNotFoundException {

		if (System.getProperty("jimi.metrics") == null) {
			System.setProperty("jimi.metrics", System.getProperty("jimi.home") + "/metrics");
		}

		File metricsDir = new File(System.getProperty("jimi.metrics"));

		if (metricsDir.isDirectory()) {

			for (File metricFile : metricsDir.listFiles()) {

				if (metricFile.isFile()
						&& metricFile.getName().endsWith("yaml")) {

					InputStream input = new FileInputStream(metricFile);
					Yaml yaml = new Yaml();

					for (Object data : yaml.loadAll(input)) {
						this.metricGroups.putAll((HashMap) data);
					}
				}
			}
		}

		if (this.metricGroups.isEmpty()) {
			
			log.error("No metrics found in " + System.getProperty("jimi.metrics"));
			System.exit(1);
			
		} else {
			
			log.info("Available metrics: " + metricGroups.keySet());
		}
	}

	public void start() throws Exception {

		log.info("Shared executor size is: " + executorThreadPoolSize);
		
		taskExecutor = Executors.newScheduledThreadPool(executorThreadPoolSize);
		sourceExecutor = Executors.newFixedThreadPool(3);

		for (Writer writer : writers) {

			if (writer.init()) { // setup writer
				new Thread(writer, writer.getClass().getSimpleName() + "Writer").start(); // start writer

			} else {
				log.error("Can't initialize writer.");
				System.exit(1);
			}
		}

		compileSources(); // process proxy sources

		int counter = 0;
		while (true) {

			for (Source source : sources) {

				switch(source.getSourceState()) {
				
				case INIT:
					
					source.init(this);
					if (source.getSourceState().equals(SourceState.CONNECTED)) {
						source.run();
					}
					break;
					
				case ONLINE:
					
					break;
					
				case BROKEN:
					source.setSourceState(SourceState.RECOVERY);
					source.shutdown();
					Thread.sleep(12000);
					source.setSourceState(SourceState.INIT);
					break;
					
				case OFFLINE:

					break;
					
				default:
					break;
				}
			}  	
				
				

//					Future<?> future = sourceExecutor.submit(Executors.callable(source));
//					try {
//						future.get(3000, TimeUnit.MILLISECONDS);	
//						
//					} catch(TimeoutException e) {
//						
//						if (log.isDebugEnabled()) {
//							e.printStackTrace();
//						} else {
//							log.info("Can't wait, going to the next task");
//						}
//						
//					} catch(Exception e) {
//						
//						if (log.isDebugEnabled()) {
//							e.printStackTrace();
//						} else {
//							log.info("An exception occurred during initialization");
//						}
//					}


			try {

				Thread.sleep(1000); // sleep for 1 second then re-check

			} catch (InterruptedException e) {
				log.error("Thread interrupted.");
				e.printStackTrace();
				System.exit(1);
			}

			counter++;
			if (counter == 300) {
				log.info("Jimi is running well. Sources count: " + sources.size());
				for (Writer writer : writers) {
					log.info(writer.getClass().getName() + " recieved " + writer.getEventCounter() + " events, total size is " + writer.getEventsSize() + " bytes.");
				}
				counter = 0;
			}
		}

	}

	private void compileSources() throws Exception {

		ArrayList<Source> compiledSources = new ArrayList<Source>();

		for (Source source : sources) {

			if (source instanceof WeblogicDomain) {
				compiledSources.addAll(((WeblogicDomain) source).getSources());
			} else {
				compiledSources.add(source);
			}
		}

		this.sources = new ArrayList<Source>();
		this.sources.addAll(compiledSources);

	}

	public static void main(String[] args) throws Exception {

		if (System.getProperty("jimi.home") == null) {
			System.setProperty("jimi.home", System.getProperty("user.dir"));
		}
		
		if (System.getProperty("jimi.name") == null) {
			System.setProperty("jimi.name", "jimi");
		}
		
		final Logger log = LoggerFactory.getLogger(Jimi.class);

		if (args.length < 1) {
			log.error("Missing parameters!\n\n java -cp ... com.opshack.jimi.Jimi path/to/config.yaml\n\n");
			System.exit(1);
		}

		try {

			InputStream configFile = new FileInputStream(new File(args[0]));

			log.info("Load configuration for " + System.getProperty("jimi.name"));

			Constructor configConstructor = new Constructor(Jimi.class);

			configConstructor.addTypeDescription(new TypeDescription(Weblogic.class, "!weblogic"));
			configConstructor.addTypeDescription(new TypeDescription(WeblogicDomain.class, "!weblogicDomain"));
			configConstructor.addTypeDescription(new TypeDescription(Jvm.class,	"!jvm"));
			configConstructor.addTypeDescription(new TypeDescription(Jboss.class,	"!jboss"));

			configConstructor.addTypeDescription(new TypeDescription(Graphite.class, "!graphite"));
			configConstructor.addTypeDescription(new TypeDescription(Console.class, "!console"));
			configConstructor.addTypeDescription(new TypeDescription(Kafka.class, "!kafka"));

			Yaml configYaml = new Yaml(configConstructor);
			Jimi jimi = (Jimi) configYaml.load(configFile);

			jimi.start();

		} catch (FileNotFoundException e) {

			log.error("FileNotFoundException occured. Please check configuration file path.");
			e.printStackTrace();
			System.exit(1);

		} catch (Exception e) {

			log.error(e.getClass() + " occured. Please check configuration.");
			e.printStackTrace();
			System.exit(1);

		}
	}

	public ArrayList<Writer> getWriters() {
		return writers;
	}

	public void setWriters(ArrayList<Writer> writers) {
		this.writers = writers;
	}

	public ArrayList<Source> getSources() {
		return sources;
	}

	public void setSources(ArrayList<Source> sources) {
		this.sources = sources;
	}

	public int getExecutorThreadPoolSize() {
		return executorThreadPoolSize;
	}

	public void setExecutorThreadPoolSize(int executorThreadPoolSize) {
		this.executorThreadPoolSize = executorThreadPoolSize;
	}
	
	public int getSourceConnectionTimeout() {
		return sourceConnectionTimeout;
	}

	public void setSourceConnectionTimeout(int sourceConnectionTimeout) {
		this.sourceConnectionTimeout = sourceConnectionTimeout;
	}

	public ScheduledExecutorService getTaskExecutor() {
		return taskExecutor;
	}

	public HashMap getMetricGroups() {
		return metricGroups;
	}	
}
