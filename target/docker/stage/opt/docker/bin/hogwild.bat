@REM hogwild launcher script
@REM
@REM Environment:
@REM JAVA_HOME - location of a JDK home dir (optional if java on path)
@REM CFG_OPTS  - JVM options (optional)
@REM Configuration:
@REM HOGWILD_config.txt found in the HOGWILD_HOME.
@setlocal enabledelayedexpansion

@echo off


if "%HOGWILD_HOME%"=="" (
  set "APP_HOME=%~dp0\\.."

  rem Also set the old env name for backwards compatibility
  set "HOGWILD_HOME=%~dp0\\.."
) else (
  set "APP_HOME=%HOGWILD_HOME%"
)

set "APP_LIB_DIR=%APP_HOME%\lib\"

rem Detect if we were double clicked, although theoretically A user could
rem manually run cmd /c
for %%x in (!cmdcmdline!) do if %%~x==/c set DOUBLECLICKED=1

rem FIRST we load the config file of extra options.
set "CFG_FILE=%APP_HOME%\HOGWILD_config.txt"
set CFG_OPTS=
call :parse_config "%CFG_FILE%" CFG_OPTS

rem We use the value of the JAVACMD environment variable if defined
set _JAVACMD=%JAVACMD%

if "%_JAVACMD%"=="" (
  if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
  )
)

if "%_JAVACMD%"=="" set _JAVACMD=java

rem Detect if this java is ok to use.
for /F %%j in ('"%_JAVACMD%" -version  2^>^&1') do (
  if %%~j==java set JAVAINSTALLED=1
  if %%~j==openjdk set JAVAINSTALLED=1
)

rem BAT has no logical or, so we do it OLD SCHOOL! Oppan Redmond Style
set JAVAOK=true
if not defined JAVAINSTALLED set JAVAOK=false

if "%JAVAOK%"=="false" (
  echo.
  echo A Java JDK is not installed or can't be found.
  if not "%JAVA_HOME%"=="" (
    echo JAVA_HOME = "%JAVA_HOME%"
  )
  echo.
  echo Please go to
  echo   http://www.oracle.com/technetwork/java/javase/downloads/index.html
  echo and download a valid Java JDK and install before running hogwild.
  echo.
  echo If you think this message is in error, please check
  echo your environment variables to see if "java.exe" and "javac.exe" are
  echo available via JAVA_HOME or PATH.
  echo.
  if defined DOUBLECLICKED pause
  exit /B 1
)


rem We use the value of the JAVA_OPTS environment variable if defined, rather than the config.
set _JAVA_OPTS=%JAVA_OPTS%
if "!_JAVA_OPTS!"=="" set _JAVA_OPTS=!CFG_OPTS!

rem We keep in _JAVA_PARAMS all -J-prefixed and -D-prefixed arguments
rem "-J" is stripped, "-D" is left as is, and everything is appended to JAVA_OPTS
set _JAVA_PARAMS=
set _APP_ARGS=

set "APP_CLASSPATH=%APP_LIB_DIR%\default.hogwild-0.1.jar;%APP_LIB_DIR%\org.scala-lang.scala-library-2.12.6.jar;%APP_LIB_DIR%\com.thesamet.scalapb.scalapb-runtime_2.12-0.7.0.jar;%APP_LIB_DIR%\com.thesamet.scalapb.lenses_2.12-0.7.0-test2.jar;%APP_LIB_DIR%\com.lihaoyi.fastparse_2.12-1.0.0.jar;%APP_LIB_DIR%\com.lihaoyi.fastparse-utils_2.12-1.0.0.jar;%APP_LIB_DIR%\com.lihaoyi.sourcecode_2.12-0.1.4.jar;%APP_LIB_DIR%\org.scalactic.scalactic_2.12-3.0.5.jar;%APP_LIB_DIR%\org.scala-lang.scala-reflect-2.12.6.jar;%APP_LIB_DIR%\io.grpc.grpc-netty-1.10.0.jar;%APP_LIB_DIR%\io.grpc.grpc-core-1.10.0.jar;%APP_LIB_DIR%\io.grpc.grpc-context-1.10.0.jar;%APP_LIB_DIR%\com.google.code.gson.gson-2.7.jar;%APP_LIB_DIR%\com.google.guava.guava-19.0.jar;%APP_LIB_DIR%\com.google.errorprone.error_prone_annotations-2.1.2.jar;%APP_LIB_DIR%\com.google.code.findbugs.jsr305-3.0.0.jar;%APP_LIB_DIR%\io.opencensus.opencensus-api-0.11.0.jar;%APP_LIB_DIR%\io.opencensus.opencensus-contrib-grpc-metrics-0.11.0.jar;%APP_LIB_DIR%\io.netty.netty-codec-http2-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-codec-http-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-codec-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-transport-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-buffer-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-common-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-resolver-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-handler-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-handler-proxy-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-codec-socks-4.1.17.Final.jar;%APP_LIB_DIR%\com.thesamet.scalapb.scalapb-runtime-grpc_2.12-0.7.0.jar;%APP_LIB_DIR%\io.grpc.grpc-stub-1.10.0.jar;%APP_LIB_DIR%\io.grpc.grpc-protobuf-1.10.0.jar;%APP_LIB_DIR%\com.google.protobuf.protobuf-java-3.5.1.jar;%APP_LIB_DIR%\com.google.protobuf.protobuf-java-util-3.5.1.jar;%APP_LIB_DIR%\com.google.api.grpc.proto-google-common-protos-1.0.0.jar;%APP_LIB_DIR%\io.grpc.grpc-protobuf-lite-1.10.0.jar;%APP_LIB_DIR%\io.monix.monix-types_2.12-2.3.0.jar;%APP_LIB_DIR%\io.monix.monix-execution_2.12-2.3.0.jar;%APP_LIB_DIR%\org.reactivestreams.reactive-streams-1.0.0.jar;%APP_LIB_DIR%\io.monix.monix-eval_2.12-2.3.0.jar;%APP_LIB_DIR%\io.monix.monix-reactive_2.12-2.3.0.jar;%APP_LIB_DIR%\org.jctools.jctools-core-2.0.1.jar"
set "APP_MAIN_CLASS=launcher.Launcher"
set "SCRIPT_CONF_FILE=%APP_HOME%\conf\application.ini"

