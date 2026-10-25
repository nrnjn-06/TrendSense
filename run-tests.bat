@echo off
setlocal
cd /d "%~dp0"

echo =======================================================================
echo Running TrendSense Automated JUnit 5 Test Suite...
echo =======================================================================

call .\mvn.bat test
