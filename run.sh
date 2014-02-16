#!/bin/bash

JIMI_HOME=.
JIMI_NAME=${1:-"jimi"}

JAVA_OPTS="-Djimi.home=${JIMI_HOME} -Djimi.name=${JIMI_NAME} -Duser.timezone=GMT -Djava.security.policy=file:./jimi.policy"
JIMI_JAR=${JIMI_HOME}/jimi.jar

CLASSPATH=$JIMI_HOME/.:$JIMI_HOME/lib/*

JIMI_CMD="${JAVA_OPTS} -cp ${CLASSPATH}:${JIMI_JAR} com.opshack.jimi.Jimi ${JIMI_HOME}/config/${JIMI_NAME}.yaml"

echo ${JIMI_CMD}

java ${JIMI_CMD}