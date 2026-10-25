@echo off
setlocal
cd /d "%~dp0"

echo =======================================================================
echo Starting TrendSense Desktop Dashboard (JavaFX 21)...
echo =======================================================================

call .\mvn.bat javafx:run
