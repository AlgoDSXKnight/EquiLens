# EquiLens Backend

Kotlin + Spring Boot REST API for the EquiLens equity research platform.

## Run

Set PostgreSQL credentials and a JWT signing secret before starting the reference monolith:

PowerShell:
```powershell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="at-least-32-bytes-of-random-secret-material"
```

Then run with Maven/IDE. The API starts on `http://localhost:8080`.

### Endpoints
- GET `/api/companies`
- GET `/api/companies/{id}`
- GET `/api/companies/symbol/{symbol}`
- POST `/api/companies`
- PUT `/api/companies/{id}`
- DELETE `/api/companies/{id}`
