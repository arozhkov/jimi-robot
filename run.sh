#!/bin/bash

JIMI_HOME=~/jimi
WLS_LIBS=$JIMI_HOME/lib

JAVA_OPTS="$JAVA_OPTS -Duser.timezone=GMT"

#JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote \
#-Dcom.sun.management.jmxremote.port=9001 \
#-Dcom.sun.management.jmxremote.local.only=false \
#-Dcom.sun.management.jmxremote.authenticate=false \
#-Dcom.sun.management.jmxremote.ssl=false"


JIMI_JAR=${JIMI_HOME}/jimi.jar

CLASSPATH=$JIMI_HOME/.
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/log4j-1.2.16.jar
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/slf4j-api-1.7.1.jar
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/slf4j-log4j12-1.7.1.jar
CLASSPATH=$CLASSPATH:$JIMI_HOME/lib/snakeyaml-1.10.jar

CLASSPATH=$CLASSPATH:$WLS_LIBS/wlclient.jar
CLASSPATH=$CLASSPATH:$WLS_LIBS/wljmxclient.jar

JIMI_CMD="${JAVA_OPTS} -cp ${CLASSPATH}:${JIMI_JAR} com.opshack.jimi.Jimi $*"

echo ${JIMI_CMD}

java ${JIMI_CMD}
