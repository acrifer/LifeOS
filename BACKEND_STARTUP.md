# Backend Startup

## Prerequisites
- JDK must already be available and `JAVA_HOME` must point to it.
- Docker Desktop must already be running if you want the scripts to start MySQL.
- These scripts only manage backend services:
  - `lifeos-user-service`
  - `lifeos-task-service`
  - `lifeos-note-service`
  - `lifeos-ai-service`
  - `lifeos-behavior-service`
  - `lifeos-gateway`

## Environment Layout
- All startup scripts and backend services read settings from the project root `.env`.
- Use `.env.example` as the template when you need a new local configuration.
- `.env` must define `MYSQL_JDBC_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `NACOS_SERVER_ADDR`, and `LIFEOS_JWT_SECRET`.
- `MySQL` is started by Docker Compose from `docker-compose.infrastructure.yml` when `3306` is not already occupied.
- `Redis` is started from the local `redis-server.exe` if available; otherwise the script falls back to Docker Compose.
- `Nacos` is started from `C:\environment\nacos`.
- `RocketMQ` is started from `C:\environment\rocketmq-all-5.4.0-bin-release`.
- If `3306` or `6379` is already occupied, the environment script treats that dependency as already available and does not try to replace it.

## Commands
- Start infrastructure only:
  - `powershell -ExecutionPolicy Bypass -File .\start-env.ps1`
- Stop infrastructure:
  - `powershell -ExecutionPolicy Bypass -File .\stop-env.ps1`
- Show infrastructure status:
  - `powershell -ExecutionPolicy Bypass -File .\status-env.ps1`
- Start all backend services:
  - `powershell -ExecutionPolicy Bypass -File .\start-backend.ps1`
- Stop services started by the script:
  - `powershell -ExecutionPolicy Bypass -File .\stop-backend.ps1`
- Show dependency and backend status:
  - `powershell -ExecutionPolicy Bypass -File .\status-backend.ps1`
- One-click start environment + backend:
  - `powershell -ExecutionPolicy Bypass -File .\start-all.ps1`
- One-click stop environment + backend:
  - `powershell -ExecutionPolicy Bypass -File .\stop-all.ps1`
- One-click view full status:
  - `powershell -ExecutionPolicy Bypass -File .\status-all.ps1`

## Notes
- Logs are written to `.runtime/logs/`.
- Script-managed process IDs are stored in `.runtime/pids/`.
- Unified Swagger UI is available through the gateway:
  - `http://127.0.0.1:8080/swagger-ui.html`
  - Aggregated OpenAPI JSON is exposed at `/service-docs/user`, `/service-docs/note`, `/service-docs/task`, `/service-docs/ai`, and `/service-docs/behavior`.
- The repository now standardizes text files on `UTF-8`. If your editor asks for an encoding, choose `UTF-8`.
- Keep `.env` local only. `LIFEOS_JWT_SECRET` should be at least 32 characters, and `LIFEOS_AI_API_KEY` can be left empty to use the local mock summary fallback.
- If a service port is already in use, the start script treats that service as already running and skips it.
- The start script installs `lifeos-common` and `lifeos-api` into the local Maven repository before launching services.
- `note-service` uses the current ShardingSphere-compatible startup configuration and must be started through the repaired Maven setup.
- `lifeos-ai-service` is now managed by the one-click backend script on port `8084`.
- If `lifeos.ai.api-key` is empty, `lifeos-ai-service` falls back to a local mock summary.
- `RocketMQ` is now used for behavior events: `note-service` and `task-service` publish events, and `behavior-service` consumes them asynchronously.
