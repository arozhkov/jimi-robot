![jimi](https://raw.github.com/arozhkov/jimi-robot/master/img/Jimi.png)

## Features

* Easy to setup and configure. _I think so._
* Light. _Well RAM is so cheap nowadays._
* Not intrusive. _As much as RMI could be._
* Integration with third-party tools. _Graphite_
* Getting data from Oracle JVM, Weblogic and in the near future from JBoss servers.  _True._
* Collect simple numeric attributes from BMeans and combined data attributes. _True._


## Concepts

* _Source_ – abstract representation of a source JMX server on a program level.
* _Metric_ – definition of a measure to be collected from the source.
* _Value_ – real value gathered from the source.
* _Event_ – the value within its context: source, metric, timestamp.
* _Writer_ – component of a program responsible for writing or sending events to external storage/analysis system. 


## Installation

__Before you begin:__ although the code is a mess, the application is stable and does what it is supposed to do.  I'm currently working on improvements.

1. Download latest version of [Jimi](http://bit.ly/TnY4NS). The archive contains all needed dependences except Weblogic/JBoss client jars.  
1. Unzip archive somewhere on your server. We are going to refer to this place as `JIMI_HOME`.
1. Copy `JIMI_HOME/config/jimi.yaml.example` into `JIMI_HOME/config/jimi.yaml` and modify it according to your needs.

If you plan to work with Weblogic or JBoss servers you have to follow these [additional steps](https://github.com/arozhkov/jimi-robot/wiki/Weblogic-JBoss).


## Configuration

All Jimi configuration defined in one file - `jimi.yaml`. You can name this file as you want, but here we will use highlighted name for consistency.

__YAML:__ read and understand syntax
> [wikipedia](http://en.wikipedia.org/wiki/YAML)  
> [yaml.org](http://yaml.org/spec/1.1/)  
> [snakeyaml](http://code.google.com/p/snakeyaml/wiki/Documentation)  


## _jimi.yaml_

The configuration starts with the definition of writers that will be used for all sources defined in the same file. Definitions of writers and sources start with a tag referring to the object's class, like `!graphite` or `!jvm`. Each class needs its own set of properties to get the work done. Properties' names are self-explanatory most of the time. 

The example file contains definitions of two writers: `!console` and `!graphite`. The first prints events to the console the second sends events to Graphite server.

Only one source is defined `!jvm`, you may need to update `host` and `port` properties. `metrics` property contains the names of metric groups defined in metrics library folder `JIMI_HOME/metrics`. You can use existing metrics or create your own, more on this [here](https://github.com/arozhkov/jimi-robot/wiki/Metrics).

The last element of this file is `executorThreadPoolSize` property. It defines the number of threads that will collect metrics.

```yaml
writers: 
  - !console
    format: "${source.label} ${metric} ${value} ${ts}"
 
  - !graphite
    host: 172.0.0.1
    port: 2003
    format: "jimi.${source.label}.${metric} ${value} ${ts}"
        
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

```bash
run.sh <name_of_config_file_without_extension>
```

## Ideas behind this project

Jimi is highly inspired by my previous non open-source works, articles from [monitoring sucks](http://monitoring.no.de/) and the jmxtrans project.

It's not that difficult to collect JMX counters, the difficulty is to create a tool easy to configure, maintain and integrate with other applications. _Having said that, it's even harder to make good use of collected data._


## Yes, you can help

* Let me know if you use Jimi
* Report bugs
* Share metrics definitions
* Correct my English :)


## [Find more information on Jimi's wiki](https://github.com/arozhkov/jimi-robot/wiki)
