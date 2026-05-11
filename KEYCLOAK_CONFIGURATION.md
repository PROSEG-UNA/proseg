# 🔐 Configuración de Keycloak - SSSI

Guía completa para configurar Keycloak como Identity Provider del sistema SSSI.

---

## 📋 Requisitos previos

- Java 17+
- Keycloak descargado desde [https://www.keycloak.org/getting-started/getting-started-zip](https://www.keycloak.org/getting-started/getting-started-zip)
- Puerto 8080 disponible

---

## Descargar e instalar Keycloak

### Opción A: Descargar manualmente

1. Descarga Keycloak desde: https://www.keycloak.org/downloads
2. Extrae el archivo ZIP
3. Navega a la carpeta `bin/`

---

## Iniciar Keycloak

### En Windows (desde carpeta `bin/`):

```bash
# Opción 1: Usar script
kc.bat start-dev

# Opción 2: Usar mvnw
mvnw.cmd spring-boot:run
```

### En Linux/Mac:

```bash
./kc.sh start-dev
```

**Espera a ver:**
```
2024-04-12 16:00:00,000 INFO  [io.quarkus] (main) Keycloak 24.0.1 on JVM started in ...
```

**Acceso:** http://localhost:8080

---

## Configuración inicial de Keycloak

### Crear admin y cambiar contraseña

1. La primera vez, Keycloak te pedirá crear un usuario admin
2. Crea con:
   - Username: `sssi-system`
   - Password: `sssi-system`

O desde la línea de comandos:
```bash
# Windows
kc.bat start-dev --import-realm

# O configurar variables de entorno
set KEYCLOAK_ADMIN=sssi-system
set KEYCLOAK_ADMIN_PASSWORD=sssi-system
kc.bat start-dev
```

---

## Crear el Realm "sssi-realm"

1. Abre http://localhost:8080/admin
2. Login con:
   - Username: `sssi-system`
   - Password: `sssi-system`

3. En la esquina superior izquierda, busca **"Master"** (dropdown)
4. Click en **"Create realm"**
5. Rellena:
   - **Realm name:** `sssi-realm`
   - **Enabled:** ON ✓
   - Click **"Create"**

### Configuración del Realm

Una vez creado, ve a la pestaña **"Settings"** del realm y ajusta:

```yaml
Name: sssi-realm
Enabled: ON
Display name: SSSI System
HTML display name: <strong>SSSI</strong> Sistema de Gestión

# Token settings
Access Token Lifespan: 5 minutes (default)
Refresh Token Lifespan: 1 hour

# Password policy
Passwords require:
- At least 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 number
```

Click **"Save"**

---

## Crear el Cliente "sssi-app"

### Crear cliente

1. En el realm **sssi-realm**, ve a **Clients** (lado izquierdo)
2. Click **"Create client"**
3. Rellena:
   - **Client ID:** `sssi-app`
   - **Name:** `SSSI App`
   - Click **"Next"**

### Configurar Capability config

En la pantalla de configuración, busca **"Capability config"** y activa:

```
✓ Client authentication: OFF (Public Client)
✓ Authorization: OFF

Authentication flow:
  ✓ Standard flow
  ✓ Direct access grants
  ☐ Implicit flow
  ☐ Service account roles

✓ Require PKCE: OFF
✓ Require DPoP bound tokens: OFF
```

Click **"Next"** → **"Save"**

### Configurar URLs

1. En el cliente **sssi-app**, ve a tab **"Settings"**
2. Configura:

```yaml
General:
  Client ID: sssi-app
  Name: SSSI App
  Enabled: ON

Login settings:
  Home URL: http://localhost:5173
  Valid redirect URIs: 
    - http://localhost:5173
    - http://localhost:5173/*
  Valid post logout redirect URIs:
    - http://localhost:5173
  Web origins:
    - http://localhost:5173
```

Click **"Save"**

### Configurar Client scopes

1. En el cliente, ve a tab **"Client scopes"**
2. Verifica que tenga estos scopes **por defecto**:
   - `profile`
   - `email`
   - `roles` (opcional)

Si falta alguno, agrégalo desde **"Default Client Scopes"**

---

## 6️⃣ Crear un usuario de prueba

1. En el realm **sssi-realm**, ve a **Manage** → **Users**
2. Click **"Create user"**
3. Rellena:

```yaml
Username: demo
Email: demo@sssi.com
First name: Demo
Last name: User
Enabled: ON
Email verified: ON
```

Click **"Create"**

### Establecer contraseña

1. En el usuario **demo**, ve a tab **"Credentials"**
2. Click **"Set password"**
3. Rellena:
   - Password: `demo123`
   - Confirmation: `demo123`
   - Temporary: OFF
   - Click **"Set Password"**

---

## 7️⃣ Verificar la configuración

### Test 1: Verificar token endpoint

Desde Postman o curl:

```bash
curl -X POST http://localhost:8080/realms/sssi-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=sssi-app&username=demo&password=demo123&scope=openid profile email"
```

**Respuesta esperada:** JWT token

### Test 2: Verificar JWK Set

```bash
curl http://localhost:8080/realms/sssi-realm/protocol/openid-connect/certs
```

**Respuesta esperada:** Claves públicas en formato JSON

---

## 8️⃣ Configuración para el Sistema SSSI

### Variables de entorno necesarias

```bash
# Keycloak
KEYCLOAK_SERVER_URL=http://localhost:8080
KEYCLOAK_REALM=sssi-realm
KEYCLOAK_CLIENT_ID=sssi-app
KEYCLOAK_ADMIN_USERNAME=sssi-system
KEYCLOAK_ADMIN_PASSWORD=sssi-system
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/sssi-realm

# Frontend (React)
VITE_KEYCLOAK_URL=http://localhost:8080
VITE_KEYCLOAK_REALM=sssi-realm
VITE_KEYCLOAK_CLIENT_ID=sssi-app
```

### Archivos de configuración del sistema

**`msvc-auth.yml`:**
```yaml
keycloak:
  server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
  realm: ${KEYCLOAK_REALM:sssi-realm}
  client:
    id: ${KEYCLOAK_CLIENT_ID:sssi-app}
    secret: ${KEYCLOAK_CLIENT_SECRET:}
  admin:
    username: ${KEYCLOAK_ADMIN_USERNAME:sssi-system}
    password: ${KEYCLOAK_ADMIN_PASSWORD:sssi-system}
```

**`msvc-gateway.yml`:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/sssi-realm}
          jwk-set-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/sssi-realm}/protocol/openid-connect/certs
