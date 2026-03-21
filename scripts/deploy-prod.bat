@echo off
setlocal

cd /d "%~dp0\.."

echo ==> Backend production deploy started
powershell -ExecutionPolicy Bypass -File "%~dp0deploy-prod.ps1"

if errorlevel 1 (
    echo.
    echo Deployment failed.
    pause
    exit /b 1
)

echo.
echo Deployment finished successfully.
pause

