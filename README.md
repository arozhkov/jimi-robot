Jimi is highly inspired by my previous non open-source works, articles from [monitoring sucks](http://monitoring.no.de/) and the jmxtrans project.

![jimi](https://raw.github.com/arozhkov/jimi-robot/master/img/Jimi.png)

## Ideas behind this project
It's not that difficult to collect JMX counters, the difficulty is to create a tool easy to configure, maintain and integrate with other applications. _Having said that, it's even harder to make good use of collected data._

## Features

* Easy to setup and configure. _I think so._
* Light. _Well RAM is so cheap nowadays._
* Not intrusive. _As much as RMI could be._
* Integration with third-party tools. _Graphite_
* Compatible with the JVM platform JMX server and Weblogic JMX server. _True._
* Collect simple numeric attributes from mbeans and combined data attributes. _True._

## Concepts

* _Source_ – abstract representation of a source JMX server on a program level.
* _Metric_ – definition of a measure to be collected from the source.
* _Value_ – real value gathered from the source.
* _Event_ – the value within its context: source, metric, timestamp.
* _Writer_ – component of a program responsible for writing or sending events to external storage/analysis system. 


## Installation

__Before you begin:__ although the code is a mess, the application is stable and does what it is supposed to do.  I'm currently working on improvements.

1. Download [Jimi](http://bit.ly/TnY4NS). The archive contains all needed dependences except Weblogic client jars.  
1. Unzip archive somewhere on your server.
1. Open `jimi/run.sh` or `jimi\run.bat` and set `JIMI_HOME` variable with a path to the `jimi` directory.

If you plan to work with Weblogic servers, set `WLS_LIBS` variable with a path to the folder where `wljmxclient.jar` and `wlclient.jar` should be found.  It's important to have both of them.


## Configuration

There are two configuration files, the first for _writer_ and _sources_ - `config.yaml`, the second for _metrics_ – `metrics.yaml`. You can name these files as you want, but here we will use highlighted names for consistency.

__YAML:__ read and understand syntax
> [wikipedia](http://en.wikipedia.org/wiki/YAML)  
> [yaml.org](http://yaml.org/spec/1.1/)  
> [snakeyaml](http://code.google.com/p/snakeyaml/wiki/Documentation)  


## _metrics.yaml_

This file is a library of all metrics that you can collect from your servers. Metrics are split into groups and you can assign these groups to the sources. Please bear in mind that the collection process is not initiated by the presence of the metric in this file.
_Here is just an example of `metrics.yaml`, of course you can define other groups and add as many metrics as you need._

    metrics:

      Memory:
        - mbean:    java.lang:type=Memory
          attr:     HeapMemoryUsage
          subattr:  used
          rate:     10
          label:    Mem.HeapUsed

        - mbean:    java.lang:type=Memory
          attr:     NonHeapMemoryUsage
          subattr:  used
          rate:     10
          label:    Mem.NonHeapUsed

        - mbean:    java.lang:type=Memory
          attr:     HeapMemoryUsage
          subattr:  committed
          rate:     30
          label:    Mem.HeapCommitted

        - mbean:    java.lang:type=Memory
          attr:     NonHeapMemoryUsage
          subattr:  committed
          rate:     30
          label:    Mem.NonHeapCommitted

      MemoryPools:
        - mbean:    java.lang:type=MemoryPool,*
          attr:     Usage
          subattr:  used
          rate:     10
          label:    10s.MemPool.$Name.used

        - mbean:    java.lang:type=MemoryPool,*
          attr:     Usage
          subattr:  committed
          rate:     10
          label:    10s.MemPool.$Name.committed

      Threading:
        - mbean:    java.lang:type=Threading
          attr:     ThreadCount
          rate:     10
          label:    Thread.Count

## _config.yaml_

This file starts with the definition of a writer that will be used for all sources defined below. Definitions of the writer and sources start with a tag referring to the object's class. Each class needs its own set of properties to get the work done. Properties' names are self-explanatory most of the time. 

Only groups of metrics which are mentioned in `metricsLists` will be collected from the sources and processed by the writer.

The last element of this file is `executorThreadPoolSize` property. It defines the number of threads that will collect metrics.


    writer: !graphite
      host: localhost
      port: 2003

    sources:
      - !weblogic
        host:     192.168.0.35
        port:     7001
        username: weblogic
        password: weblogic01
        prefix:   wls
        metricsLists:
          - Memory
          - Threading
          
      - !weblogic
        host  :   192.168.0.35
        port  :   7002
        username: weblogic
        password: weblogic01
        prefix:   wls
        metricsLists:
          - Memory
          - MemoryPools
          
    executorThreadPoolSize: 2

## Usage

    run.sh path/to/config.yaml path/to/metrics.yaml
    
## Yes, you can help

* Let me know if you use Jimi
* Report bugs
* Share metrics definitions
* Correct my English :)


## [Find more information on Jimi's wiki](https://github.com/arozhkov/jimi-robot/wiki)

