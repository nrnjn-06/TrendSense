@echo off
setlocal

:: Discover Java if JAVA_HOME is not set
if not defined JAVA_HOME (
    where java >nul 2>&1
    if errorlevel 1 (
        for /d %%J in ("%USERPROFILE%\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21*\jre") do (
            if exist "%%J\bin\java.exe" set "JAVA_HOME=%%J"
        )
    )
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

:: 1. Search for external mvn.cmd or mvn.exe in PATH (excluding this batch file)
set "EXTERNAL_MVN="
for /f "delims=" %%I in ('where "$path:mvn.cmd" 2^>nul') do (
    if /i not "%%~dpnxI"=="%~dpnx0" (
        set "EXTERNAL_MVN=%%I"
        goto :runExternal
    )
)
for /f "delims=" %%I in ('where "$path:mvn.exe" 2^>nul') do (
    if /i not "%%~dpnxI"=="%~dpnx0" (
        set "EXTERNAL_MVN=%%I"
        goto :runExternal
    )
)

:: 2. Check MAVEN_HOME or M2_HOME
if defined MAVEN_HOME (
    if exist "%MAVEN_HOME%\bin\mvn.cmd" (
        set "EXTERNAL_MVN=%MAVEN_HOME%\bin\mvn.cmd"
        goto :runExternal
    )
)
if defined M2_HOME (
    if exist "%M2_HOME%\bin\mvn.cmd" (
        set "EXTERNAL_MVN=%M2_HOME%\bin\mvn.cmd"
        goto :runExternal
    )
)

:: 3. Check VS Code Java extension bundled Maven
for /d %%D in ("%USERPROFILE%\.vscode\extensions\oracle.oracle-java*") do (
    if exist "%%D\nbcode\java\maven\bin\mvn.cmd" (
        set "EXTERNAL_MVN=%%D\nbcode\java\maven\bin\mvn.cmd"
        goto :runExternal
    )
)

echo [ERROR] Apache Maven was not found in PATH or standard installation locations.
echo Please install Apache Maven and add it to your PATH: https://maven.apache.org/download.cgi
exit /b 1

:runExternal
"%EXTERNAL_MVN%" %*
exit /b %ERRORLEVEL%
