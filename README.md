# Centralized Log Monitoring Platform

Production-ready log monitoring platform for **Paytm** infrastructure — securely fetch, search, and stream logs from remote Linux/Tomcat servers over SSH. Includes a Paytm-branded React frontend and Spring Boot backend.

## Features

- **JWT authentication** with role-based access control (ADMIN, DEV, SUPPORT)
- **SSH log retrieval** via SSHJ with host key verification (no promiscuous trust)
- **Command whitelisting** — only pre-approved commands from the database are executed
- **Real-time streaming** over STOMP WebSocket (`tail -f`)
- **Server registry** with AES-encrypted SSH private keys at rest
- **Audit trail** for logins, log access, and server changes
- **OpenAPI / Swagger** interactive documentation
- **Flyway** database migrations

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Database | PostgreSQL |
| ORM | Spring Data JPA |
| Security | Spring Security, JWT (JJWT) |
| SSH | SSHJ |
| Real-time | WebSocket, STOMP |
| API docs | SpringDoc OpenAPI |
| Migrations | Flyway |
| Build | Maven |
| Tests | JUnit 5, Mockito |

### Frontend (`frontend/`)

| Layer | Technology |
|-------|------------|
| Framework | React 18 + TypeScript |
| Build | Vite 5 |
| Routing | React Router 6 |
| HTTP | Axios |
| WebSocket | @stomp/stompjs |
| Icons | Lucide React |

## Architecture

```
Controller → Service → Repository / SSH Gateway
```

| Package | Responsibility |
|---------|----------------|
| `controller` | REST endpoints |
| `auth` | Login and token issuance |
| `service` | Business logic (servers, logs, streaming) |
| `ssh` | SSH connection and command execution |
| `websocket` | STOMP log streaming handlers |
| `security` | JWT filter, Spring Security config |
| `audit` | Audit logging (AOP + explicit calls) |
| `entity` / `repository` | JPA persistence |

## Prerequisites

- **Java 17** or later
- **Maven 3.9+**
- **PostgreSQL 14+**
- **OpenSSH `known_hosts` file** with entries for target servers
- Network access from this application to target servers on SSH (port 22)

## Quick Start (Local)

### 1. Start PostgreSQL

The backend **requires PostgreSQL on `localhost:5432`** before startup. If you see `Connection to localhost:5432 refused`, the database is not running.

**Option A — Docker (recommended)**

```powershell
docker compose up -d
```

Or use the helper script (checks port, starts Compose, waits until ready):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-db.ps1
```

Default credentials match `application.yml` (`local` profile): database `logmonitor`, user `logmonitor`, password `logmonitor`.

**Option B — Local PostgreSQL install**

```powershell
winget install PostgreSQL.PostgreSQL.17
```

Then create the database:

```sql
CREATE DATABASE logmonitor;
CREATE USER logmonitor WITH ENCRYPTED PASSWORD 'logmonitor';
GRANT ALL PRIVILEGES ON DATABASE logmonitor TO logmonitor;
```

### 2. Configure environment variables

Copy the example file and edit values:

```powershell
copy .env.example .env
```

Key variables:

| Variable | Description |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | `local`, `dev`, `uat`, or `prod` |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `JWT_SECRET` | Min 32 characters; used to sign JWTs |
| `ENCRYPTION_SECRET_KEY` | Min 32 characters; encrypts SSH private keys in DB |
| `SSH_KNOWN_HOSTS_PATH` | Absolute path to your OpenSSH `known_hosts` file |

The `local` profile provides defaults for JWT and encryption secrets if they are not set.

### 3. Build and run the backend

Maven is **not required globally**. Use the project wrapper (downloads Maven to `.tools/` on first setup):

```powershell
# One-time: install local Maven
powershell -ExecutionPolicy Bypass -File scripts\setup-tools.ps1

# Start PostgreSQL (if not already running)
docker compose up -d

# Start backend (fails fast if port 5432 is closed)
powershell -ExecutionPolicy Bypass -File scripts\run-backend.ps1
```

Or without the helper script:

```powershell
.\mvnw.cmd spring-boot:run
```

If you already have Maven on PATH: `mvn spring-boot:run`

Or run the JAR after building:

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target\log-monitor-1.0.0-SNAPSHOT.jar
```

The API starts at **http://localhost:8080**.

### 5. Start the frontend

The frontend lives in the **`frontend/`** folder (not the repo root). Either:

```powershell
cd frontend
npm install
npm run dev
```

Or from the repo root:

```powershell
npm run install:frontend
npm run dev
```

Open **http://localhost:5173** — the Vite dev server proxies `/api` and `/ws` to the backend.

Default login: `admin` / `Admin@123`

### 5. Default admin user (local/dev only)

