@echo off
setlocal

set JIMI_HOME=.
set WLS_LIBS=%JIMI_HOME%\lib

set JAVA_OPTS="-Duser.timezone=GMT "

set JIMI_JAR=%JIMI_HOME%\jimi.jar

set CLASSPATH=%JIMI_HOME%\.
set CLASSPATH=%CLASSPATH%;%JIMI_HOME%\lib\log4j-1.2.16.jar
set CLASSPATH=%CLASSPATH%;%JIMI_HOME%\lib\slf4j-api-1.7.1.jar
set CLASSPATH=%CLASSPATH%;%JIMI_HOME%\lib\slf4j-log4j12-1.7.1.jar
set CLASSPATH=%CLASSPATH%;%JIMI_HOME%\lib\snakeyaml-1.10.jar

if NOT "%WLS_LIBS%"=="" (
    set CLASSPATH=%CLASSPATH%;%WLS_LIBS%\wljmxclient.jar
)

set CLASSPATH=%CLASSPATH%;%JIMI_JAR%


if NOT %JAVA_OPTS%=="" (
  set JIMI_CMD=%JAVA_OPTS% -cp %CLASSPATH%;%JIMI_JAR% com.opshack.jimi.Jimi %*
) else (
  et JIMI_CMD=-cp %CLASSPATH%;%JIMI_JAR% com.opshack.jimi.Jimi %*
)

echo.
echo JIMI_HOME = %JIMI_HOME%
echo JAVA_OPTS = %JAVA_OPTS%
echo CLASSPATH = %CLASSPATH%
echo.

java.exe %JIMI_CMD% 

endlocal