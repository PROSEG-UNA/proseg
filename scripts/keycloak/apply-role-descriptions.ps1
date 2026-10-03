param(
    [string]$KeycloakUrl = $env:KEYCLOAK_SERVER_URL,
    [string]$Realm       = $env:KEYCLOAK_REALM,
    [string]$AdminUser   = $env:KEYCLOAK_ADMIN_USERNAME,
    [string]$AdminPass   = $env:KEYCLOAK_ADMIN_PASSWORD,
    [switch]$Apply
)

# Solo completa descripciones vacias. No sobrescribe descripciones existentes ni toca composites, atributos o asignaciones.
# Sin -Apply solo muestra lo que haria (dry-run).

if (-not $KeycloakUrl) { $KeycloakUrl = "http://localhost:8180/auth" }
if (-not $Realm)       { $Realm       = "proseg-realm" }
if (-not $AdminUser)   { throw "Falta KEYCLOAK_ADMIN_USERNAME o el parametro -AdminUser" }
if (-not $AdminPass)   { throw "Falta KEYCLOAK_ADMIN_PASSWORD o el parametro -AdminPass" }

$descriptions = Get-Content (Join-Path $PSScriptRoot "role-descriptions.json") -Raw -Encoding UTF8 | ConvertFrom-Json

$tokenBody = "grant_type=password&client_id=admin-cli&username=$AdminUser&password=$([uri]::EscapeDataString($AdminPass))"
$token = (Invoke-RestMethod `
    -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" `
    -Method Post `
    -ContentType "application/x-www-form-urlencoded" `
    -Body $tokenBody).access_token
$headers = @{ Authorization = "Bearer $token" }

if ($Apply) { Write-Host "MODO APLICAR en $KeycloakUrl (realm: $Realm)" } else { Write-Host "DRY-RUN (usa -Apply para escribir) en $KeycloakUrl (realm: $Realm)" }

foreach ($prop in $descriptions.PSObject.Properties) {
    $name = $prop.Name
    $newDescription = $prop.Value

    try {
        $role = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/roles/$name" -Headers $headers -Method Get
    } catch {
        Write-Host "  WARN  $name - no existe en el realm, omitido"
        continue
    }

    if ($role.description -and $role.description.Trim() -ne "") {
        Write-Host "  SKIP  $name (ya tiene descripcion)"
        continue
    }

    if (-not $Apply) {
        Write-Host "  PLAN  $name -> $newDescription"
        continue
    }

    $role | Add-Member -NotePropertyName description -NotePropertyValue $newDescription -Force
    $bodyJson = $role | ConvertTo-Json -Depth 10
    $bodyJson = [regex]::Replace($bodyJson, '[^\x00-\x7F]', { '\u{0:x4}' -f [int][char]$args[0].Value })

    try {
        Invoke-RestMethod `
            -Uri "$KeycloakUrl/admin/realms/$Realm/roles/$name" `
            -Method Put `
            -Headers $headers `
            -ContentType "application/json" `
            -Body $bodyJson | Out-Null
        Write-Host "  OK    $name"
    } catch {
        Write-Host "  FAIL  $name"
        if ($_.ErrorDetails.Message) { Write-Host "        Keycloak: $($_.ErrorDetails.Message)" }
    }
}