On first startup with an empty `users` table, a default admin is seeded:

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `Admin@123` |
| Email | `admin@logmonitor.local` |

Change this password immediately in any shared environment.

## Local testing with WSL + Tomcat (Option A)

Use WSL Ubuntu as a real Linux SSH target with Tomcat logs — no cloud VM required.

### Prerequisites

- Windows 10/11 with WSL2
- PostgreSQL running locally (see Quick Start above)
- Admin access once, to install WSL

### Step 1 — Install WSL Ubuntu (one time)

Open **PowerShell as Administrator**:

```powershell
wsl --install -d Ubuntu
```

Restart your PC when prompted. Open **Ubuntu** from the Start menu and create your Linux username.

### Step 2 — Run the setup script

From the project root in a normal PowerShell window:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\setup-wsl-tomcat.ps1
```

This inside WSL will:

- Install Java 17, Tomcat (`tomcat10` on Ubuntu 24+, `tomcat9` on older releases), and OpenSSH
- Symlink logs to `/var/log/tomcat/` (paths the app expects)
- Generate an SSH key at `~/.ssh/logmonitor`
- Copy the private key to `.wsl-keys/logmonitor` and add the host to `known_hosts`

At the end it prints the **WSL IP**, **username**, and key path for the UI.

### Step 3 — Start the backend

```powershell
$env:SSH_KNOWN_HOSTS_PATH = "$env:USERPROFILE\.ssh\known_hosts"
.\mvnw.cmd spring-boot:run
```

### Step 4 — Register the server in the UI

Open http://localhost:5173 → **Servers** → **Add Server**:

| Field | Value |
|-------|--------|
| Server name | `local-tomcat-wsl` |
| Host | WSL IP from setup script (e.g. `172.x.x.x`) |
| Port | `22` |
| Username | Your WSL username |
| Private key | Paste contents of `.wsl-keys\logmonitor` |
| Environment | `local` |

### Step 5 — Test logs

**Log Explorer** → server `local-tomcat-wsl`:

| Tab | Command key | What to expect |
|-----|-------------|----------------|
| Fetch | `Tomcat Catalina Log` | Last lines of `catalina.out` |
| Search | `ERROR` or `INFO` | Matching grep results |
| Stream | `Tomcat Catalina Tail` | Live `tail -f` output |

Generate more Tomcat traffic from WSL:

```bash
curl http://127.0.0.1:8080/
```

### Troubleshooting

| Issue | Fix |
|-------|-----|
| WSL IP changed after reboot | Re-run `wsl hostname -I` and update the server host in the UI |
| `SSH known_hosts path is not configured` | Set `SSH_KNOWN_HOSTS_PATH` before starting the backend |
| Empty access log | Hit Tomcat in WSL with `curl`; access log file is created on first request |
| `Port 8080 was already in use` | WSL Tomcat defaults to 8080 — run `wsl -u root bash scripts/wsl/setup-tomcat.sh root` to move it to **8081**, or stop Tomcat: `wsl sudo service tomcat10 stop` |
| `sudo: timed out` | Run from Ubuntu instead: `sudo bash scripts/wsl/setup-tomcat.sh root` then `bash scripts/wsl/setup-tomcat.sh user` |
| `Failed to connect` / auth errors with correct host & key | Backend uses **SSHJ**, which cannot parse `BEGIN OPENSSH PRIVATE KEY` (ed25519). Re-run `scripts\setup-wsl-tomcat.ps1` or regenerate: `ssh-keygen -t rsa -b 4096 -m PEM -f ~/.ssh/logmonitor -N ""` then re-paste the key in the UI |
| `Command exited with status 1` on Tomcat Error Log | Tomcat 10 may not create `localhost.log`. Re-run root setup: `wsl -u root bash scripts/wsl/setup-tomcat.sh root` — it symlinks error log to `catalina.out` when needed |
| `Connection timed out` to WSL IP | SSH is not running in WSL. Run: `wsl -u root systemctl start ssh` and `wsl -u root systemctl enable ssh` |

## Phase 2 — Application-wise logs (Tomcat + logback)

Browse logs per deployed application using paths from each app's `logback.xml`.

### Flow

1. **Servers** — register the SSH host (unchanged).
2. **Application Logs** (`/app-logs`) — select server → **Discover Tomcat** (`~/local/apache-tomcat-*`).
3. Select a Tomcat instance → **Discover applications** (`webapps/`).
4. Select an app → **Load logback.xml** (reads `WEB-INF/classes/logback.xml`, caches paths in DB).
5. View logs:
   | Mode | SSH command (whitelisted) |
   |------|----------------------------|
   | **Current** | `tail -n 5000` on active log file |
   | **Date-wise** | `zcat` + `tail` on archived file from rolling pattern |
   | **Live** | `tail -f` via WebSocket |

### Server layout expected

Tomcat must live under the SSH user's home:

```
/home/<user>/local/apache-tomcat-9.0.85/
  webapps/
    myapp/
      WEB-INF/classes/logback.xml
