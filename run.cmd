@echo off
setlocal

REM ---------------------------------------------------------------------
REM  Self-contained ETL demo runner for Windows.
REM    1. Locate a JDK 17+ (JAVA_HOME or PATH).
REM    2. Compile every .java file under src\ into bin\.
REM    3. Run etl.EtlPipeline from bin\.
REM ---------------------------------------------------------------------

cd /d "%~dp0"

set "JAVA_EXE=java"
set "JAVAC_EXE=javac"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"

"%JAVAC_EXE%" -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERROR] No JDK found on PATH and JAVA_HOME is not set.
    echo Install JDK 17+ from https://adoptium.net/ and either:
    echo   - add it to your PATH, or
    echo   - set JAVA_HOME to the JDK install folder.
    exit /b 1
)

if not exist bin mkdir bin

REM Collect every .java file under src\ into a sources file (forward slashes are fine on Windows javac)
set "SOURCES=%TEMP%\java-etl-demo-sources.txt"
if exist "%SOURCES%" del "%SOURCES%"
for /r src %%f in (*.java) do echo %%f>>"%SOURCES%"

echo Compiling sources...
"%JAVAC_EXE%" -d bin "@%SOURCES%"
set "RC=%ERRORLEVEL%"
del "%SOURCES%" 2>nul
if not "%RC%"=="0" exit /b %RC%

echo.
echo Using Java:
"%JAVA_EXE%" -version
echo.

"%JAVA_EXE%" -cp bin etl.EtlPipeline
exit /b %ERRORLEVEL%
