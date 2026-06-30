#!/usr/bin/env bash
# Run inside WSL (Ubuntu): prepares Tomcat, log symlinks, and SSH for Log Monitor testing.
# Usage: bash setup-tomcat.sh [root|user|all]
set -euo pipefail

MODE="${1:-all}"

detect_tomcat_package() {
  for pkg in tomcat10 tomcat9 tomcat11; do
    if apt-cache show "${pkg}" &>/dev/null; then
      echo "${pkg}"
      return 0
    fi
  done
  echo "No Tomcat package found (tomcat10/tomcat9/tomcat11). Enable universe: apt-get install -y software-properties-common" >&2
  return 1
}

run_root_setup() {
  echo "==> Updating packages (root)..."
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq

  TOMCAT_PKG="$(detect_tomcat_package)"
  TOMCAT_LOG_DIR="/var/log/${TOMCAT_PKG}"
  echo "==> Installing ${TOMCAT_PKG}..."
  apt-get install -y openjdk-17-jdk "${TOMCAT_PKG}" openssh-server curl

  echo "==> Configuring Tomcat log paths for Log Monitor (${TOMCAT_LOG_DIR} -> /var/log/tomcat)..."
  mkdir -p /var/log/tomcat
  ln -sfn "${TOMCAT_LOG_DIR}/catalina.out" /var/log/tomcat/catalina.out

  # Tomcat 10 uses date-rotated localhost logs; fall back to catalina.out when absent.
  LOCALHOST_LOG="$(ls -t "${TOMCAT_LOG_DIR}"/localhost.[0-9]*.log 2>/dev/null | head -1 || true)"
  if [[ -n "${LOCALHOST_LOG}" && -f "${LOCALHOST_LOG}" ]]; then
    ln -sfn "${LOCALHOST_LOG}" /var/log/tomcat/localhost.log
  elif [[ -f "${TOMCAT_LOG_DIR}/catalina.out" ]]; then
    ln -sfn "${TOMCAT_LOG_DIR}/catalina.out" /var/log/tomcat/localhost.log
  else
    CATALINA_LOG="$(ls -t "${TOMCAT_LOG_DIR}"/catalina.[0-9]*.log 2>/dev/null | head -1 || true)"
    if [[ -n "${CATALINA_LOG}" && -f "${CATALINA_LOG}" ]]; then
      ln -sfn "${CATALINA_LOG}" /var/log/tomcat/localhost.log
    else
      touch "${TOMCAT_LOG_DIR}/localhost.log"
      chown tomcat:adm "${TOMCAT_LOG_DIR}/localhost.log" 2>/dev/null || true
      ln -sfn "${TOMCAT_LOG_DIR}/localhost.log" /var/log/tomcat/localhost.log
    fi
  fi

  ACCESS_LOG="$(ls "${TOMCAT_LOG_DIR}"/localhost_access_log*.txt 2>/dev/null | head -1 || true)"
  if [[ -n "${ACCESS_LOG}" ]]; then
    ln -sfn "${ACCESS_LOG}" /var/log/tomcat/localhost_access_log.txt
  else
    touch "${TOMCAT_LOG_DIR}/localhost_access_log.txt"
    ln -sfn "${TOMCAT_LOG_DIR}/localhost_access_log.txt" /var/log/tomcat/localhost_access_log.txt
  fi

  if [[ -f /var/log/syslog ]]; then
    ln -sfn /var/log/syslog /var/log/messages
  elif [[ ! -f /var/log/messages ]]; then
    touch /var/log/messages
  fi

  TOMCAT_HTTP_PORT=8081
  SERVER_XML="/etc/${TOMCAT_PKG}/server.xml"
  if [[ -f "${SERVER_XML}" ]]; then
    echo "==> Setting Tomcat HTTP port to ${TOMCAT_HTTP_PORT} (Spring Boot uses 8080 on Windows)..."
    sed -i 's/port="8080" protocol="HTTP\/1.1"/port="'"${TOMCAT_HTTP_PORT}"'" protocol="HTTP\/1.1"/' "${SERVER_XML}"
  fi

  echo "==> Starting Tomcat and SSH..."
  service "${TOMCAT_PKG}" start 2>/dev/null || systemctl start "${TOMCAT_PKG}"
  systemctl enable ssh 2>/dev/null || true
  service ssh start 2>/dev/null || systemctl start ssh
}

run_user_setup() {
  echo "==> Generating SSH key for Log Monitor..."
  mkdir -p ~/.ssh
  chmod 700 ~/.ssh
  # RSA PEM keys work with SSHJ on Java 17+; OpenSSH ed25519 format often fails to parse.
  if [[ -f ~/.ssh/logmonitor ]] && grep -q "BEGIN OPENSSH PRIVATE KEY" ~/.ssh/logmonitor 2>/dev/null; then
    echo "==> Replacing OpenSSH-format key with RSA PEM (required for Log Monitor backend)..."
    mv ~/.ssh/logmonitor ~/.ssh/logmonitor.openssh.bak
    mv ~/.ssh/logmonitor.pub ~/.ssh/logmonitor.pub.openssh.bak 2>/dev/null || true
  fi
  if [[ ! -f ~/.ssh/logmonitor ]]; then
    ssh-keygen -t rsa -b 4096 -m PEM -f ~/.ssh/logmonitor -N "" -C "logmonitor-local"
  fi
  chmod 600 ~/.ssh/logmonitor
  touch ~/.ssh/authorized_keys
  chmod 600 ~/.ssh/authorized_keys
  grep -qF "$(cat ~/.ssh/logmonitor.pub)" ~/.ssh/authorized_keys 2>/dev/null \
    || cat ~/.ssh/logmonitor.pub >> ~/.ssh/authorized_keys

  echo "==> Generating sample Tomcat traffic..."
  curl -sf http://127.0.0.1:8081/ >/dev/null || true
  sleep 1

  WSL_IP="$(hostname -I | awk '{print $1}')"
  WSL_USER="$(whoami)"

  echo ""
  echo "=============================================="
  echo " WSL Tomcat test environment is ready"
  echo "=============================================="
  echo "WSL IP:       ${WSL_IP}"
  echo "SSH user:     ${WSL_USER}"
  echo "SSH port:     22"
  echo "Private key:  ~/.ssh/logmonitor"
  echo ""
  echo "Verify logs:"
  echo "  tail -n 3 /var/log/tomcat/catalina.out"
  echo ""
  tail -n 3 /var/log/tomcat/catalina.out 2>/dev/null || true
  echo ""
  echo "From Windows PowerShell, trust this host:"
  echo "  ssh -i \\\\wsl\$\\Ubuntu\\home\\${WSL_USER}\\.ssh\\logmonitor ${WSL_USER}@${WSL_IP}"
  echo ""
  echo "Copy private key for the UI:"
  echo "  wsl cat ~/.ssh/logmonitor"
  echo "=============================================="
}

case "${MODE}" in
  root) run_root_setup ;;
  user) run_user_setup ;;
  all)
    if [[ "${EUID}" -eq 0 ]]; then
      run_root_setup
      exit 0
    fi
    echo "Run as root for package install, then as your user for SSH keys."
    echo "  sudo bash $0 root"
    echo "  bash $0 user"
    exit 1
    ;;
  *)
    echo "Usage: $0 [root|user]" >&2
    exit 1
    ;;
esac
