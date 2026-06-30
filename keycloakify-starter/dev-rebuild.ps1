param(
    [switch]$SkipBuild
)

$COMPOSE_FILE = "docker-compose.local.yml"
$KEYCLOAK_URL = "http://localhost:8080"

Write-Host "=== Stopping Keycloak ===" -ForegroundColor Cyan
docker compose -f $COMPOSE_FILE down 2>&1 | Out-Null

if (-not $SkipBuild) {
    Write-Host "=== Building theme ===" -ForegroundColor Cyan
    yarn build-keycloak-theme 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed. Aborting." -ForegroundColor Red
        exit 1
    }
}

Write-Host "=== Starting Keycloak ===" -ForegroundColor Cyan
docker compose -f $COMPOSE_FILE up -d 2>&1 | Out-Null

Write-Host "=== Waiting for Keycloak to be ready ===" -ForegroundColor Cyan
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep 2
    try {
        $status = curl.exe -s -o nul -w "%{http_code}" "$KEYCLOAK_URL" 2>$null
        if ($status -eq "302") { $ready = $true; break }
    } catch {}
}
if (-not $ready) {
    Write-Host "Keycloak did not start in time." -ForegroundColor Red
    exit 1
}

Write-Host "=== Getting admin token ===" -ForegroundColor Cyan
$token = curl.exe -s "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "client_id=admin-cli&username=admin&password=admin&grant_type=password" | ConvertFrom-Json

Write-Host "=== Creating test realm ===" -ForegroundColor Cyan
$realmJson = '{"realm":"test","enabled":true,"loginTheme":"keycloakify-starter-angular-vite","registrationAllowed":true,"registrationEmailAsUsername":false}'
Set-Content -Path "$env:TEMP\kc-realm.json" -Value $realmJson -Encoding ASCII
$realmCode = curl.exe -s -X POST "$KEYCLOAK_URL/admin/realms" `
    -H "Content-Type: application/json" `
    -H "Authorization: Bearer $($token.access_token)" `
    -d "@$env:TEMP\kc-realm.json" -w "%{http_code}" 2>$null
if ($realmCode -eq "201") {
    Write-Host "  Realm 'test' created." -ForegroundColor Green
} elseif ($realmCode -eq "409") {
    Write-Host "  Realm 'test' already exists." -ForegroundColor Green
} else {
    Write-Host "  Realm creation returned HTTP $realmCode." -ForegroundColor Yellow
}

Write-Host "=== Creating test user ===" -ForegroundColor Cyan
$userJson = '{"username":"testuser","enabled":true,"email":"test@example.com","firstName":"Test","lastName":"User","credentials":[{"type":"password","value":"test123","temporary":false}]}'
Set-Content -Path "$env:TEMP\kc-user.json" -Value $userJson -Encoding ASCII
$userCode = curl.exe -s -X POST "$KEYCLOAK_URL/admin/realms/test/users" `
    -H "Content-Type: application/json" `
    -H "Authorization: Bearer $($token.access_token)" `
    -d "@$env:TEMP\kc-user.json" -w "%{http_code}" 2>$null
if ($userCode -eq "201") {
    Write-Host "  User 'testuser' created." -ForegroundColor Green
} elseif ($userCode -eq "409") {
    Write-Host "  User 'testuser' already exists." -ForegroundColor Green
} else {
    Write-Host "  User creation returned HTTP $userCode." -ForegroundColor Yellow
}

Write-Host "`n=== Done ===" -ForegroundColor Cyan
Write-Host "Login:  http://localhost:8080/realms/test/account/"
Write-Host "Admin:  http://localhost:8080/admin/master/console/ (admin/admin)"
Write-Host "User:   testuser / test123"