```

`logback.xml` must define a `<file>` path (and optionally `<fileNamePattern>` for date archives).

### API endpoints

| Method | Path | Role |
|--------|------|------|
| GET | `/api/servers/{id}/tomcat/instances` | All |
| POST | `/api/servers/{id}/tomcat/instances/discover` | ADMIN, DEV |
| GET/POST | `.../instances/{id}/applications` | All / discover: ADMIN, DEV |
| POST | `.../applications/{id}/log-config/cache` | ADMIN, DEV |
| POST | `/api/app-logs/fetch` | All |
| WS | `/app/logs/stream/app/start` | All |

Paths are never taken from the client — only from cached logback parsing after validation.

## Frontend UI

The Paytm-branded React app (`frontend/`) provides:

| Page | Path | Access |
|------|------|--------|
| Login | `/login` | Public |
| Dashboard | `/` | All roles |
| Servers | `/servers` | View: all · Manage: ADMIN |
| Log Explorer | `/logs` | Fetch/stream: all · Search: ADMIN, DEV |
| Application Logs | `/app-logs` | Tomcat → app → logback logs: all · Discover: ADMIN, DEV |
| Audit Trail | `/audit` | ADMIN only |

**Production build:**

```powershell
cd frontend
npm run build
```

Serve the `frontend/dist/` folder via any static host. Set `CORS_ALLOWED_ORIGINS` on the backend to your frontend URL.

## API Documentation

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/api-docs | OpenAPI JSON |
| http://localhost:8080/actuator/health | Health check |

## Authentication

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123"
}
```

Response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Use the token on all protected requests:

```http
Authorization: Bearer <accessToken>
```

## REST API Reference

### Servers

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| `GET` | `/api/servers` | ADMIN, DEV, SUPPORT | List servers |
| `GET` | `/api/servers/{id}` | ADMIN, DEV, SUPPORT | Get server |
| `POST` | `/api/servers` | ADMIN | Register server |
| `PUT` | `/api/servers/{id}` | ADMIN | Update server |
| `DELETE` | `/api/servers/{id}` | ADMIN | Delete server |

**Create server example:**

```http
POST /api/servers
Authorization: Bearer <token>
Content-Type: application/json

{
  "serverName": "prod-tomcat-01",
  "host": "10.0.1.50",
  "port": 22,
  "username": "deploy",
  "privateKey": "-----BEGIN OPENSSH PRIVATE KEY-----\n...\n-----END OPENSSH PRIVATE KEY-----",
  "environment": "prod",
  "active": true
}
```

### Logs

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| `GET` | `/api/log-types` | ADMIN, DEV, SUPPORT | List fetchable log types |
| `POST` | `/api/logs/fetch` | ADMIN, DEV, SUPPORT | Fetch recent log lines |
| `POST` | `/api/logs/search` | ADMIN, DEV | Search logs (grep) |

**Fetch logs:**

```http
POST /api/logs/fetch
Authorization: Bearer <token>
Content-Type: application/json

{
  "serverId": 1,
  "commandKey": "TOMCAT_LOG"
}
```

**Search logs:**

```http
POST /api/logs/search
Authorization: Bearer <token>
Content-Type: application/json

{
  "serverId": 1,
  "commandKey": "TOMCAT_LOG",
  "searchTerm": "OutOfMemoryError"
}
```

Search terms allow only letters, digits, spaces, dash, dot, and underscore (max 100 characters).

### Audit

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| `GET` | `/api/audit` | ADMIN | Paginated audit log |

Query parameters: `page`, `size`, `sort`, `username`, `action`

```http
GET /api/audit?page=0&size=20&action=USER_LOGIN
Authorization: Bearer <token>
```

## Whitelisted Log Commands

Commands are stored in `log_config` and applied via Flyway migrations. Users cannot submit arbitrary shell commands.

| Command Key | Purpose |
|-------------|---------|
| `TOMCAT_LOG` | Tail Tomcat catalina.out |
| `ERROR_LOG` | Tail Tomcat localhost.log |
| `ACCESS_LOG` | Tail access log |
| `SYSLOG` | Tail /var/log/messages |
| `TOMCAT_TAIL` | Stream catalina.out (`tail -f`) |
| `TOMCAT_LOG_SEARCH` | Grep catalina.out |
| `ERROR_LOG_SEARCH` | Grep error log |
| `ACCESS_LOG_SEARCH` | Grep access log |
| `SYSLOG_SEARCH` | Grep syslog |

Add new commands by inserting rows into `log_config` (or a new Flyway migration). Never construct commands from user input.

