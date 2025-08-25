@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass any JVM options to Gradle and Java processes.
@rem For more information, see https://docs.gradle.org/current/userguide/build_environment.html
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windowz variants
if not "%OS%" == "Windows_NT" goto oldWindows
if "%~1"=="" goto main
if "%~1"=="-version" goto main
if "%~1"=="--version" goto main
if "%~1"=="-v" goto main
if "%~1"=="--v" goto main
if "%~1"=="-?" goto main
if "%~1"=="/?" goto main
if "%~1"=="-h" goto main
if "%~1"=="--help" goto main
if "%~1"=="-d" goto main
if "%~1"=="--debug" goto main
if "%~1"=="-i" goto main
if "%~1"=="--info" goto main
if "%~1"=="-s" goto main
if "%~1"=="--stacktrace" goto main
if "%~1"=="-q" goto main
if "%~1"=="--quiet" goto main
goto main

:oldWindows
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
goto main

:main
@rem %* is the remainder of the command line
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code.
if not "" == "%GRADLE_EXIT_CONSOLE%" (
  exit 1
) else (
  exit /b 1
)

:mainEnd
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code.
if not "" == "%GRADLE_EXIT_CONSOLE%" (
  exit 0
) else (
  exit /b 0
)
