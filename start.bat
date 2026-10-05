@echo off
setlocal EnableExtensions

REM ============================================================
REM EquiLens - One-click local development startup
REM ============================================================

set "EQUILENS_ROOT=D:\Project main\equilens-work"
set "ML_DIR=%EQUILENS_ROOT%\ml-service"
set "RESEARCH_DIR=%EQUILENS_ROOT%\research-service"
set "PORTFOLIO_DIR=%EQUILENS_ROOT%\portfolio-service"
set "FRONTEND_DIR=%EQUILENS_ROOT%\frontend"
set "ENV_FILE=%EQUILENS_ROOT%\.env"

title EquiLens Launcher

echo.
echo ============================================================
echo                    EQUILENS STARTUP
echo ============================================================
echo.

REM ---------- Basic checks ----------

if not exist "%EQUILENS_ROOT%" (
    echo [ERROR] EquiLens root folder not found:
    echo %EQUILENS_ROOT%
    pause
    exit /b 1
)

if not exist "%ENV_FILE%" (
    echo [ERROR] .env file not found:
    echo %ENV_FILE%
    pause
    exit /b 1
)

if not exist "%ML_DIR%\.venv313\Scripts\python.exe" (
    echo [ERROR] ML virtual environment not found.
    echo.
    echo Expected:
    echo %ML_DIR%\.venv313
    echo.
    echo Create it with:
    echo cd "%ML_DIR%"
    echo python -m venv .venv313
    echo .\.venv313\Scripts\python.exe -m pip install -r requirements.txt
    pause
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not available on PATH.
    pause
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven is not available on PATH.
    pause
    exit /b 1
)

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js is not available on PATH.
    pause
    exit /b 1
)

echo [OK] Prerequisites found.
echo.

REM ---------- Load .env ----------
REM Secrets are read from .env and are NOT stored in this file.

for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    if not "%%A"=="" set "%%A=%%B"
)

REM Map .env PostgreSQL variables to Spring Boot variables
if "%DB_URL%"=="" set "DB_URL=jdbc:postgresql://localhost:5432/%POSTGRES_DB%"
if "%DB_USERNAME%"=="" set "DB_USERNAME=%POSTGRES_USER%"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=%POSTGRES_PASSWORD%"

if "%POSTGRES_DB%"=="" set "POSTGRES_DB=equilens"
if "%POSTGRES_USER%"=="" set "POSTGRES_USER=postgres"

if "%JWT_SECRET%"=="" (
    echo [ERROR] JWT_SECRET is missing from .env
    pause
    exit /b 1
)

echo [OK] Environment loaded.
echo.

REM ============================================================
REM 1. PostgreSQL
REM ============================================================

echo [1/4] Checking PostgreSQL...

sc query postgresql-x64-18 >nul 2>&1

if not errorlevel 1 (
    sc query postgresql-x64-18 | findstr /I "RUNNING" >nul

    if errorlevel 1 (
        echo       Starting PostgreSQL...
        net start postgresql-x64-18 >nul 2>&1
    ) else (
        echo       PostgreSQL already running.
    )
) else (
    echo       PostgreSQL service not detected.
    echo       Continuing assuming PostgreSQL is already running.
)

REM ============================================================
REM 2. ML Service
REM ============================================================

echo [2/4] Starting ML Service :8000...

start "EquiLens - ML :8000" powershell.exe ^
-NoExit ^
-ExecutionPolicy Bypass ^
-Command ^
"$ErrorActionPreference='Stop'; Set-Location -LiteralPath '%ML_DIR%'; & '.\.venv313\Scripts\python.exe' -m uvicorn app.main:app --host 0.0.0.0 --port 8000"

REM ============================================================
REM 3. Research + Portfolio Services
REM ============================================================

echo [3/4] Starting Research Service :8080...

start "EquiLens - Research :8080" powershell.exe ^
-NoExit ^
-ExecutionPolicy Bypass ^
-Command ^
"$ErrorActionPreference='Stop'; Set-Location -LiteralPath '%RESEARCH_DIR%'; mvn spring-boot:run"

echo       Starting Portfolio Service :8081...

start "EquiLens - Portfolio :8081" powershell.exe ^
-NoExit ^
-ExecutionPolicy Bypass ^
-Command ^
"$ErrorActionPreference='Stop'; Set-Location -LiteralPath '%PORTFOLIO_DIR%'; mvn spring-boot:run"

REM ============================================================
REM 4. React Frontend
REM ============================================================

echo [4/4] Starting React Frontend :5173...

start "EquiLens - Frontend :5173" powershell.exe ^
-NoExit ^
-ExecutionPolicy Bypass ^
-Command ^
"$ErrorActionPreference='Stop'; Set-Location -LiteralPath '%FRONTEND_DIR%'; npm run dev -- --host 0.0.0.0"

REM ============================================================

echo.
echo ============================================================
echo                 EQUILENS IS LAUNCHING
echo ============================================================
echo.
echo   ML Service        http://localhost:8000
echo   Research Service  http://localhost:8080
echo   Portfolio Service http://localhost:8081
echo   React Frontend    http://localhost:5173
echo.
echo ============================================================
echo.
echo Waiting 8 seconds before opening frontend...
timeout /t 8 /nobreak >nul

start "" "http://localhost:5173"

echo.
echo [DONE] All startup processes have been launched.
echo.
echo Keep the service windows open while using EquiLens.
echo.
pause

endlocal