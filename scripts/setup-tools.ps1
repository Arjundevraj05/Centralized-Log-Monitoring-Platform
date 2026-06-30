# Downloads a local Maven distribution into .tools/ (no global install required).
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$toolsDir = Join-Path $root ".tools"
$mavenVersion = "3.9.6"
$mavenDir = Join-Path $toolsDir "apache-maven-$mavenVersion"
$mavenZip = Join-Path $toolsDir "apache-maven-$mavenVersion-bin.zip"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"

if (Test-Path (Join-Path $mavenDir "bin\mvn.cmd")) {
    Write-Host "Maven $mavenVersion already installed at $mavenDir"
    exit 0
}

Write-Host "Downloading Apache Maven $mavenVersion..."
New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip
Expand-Archive -Path $mavenZip -DestinationPath $toolsDir -Force
Remove-Item $mavenZip

Write-Host "Maven installed. Run the backend with:"
Write-Host "  .\mvnw.cmd spring-boot:run"
