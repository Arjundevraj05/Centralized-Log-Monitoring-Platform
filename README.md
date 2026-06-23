# Centralized Log Monitoring Platform

Production-ready Spring Boot backend for securely fetching, searching, and streaming logs from remote Linux/Tomcat servers over SSH. Designed to pair with a React frontend (not included in this repository).

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

### 1. Create the database

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

### 3. Build and run

```powershell
cd Paytm
mvn clean package -DskipTests
mvn spring-boot:run
```

Or run the JAR:

```powershell
java -jar target/log-monitor-1.0.0-SNAPSHOT.jar
```

The API starts at **http://localhost:8080**.

### 4. Default admin user (local/dev only)

On first startup with an empty `users` table, a default admin is seeded:

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `Admin@123` |
| Email | `admin@logmonitor.local` |

Change this password immediately in any shared environment.

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
src/main/java/com/logmonitor/
├── auth/           # Login controller and service
├── audit/          # Audit service and AOP
├── config/         # Spring, WebSocket, OpenAPI config
├── controller/     # REST controllers
├── dto/            # Request/response objects
├── entity/         # JPA entities
├── exception/      # Global exception handler
├── mapper/         # Entity ↔ DTO mappers
├── repository/     # Spring Data JPA
├── security/       # JWT and Spring Security
├── service/        # Business logic
├── ssh/            # SSH gateway (SSHJ)
├── util/           # Encryption, security helpers
└── websocket/      # STOMP streaming

src/main/resources/
├── application.yml
├── logback-spring.xml
└── db/migration/   # Flyway scripts (V1, V2, ...)
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `SSH known_hosts path is not configured` | Set `SSH_KNOWN_HOSTS_PATH` to a valid file path |
| `SSH known_hosts file not found` | Create the file or SSH once manually to populate it |
| `Command key is not whitelisted` | Use a key from `/api/log-types` or `log_config` table |
| `Failed to connect after N attempts` | Check host, port, firewall, and SSH key |
| Flyway migration fails | Ensure PostgreSQL is running and credentials match |
| 401 on all requests | Login again; check `JWT_SECRET` has not changed |

## License

Internal use — adjust as needed for your organization.
