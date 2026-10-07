param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CommandArgs
)

if (-not $CommandArgs -or $CommandArgs.Length -eq 0 -or $CommandArgs[0] -eq "help") {
    Write-Host ""
    Write-Host " ComeFort Developer Runner (PowerShell)" -ForegroundColor Cyan
    Write-Host " ======================================" -ForegroundColor DarkGray
    Write-Host " .\dev.ps1 live          - Real-Time Live Mode (like 'npm run dev')" -ForegroundColor Green
    Write-Host " .\dev.ps1 gui           - Launch Desktop GUI once"
    Write-Host " .\dev.ps1 watch         - Continuous file watcher (compiles on save)"
    Write-Host " .\dev.ps1 test          - Run test suite"
    Write-Host " .\dev.ps1 build         - Rebuild local distribution"
    Write-Host ""
    Write-Host " Run any CLI command directly (equivalent to cmf / comefort):" -ForegroundColor Yellow
    Write-Host " .\dev.ps1 today"
    Write-Host " .\dev.ps1 c 'your thought'"
    Write-Host " .\dev.ps1 inbox"
    Write-Host " .\dev.ps1 task list"
    Write-Host ""
    Write-Host " Note: In installed / production mode, use 'cmf' or 'comefort' directly." -ForegroundColor DarkGray
    Write-Host ""
    exit 0
}

$action = $CommandArgs[0].ToLower()

switch ($action) {
    "live" {
        Write-Host ""
        Write-Host " ========================================================" -ForegroundColor Cyan
        Write-Host "  ComeFort Real-Time Live Mode (like 'npm run dev')" -ForegroundColor Yellow
        Write-Host " ========================================================" -ForegroundColor Cyan
        Write-Host " -> CSS Changes: Instant live-reload on Ctrl+S (0s, no restart)" -ForegroundColor Green
        Write-Host " -> Java Changes: Auto-recompiles & relaunches on Ctrl+S" -ForegroundColor Green
        Write-Host " -> In-App Refresh: Press F5 or Ctrl+R in GUI anytime" -ForegroundColor Green
        Write-Host " -> Press Ctrl+C in this terminal to stop." -ForegroundColor DarkGray
        Write-Host ""

        function Start-GuiProc {
            $psi = New-Object System.Diagnostics.ProcessStartInfo
            $psi.FileName = "cmd.exe"
            $psi.Arguments = "/c .\gradlew.bat gui --quiet"
            $psi.UseShellExecute = $false
            $proc = [System.Diagnostics.Process]::Start($psi)
            return $proc
        }

        # Watch src/main/java for code changes
        $srcPath = Join-Path (Get-Location) "src\main\java"
        $watcher = New-Object System.IO.FileSystemWatcher
        $watcher.Path = $srcPath
        $watcher.IncludeSubdirectories = $true
        $watcher.EnableRaisingEvents = $true
        $watcher.Filter = "*.java"

        $guiProc = Start-GuiProc
        Write-Host "[ComeFort Live] Desktop GUI started (PID: $($guiProc.Id)). Monitoring for changes..." -ForegroundColor Cyan

        try {
            while ($true) {
                # Check for file changes
                $change = $watcher.WaitForChanged([System.IO.WatcherChangeTypes]::Changed -bor [System.IO.WatcherChangeTypes]::Created, 1500)
                if ($change.TimedOut) {
                    continue
                }

                # Debounce editor write burst
                Start-Sleep -Milliseconds 400

                Write-Host "`n[ComeFort Live] File changed: $($change.Name)" -ForegroundColor Yellow
                Write-Host "[ComeFort Live] Recompiling classes..." -ForegroundColor Cyan

                & .\gradlew.bat classes --quiet
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "[ComeFort Live] Compile OK! Relaunching GUI..." -ForegroundColor Green
                    if ($guiProc -and -not $guiProc.HasExited) {
                        taskkill.exe /T /F /PID $guiProc.Id | Out-Null
                    }
                    Start-Sleep -Milliseconds 300
                    $guiProc = Start-GuiProc
                    Write-Host "[ComeFort Live] GUI updated and restarted (PID: $($guiProc.Id))." -ForegroundColor Green
                } else {
                    Write-Host "[ComeFort Live] Compile error! Fix error and save file to reload." -ForegroundColor Red
                }
            }
        } finally {
            $watcher.Dispose()
            if ($guiProc -and -not $guiProc.HasExited) {
                taskkill.exe /T /F /PID $guiProc.Id | Out-Null
            }
        }
    }
    "gui" {
        Write-Host "[ComeFort Dev] Launching Desktop GUI..." -ForegroundColor Green
        & .\gradlew.bat gui --quiet
    }
    "watch" {
        Write-Host "[ComeFort Dev] Continuous Watch Mode (auto-recompile on Ctrl+S)..." -ForegroundColor Green
        & .\gradlew.bat -t test
    }
    "test" {
        Write-Host "[ComeFort Dev] Running Test Suite..." -ForegroundColor Green
        & .\gradlew.bat test
    }
    "build" {
        Write-Host "[ComeFort Dev] Rebuilding local binaries..." -ForegroundColor Green
        & .\gradlew.bat installDist
    }
    default {
        $joined = $CommandArgs -join " "
        & .\gradlew.bat run --args="$joined" --quiet
    }
}
