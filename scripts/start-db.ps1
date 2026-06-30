# Starts PostgreSQL for local development (Docker Compose).
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Test-PostgresPort {
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.Connect("127.0.0.1", 5432)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

if (Test-PostgresPort) {
    Write-Host "PostgreSQL is already listening on localhost:5432."
    exit 0
}

$docker = Get-Command docker -ErrorAction SilentlyContinue
if (-not $docker) {
    Write-Host ""
    Write-Host "PostgreSQL is not running and Docker was not found on PATH."
    Write-Host ""
    Write-Host "Option A — Install Docker Desktop, then run:"
    Write-Host "  docker compose up -d"
    Write-Host ""
    Write-Host "Option B — Install PostgreSQL locally (winget):"
    Write-Host "  winget install PostgreSQL.PostgreSQL.17"
    Write-Host ""
    Write-Host "Then create the database (psql or pgAdmin):"
    Write-Host "  CREATE DATABASE logmonitor;"
    Write-Host "  CREATE USER logmonitor WITH ENCRYPTED PASSWORD 'logmonitor';"
    Write-Host "  GRANT ALL PRIVILEGES ON DATABASE logmonitor TO logmonitor;"
    Write-Host ""
    exit 1
}

Write-Host "Starting PostgreSQL via Docker Compose..."
& docker compose up -d

$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    if (Test-PostgresPort) {
        Write-Host "PostgreSQL is ready on localhost:5432 (db=logmonitor, user=logmonitor)."
        exit 0
    }
    Start-Sleep -Seconds 2
}

Write-Host "Timed out waiting for PostgreSQL on port 5432. Check: docker compose logs postgres"
exit 1