## Real-Time Log Streaming (WebSocket)

### Connection

- **Endpoint:** `ws://localhost:8080/ws`
- **Subscribe:** `/topic/logs`
- **Send:** `/app/logs/stream/start`, `/app/logs/stream/stop`

### Authentication

Pass JWT on the STOMP `CONNECT` frame:

```
Authorization: Bearer <accessToken>
```

Alternatively, pass `token: <accessToken>` as a STOMP native header, or append `?access_token=<token>` to the WebSocket URL for the HTTP upgrade.

### Start streaming

Send to `/app/logs/stream/start`:

```json
{
  "serverId": 1,
  "commandKey": "TOMCAT_TAIL",
  "streamId": "optional-client-id"
}
```

Messages on `/topic/logs`:

```json
{
  "streamId": "abc-123",
  "serverId": 1,
  "commandKey": "TOMCAT_TAIL",
  "line": "2024-01-01 INFO Server started",
  "type": "LOG",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

`type` is `LOG`, `ERROR`, or `END`.

### Stop streaming

Send to `/app/logs/stream/stop`:

```json
{
  "streamId": "abc-123"
}
```

Streams are automatically stopped when the WebSocket session disconnects.

## Role-Based Access Control

| Capability | ADMIN | DEV | SUPPORT |
|------------|:-----:|:---:|:-------:|
| Manage servers | Yes | No | No |
| View servers | Yes | Yes | Yes |
| Fetch logs | Yes | Yes | Yes |
| Search logs | Yes | Yes | No |
| Stream logs | Yes | Yes | Yes |
| View audit trail | Yes | No | No |

## Security Notes

- SSH private keys are **AES-256-GCM encrypted** before storage and never logged.
- Host keys are validated against **`SSH_KNOWN_HOSTS_PATH`**; unknown hosts are rejected.
- **`PromiscuousVerifier` is not used.**
- Passwords are hashed with **BCrypt** (strength 12).
- All sensitive operations are written to **`audit_logs`**.
- Use strong, unique values for `JWT_SECRET` and `ENCRYPTION_SECRET_KEY` in production.

## Error Responses

All API errors return a consistent JSON body:

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 404,
  "message": "Server not found with id: 99",
  "path": "/api/servers/99"
}
```

| HTTP Status | Typical cause |
|-------------|---------------|
| 400 | Validation failure, non-whitelisted command, invalid search term |
| 401 | Missing or invalid JWT |
| 403 | Insufficient role |
| 404 | Resource not found |
| 502 | SSH connection or remote command failure |
| 500 | Unexpected server error |

## Profiles

| Profile | Use case |
|---------|----------|
| `local` | Development with local PostgreSQL defaults |
| `dev` | Shared development environment |
| `uat` | User acceptance testing |
| `prod` | Production (all secrets via environment variables) |

Activate with:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
```

## Running Tests

```powershell
mvn test
```

Unit tests cover JWT, services, controllers, and security rules. The full Spring context integration test requires a running PostgreSQL instance and is disabled by default.

## Project Structure

```
Paytm/
├── frontend/                 # React + TypeScript UI (Vite)
│   └── src/
│       ├── api/              # Axios HTTP client
│       ├── components/       # Layout, routing
│       ├── context/          # Auth context
│       ├── hooks/            # WebSocket streaming hook
│       ├── pages/            # Login, Dashboard, Servers, Logs, Audit
│       └── types/
├── src/main/java/com/logmonitor/
│   ├── auth/                 # Login controller and service
│   ├── audit/                # Audit service and AOP
│   ├── config/               # Spring, WebSocket, CORS, OpenAPI
│   ├── controller/           # REST controllers
│   ├── service/              # Business logic
│   ├── ssh/                  # SSH gateway (SSHJ)
│   └── websocket/            # STOMP streaming
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/         # Flyway scripts
└── pom.xml
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `SSH known_hosts path is not configured` | Set `SSH_KNOWN_HOSTS_PATH` to a valid file path |
| `SSH known_hosts file not found` | Create the file or SSH once manually to populate it |
| `Command key is not whitelisted` | Use a key from `/api/log-types` or `log_config` table |
| `Failed to connect after N attempts` | Check host, port, firewall, and SSH key. If the key starts with `BEGIN OPENSSH PRIVATE KEY`, regenerate RSA PEM (`ssh-keygen -t rsa -b 4096 -m PEM`) |
| `mvn` not recognized | Use `.\mvnw.cmd` instead, or run `scripts\setup-tools.ps1` |
| `npm install` fails at repo root | Run from `frontend/` or use `npm run install:frontend` |
| 401 on all requests | Login again; check `JWT_SECRET` has not changed |

## License

Internal use — adjust as needed for your organization.
