Param(
    [string]$Branch = "main",
    [switch]$SkipPull
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot ".env.prod"
$composeArgs = @("-f", "docker-compose.prod.yml", "--env-file", ".env.prod")

Write-Host "==> Backend production deploy started" -ForegroundColor Cyan
Write-Host "Project root: $projectRoot"

if (-not (Test-Path $envFile)) {
    throw ".env.prod file was not found. Expected path: $envFile"
}

Set-Location $projectRoot

if (-not $SkipPull) {
    Write-Host "==> Pulling latest code from origin/$Branch" -ForegroundColor Yellow
    git fetch origin $Branch
    git checkout $Branch
    git pull origin $Branch
}

Write-Host "==> Rebuilding and restarting containers" -ForegroundColor Yellow
docker compose @composeArgs up --build -d

Write-Host "==> Current container status" -ForegroundColor Yellow
docker compose @composeArgs ps

Write-Host "==> Recent backend logs" -ForegroundColor Yellow
docker compose @composeArgs logs app --tail=100

Write-Host "==> Production deploy finished" -ForegroundColor Green

