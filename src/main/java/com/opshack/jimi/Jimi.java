package com.opshack.jimi;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
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
import com.opshack.jimi.writers.Riemann;
import com.opshack.jimi.writers.Writer;

public class Jimi {

	final private Logger log = LoggerFactory.getLogger(this.getClass());

	private ArrayList<Source> sources;
	
	private int executorThreadPoolSize = 2;
	private int internalExecutorThreadPoolSize = 2;
	private int sourceExecutorThreadPoolSize = 5;
	private int sourceConnectionTimeout = 10000;
	private int mainLoopTime = 100;
	private int sourceStartTime = 10;
	private int offlineRecoveryTime = 300000;
	private int breakRecoveryTime = 10;

	private ArrayList<Writer> writers;
	public final ScheduledExecutorService internalExecutor;
	public final ScheduledExecutorService sourceExecutor;
	public final MetricExecutor taskExecutor;
	
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
		
		log.info("Shared executor size is: " + executorThreadPoolSize);
		
		this.internalExecutor = Executors.newScheduledThreadPool(this.internalExecutorThreadPoolSize);// cancel long running workers
		this.sourceExecutor = Executors.newScheduledThreadPool(this.sourceExecutorThreadPoolSize); 	// init, start workers
		this.taskExecutor = new MetricExecutor(this.executorThreadPoolSize); //Executors.newScheduledThreadPool(this.executorThreadPoolSize); 	// run tasks
		
	}

	public void start() throws Exception {


		for (Writer writer : writers) {

			if (writer.init()) { // setup writer
				new Thread(writer, writer.getClass().getSimpleName() + "Writer").start(); // start writer

			} else {
				log.error("Can't initialize writer.");
				System.exit(1);
			}
		}

		compileSources(); // process proxy sources

		for (Source source : sources) {
			source.setJimi(this);
			this.initSource(source, 1);
		}
		
		this.startReporter(this);
		
		while (true) {

			for (Source source : sources) {

				switch(source.getState()) {
				case ONLINE:
					source.setState(SourceState.CONNECTING);
					this.startSource(source);
					break;

				case OFFLINE:
					source.setState(SourceState.RECOVERY);
					this.initSource(source, this.offlineRecoveryTime);
					break;

				case BROKEN:
					source.setState(SourceState.SHUTINGDOWN);
					source.shutdown();
					break;

				case SHUTDOWN:
					source.setState(SourceState.RECOVERY);
					this.initSource(source, this.breakRecoveryTime);
					break;
				}
			}  	

			try {

				Thread.sleep(this.mainLoopTime); // sleep for 1 second then re-check

			} catch (InterruptedException e) {
				log.error("Jimi is interrupted ...");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}


	private void startSource(Source source) {

		final Source s = source;
		final ScheduledFuture<?> sourceHandle = sourceExecutor.schedule(new Runnable() {
			public void run() {
				s.start();
			}
		}, 10, TimeUnit.MILLISECONDS);

		// cancel w.start() if longer than X seconds
		internalExecutor.schedule(new Runnable() {
			public void run() {
				if (!sourceHandle.isDone()) {
					sourceHandle.cancel(true);
					log.error(s + " start canceled: " + sourceHandle.isCancelled());
					s.setState(SourceState.BROKEN);
				}
			}
		}, 60, TimeUnit.SECONDS);
	}


	private void initSource(Source source, int t) {

		final Source s = source;
		final ScheduledFuture<?> sourceHandle = sourceExecutor.schedule(new Runnable() {
			public void run() {
				s.init();
			}
		}, t, TimeUnit.MILLISECONDS);

		// cancel w.init() if longer than X seconds
		internalExecutor.schedule(new Runnable() {
			public void run() {
				if (!sourceHandle.isDone()) {
					sourceHandle.cancel(true);
					log.error(s + " init canceled: " + sourceHandle.isCancelled());
					s.setState(SourceState.OFFLINE);
				}
			}
		}, (t * 1000) + 15, TimeUnit.SECONDS);
	}
	
	
	private void startReporter(Jimi jimi) {
		
		final Jimi j = jimi;
		internalExecutor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				
				int t = j.sources.size();   // total
				int c = 0;					// connected
				int x = 0;					// others
				
				for (Source source: j.sources) {
					switch (source.getState()) {
					case CONNECTED:
						++c;
						break;
					default:
						++x;
					}
				}
				log.info("Report sources total: " + t + "; connected: " + c + "; others: " + x);
				log.info("Report executor avgExecTime: " + taskExecutor.getAverageTaskTime() 
						+ "; total execs: " + taskExecutor.getTotalTasks()
						+ "; current task count: " + taskExecutor.getInProgressTasks().size());
			}
		}, 60, 60, TimeUnit.SECONDS);
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
	
	private static String compileConfigFile(String configFile) {
		
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogSystem");
		ve.init();
		
		Map<String, String> env = System.getenv();

		VelocityContext configContext = new VelocityContext();
		configContext.put("env", env);
		
		Template template = null;

		try
		{
		   template = ve.getTemplate(configFile);
		} catch(Exception e) {
			System.out.println("Get configuration file template: " + e.getMessage());
		}

		StringWriter sw = new StringWriter();
		
		if (template != null) {
			template.merge( configContext, sw );
		}
		
		ve = null;
		
		return sw.toString();
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
			
			String configuration = Jimi.compileConfigFile(args[0]);
			
			if (configuration == null || configuration.isEmpty()) {
				throw new Exception("Empty configuration");
			}
			
			log.info("Load configuration for " + System.getProperty("jimi.name"));

			Constructor configConstructor = new Constructor(Jimi.class);

			configConstructor.addTypeDescription(new TypeDescription(Weblogic.class, "!weblogic"));
			configConstructor.addTypeDescription(new TypeDescription(WeblogicDomain.class, "!weblogicDomain"));
			configConstructor.addTypeDescription(new TypeDescription(Jvm.class,	"!jvm"));
			configConstructor.addTypeDescription(new TypeDescription(Jboss.class,	"!jboss"));

			configConstructor.addTypeDescription(new TypeDescription(Graphite.class, "!graphite"));
			configConstructor.addTypeDescription(new TypeDescription(Console.class, "!console"));
			configConstructor.addTypeDescription(new TypeDescription(Kafka.class, "!kafka"));
			configConstructor.addTypeDescription(new TypeDescription(Riemann.class, "!riemann"));

			Yaml configYaml = new Yaml(configConstructor);
			Jimi jimi = (Jimi) configYaml.load(configuration);

			jimi.start();

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

	public int getInternalExecutorThreadPoolSize() {
		return internalExecutorThreadPoolSize;
	}

	public void setInternalExecutorThreadPoolSize(int internalExecutorThreadPoolSize) {
		this.internalExecutorThreadPoolSize = internalExecutorThreadPoolSize;
	}

	public int getSourceExecutorThreadPoolSize() {
		return sourceExecutorThreadPoolSize;
	}

	public void setSourceExecutorThreadPoolSize(int sourceExecutorThreadPoolSize) {
		this.sourceExecutorThreadPoolSize = sourceExecutorThreadPoolSize;
	}

	public int getMainLoopTime() {
		return mainLoopTime;
	}

	public void setMainLoopTime(int mainLoopTime) {
		this.mainLoopTime = mainLoopTime;
	}

	public int getSourceStartTime() {
		return sourceStartTime;
	}

	public void setSourceStartTime(int sourceStartTime) {
		this.sourceStartTime = sourceStartTime;
	}

	public int getOfflineRecoveryTime() {
		return offlineRecoveryTime;
	}

	public void setOfflineRecoveryTime(int offlineRecoveryTime) {
		this.offlineRecoveryTime = offlineRecoveryTime;
	}

	public int getBreakRecoveryTime() {
		return breakRecoveryTime;
	}

	public void setBreakRecoveryTime(int breakRecoveryTime) {
		this.breakRecoveryTime = breakRecoveryTime;
	}	
	
}
