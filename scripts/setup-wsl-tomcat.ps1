# Sets up WSL Ubuntu + Tomcat for local Log Monitor testing (Option A).
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$wslScript = Join-Path $PSScriptRoot "wsl\setup-tomcat.sh"

function Test-WslDistro {
    $output = wsl -l -q 2>&1 | Out-String
    if ($output -match 'no installed distributions') { return $false }
    $names = wsl -l -q 2>$null | Where-Object { $_ -and $_.Trim() -ne '' }
    return ($names | Measure-Object).Count -gt 0
}

function Get-WslScriptPath {
    $drive = $wslScript.Substring(0, 1).ToLower()
    return "/mnt/$drive/" + ($wslScript.Substring(3) -replace '\\', '/')
}

if (-not (Test-WslDistro)) {
    Write-Host ""
    Write-Host "WSL Ubuntu is not installed yet."
    Write-Host ""
    Write-Host "Run this in an elevated PowerShell (Admin), then RESTART your PC:"
    Write-Host "  wsl --install -d Ubuntu"
    Write-Host ""
    Write-Host "After restart, open Ubuntu from the Start menu, create your Linux username,"
    Write-Host "then run this script again:"
    Write-Host "  powershell -ExecutionPolicy Bypass -File scripts\setup-wsl-tomcat.ps1"
    Write-Host ""
    exit 1
}

$wslPath = Get-WslScriptPath
Write-Host "Running Tomcat + SSH setup inside WSL..."

# Ensure Unix LF line endings (CRLF breaks bash "set -o pipefail" on /mnt/c files).
$lfScript = [System.IO.File]::ReadAllText($wslScript) -replace "`r`n", "`n" -replace "`r", ""
[System.IO.File]::WriteAllText($wslScript, $lfScript, (New-Object System.Text.UTF8Encoding $false))

function Invoke-WslBash {
    param(
        [string]$Mode,
        [switch]$AsRoot
    )
    if ($AsRoot) {
        wsl -u root bash $wslPath $Mode
    } else {
        wsl bash $wslPath $Mode
    }
    if ($LASTEXITCODE -ne 0) {
        throw "WSL setup step '$Mode' failed (exit $LASTEXITCODE)."
    }
}

try {
    Invoke-WslBash -Mode root -AsRoot
    Invoke-WslBash -Mode user
} catch {
    Write-Host ""
    Write-Host $_.Exception.Message
    Write-Host ""
    $wslRoot = "/mnt/" + $root.Substring(0, 1).ToLower() + "/" + ($root.Substring(3) -replace '\\', '/')
    Write-Host "If this keeps failing, open the Ubuntu app and run:"
    Write-Host "  cd $wslRoot"
    Write-Host "  sed -i 's/\r$//' scripts/wsl/setup-tomcat.sh"
    Write-Host "  sudo bash scripts/wsl/setup-tomcat.sh root"
    Write-Host "  bash scripts/wsl/setup-tomcat.sh user"
    exit 1
}

Write-Host ""
Write-Host "Collecting connection details..."
$wslIp = (wsl hostname -I).Trim().Split(' ')[0]
$wslUser = (wsl whoami).Trim()

$keysDir = Join-Path $root ".wsl-keys"
New-Item -ItemType Directory -Force -Path $keysDir | Out-Null
$keyPath = Join-Path $keysDir "logmonitor"
$wslKeyUnc = "\\wsl.localhost\Ubuntu\home\$wslUser\.ssh\logmonitor"
if (Test-Path $wslKeyUnc) {
    Copy-Item $wslKeyUnc $keyPath -Force
} else {
    $keyContent = (wsl cat ~/.ssh/logmonitor).TrimEnd()
    [System.IO.File]::WriteAllText($keyPath, $keyContent + [Environment]::NewLine, (New-Object System.Text.UTF8Encoding $false))
}
if ((Get-Content $keyPath -TotalCount 1) -notmatch "BEGIN RSA PRIVATE KEY") {
    Write-Host "WARNING: Private key is not RSA PEM format. Re-run: bash scripts/wsl/setup-tomcat.sh user"
}

$knownHosts = Join-Path $env:USERPROFILE ".ssh\known_hosts"
$sshDir = Split-Path $knownHosts
if (-not (Test-Path $sshDir)) {
    New-Item -ItemType Directory -Force -Path $sshDir | Out-Null
}

Write-Host "Adding WSL host key to known_hosts..."
$keyscan = ssh-keyscan -H $wslIp 2>$null
if (-not $keyscan) {
    Write-Host "ssh-keyscan failed; run manually after WSL starts:"
    Write-Host "  ssh-keyscan -H $wslIp >> `"$knownHosts`""
} else {
    $keyscan | Add-Content -Path $knownHosts
}

ssh -i $keyPath -o BatchMode=yes -o ConnectTimeout=10 "${wslUser}@${wslIp}" "echo SSH OK" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "SSH test failed. Try manually:"
    Write-Host "  ssh -i $keyPath ${wslUser}@${wslIp}"
    exit 1
}

Write-Host ""
Write-Host "=============================================="
Write-Host " Register this server in the Log Monitor UI"
Write-Host "=============================================="
Write-Host "Server name:  local-tomcat-wsl"
Write-Host "Host:         $wslIp"
Write-Host "Port:         22"
Write-Host "Username:     $wslUser"
Write-Host "Environment:  local"
Write-Host "Private key file: $keyPath"
Write-Host "(paste file contents into the Private key field in the UI)"
Write-Host ""
Write-Host "Start backend:"
Write-Host "  `$env:SSH_KNOWN_HOSTS_PATH = `"$knownHosts`""
Write-Host "  .\mvnw.cmd spring-boot:run"
Write-Host ""
Write-Host "Then Log Explorer -> Fetch TOMCAT_LOG / Stream TOMCAT_TAIL"
Write-Host "=============================================="
