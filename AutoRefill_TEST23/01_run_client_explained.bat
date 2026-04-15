@echo off
call "%~dp0_set_java25_env.bat"
cd /d "%~dp0"
echo ==========================================
echo AutoRefill TEST20 - runClient
echo ==========================================
echo.
call gradlew.bat runClient
pause
