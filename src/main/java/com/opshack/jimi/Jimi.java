package com.opshack.jimi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.opshack.jimi.sources.Source;
import com.opshack.jimi.sources.Jvm;
import com.opshack.jimi.sources.Weblogic;
import com.opshack.jimi.sources.WeblogicDomain;
import com.opshack.jimi.writers.Console;
import com.opshack.jimi.writers.Graphite;
import com.opshack.jimi.writers.Writer;

public class Jimi {

	final private Logger log = LoggerFactory.getLogger(this.getClass());

	private ArrayList<Source> sources;
	private int executorThreadPoolSize = 2;
	
	private ArrayList<Writer> writers;
	public ScheduledExecutorService taskExecutor;
	public HashMap metricGroups = new HashMap();

	Jimi() throws FileNotFoundException {

		if (System.getProperty("jimi.metrics") == null) {
			System.setProperty("jimi.metrics", System.getProperty("jimi.home")
					+ "/metrics");
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
			log.error("No metrics found in "
					+ System.getProperty("jimi.metrics"));
			System.exit(1);
		} else {
			log.info("Available metrics: " + metricGroups.keySet());
		}
	}

	public void start() throws Exception {

		log.info("Shared executor size is: " + executorThreadPoolSize);
		taskExecutor = Executors.newScheduledThreadPool(executorThreadPoolSize);

		for (Writer writer : writers) {

			if (writer.init()) { // setup writer
				new Thread(writer).start(); // start writer

			} else {
				log.error("Can't initialize writer.");
				System.exit(1);
			}
		}

		compileSources(); // process proxy sources

		boolean startedSources = false; // is there any running source?
		for (Source source : sources) {
			if (source.init(this)) {
				new Thread(source).start();
				startedSources = true; // yes
			}
		}

		if (!startedSources) { // if not, stop execution
			log.info("There is nothing to do, check configuration.");
			System.exit(0);

		} else {

			int counter = 0;
			while (true) {

				for (Source source : sources) {

					if (source.isBroken()) { // check source state
						log.warn(source + " is broken.");
						source.shutdown(); // shutdown and cleanup if broken

						if (source.init(this)) {
							new Thread(source).start(); // restart
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

				counter++;
				if (counter == 300) {
					log.info("Jimi is running well. Sources count: " + sources.size());
					for (Writer writer : writers) {
						log.info(writer.getClass().getName() + " recieved " + writer.getEventCounter() + " events, total size is " + writer.getEventsSize());
					}
					counter = 0;
				}
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
		
		final Logger log = LoggerFactory.getLogger(Jimi.class);

		if (args.length < 1) {
			log.error("Missing parameters!\n\n java -cp ... com.opshack.jimi.Jimi path/to/config.yaml\n\n");
			System.exit(1);
		}

		try {

			InputStream configFile = new FileInputStream(new File(args[0]));

			log.info("Load configuration");

			Constructor configConstructor = new Constructor(Jimi.class);

			configConstructor.addTypeDescription(new TypeDescription(
					Weblogic.class, "!weblogic"));
			configConstructor.addTypeDescription(new TypeDescription(
					WeblogicDomain.class, "!weblogicDomain"));
			configConstructor.addTypeDescription(new TypeDescription(Jvm.class,
					"!jvm"));

			configConstructor.addTypeDescription(new TypeDescription(
					Graphite.class, "!graphite"));
			//configConstructor.addTypeDescription(new TypeDescription(
			//		GraphiteTCP.class, "!graphiteTCP"));
			configConstructor.addTypeDescription(new TypeDescription(
					Console.class, "!console"));

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

	public ScheduledExecutorService getTaskExecutor() {
		return taskExecutor;
	}

	public HashMap getMetricGroups() {
		return metricGroups;
	}	
}
