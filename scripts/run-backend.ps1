# Starts the Spring Boot backend using the local Maven in .tools/
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

if (-not (Test-PostgresPort)) {
    Write-Host "PostgreSQL is not reachable on localhost:5432."
    Write-Host "Start the database first:"
    Write-Host "  powershell -ExecutionPolicy Bypass -File scripts\start-db.ps1"
    Write-Host "Or: docker compose up -d"
    exit 1
}

$mvn = Join-Path $root ".tools\apache-maven-3.9.6\bin\mvn.cmd"
if (-not (Test-Path $mvn)) {
    Write-Host "Maven not found. Running setup..."
    & (Join-Path $PSScriptRoot "setup-tools.ps1")
}

$defaultKnownHosts = Join-Path $env:USERPROFILE ".ssh\known_hosts"
if (-not $env:SSH_KNOWN_HOSTS_PATH) {
    if (Test-Path $defaultKnownHosts) {
        $env:SSH_KNOWN_HOSTS_PATH = $defaultKnownHosts
        Write-Host "Using SSH known_hosts: $defaultKnownHosts"
    } else {
        Write-Host ""
        Write-Host "WARNING: SSH_KNOWN_HOSTS_PATH is not set and $defaultKnownHosts was not found."
        Write-Host "Log fetch/stream will fail until you create it, e.g.:"
        Write-Host "  ssh-keyscan -H <server-ip> >> `"$defaultKnownHosts`""
        Write-Host "  `$env:SSH_KNOWN_HOSTS_PATH = `"$defaultKnownHosts`""
        Write-Host ""
    }
}

& (Join-Path $root "mvnw.cmd") spring-boot:run @args
