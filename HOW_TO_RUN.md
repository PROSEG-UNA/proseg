# Cómo ejecutar PROSEG en local

Los comandos del paso 1 se ejecutan desde la raíz del proyecto. A partir del paso 2 se trabaja dentro de `services/`, y el frontend del paso 5 en `app/proseg-app/`. Cada sección lo indica.

El entorno local es autónomo: Postgres, Keycloak, MinIO y Kafka corren en tu máquina. No depende de ningún servidor externo.

## Qué levanta

17 servicios: los 9 microservicios, más Postgres, Keycloak, MinIO y Kafka, más cuatro contenedores de inicialización que corren una vez y terminan (`keycloak-init`, `minio-init`, `dev-user-init` y `dev-user-approve`).

**El frontend no va en Docker en local.** Se corre con `npm run dev` para conservar el hot reload.

## 1. Requisitos previos

Docker Desktop corriendo y Node 22 o superior

### Si todavía no tenés Docker (Windows)

Descargalo de [docker.com](https://www.docker.com/products/docker-desktop/) y durante la instalación dejá marcada la opción **Use WSL 2**. Después, en PowerShell como administrador:

```powershell
wsl --install; wsl --update; wsl --set-default-version 2
```

Reiniciá el equipo y comprobá que quedó bien:

```bash
docker run hello-world
```

### Los archivos de entorno (.env)

Copiar los dos:

```bash
cp services/.env.example services/.env
```

```bash
cp app/proseg-app/.env.example app/proseg-app/.env
```

**Eso es todo. No hay que completar nada**: las plantillas traen valores por omisión que funcionan tal cual en local. Podés cambiarlos cuando quieras.

### Lo que queda vacío a propósito

Dos variables no se pueden rellenar porque vienen de servicios externos, y el entorno local funciona sin ellas:

- `SPRING_MAIL_USERNAME` y `SPRING_MAIL_PASSWORD` — sin esto no se envían correos
- `CF_TUNNEL_TOKEN` — solo lo usa el despliegue en el servidor

Las llaves de Turnstile sí vienen puestas, pero son las **llaves de prueba públicas de Cloudflare**: el captcha siempre aprueba, sin necesidad de una cuenta. En producción se reemplazan por las reales.

### Si algún puerto está ocupado

Los puertos que se publican al host son configurables desde `services/.env`. Por omisión:

```
POSTGRES_HOST_PORT=5433
KEYCLOAK_HOST_PORT=8180
MINIO_CONSOLE_HOST_PORT=9101
```

Esos valores evitan los choques mas comunes: el 5432 lo suele tomar un PostgreSQL instalado en la maquina, y el 9001 lo usa Docker Desktop.

## 2. Compilar y levantar

Todos los comandos de esta sección y de las siguientes se corren desde `services/`:

```bash
cd services
```

```bash
docker compose -f docker-compose.yml build
```

La primera vez tarda: son 9 servicios Java compilando sin caché.

```bash
docker compose -f docker-compose.yml up -d
```

## 3. Credenciales y accesos

| Qué | Dirección | Usuario | Contraseña |
|---|---|---|---|
| Aplicación | `http://localhost:5173` | `dev` | `dev12345` |
| Panel de Keycloak | `http://localhost:8180/auth` | `proseg-admin` | `proseg-local-dev` |
| Panel de MinIO | `http://localhost:9101` | `proseg` | `proseg-local-dev` |
| Postgres | `localhost:5433`, base `proseg-bd` | `proseg` | `proseg-local-dev` |
| API (gateway) | `http://localhost:8081` | — | — |

Esas contraseñas son deliberadamente obvias: sirven para levantar el entorno en tu máquina y **no deben usarse en producción**.

Para cambiarlas hay que editar `services/.env` y volver a levantar con `down -v`, porque Postgres, Keycloak y MinIO se crean con esos valores la primera vez y después los ignoran.

Una regla al inventar valores: evitá el signo `$`, que Docker Compose interpreta como variable. Y la clave de MinIO necesita al menos 8 caracteres.

## 4. El usuario de desarrollo

No hay que crear nada a mano: el entorno local se levanta con un usuario listo para entrar.

| | |
|---|---|
| Usuario | `dev` |
| Contraseña | `dev12345` |
| Rol | `SUPER_ADMINISTRADOR` |

Se puede cambiar en `services/.env` con `DEV_USER_USERNAME`, `DEV_USER_PASSWORD`, `DEV_USER_EMAIL` y `DEV_USER_ROLE`.

Si volvés a levantar el entorno, el usuario no se pisa: si ya existe, no se le toca la contraseña. Cambiarla en `.env` no la actualiza — para eso hay que borrar el usuario desde el panel de Keycloak, o hacer `down -v`.

## 5. Levantar el frontend

Esto va en una segunda terminal, porque el servidor de desarrollo queda ocupando la que uses. Desde la raíz del repositorio:

```bash
cd app/proseg-app
```

Instalar las dependencias. Solo hace falta la primera vez, y cada vez que alguien cambie `package.json`:

```bash
npm install
```

Levantar el servidor de desarrollo, con recarga automática:

```bash
npm run dev
```

Queda en `http://localhost:5173` y habla con el gateway en `http://localhost:8081`.

## 6. Verificación

Comprobá que los contenedores quedaron arriba:

```bash
docker compose -f docker-compose.yml ps
```

Postgres, Keycloak, Kafka y MinIO deben decir `healthy`. Los servicios Java tardan un par de minutos más.

Los cuatro contenedores de inicialización no aparecen acá, porque ya terminaron. Para verlos:

```bash
docker compose -f docker-compose.yml ps -a
```

Deben figurar como `Exited (0)`. Cualquier otro código de salida es un fallo.

Revisá que hicieron lo suyo:

```bash
docker compose -f docker-compose.yml logs keycloak-init dev-user-init dev-user-approve
```

Lo que tendrías que leer:

- `keycloak-init` — el resumen de roles y privilegios. Si el realm se importó bien, dice `0 privilegios, 0 roles, 0 asignaciones`
- `dev-user-init` — `usuario creado` y `rol SUPER_ADMINISTRADOR asignado`, o que ya existía
- `dev-user-approve` — `Usuario de desarrollo aprobado (estado: APPROVED)`

La prueba final: entrar a `http://localhost:5173` con `dev` / `dev12345`.

## 7. Después de cambiar código

Recompilá solo el servicio que tocaste:

```bash
docker compose -f docker-compose.yml up -d --build msvc-auth
```

Reemplazá `msvc-auth` por el servicio correspondiente.

El frontend no necesita nada: con `npm run dev` el cambio se ve al guardar.

## 8. Apagar

```bash
docker compose -f docker-compose.yml down
```

Conserva los datos. Para borrar todo y volver a foja cero:

```bash
docker compose -f docker-compose.yml down -v
```

Eso elimina tu usuario, la base y los archivos de MinIO. El siguiente arranque reimporta el realm desde cero.

## 9. Agregar o quitar un rol o un privilegio

Se declaran en `services/keycloak/roles-privilegios.conf`:

```
[privilegios]
LEER_ACTIVOS
EXPORTAR_REPORTES

[rol SUPER_ADMINISTRADOR]
LEER_ACTIVOS
EXPORTAR_REPORTES
```

En cada `up`, el contenedor `keycloak-init` ajusta Keycloak a lo que diga el archivo:

- **Un privilegio que agregás** se crea, y se asigna a los roles que lo listen
- **Un privilegio que quitás** se borra del realm, y con él desaparece de todos los roles que lo tuvieran
- **Un rol que agregás** se crea con los privilegios que le pongas debajo
- **Un rol existente** queda con exactamente los privilegios que diga el archivo: se agregan los que falten y se revocan los que sobren

Dos cosas que **nunca** borra: los roles, aunque no estén en el archivo, y los internos de Keycloak (`offline_access`, `uma_authorization`, `default-roles-proseg-realm`).

Para que un compañero reciba el cambio le alcanza con hacer `docker compose -f docker-compose.yml up -d`. No necesita borrar su volumen. Los borrados quedan listados en el log, con un `-` adelante.

> El borrado **solo ocurre en desarrollo**. Lo activa `SYNC_DELETE: "true"`, que únicamente pone el compose de desarrollo; sin esa variable el script solo agrega.

## Cosas que conviene saber

**La base arranca vacía.** Sin catálogos: tipos de activo, estados y unidades se cargan desde la aplicación.

**Los datos son tuyos.** Cada quien tiene su propia base, su propio Keycloak y su propio MinIO. Un usuario o un rol que crees a mano no lo ve nadie más. Los cambios que deban compartirse van en `roles-privilegios.conf`.

**Bloqueo por intentos fallidos.** A los 10 intentos fallidos Keycloak bloquea la cuenta temporalmente, empezando en 1 minuto.

**Los volúmenes sobreviven a los cambios del `.env`.** Es la causa de casi todos los arranques fallidos, y da errores que apuntan a otro lado:

| Síntoma | Qué pasó |
|---|---|
| Kafka sale con `Invalid cluster.id` | El volumen se formateó con otro `KAFKA_CLUSTER_ID` |
| Keycloak sale con `password authentication failed for user "proseg"` | El volumen de Postgres se creó con otra `POSTGRES_PASSWORD` |

Postgres y Kafka solo leen esas variables **la primera vez**, cuando inicializan su volumen. Si después cambian en el `.env`, el volumen sigue con los valores viejos y el arranque falla.

Se arregla borrando el volumen afectado. En desarrollo no se pierde nada que importe: la base la reconstruye Hibernate y Kafka solo guarda eventos y offsets.

```bash
docker compose -f docker-compose.yml down
docker volume rm proseg-dev_pgdata proseg-dev_kafkadata
```

Si preferís empezar de cero del todo, `down -v` borra todos los volúmenes del proyecto de una.

## Levantar el stack de producción en local

Para verificar cómo queda el sistema tal como corre en el servidor —con el frontend dockerizado y Caddy al frente— hay un compose aparte:

```bash
docker compose -f docker-compose.prod.yml -f docker-compose.local.yml up -d
```

Usa imágenes de GHCR en vez de compilar, y expone todo en `http://localhost:8080`. No es para el día a día.
