@echo off
cd /d "%~dp0"
echo ==========================================
echo AutoRefill TEST20 - clean ^& build
echo ==========================================
gradlew.bat clean build > build_log.txt 2>&1
type build_log.txt
echo.
echo [done] build_log.txt saved in this folder
pause
