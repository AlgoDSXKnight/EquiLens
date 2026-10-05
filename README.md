# EquiLens

EquiLens is a full-stack equity research and portfolio-analysis project built around a React research terminal, Flutter investor companion, two Kotlin/Spring Boot services, PostgreSQL, and a Python FastAPI ML service.

## Architecture

```text
React / Flutter
      │
      ├── Research Service :8080 ──┐
      │                            ├── PostgreSQL
      └── Portfolio Service :8081 ┘
                   │
                   └── Research Service → ML Service :8000
```

The old `backend/` directory is retained as the original/reference implementation. The active Experiment 9 architecture is `research-service/` + `portfolio-service/`.

## Local stack with Docker

1. Copy `.env.example` to `.env`.
2. Replace `JWT_SECRET` with a random secret of at least 32 bytes.
3. Run:

```bash
docker compose up --build
```

4. Open `http://localhost:5173`.

The browser-facing API URLs intentionally use `localhost` because the React application runs in the user's browser; the Spring services use Docker service names internally.

## Manual development

### Research service

```powershell
cd research-service
$env:DB_PASSWORD="<your-db-password>"
$env:JWT_SECRET="<32-byte-or-longer-secret>"
mvn spring-boot:run
```

### Portfolio service

```powershell
cd portfolio-service
$env:DB_PASSWORD="<your-db-password>"
$env:JWT_SECRET="<32-byte-or-longer-secret>"
mvn spring-boot:run
```

### ML service

```powershell
cd ml-service
.venv313\Scripts\Activate.ps1
uvicorn app.main:app --reload --port 8000
```

### React

```powershell
cd frontend
npm install
npm test
npm run build
npm run dev
```

### Flutter

```powershell
cd mobile
flutter pub get
flutter test
flutter run --dart-define=RESEARCH_API_URL=http://10.0.2.2:8080/api --dart-define=PORTFOLIO_API_URL=http://10.0.2.2:8081/api
```

For Firebase preferences, add the platform Firebase configuration files described in `mobile/README.md`. Firebase is optional for the backend REST workflow.

## Verification

The repository contains automated tests for the Spring services, ML service, React API helpers, and Flutter state/auth flows. The ML training data is explicitly a controlled synthetic demonstration dataset; its metrics must not be presented as real-world investment-performance evidence.

## Security notes

- JWT identity is derived server-side; portfolio/watchlist requests do not accept a client-supplied user ID for ownership.
- Passwords in the Portfolio Service are stored using BCrypt.
- Never commit `.env`, database passwords, Firebase config files, or generated model artifacts.
- The included Docker configuration is for local/demo use; production deployments should use HTTPS, managed secrets, restricted database access, proper signing keys, and production Firebase configuration.
