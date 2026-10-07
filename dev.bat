@echo off
setlocal

if "%~1"=="" goto help
if /i "%~1"=="gui" goto run_gui
if /i "%~1"=="watch" goto run_watch
if /i "%~1"=="test" goto run_test
if /i "%~1"=="build" goto run_build
if /i "%~1"=="help" goto help

:: Pass everything to CLI
goto run_cli

:run_gui
echo [ComeFort Dev] Launching Desktop GUI...
call .\gradlew.bat gui --quiet
goto end

:run_watch
echo [ComeFort Dev] Continuous Watch Mode (auto-recompiles & tests on file save)...
echo Press Ctrl+C to stop.
call .\gradlew.bat -t test
goto end

:run_test
echo [ComeFort Dev] Running Test Suite...
call .\gradlew.bat test
goto end

:run_build
echo [ComeFort Dev] Rebuilding local binaries...
call .\gradlew.bat installDist
goto end

:run_cli
call .\gradlew.bat run --args="%*" --quiet
goto end

:help
echo.
echo  ComeFort Developer Runner (Dev Mode)
echo  ====================================
echo  dev gui             - Launch Desktop GUI instantly
echo  dev watch           - Continuous file watcher (like npm run dev)
echo  dev test            - Run test suite
echo  dev build           - Rebuild local distribution
echo.
echo  Run any CLI command directly (equivalent to cmf / comefort):
echo  dev today           - View Today dashboard
echo  dev c "thought"     - Quick capture
echo  dev inbox           - View inbox
echo  dev task list       - List tasks
echo  dev status          - View status and stats
echo.
echo  Note: In installed / production mode, use 'cmf' or 'comefort' directly.
echo.

:end
endlocal
