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
echo   [OK] Environment configured! Both 'cmf' and 'comefort' are available.
echo        (They are identical twin commands, use whichever you prefer!)
echo.
echo   Quick Examples:
echo     cmf c "your thought here"          -- Quick capture to inbox
echo     cmf project add MyProject          -- Create a project
echo     cmf task add "Task" -p MyProject   -- Add a task
echo     cmf today                          -- Daily overview
echo     cmf gui                            -- Open the desktop GUI
echo     cmf --help (or comefort --help)    -- View all commands
echo.
echo ======================================================================
echo.

:: Initialize or show current status
if exist "%SCRIPT_DIR%bin\cmf.bat" (
    call "%SCRIPT_DIR%bin\cmf.bat" status
) else (
    call "%SCRIPT_DIR%bin\comefort.bat" status
)

echo.
cmd /k "echo Tip: Run 'cmf --help' or 'comefort --help' anytime to explore commands. & echo."
