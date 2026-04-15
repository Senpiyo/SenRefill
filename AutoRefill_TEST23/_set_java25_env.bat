@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "JAVA25_FOUND="

call :tryCandidate "%JAVA_HOME%"
call :tryCandidate "%JDK25_HOME%"

for %%R in ("%LOCALAPPDATA%\Programs\Eclipse Adoptium" "%ProgramFiles%\Eclipse Adoptium" "%ProgramFiles%\Java" "%ProgramFiles%\Microsoft" "%LOCALAPPDATA%\Programs\Microsoft" "C:\Users\%USERNAME%\AppData\Local\Programs\Eclipse Adoptium") do (
    if not defined JAVA25_FOUND if exist "%%~fR" (
        for /f "delims=" %%D in ('dir /ad /b "%%~fR" 2^>nul ^| findstr /i /r "^jdk-25 ^jdk25 ^openjdk-25 ^openjdk25"') do (
            if not defined JAVA25_FOUND call :tryCandidate "%%~fR\%%~D"
        )
    )
)

if not defined JAVA25_FOUND call :tryRegistry "HKLM\SOFTWARE\Eclipse Adoptium\JDK\25" "Path"
if not defined JAVA25_FOUND call :tryRegistry "HKCU\SOFTWARE\Eclipse Adoptium\JDK\25" "Path"
if not defined JAVA25_FOUND call :tryRegistry "HKLM\SOFTWARE\JavaSoft\JDK\25" "JavaHome"
if not defined JAVA25_FOUND call :tryRegistry "HKCU\SOFTWARE\JavaSoft\JDK\25" "JavaHome"
if not defined JAVA25_FOUND call :tryRegistry "HKLM\SOFTWARE\Microsoft\JDK\25" "Path"
if not defined JAVA25_FOUND call :tryRegistry "HKCU\SOFTWARE\Microsoft\JDK\25" "Path"

if not defined JAVA25_FOUND (
    echo [ERROR] Java 25 was not found.
    echo.
    echo Checked:
    echo - JAVA_HOME
    echo - JDK25_HOME
    echo - %%LOCALAPPDATA%%\Programs\Eclipse Adoptium
    echo - %%ProgramFiles%%\Eclipse Adoptium
    echo - %%ProgramFiles%%\Java
    echo - %%ProgramFiles%%\Microsoft
    echo - Java registry keys
    echo.
    echo Install Java 25 first, then run this file again.
    endlocal & exit /b 1
)

set "NEWPATH=%JAVA25_FOUND%\bin;%PATH%"
echo [OK] Java 25 found:
echo %JAVA25_FOUND%
endlocal & (
    set "JAVA_HOME=%JAVA25_FOUND%"
    set "PATH=%NEWPATH%"
)
exit /b 0

:tryRegistry
for /f "skip=2 tokens=1,2,*" %%A in ('reg query "%~1" /v "%~2" 2^>nul') do (
    if /i "%%A"=="%~2" call :tryCandidate "%%C"
)
exit /b 0

:tryCandidate
set "CAND=%~1"
if not defined CAND exit /b 0
if "%CAND:~-1%"=="\" set "CAND=%CAND:~0,-1%"
if /i "%CAND:~-4%"=="\bin" set "CAND=%CAND:~0,-4%"
if not exist "%CAND%\bin\java.exe" exit /b 0

if exist "%CAND%\release" (
    findstr /i /c:"JAVA_VERSION=\"25" "%CAND%\release" 1>nul 2>nul
    if errorlevel 1 exit /b 0
) else (
    echo %CAND%| findstr /i "25" 1>nul 2>nul
    if errorlevel 1 exit /b 0
)
set "JAVA25_FOUND=%CAND%"
exit /b 0
