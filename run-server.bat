@echo off
SETLOCAL EnableDelayedExpansion

:: ============================================================================
:: Script di Avvio Rapido - Secure Web Application
:: ============================================================================

set "PROJECT_DIR=%~dp0"
set "JAVA_HOME_LOCAL=%PROJECT_DIR%tools\jdk"
set "JETTY_RUNNER=%PROJECT_DIR%jetty-runner.jar"
set "WAR_FILE=%PROJECT_DIR%target\secure-web-app.war"
set "PORT=9090"

echo.
echo ============================================================
echo   AVVIO SECURE WEB APPLICATION (Porta: %PORT%)
echo ============================================================
echo.

:: 1. Verifica JAVA_HOME
if exist "%JAVA_HOME_LOCAL%\bin\java.exe" (
    set "JAVA_EXE=%JAVA_HOME_LOCAL%\bin\java.exe"
    echo [INFO] Utilizzo JDK locale trovato in tools\jdk
) else (
    where java >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        set "JAVA_EXE=java"
        echo [INFO] Utilizzo Java di sistema
    ) else (
        echo [ERRORE] Java non trovato. Assicurarsi che sia nel PATH o in tools\jdk.
        pause
        exit /b 1
    )
)

:: 2. Verifica Jetty Runner
if not exist "%JETTY_RUNNER%" (
    echo [ERRORE] jetty-runner.jar non trovato.
    echo Eseguire prima deploy.ps1 o scaricare manualmente jetty-runner.jar.
    pause
    exit /b 1
)

:: 3. Verifica WAR
if not exist "%WAR_FILE%" (
    echo [AVVISO] File WAR non trovato in %WAR_FILE%
    echo Eseguire 'mvn clean package' prima di avviare il server.
    pause
    exit /b 1
)

:: 4. Avvio Server
echo [LIVE] Avvio server su http://localhost:%PORT%/
echo [INFO] Premere Ctrl+C per fermare il server.
echo.

"%JAVA_EXE%" -jar "%JETTY_RUNNER%" --port %PORT% "%WAR_FILE%"

pause
