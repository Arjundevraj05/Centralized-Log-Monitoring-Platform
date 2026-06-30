# Creates logmonitor database and user (matches application.yml local profile).
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sql = Join-Path $PSScriptRoot "setup-db.sql"

$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path $psql)) {
    $psql = "C:\Program Files\PostgreSQL\17\bin\psql.exe"
}
if (-not (Test-Path $psql)) {
    $psqlCmd = Get-Command psql -ErrorAction SilentlyContinue
    if ($psqlCmd) { $psql = $psqlCmd.Source } else { throw "psql not found. Add PostgreSQL bin to PATH or install PostgreSQL." }
}

Write-Host "You will be prompted for the postgres superuser password (set during PostgreSQL install)."
Write-Host ""
& $psql -U postgres -h localhost -p 5432 -f $sql
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "Database ready. Start the backend:"
Write-Host "  cd $root"
Write-Host "  .\mvnw.cmd spring-boot:run"
