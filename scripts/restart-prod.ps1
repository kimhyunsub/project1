Param()

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot ".env.prod"
$composeArgs = @("-f", "docker-compose.prod.yml", "--env-file", ".env.prod")

Write-Host "==> Backend production restart started" -ForegroundColor Cyan
Write-Host "Project root: $projectRoot"

if (-not (Test-Path $envFile)) {
    throw ".env.prod file was not found. Expected path: $envFile"
}

Set-Location $projectRoot

Write-Host "==> Rebuilding and restarting containers without git pull" -ForegroundColor Yellow
docker compose @composeArgs up --build -d

Write-Host "==> Current container status" -ForegroundColor Yellow
docker compose @composeArgs ps

Write-Host "==> Recent backend logs" -ForegroundColor Yellow
docker compose @composeArgs logs app --tail=100

Write-Host "==> Backend production restart finished" -ForegroundColor Green

