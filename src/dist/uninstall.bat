@echo off
setlocal
title ComeFort - Uninstaller

echo ======================================================================
echo                  ComeFort - Uninstall Helper
echo ======================================================================
echo.

set "BIN_DIR=%~dp0bin"

echo Removing from User PATH...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$currentPath = [Environment]::GetEnvironmentVariable('Path', 'User'); $parts = $currentPath.Split(';') | Where-Object { $_ -ne '%BIN_DIR%' -and $_ -ne '' }; [Environment]::SetEnvironmentVariable('Path', ($parts -join ';'), 'User'); Write-Host '  [OK] Removed from User PATH.'"

echo Removing Desktop Shortcut...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$desktop = [Environment]::GetFolderPath('Desktop'); $lnk = \"$desktop\ComeFort.lnk\"; if (Test-Path $lnk) { Remove-Item $lnk; Write-Host '  [OK] Removed Desktop shortcut.' } else { Write-Host '  [OK] No shortcut found.' }"

echo.
echo ======================================================================
echo   ComeFort uninstalled from system PATH and shortcuts removed.
echo   (To completely remove all data, delete ~/.comefort/ folder).
echo ======================================================================
echo.
pause
