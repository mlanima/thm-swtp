param([switch]$SkipTests)

$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.+)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            if ($value) {
                [Environment]::SetEnvironmentVariable($name, $value)
            }
        }
    }
}

$cmd = "spring-boot:run"
if ($SkipTests) {
    $cmd += " -DskipTests"
}

Write-Host "Starting app with env from .env ..." -ForegroundColor Green
& $PSScriptRoot\mvnw.cmd $cmd
