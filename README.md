# Animal Aid and Rescue

The application supports two local run modes. Both modes preserve the current
PostgreSQL database by default. Use the explicit fresh-start command only when
you want to delete the current database and populate the original demo data.

## 1. Development mode

Use this mode for day-to-day development and fast hot reload. PostgreSQL runs in
Docker, while Spring Boot and Next.js run directly on the host.

Requirements:

- Docker Desktop or Docker Engine with Docker Compose
- Java 21+
- Maven 3.9+
- Node.js 22+
- npm

### Windows

Run from PowerShell at the repository root:

```powershell
.\scripts\dev.ps1
```

Run in the background:

```powershell
.\scripts\dev.ps1 -Background
```

Stop the backend and frontend while keeping PostgreSQL running:

```powershell
.\scripts\dev.ps1 stop
```

If PowerShell blocks local scripts:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\dev.ps1
```

Reset the database and start with the original demo data:

```powershell
.\scripts\dev.ps1 -Fresh
```

Add `-Background` to run a fresh environment in the background.

### Linux and macOS

```bash
chmod +x scripts/dev.sh
./scripts/dev.sh
```

Run in the background:

```bash
./scripts/dev.sh --bg
```

Stop the backend and frontend while keeping PostgreSQL running:

```bash
./scripts/dev.sh stop
```

Reset the database and start with the original demo data:

```bash
./scripts/dev.sh --fresh
```

Add `--bg` to run a fresh environment in the background.

Development logs are written to `.dev-logs/`.

## 2. Docker-only mode

Use this mode to run or share the complete application without installing Java,
Maven, Node.js, or npm on the host.

Requirements:

- Windows: Docker Desktop with WSL 2 enabled
- Linux: Docker Engine with the Docker Compose plugin
- macOS: Docker Desktop

The commands are the same in PowerShell, Command Prompt, Bash, and other shells.

Build and start PostgreSQL, the backend, the frontend, and the backup service:

```bash
docker compose --profile app up --build
```

Run in the background:

```bash
docker compose --profile app up --build -d
```

After the first successful build, start without rebuilding:

```bash
docker compose --profile app up -d
```

View logs:

```bash
docker compose --profile app logs -f
```

Stop and remove the containers:

```bash
docker compose --profile app down
```

Reset the database and start the complete application with the original demo
data:

```bash
docker compose --profile app down --volumes
docker compose --profile app up --build -d
```

`--volumes` permanently deletes the current PostgreSQL database. Without it,
all normal start, stop, rebuild, and restart commands preserve the existing data.
Uploaded files are stored in `backend/data/uploads`.

## URLs

- Application: http://localhost:3000
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## Demo accounts

All demo accounts use the password `Password@123`.

| Email | Role |
| --- | --- |
| `manager@aar.local` | Manager |
| `staff1@aar.local` | Staff |
| `staff2@aar.local` | Staff |
| `adopter1@aar.local` | Adopter |

See [DEMO.md](DEMO.md) for the step-by-step demo scenarios.
