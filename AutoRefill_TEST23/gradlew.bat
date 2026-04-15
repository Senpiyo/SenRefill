@echo off
setlocal EnableExtensions

set "APP_DIR=%~dp0"
if "%APP_DIR:~-1%"=="\" set "APP_DIR=%APP_DIR:~0,-1%"

call "%APP_DIR%\_set_java25_env.bat"
if errorlevel 1 (
    echo [TEST20] Java 25 setup failed.
    exit /b 1
)

set "GRADLE_VERSION=9.4.0"
set "DIST_DIR=%APP_DIR%\.gradle-dist\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%DIST_DIR%\bin\gradle.bat"
set "ZIP_PATH=%APP_DIR%\.gradle-dist\gradle-%GRADLE_VERSION%-bin.zip"
set "URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%APP_DIR%\.gradle-dist" mkdir "%APP_DIR%\.gradle-dist"

if not exist "%GRADLE_BIN%" (
    echo [TEST20] Local Gradle %GRADLE_VERSION% not found. Downloading...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%URL%' -OutFile '%ZIP_PATH%'"
    if errorlevel 1 goto :fail
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%APP_DIR%\.gradle-dist' -Force"
    if errorlevel 1 goto :fail
)

call "%GRADLE_BIN%" %*
exit /b %errorlevel%

:fail
echo [TEST20] Gradle download or extraction failed.
exit /b 1
