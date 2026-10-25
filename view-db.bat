@echo off
setlocal
cd /d "%~dp0"

echo =======================================================================
echo Starting H2 Database Web Console...
echo =======================================================================
echo.
echo Connection parameters to enter in your browser:
echo   - Driver Class : org.h2.Driver
echo   - JDBC URL     : jdbc:h2:./data/social_analytics;MODE=MySQL;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE
echo   - User Name    : sa
echo   - Password     : (leave completely empty)
echo.
echo Opening browser at http://localhost:8082 ...
echo.
echo (Keep this window open while using the database. Press Ctrl+C to stop.)
echo =======================================================================

set "H2_JAR=%USERPROFILE%\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar"

if exist "%H2_JAR%" (
    start "" "http://localhost:8082"
    java -cp "%H2_JAR%" org.h2.tools.Console -web -webPort 8082 -webAllowOthers
) else (
    call .\mvn.bat exec:java -Dexec.mainClass="com.socialanalytics.database.H2ConsoleLauncher"
)

if %ERRORLEVEL% neq 0 (
    echo.
    echo An error occurred while starting H2 console.
    pause
)
