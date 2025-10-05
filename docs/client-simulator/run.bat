@echo off
setlocal

echo.
echo ========================================
echo   Movie Service Client Simulator
echo ========================================
echo.

REM Check if Node.js is installed
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Node.js is not installed or not in PATH
    echo Please install Node.js from https://nodejs.org/
    pause
    exit /b 1
)

REM Check if package.json exists
if not exist "package.json" (
    echo Error: package.json not found
    echo Make sure you're in the client-simulator directory
    pause
    exit /b 1
)

REM Install dependencies if node_modules doesn't exist
if not exist "node_modules" (
    echo Installing dependencies...
    call npm install
    if %errorlevel% neq 0 (
        echo Error: Failed to install dependencies
        pause
        exit /b 1
    )
    echo Dependencies installed successfully
    echo.
)

REM Run the main script with arguments
if "%1"=="" (
    node index.js help
) else (
    node index.js %*
)

if %errorlevel% neq 0 (
    echo.
    echo Operation completed with errors
    pause
    exit /b 1
)

echo.
echo Operation completed successfully
pause