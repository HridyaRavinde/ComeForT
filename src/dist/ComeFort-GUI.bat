@echo off
setlocal
title ComeFort Desktop

:: Check if Java is available
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if not defined JAVA_HOME (
        echo [ERROR] Java is not installed or not in PATH.
        echo Please install Java 21 or later from https://adoptium.net/
        echo.
        pause
        exit /b 1
    )
)

:: Locate and launch cf.bat with gui argument
set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%bin\cf.bat" (
    start "" "%SCRIPT_DIR%bin\cf.bat" gui
) else if exist "%SCRIPT_DIR%cf.bat" (
    start "" "%SCRIPT_DIR%cf.bat" gui
) else (
    echo [ERROR] Could not locate cf.bat in %SCRIPT_DIR%bin\
    pause
    exit /b 1
)