rem if configuration files exist, prepend their contents to the script arguments so it can be processed by this runner
call :parse_config "%SCRIPT_CONF_FILE%" SCRIPT_CONF_ARGS

call :process_args %SCRIPT_CONF_ARGS% %%*

set _JAVA_OPTS=!_JAVA_OPTS! !_JAVA_PARAMS!

if defined CUSTOM_MAIN_CLASS (
    set MAIN_CLASS=!CUSTOM_MAIN_CLASS!
) else (
    set MAIN_CLASS=!APP_MAIN_CLASS!
)

rem Call the application and pass all arguments unchanged.
"%_JAVACMD%" !_JAVA_OPTS! !HOGWILD_OPTS! -cp "%APP_CLASSPATH%" %MAIN_CLASS% !_APP_ARGS!

@endlocal

exit /B %ERRORLEVEL%


rem Loads a configuration file full of default command line options for this script.
rem First argument is the path to the config file.
rem Second argument is the name of the environment variable to write to.
:parse_config
  set _PARSE_FILE=%~1
  set _PARSE_OUT=
  if exist "%_PARSE_FILE%" (
    FOR /F "tokens=* eol=# usebackq delims=" %%i IN ("%_PARSE_FILE%") DO (
      set _PARSE_OUT=!_PARSE_OUT! %%i
    )
  )
  set %2=!_PARSE_OUT!
exit /B 0


:add_java
  set _JAVA_PARAMS=!_JAVA_PARAMS! %*
exit /B 0


:add_app
  set _APP_ARGS=!_APP_ARGS! %*
exit /B 0


rem Processes incoming arguments and places them in appropriate global variables
:process_args
  :param_loop
  call set _PARAM1=%%1
  set "_TEST_PARAM=%~1"

  if ["!_PARAM1!"]==[""] goto param_afterloop


  rem ignore arguments that do not start with '-'
  if "%_TEST_PARAM:~0,1%"=="-" goto param_java_check
  set _APP_ARGS=!_APP_ARGS! !_PARAM1!
  shift
  goto param_loop

  :param_java_check
  if "!_TEST_PARAM:~0,2!"=="-J" (
    rem strip -J prefix
    set _JAVA_PARAMS=!_JAVA_PARAMS! !_TEST_PARAM:~2!
    shift
    goto param_loop
  )

  if "!_TEST_PARAM:~0,2!"=="-D" (
    rem test if this was double-quoted property "-Dprop=42"
    for /F "delims== tokens=1,*" %%G in ("!_TEST_PARAM!") DO (
      if not ["%%H"] == [""] (
        set _JAVA_PARAMS=!_JAVA_PARAMS! !_PARAM1!
      ) else if [%2] neq [] (
        rem it was a normal property: -Dprop=42 or -Drop="42"
        call set _PARAM1=%%1=%%2
        set _JAVA_PARAMS=!_JAVA_PARAMS! !_PARAM1!
        shift
      )
    )
  ) else (
    if "!_TEST_PARAM!"=="-main" (
      call set CUSTOM_MAIN_CLASS=%%2
      shift
    ) else (
      set _APP_ARGS=!_APP_ARGS! !_PARAM1!
    )
  )
  shift
  goto param_loop
  :param_afterloop

exit /B 0
