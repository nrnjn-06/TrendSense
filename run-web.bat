@echo off
setlocal
cd /d "%~dp0"

echo =======================================================================
echo Starting TrendSense Web Server (Jakarta Servlets + JSP)...
echo Access via browser at: http://localhost:8080/
echo =======================================================================
echo.

:: Automatically open default browser after a brief delay
start "" "http://localhost:8080/"

call .\mvn.bat exec:java -Dexec.mainClass="com.socialanalytics.web.WebServer"

if %ERRORLEVEL% neq 0 (
    echo.
    echo Server stopped with error.
    pause
)
