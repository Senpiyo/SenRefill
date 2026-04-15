@echo off
call "%~dp0_set_java25_env.bat"
cd /d "%~dp0"
echo ==========================================
echo AutoRefill TEST20 - build
echo ==========================================
echo.
call gradlew.bat clean
call gradlew.bat build
pause
