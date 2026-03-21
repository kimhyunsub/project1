@echo off
setlocal

cd /d "%~dp0\.."

echo ==> Backend production restart started
powershell -ExecutionPolicy Bypass -File "%~dp0restart-prod.ps1"

if errorlevel 1 (
    echo.
    echo Restart failed.
    pause
    exit /b 1
)

echo.
echo Restart finished successfully.
pause

