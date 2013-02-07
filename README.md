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

1. Download [Jimi](http://bit.ly/TnY4NS). The archive contains all needed dependences except Weblogic/JBoss client jars.  
1. Unzip archive somewhere on your server.
1. Open `jimi/run.sh` or `jimi\run.bat` and set `JIMI_HOME` variable with a path to the `jimi` directory.
1. Copy config.yaml.example into config.yaml

If you plan to work with Weblogic or JBoss servers you have follow these additional steps.


## Configuration

All Jimi configuration defined in one file - `config.yaml`. You can name this file as you want, but here we will use highlighted name for consistency.

__YAML:__ read and understand syntax
> [wikipedia](http://en.wikipedia.org/wiki/YAML)  
> [yaml.org](http://yaml.org/spec/1.1/)  
> [snakeyaml](http://code.google.com/p/snakeyaml/wiki/Documentation)  


## _config.yaml_

The configuration starts with the definition of writers that will be used for all sources defined in the same file. Definitions of writers and sources start with a tag referring to the object's class, like `!graphite` or `!weblogic`. Each class needs its own set of properties to get the work done. Properties' names are self-explanatory most of the time. 

`metrics` property contains the names of metric groups defined in metrics library folder `JIMI_HOME/metrics`

The last element of this file is `executorThreadPoolSize` property. It defines the number of threads that will collect metrics.

    writers: 
    - !graphite
        host: localhost
        port: 2003

    sources:
      - !weblogic
        host:     localhost
        port:     7001
        username: weblogic
        password: weblogic01
        prefix:   wls
        metrics:
          - Memory
          - Threading
          
    executorThreadPoolSize: 2


## Usage

    run.sh path/to/config.yaml


## Ideas behind this project

Jimi is highly inspired by my previous non open-source works, articles from [monitoring sucks](http://monitoring.no.de/) and the jmxtrans project.

It's not that difficult to collect JMX counters, the difficulty is to create a tool easy to configure, maintain and integrate with other applications. _Having said that, it's even harder to make good use of collected data._


## Yes, you can help

* Let me know if you use Jimi
* Report bugs
* Share metrics definitions
* Correct my English :)


## [Find more information on Jimi's wiki](https://github.com/arozhkov/jimi-robot/wiki)
