@echo off
setlocal

:: Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in your PATH.
    echo Please install Java 11 or newer to run this unified console.
    echo Downloading... opening https://adoptium.net in your browser
    start https://adoptium.net/
    pause
    exit /b 1
)

:: Get Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVAVER=%%g
)
set JAVAVER=%JAVAVER:"=%
for /f "delims=. tokens=1-3" %%v in ("%JAVAVER%") do (
    set JAVAVER_MAJOR=%%v
)

:: Handle 1.8.x version format mapping
if "%JAVAVER_MAJOR%"=="1" (
    echo [ERROR] Java 11 or newer is required. You are running an older 1.x version.
    start https://adoptium.net/
    pause
    exit /b 1
)

if %JAVAVER_MAJOR% lss 11 (
    echo [ERROR] Java 11 or newer is required. You are running version %JAVAVER_MAJOR%.
    start https://adoptium.net/
    pause
    exit /b 1
)

:: Run the fat JAR
echo Launching Unified Security Console...
set "APP_DIR=%~dp0"
java -jar "%APP_DIR%..\target\unified-console-1.0-SNAPSHOT.jar"

if %errorlevel% neq 0 (
    echo [ERROR] Application crashed or failed to launch. Check app_crash.log.
    pause
)

endlocal
