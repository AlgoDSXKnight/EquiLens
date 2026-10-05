@echo off
setlocal EnableExtensions

title EquiLens Shutdown

echo.
echo ============================================================
echo                    EQUILENS SHUTDOWN
echo ============================================================
echo.

echo [1/4] Stopping ML Service...

taskkill /FI "WINDOWTITLE eq EquiLens - ML :8000" /T /F >nul 2>&1

echo       Done.

echo [2/4] Stopping Research Service...

taskkill /FI "WINDOWTITLE eq EquiLens - Research :8080" /T /F >nul 2>&1

echo       Done.

echo [3/4] Stopping Portfolio Service...

taskkill /FI "WINDOWTITLE eq EquiLens - Portfolio :8081" /T /F >nul 2>&1

echo       Done.

echo [4/4] Stopping React Frontend...

taskkill /FI "WINDOWTITLE eq EquiLens - Frontend :5173" /T /F >nul 2>&1

echo       Done.

echo.
echo ============================================================
echo                 EQUILENS SERVICES STOPPED
echo ============================================================
echo.
echo PostgreSQL was NOT stopped.
echo Your database remains available for other applications.
echo.
pause

endlocal