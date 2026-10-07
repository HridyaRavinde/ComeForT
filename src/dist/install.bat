@echo off
setlocal EnableDelayedExpansion
title ComeFort - Setup & Installation

echo ======================================================================
echo                  ComeFort - Setup & Installation
echo ======================================================================
echo.

set "BIN_DIR=%~dp0bin"
set "GUI_BAT=%~dp0ComeFort-GUI.bat"

echo Step 1: Adding ComeFort to your User PATH...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$currentPath = [Environment]::GetEnvironmentVariable('Path', 'User'); if ($currentPath -notlike '*%BIN_DIR%*') { [Environment]::SetEnvironmentVariable('Path', $currentPath + ';%BIN_DIR%', 'User'); Write-Host '  [OK] Successfully added to User PATH.' } else { Write-Host '  [OK] Already present in User PATH.' }"

echo.
echo Step 2: Creating Desktop Shortcut for ComeFort GUI...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$desktop = [Environment]::GetFolderPath('Desktop'); $ws = New-Object -ComObject WScript.Shell; $shortcut = $ws.CreateShortcut(\"$desktop\ComeFort.lnk\"); $shortcut.TargetPath = '%GUI_BAT%'; $shortcut.WorkingDirectory = '%~dp0'; $shortcut.Description = 'ComeFort - Developer Life OS'; $shortcut.Save(); Write-Host '  [OK] Desktop shortcut created at:' (\"$desktop\ComeFort.lnk\")"

echo.
echo ======================================================================
echo   Setup Complete!
echo.
echo   How to use:
echo     1. GUI: Double-click 'ComeFort' shortcut on your Desktop or run 'ComeFort-GUI.bat'.
echo     2. CLI: Open a NEW terminal (CMD / PowerShell) and type 'cmf' or 'comefort'.
echo ======================================================================
echo.
pause
