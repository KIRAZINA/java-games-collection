@echo off
setlocal

cd /d "%~dp0"
call mvn clean package
if errorlevel 1 exit /b %errorlevel%

java -jar games-launcher\target\games-launcher-1.0-SNAPSHOT.jar
