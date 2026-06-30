@ECHO OFF
setlocal

set "MAVEN_HOME=%~dp0.tools\apache-maven-3.9.6"
set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if not exist "%MVN_CMD%" (
  echo Maven not found in .tools folder.
  echo Run: powershell -ExecutionPolicy Bypass -File scripts\setup-tools.ps1
  exit /b 1
)

call "%MVN_CMD%" %*
