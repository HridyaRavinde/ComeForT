param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CommandArgs
)

if ($CommandArgs -and $CommandArgs.Length -gt 0 -and ($CommandArgs[0] -in "help", "--help", "-h")) {
    Write-Host ""
    Write-Host " ComeFort Developer Runner (PowerShell)" -ForegroundColor Cyan
    Write-Host " ======================================" -ForegroundColor DarkGray
    Write-Host " .\dev.ps1               - Real-Time Live Mode (like 'npm run dev')" -ForegroundColor Green
    Write-Host " .\dev.ps1 gui           - Launch Desktop GUI with Live Reload" -ForegroundColor Green
    Write-Host " .\dev.ps1 live          - Launch Desktop GUI with Live Reload" -ForegroundColor Green
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

$action = if (-not $CommandArgs -or $CommandArgs.Length -eq 0) { "live" } else { $CommandArgs[0].ToLower() }

function Get-JavaCommand {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $candidate) {
            return $candidate
        }
    }
    $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }
    return "java.exe"
}

function Start-GuiProc {
    param([string]$javaCmd)
    $argFile = Join-Path (Get-Location) "build\dev-args.txt"
    if (-not (Test-Path $argFile)) {
        & .\gradlew.bat classes --quiet --console=plain
    }
    $proc = Start-Process -FilePath $javaCmd -ArgumentList "@$argFile" -WorkingDirectory (Get-Location).Path -PassThru
    return $proc
}

switch ($action) {
    { $_ -in "live", "gui" } {
        Write-Host ""
        Write-Host " ========================================================" -ForegroundColor Cyan
        Write-Host "  ComeFort Real-Time Live Mode (like 'npm run dev')" -ForegroundColor Yellow
        Write-Host " ========================================================" -ForegroundColor Cyan
        Write-Host " -> CSS Changes: Instant live-reload on Ctrl+S (0s, no restart)" -ForegroundColor Green
        Write-Host " -> Java Changes: Auto-recompiles & relaunches on Ctrl+S" -ForegroundColor Green
        Write-Host " -> State Preserved: Keeps active screen across reloads" -ForegroundColor Green
        Write-Host " -> In-App Refresh: Press F5 or Ctrl+R in GUI anytime" -ForegroundColor Green
        Write-Host " -> Press Ctrl+C in this terminal to stop." -ForegroundColor DarkGray
        Write-Host ""

        $javaCmd = Get-JavaCommand
        $argFile = Join-Path (Get-Location) "build\dev-args.txt"
        if (-not (Test-Path $argFile)) {
            Write-Host "[ComeFort Live] Building initial workspace..." -ForegroundColor Cyan
            & .\gradlew.bat classes --quiet --console=plain
        }

        # Watch src/main for code and resource changes (with atomic save detection)
        $srcPath = Join-Path (Get-Location) "src\main"
        $watcher = New-Object System.IO.FileSystemWatcher
        $watcher.Path = $srcPath
        $watcher.IncludeSubdirectories = $true
        $watcher.EnableRaisingEvents = $true
        $watcher.NotifyFilter = [System.IO.NotifyFilters]::FileName -bor [System.IO.NotifyFilters]::LastWrite -bor [System.IO.NotifyFilters]::CreationTime

        $guiProc = Start-GuiProc -javaCmd $javaCmd
        Write-Host "[ComeFort Live] Desktop GUI window is now open (PID: $($guiProc.Id)). Monitoring for changes..." -ForegroundColor Cyan

        try {
            while ($true) {
                if ($guiProc -and $guiProc.HasExited) {
                    Write-Host "[ComeFort Live] GUI window closed by user. Exiting live mode." -ForegroundColor DarkGray
                    break
                }

                # Catch changes, creations, and atomic save renames
                $change = $watcher.WaitForChanged([System.IO.WatcherChangeTypes]::All, 800)
                if ($change.TimedOut) {
                    continue
                }

                # If CSS changed, the JavaFX in-app CSS watcher handles it instantly without restart
                if ($change.Name.EndsWith(".css")) {
                    Write-Host "[ComeFort Live] Style updated: $($change.Name) (Hot-reloaded in-app ⚡)" -ForegroundColor Magenta
                    continue
                }

                # Debounce editor write burst
                Start-Sleep -Milliseconds 350

                Write-Host "`n[ComeFort Live] File changed: $($change.Name)" -ForegroundColor Yellow
                Write-Host "[ComeFort Live] Recompiling classes..." -ForegroundColor Cyan

                & .\gradlew.bat classes --quiet --console=plain
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "[ComeFort Live] Compile OK! Relaunching GUI..." -ForegroundColor Green
                    if ($guiProc -and -not $guiProc.HasExited) {
                        Stop-Process -Id $guiProc.Id -Force -ErrorAction SilentlyContinue
                    }
                    Start-Sleep -Milliseconds 200
                    $guiProc = Start-GuiProc -javaCmd $javaCmd
                    Write-Host "[ComeFort Live] GUI window updated and active (PID: $($guiProc.Id))." -ForegroundColor Green
                } else {
                    Write-Host "[ComeFort Live] Compile error! Fix error and save file to reload." -ForegroundColor Red
                }
            }
        } finally {
            $watcher.Dispose()
            if ($guiProc -and -not $guiProc.HasExited) {
                Stop-Process -Id $guiProc.Id -Force -ErrorAction SilentlyContinue
            }
        }
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
