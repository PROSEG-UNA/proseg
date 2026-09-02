param(
    [string]$KeycloakUrl      = $env:KEYCLOAK_SERVER_URL,
    [string]$Realm            = $env:KEYCLOAK_REALM,
    [string]$AdminUser        = $env:KEYCLOAK_ADMIN_USERNAME,
    [string]$AdminPass        = $env:KEYCLOAK_ADMIN_PASSWORD
)

if (-not $KeycloakUrl) { $KeycloakUrl = "http://localhost:8080" }
if (-not $Realm)       { $Realm       = "proseg-realm" }
if (-not $AdminUser)   { $AdminUser   = "admin" }
if (-not $AdminPass)   { $AdminPass   = "sssi-system" }

Write-Host "Conectando a $KeycloakUrl (realm: $Realm)..."

$tokenBody = "grant_type=password&client_id=admin-cli&username=$AdminUser&password=$AdminPass"
try {
    $tokenResponse = Invoke-RestMethod `
        -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $tokenBody
    $token = $tokenResponse.access_token
} catch {
    Write-Error "ERROR: No se pudo obtener token de administrador. Verificá las credenciales."
    exit 1
}

if (-not $token) {
    Write-Error "ERROR: Token vacio. Verificá las credenciales."
    exit 1
}

Write-Host "Token de administrador obtenido."

function ConvertTo-Hashtable {
    param($Obj)
    if ($Obj -is [System.Management.Automation.PSCustomObject]) {
        $ht = @{}
        $Obj.PSObject.Properties | ForEach-Object { $ht[$_.Name] = ConvertTo-Hashtable $_.Value }
        return $ht
    } elseif ($Obj -is [System.Array]) {
        return @($Obj | ForEach-Object { ConvertTo-Hashtable $_ })
    } else {
        return $Obj
    }
}

function Set-RoleDomain {
    param([string]$RoleName, [string]$Domain)

    $headers = @{ Authorization = "Bearer $token" }

    try {
        $role = Invoke-RestMethod `
            -Uri "$KeycloakUrl/admin/realms/$Realm/roles/$RoleName" `
            -Headers $headers `
            -Method Get
    } catch {
        Write-Host "  WARN  $RoleName — rol no encontrado en el realm, omitiendo"
        return
    }

    $currentDomain = $null
    if ($role.attributes -and $role.attributes.PSObject.Properties['domain']) {
        $currentDomain = $role.attributes.domain[0]
    }

    if ($currentDomain -eq $Domain) {
        Write-Host "  SKIP  $RoleName -> $Domain (ya configurado)"
        return
    }

    $body = ConvertTo-Hashtable $role
    if (-not $body['attributes']) { $body['attributes'] = @{} }

    foreach ($key in @($body['attributes'].Keys)) {
        if ($body['attributes'][$key] -isnot [System.Array]) {
            $body['attributes'][$key] = @($body['attributes'][$key])
        }
    }

    $body['attributes']['domain'] = @($Domain)

    $bodyJson = $body | ConvertTo-Json -Depth 10
    $bodyJson = [regex]::Replace($bodyJson, '[^\x00-\x7F]', {
        '\u{0:x4}' -f [int][char]$args[0].Value
    })

    try {
        Invoke-RestMethod `
            -Uri "$KeycloakUrl/admin/realms/$Realm/roles/$RoleName" `
            -Method Put `
            -Headers $headers `
            -ContentType "application/json" `
            -Body $bodyJson | Out-Null
        Write-Host "  OK    $RoleName -> $Domain"
    } catch {
        $detail = $_.ErrorDetails.Message
        Write-Host "  FAIL  $RoleName"
        if ($detail) { Write-Host "        Keycloak: $detail" }
        Write-Host "        JSON enviado: $bodyJson"
    }
}

Write-Host ""
Write-Host "--- Usuarios ---"
Set-RoleDomain "LEER_USUARIOS"                "Usuarios"
Set-RoleDomain "LEER_USUARIO"                 "Usuarios"
Set-RoleDomain "CREAR_USUARIO"                "Usuarios"
Set-RoleDomain "APROBAR_USUARIO"              "Usuarios"
Set-RoleDomain "LEER_USUARIOS_POR_ROL"        "Usuarios"
Set-RoleDomain "LEER_INVITACIONES_PENDIENTES" "Usuarios"

Write-Host ""
Write-Host "--- Roles ---"
Set-RoleDomain "CREAR_ROL"             "Roles"
Set-RoleDomain "EDITAR_ROL"            "Roles"
Set-RoleDomain "ELIMINAR_ROL"          "Roles"
Set-RoleDomain "LEER_ROLES_BASE"       "Roles"
Set-RoleDomain "LEER_ROLES_COMPUESTOS" "Roles"
Set-RoleDomain "LEER_COMPOSITES_ROL"   "Roles"

Write-Host ""
Write-Host "--- Roles de Usuario ---"
Set-RoleDomain "ASIGNAR_ROL_USUARIO"  "Roles de Usuario"
Set-RoleDomain "ELIMINAR_ROL_USUARIO" "Roles de Usuario"
Set-RoleDomain "LEER_ROLES_USUARIO"   "Roles de Usuario"

Write-Host ""
Write-Host "--- Inventario ---"
Set-RoleDomain "LEER_ACTIVOS"      "Inventario"
Set-RoleDomain "GESTIONAR_ACTIVOS" "Inventario"
Set-RoleDomain "ELIMINAR_ACTIVOS"  "Inventario"
Set-RoleDomain "IMPORTAR_ACTIVOS"  "Inventario"

Write-Host ""
Write-Host "--- Ubicaciones ---"
Set-RoleDomain "LEER_UBICACIONES"      "Ubicaciones"
Set-RoleDomain "GESTIONAR_UBICACIONES" "Ubicaciones"
Set-RoleDomain "ELIMINAR_UBICACIONES"  "Ubicaciones"

Write-Host ""
Write-Host "--- Archivos ---"
Set-RoleDomain "LEER_ARCHIVOS"  "Archivos"
Set-RoleDomain "SUBIR_ARCHIVOS" "Archivos"

Write-Host ""
Write-Host "--- Empresas ---"
Set-RoleDomain "LEER_EMPRESAS"               "Empresas"
Set-RoleDomain "GESTIONAR_EMPRESAS"          "Empresas"
Set-RoleDomain "ELIMINAR_EMPRESAS"           "Empresas"
Set-RoleDomain "LEER_USUARIOS_EMPRESAS"      "Empresas"
Set-RoleDomain "GESTIONAR_USUARIOS_EMPRESAS" "Empresas"
Set-RoleDomain "ELIMINAR_USUARIOS_EMPRESAS"  "Empresas"

Write-Host ""
Write-Host "--- Solicitud de mantenimiento ---"
Set-RoleDomain "LEER_SOLICITUDES_MANTENIMIENTO"                  "Solicitud de mantenimiento"
Set-RoleDomain "SOLICITAR_MANTENIMIENTO"                         "Solicitud de mantenimiento"
Set-RoleDomain "EDITAR_SOLICITUDES_MANTENIMIENTO"                "Solicitud de mantenimiento"
Set-RoleDomain "SELECCIONAR_EMPRESA_EN_SOLICITUD_MANTENIMIENTO"  "Solicitud de mantenimiento"
Set-RoleDomain "ELIMINAR_SOLICITUDES_MANTENIMIENTO"              "Solicitud de mantenimiento"
Set-RoleDomain "CANCELAR_SOLICITUDES_MANTENIMIENTO"              "Solicitud de mantenimiento"

Write-Host ""
Write-Host "--- Registro de mantenimiento ---"
Set-RoleDomain "LEER_REGISTROS_MANTENIMIENTO"           "Registro de mantenimiento"
Set-RoleDomain "GESTIONAR_REGISTROS_MANTENIMIENTO"      "Registro de mantenimiento"
Set-RoleDomain "LEER_HISTORIAL_REGISTROS_MANTENIMIENTO" "Registro de mantenimiento"
Set-RoleDomain "LEER_TECNICOS_MANTENIMIENTO"            "Registro de mantenimiento"
Set-RoleDomain "GESTIONAR_TECNICOS_MANTENIMIENTO"       "Registro de mantenimiento"
Set-RoleDomain "ELIMINAR_TECNICOS_MANTENIMIENTO"        "Registro de mantenimiento"

Write-Host ""
Write-Host "--- Tickets ---"
Set-RoleDomain "LEER_TICKET_MANTENIMIENTO"               "Tickets"
Set-RoleDomain "LEER_TICKETS_MANTENIMIENTO"              "Tickets"
Set-RoleDomain "LEER_TODOS_TICKETS_MANTENIMIENTO"        "Tickets"
Set-RoleDomain "CREAR_TICKETS_MANTENIMIENTO"             "Tickets"
Set-RoleDomain "EDITAR_TICKETS_MANTENIMIENTO"            "Tickets"
Set-RoleDomain "ELIMINAR_TICKETS_MANTENIMIENTO"          "Tickets"
Set-RoleDomain "COMENTAR_TICKETS_MANTENIMIENTO"          "Tickets"
Set-RoleDomain "ASIGNAR_PRIORIDAD_TICKETS_MANTENIMIENTO" "Tickets"

Write-Host ""
Write-Host "Completado."