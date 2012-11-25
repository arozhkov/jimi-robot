@echo off
setlocal

rem set JAVA_HOME=c:\java\jdk1.6_35
rem set JIMI_HOME=c:\jimi
rem set WLS_LIBS=c:\Oracle\Middleware\wlserver_10.3\server\lib

set JAVA_OPTS=-Duser.timezone=GMT

if "%JAVA_HOME%"=="" (
    echo "Undefined JAVA_HOME"
    exit 1
)

if "%JIMI_HOME%"=="" (
    echo "Undefined JIMI_HOME"
    exit 1
)

set JIMI_JAR=%JIMI_HOME%\jimi-0.0.1-SNAPSHOT.jar

set CLASSPATH=%JIMI_HOME%\
set CLASSPATH=%CLASSPATH%;%JIMI_HOME%\lib\log4j-1.2.16.jar
set CLASSPATH=%CLASSPATH%;%JIMI_HOME%\lib\slf4j-api-1.7.1.jar
set CLASSPATH=%CLASSPATH%;%JIMI_HOME%\lib\slf4j-log4j12-1.7.1.jar
set CLASSPATH=%CLASSPATH%;%JIMI_HOME%\lib\snakeyaml-1.10.jar

if NOT "%WLS_LIBS%"=="" (
    set CLASSPATH=%CLASSPATH%;%WLS_LIBS%\wlclient.jar
    set CLASSPATH=%CLASSPATH%;%WLS_LIBS%\wljmxclient.jar
)

if NOT %JAVA_OPTS%=="" (
  set JIMI_CMD=%JAVA_OPTS% -cp %CLASSPATH%;%JIMI_JAR% com.opshack.jimi.Jimi %*
) else (
  et JIMI_CMD=-cp %CLASSPATH%;%JIMI_JAR% com.opshack.jimi.Jimi %*
)

echo.
echo Start jimi with
echo JAVA_HOME = %JAVA_HOME%
echo JIMI_HOME = %JIMI_HOME%
echo JAVA_OPTS = %JAVA_OPTS%
echo CLASSPATH = %CLASSPATH%
echo.

"%JAVA_HOME%\bin\java.exe" %JIMI_CMD% 

endlocal