@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ==========================================
echo AutoRefill TEST20 - Java 25 check
echo ==========================================
echo.
echo This file tries to find Java 25 automatically.
echo If Java 25 is found, it will show the exact java used.
echo.

echo [1/4] Find Java 25
echo ------------------------------------------
call "%~dp0\_set_java25_env.bat"
set "ERR=%ERRORLEVEL%"
echo ------------------------------------------
echo.

if not "%ERR%"=="0" goto :notfound

echo [2/4] java -version
echo ------------------------------------------
java -version
echo ------------------------------------------
echo.

echo [3/4] where java
echo ------------------------------------------
where java
echo ------------------------------------------
echo.

echo [4/4] JAVA_HOME
echo ------------------------------------------
echo %JAVA_HOME%
echo ------------------------------------------
echo.
echo OK: If you see version 25, you can run 01_run_client_explained.bat
echo.
pause
exit /b 0

:notfound
echo Result: Java 25 is not ready for this project.
echo.
echo Quick check of common folders:
echo ------------------------------------------
if exist "%LOCALAPPDATA%\Programs\Eclipse Adoptium" dir /ad /b "%LOCALAPPDATA%\Programs\Eclipse Adoptium"
if exist "%ProgramFiles%\Eclipse Adoptium" dir /ad /b "%ProgramFiles%\Eclipse Adoptium"
if exist "%ProgramFiles%\Java" dir /ad /b "%ProgramFiles%\Java"
if exist "%ProgramFiles%\Microsoft" dir /ad /b "%ProgramFiles%\Microsoft"
echo ------------------------------------------
echo.
echo Stop here. Do not run 01 or 02 yet.
echo.
pause
exit /b %ERR%
