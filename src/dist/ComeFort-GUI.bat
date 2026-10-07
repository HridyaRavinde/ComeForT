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

:: Locate and launch comefort.bat or cmf.bat with gui argument
set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%bin\comefort.bat" (
    start "" "%SCRIPT_DIR%bin\comefort.bat" gui
) else if exist "%SCRIPT_DIR%bin\cmf.bat" (
    start "" "%SCRIPT_DIR%bin\cmf.bat" gui
) else if exist "%SCRIPT_DIR%comefort.bat" (
    start "" "%SCRIPT_DIR%comefort.bat" gui
) else if exist "%SCRIPT_DIR%cmf.bat" (
    start "" "%SCRIPT_DIR%cmf.bat" gui
) else (
    echo [ERROR] Could not locate comefort.bat or cmf.bat in %SCRIPT_DIR%bin\
    pause
    exit /b 1
)
