![jimi](https://raw.github.com/arozhkov/jimi-robot/master/img/Jimi.png)

## Features

* Easy to setup and configure. _I think so._
* Light. _Well RAM is so cheap nowadays._
* Not intrusive. _As much as RMI could be._
* Integration with third-party tools. _Graphite, Kafka_
* Getting data from Oracle JVM, Weblogic and JBoss servers.  _True._
* Collecting simple numeric attributes from BMeans and combined data attributes. _True._


## Concepts

* _Source_ – abstract representation of a source JMX server on a program level.
* _Metric_ – definition of a measure to be collected from the source.
* _Value_ – real value gathered from the source.
* _Event_ – the value within its context: source, metric, timestamp.
* _Writer_ – component of a program responsible for writing or sending events to external storage/analysis system. 


## Installation

__Before you begin:__ although the code is a mess, the application is stable and does what it is supposed to do.  I'm currently working on improvements.

1. Download latest version of [Jimi](http://sourceforge.net/projects/jimi-robot/). The archive contains all needed dependences except Weblogic/JBoss client jars.  
1. Unzip archive somewhere on your server. We are going to refer to this place as `JIMI_HOME`.
1. Copy `JIMI_HOME/config/jimi.yaml.example` into `JIMI_HOME/config/jimi.yaml` and modify it according to your needs.

If you plan to work with Weblogic or JBoss servers you have to follow these [additional steps](https://github.com/arozhkov/jimi-robot/wiki/Weblogic-JBoss).


## Configuration

All Jimi configuration defined in one file - `jimi.yaml`. You can name this file as you want, but here we will use highlighted name for consistency.

__YAML:__ read and understand syntax
> [yaml.org](http://yaml.org/spec/1.1/)  
> [snakeyaml](http://code.google.com/p/snakeyaml/wiki/Documentation)  


## _jimi.yaml_

This file contains definitions of writers and sources that will be used by Jimi. The definitions start with a tag referring to the object's class, like `!graphite` for Graphite writer, `!console` for console writer and `!jvm` for JVM source. Each writer and source need its own set of properties to get the work done. Properties' names are self-explanatory most of the time. Full list of possible properties for each class can be found on [Jimi's wiki](https://github.com/arozhkov/jimi-robot/wiki).

Events produces by sources will be forwarded to all defined writers. The example file contains definitions of two writers: `!console` and `!graphite`. The first prints events to the console the second sends events to Graphite server. 

The tag `!jvm` tells us that the source is a standard JVM JMX server. You may need to update `host` and `port` properties. 
`metrics` property contains the names of metric groups defined in metrics library folder `JIMI_HOME/metrics`. You can use existing metrics or create your own, more on this [here](https://github.com/arozhkov/jimi-robot/wiki/Metrics).

The last element of this file, `executorThreadPoolSize` property, defines the size of the thread pool used by Jimi for collection of metrics.

```yaml
writers: 
  - !console
    format: "${source.label} ${metric.get('label')} ${value} ${ts.format('hh:mm:ss.SSS')}"
 
  - !graphite
    host: 172.0.0.1
    port: 2003
    format: "jimi.${source.label}.${metric.get('rate')}s.${metric.get('label')} ${value} ${ts.s}"
        
sources:
  - !jvm
    host  : 172.0.0.1
    port  : 9001
    metrics:
      - Memory
      - MemoryPools
      - GC
      - Threading
      - System
      
executorThreadPoolSize: 2
```


## Usage

Jimi comes with a simple startup script `run.sh` that makes some assumptions about your setup: java is in your `PATH`, definitions of metrics are stored in `JIMI_HOME/metrics` folder, the main configuration file is in `JIMI_HOME/config` folder and the name of this file will be used as a name of Jimi instance, `JIMI_HOME/logs` folder exists.

```bash
run.sh <name_of_config_file_without_extension>
```

You may write your own startup script, Jimi's main class `com.opshack.jimi.Jimi` takes only one parameter: the path to the configuration file. The following additional options are available:    
`jimi.home` - Jimi's home folder    
`jimi.name` - Jimi's instance name lets you run multiple instances in the same home    
`jimi.metrics` - folder where you store yaml files with metric's definitions    

Don't forget to put jimi.jar and all jars from `JIMI_HOME/lib` to the classpath. If you use Kafka writer, option `-Djava.security.policy=file:$JIMI_HOME/jimi.policy` is mandatory.


## Ideas behind this project

Jimi is highly inspired by my previous non open-source works, articles from [monitoring sucks](http://monitoring.no.de/) and the jmxtrans project.

It's not that difficult to collect JMX counters, the difficulty is to create a tool easy to configure, maintain and integrate with other applications. _Having said that, it's even harder to make good use of collected data._


## Yes, you can help

* Let me know if you use Jimi
* Report bugs
* Share metrics definitions
* Correct my English :)


## [Find more information on Jimi's wiki](https://github.com/arozhkov/jimi-robot/wiki)
