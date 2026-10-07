@echo off
setlocal
title ComeFort CLI Environment

set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%bin" (
    set "PATH=%SCRIPT_DIR%bin;%PATH%"
)

echo ======================================================================
echo                 ComeFort - Developer Life OS (CLI)
echo ======================================================================
echo.
echo   [OK] Environment configured! 'cf' command is available in this shell.
echo.
echo   Quick Examples:
echo     cf c "your thought here"          -- Quick capture to inbox
echo     cf project add MyProject          -- Create a project
echo     cf task add "Task" -p MyProject   -- Add a task
echo     cf today                          -- Daily overview
echo     cf gui                            -- Open the desktop GUI
echo     cf --help                         -- View all commands
echo.
echo ======================================================================
echo.

:: Initialize or show current status
call "%SCRIPT_DIR%bin\cf.bat" status

echo.
cmd /k "echo Tip: Run 'cf --help' anytime to explore commands. & echo."
