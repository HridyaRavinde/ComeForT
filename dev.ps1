param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CommandArgs
)

if (-not $CommandArgs -or $CommandArgs.Length -eq 0 -or $CommandArgs[0] -eq "help") {
    Write-Host ""
    Write-Host " ComeFort Developer Runner (PowerShell)" -ForegroundColor Cyan
    Write-Host " ======================================" -ForegroundColor DarkGray
    Write-Host " .\dev.ps1 gui           - Launch Desktop GUI instantly"
    Write-Host " .\dev.ps1 watch         - Continuous file watcher (like npm run dev)"
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
