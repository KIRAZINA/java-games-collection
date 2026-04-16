@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

set "APP_NAME=Java Games Hub"
set "APP_VERSION=1.0.0"
set "VENDOR=KIRA_ZINA"
set "DIST_DIR=%CD%\dist"
set "BUILD_DIR=%CD%\build\jpackage"
set "INPUT_DIR=%BUILD_DIR%\input"
set "APP_CONTENT_DIR=%BUILD_DIR%\app-content"
set "APP_IMAGE_DIR=%DIST_DIR%\%APP_NAME%"

call :resolve_jpackage
if errorlevel 1 exit /b %errorlevel%

call mvn clean package
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b %errorlevel%
)

if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
if exist "%APP_IMAGE_DIR%" rmdir /s /q "%APP_IMAGE_DIR%"
if exist "%DIST_DIR%\%APP_NAME%-%APP_VERSION%.exe" del /f /q "%DIST_DIR%\%APP_NAME%-%APP_VERSION%.exe"
if not exist "%INPUT_DIR%" mkdir "%INPUT_DIR%"
if not exist "%APP_CONTENT_DIR%\2048-game\target" mkdir "%APP_CONTENT_DIR%\2048-game\target"
if not exist "%APP_CONTENT_DIR%\Minesweeper-game\target" mkdir "%APP_CONTENT_DIR%\Minesweeper-game\target"
if not exist "%APP_CONTENT_DIR%\Black-Jack-game\target" mkdir "%APP_CONTENT_DIR%\Black-Jack-game\target"
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

copy /y "games-launcher\target\games-launcher-1.0-SNAPSHOT.jar" "%INPUT_DIR%\" >nul
copy /y "2048-game\target\2048-game-1.0-SNAPSHOT.jar" "%APP_CONTENT_DIR%\2048-game\target\" >nul
copy /y "Minesweeper-game\target\Minesweeper-game-1.0-SNAPSHOT.jar" "%APP_CONTENT_DIR%\Minesweeper-game\target\" >nul
copy /y "Black-Jack-game\target\Black-Jack-game-1.0-SNAPSHOT.jar" "%APP_CONTENT_DIR%\Black-Jack-game\target\" >nul

echo Creating application image...
"%JPACKAGE_EXE%" ^
  --type app-image ^
  --dest "%DIST_DIR%" ^
  --input "%INPUT_DIR%" ^
  --name "%APP_NAME%" ^
  --main-jar "games-launcher-1.0-SNAPSHOT.jar" ^
  --main-class "com.KIRA_ZINA.launcher.GameHubLauncher" ^
  --app-version "%APP_VERSION%" ^
  --vendor "%VENDOR%" ^
  --description "Unified launcher for 2048, Minesweeper, and Blackjack."
if errorlevel 1 (
    echo Failed to create the application image.
    pause
    exit /b %errorlevel%
)

if not exist "%APP_IMAGE_DIR%\app\2048-game\target" mkdir "%APP_IMAGE_DIR%\app\2048-game\target"
if not exist "%APP_IMAGE_DIR%\app\Minesweeper-game\target" mkdir "%APP_IMAGE_DIR%\app\Minesweeper-game\target"
if not exist "%APP_IMAGE_DIR%\app\Black-Jack-game\target" mkdir "%APP_IMAGE_DIR%\app\Black-Jack-game\target"

copy /y "%APP_CONTENT_DIR%\2048-game\target\2048-game-1.0-SNAPSHOT.jar" "%APP_IMAGE_DIR%\app\2048-game\target\" >nul
copy /y "%APP_CONTENT_DIR%\Minesweeper-game\target\Minesweeper-game-1.0-SNAPSHOT.jar" "%APP_IMAGE_DIR%\app\Minesweeper-game\target\" >nul
copy /y "%APP_CONTENT_DIR%\Black-Jack-game\target\Black-Jack-game-1.0-SNAPSHOT.jar" "%APP_IMAGE_DIR%\app\Black-Jack-game\target\" >nul

echo Creating Windows installer...
where candle.exe >nul 2>nul
if errorlevel 1 (
    echo WiX Toolset was not found in PATH.
    echo Skipping installer creation and keeping the portable EXE build.
    echo.
    echo Done.
    echo Portable app image: "%APP_IMAGE_DIR%\%APP_NAME%.exe"
    echo Installer: skipped because WiX is not installed.
    pause
    exit /b 0
)

"%JPACKAGE_EXE%" ^
  --type exe ^
  --dest "%DIST_DIR%" ^
  --app-image "%APP_IMAGE_DIR%" ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "%VENDOR%" ^
  --description "Unified launcher for 2048, Minesweeper, and Blackjack." ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut ^
  --win-shortcut-prompt ^
  --win-per-user-install ^
  --install-dir "%APP_NAME%"
if errorlevel 1 (
    echo Failed to create the Windows installer.
    pause
    exit /b %errorlevel%
)

echo.
echo Done.
echo Portable app image: "%DIST_DIR%\%APP_NAME%\%APP_NAME%.exe"
echo Installer: "%DIST_DIR%\%APP_NAME%-%APP_VERSION%.exe"
pause
exit /b 0

:resolve_jpackage
set "JPACKAGE_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JPACKAGE_EXE=%JAVA_HOME%\bin\jpackage.exe"
if not defined JPACKAGE_EXE for /f "delims=" %%I in ('powershell -NoProfile -Command "$file = Get-ChildItem -Path \"$env:ProgramFiles\\Java\\*\\bin\\jpackage.exe\" -ErrorAction SilentlyContinue | Select-Object -Expand FullName -First 1; if ($file) { Write-Output $file }"') do (
    set "JPACKAGE_EXE=%%I"
)
if not defined JPACKAGE_EXE for /f "delims=" %%I in ('powershell -NoProfile -Command "$file = Get-ChildItem -Path \"$env:ProgramFiles\\Eclipse Adoptium\\*\\bin\\jpackage.exe\" -ErrorAction SilentlyContinue | Select-Object -Expand FullName -First 1; if ($file) { Write-Output $file }"') do (
    set "JPACKAGE_EXE=%%I"
)
if not defined JPACKAGE_EXE (
    echo jpackage.exe was not found.
    echo Install JDK 17+ and ensure JAVA_HOME points to the JDK folder.
    pause
    exit /b 1
)
exit /b 0