```

---

## 9️⃣ Casos de uso comunes

### Caso 1: Cambiar contraseña admin

1. Abre http://localhost:8080/admin
2. Click en tu usuario (arriba a la derecha)
3. **"Manage account"** → **"Signing in"** → **"Change password"**

### Caso 2: Habilitar SMTP para emails

1. En el realm, ve a **Configure** → **Email**
2. Configura tu servidor SMTP (Gmail, SendGrid, etc.)
3. Ahora Keycloak puede enviar emails de verificación

### Caso 3: Agregar roles

1. En el realm, ve a **Configure** → **Roles**
2. Click **"Create role"**
3. Nombre: `admin`, `user`, `moderator`, etc.

### Caso 4: Asignar roles a usuario

1. En el usuario, ve a tab **"Role mapping"**
2. Click **"Assign role"**
3. Selecciona el rol
4. Click **"Assign"**

---

## ✅ Verificación final

Antes de usar en producción, verifica:

- [ ] Realm `sssi-realm` creado
- [ ] Admin `sssi-system` con contraseña correcta
- [ ] Cliente `sssi-app` como Public Client
- [ ] Auth flows: Standard flow ✓, Direct access grants ✓
- [ ] URLs configuradas: http://localhost:5173
- [ ] Usuario `demo` creado con contraseña
- [ ] Token endpoint funciona (test en Postman)
- [ ] JWK set endpoint accesible
- [ ] Gateway obtiene tokens correctamente

---

## 📚 Referencias

- [Keycloak Getting Started](https://www.keycloak.org/getting-started/getting-started-zip)
- [Keycloak Admin REST API](https://www.keycloak.org/docs/latest/server_admin/)
- [OpenID Connect Direct Grant Flow](https://openid.net/specs/openid-connect-core-1_0.html)
- [SSSI System Integration](./INTEGRATION.md)

---

**Última actualización:** Abril 2026  
**Versión:** 1.0
