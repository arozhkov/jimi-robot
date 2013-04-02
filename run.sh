#!/bin/bash

JIMI_HOME=.
JIMI_NAME=${1:-"jimi"}

WLS_LIBS=$JIMI_HOME/lib
JAVA_OPTS="-Djimi.home=${JIMI_HOME} -Djimi.name=${JIMI_NAME} -Duser.timezone=GMT"
JIMI_JAR=${JIMI_HOME}/jimi.jar

CLASSPATH=$JIMI_HOME/.
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/log4j-1.2.16.jar
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/slf4j-api-1.7.1.jar
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/slf4j-log4j12-1.7.1.jar
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/snakeyaml-1.10.jar
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/velocity-1.7.jar
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/commons-collections-3.2.1.jar
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/commons-lang-2.4.jar

CLASSPATH=$CLASSPATH:$WLS_LIBS/wlclient.jar
CLASSPATH=$CLASSPATH:$WLS_LIBS/wljmxclient.jar

JIMI_CMD="${JAVA_OPTS} -cp ${CLASSPATH}:${JIMI_JAR} com.opshack.jimi.Jimi ${JIMI_HOME}/config/${JIMI_NAME}.yaml"

echo ${JIMI_CMD}

nohup java ${JIMI_CMD} > ${JIMI_HOME}/logs/${JIMI_NAME}.out 2>&1 &
echo "$!" > ${JIMI_HOME}/logs/${JIMI_NAME}.pid
