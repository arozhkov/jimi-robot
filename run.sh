#!/bin/bash

JIMI_HOME=.
JIMI_NAME=${1:-"jimi"}

WLS_LIBS=$JIMI_HOME/lib
JAVA_OPTS="-Djimi.home=${JIMI_HOME} -Djimi.name=${JIMI_NAME} -Duser.timezone=GMT -Djava.security.policy=file:./jimi.policy"
JIMI_JAR=${JIMI_HOME}/jimi.jar

CLASSPATH=$JIMI_HOME/.:$JIMI_HOME/lib/*.jar

CLASSPATH=$CLASSPATH:$WLS_LIBS/wlclient.jar
CLASSPATH=$CLASSPATH:$WLS_LIBS/wljmxclient.jar

JIMI_CMD="${JAVA_OPTS} -cp ${CLASSPATH}:${JIMI_JAR} com.opshack.jimi.Jimi ${JIMI_HOME}/config/${JIMI_NAME}.yaml"

echo ${JIMI_CMD}

nohup java ${JIMI_CMD} > ${JIMI_HOME}/logs/${JIMI_NAME}.out 2>&1 &
echo "$!" > ${JIMI_HOME}/logs/${JIMI_NAME}.pid
