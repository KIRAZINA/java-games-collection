@echo off
setlocal

cd /d "%~dp0"

set "LAUNCHER_JAR=games-launcher\target\games-launcher-1.0-SNAPSHOT.jar"

if not exist "%LAUNCHER_JAR%" (
    call mvn clean package
    if errorlevel 1 (
        echo Build failed.
        pause
        exit /b %errorlevel%
    )
)

set "JAVA_GUI_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVA_GUI_EXE=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVA_GUI_EXE for %%I in (javaw.exe) do set "JAVA_GUI_EXE=%%~$PATH:I"
if not defined JAVA_GUI_EXE if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_GUI_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_GUI_EXE for %%I in (java.exe) do set "JAVA_GUI_EXE=%%~$PATH:I"

if not defined JAVA_GUI_EXE (
    echo Java was not found. Install JDK 17+ and try again.
    pause
    exit /b 1
)

start "" "%JAVA_GUI_EXE%" -jar "%LAUNCHER_JAR%"
exit /b 0
