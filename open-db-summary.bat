@echo off
setlocal
cd /d "%~dp0"

echo =======================================================================
echo TrendSense Database Summary Viewer
echo =======================================================================
echo.

set "H2_JAR=%USERPROFILE%\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar"

if exist "%H2_JAR%" (
    java -cp "target\classes;%H2_JAR%" com.socialanalytics.database.DbSummaryViewer
) else (
    call .\mvn.bat exec:java -Dexec.mainClass="com.socialanalytics.database.DbSummaryViewer"
)

echo.
pause
