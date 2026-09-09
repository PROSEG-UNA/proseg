# Cómo manejar privilegios, dominios y roles en Keycloak

Todo se declara en un solo archivo: `services/keycloak/roles-privilegios.conf`.

Ese archivo es la fuente de verdad. Keycloak manda sobre la autorización de la aplicación, pero el archivo manda sobre Keycloak: en cada arranque el contenedor `keycloak-init` compara lo que dice el archivo contra el realm y ajusta lo que haga falta. Lo que crees a mano desde el panel de Keycloak no lo ve nadie más y, si contradice al archivo, se pisa en el siguiente arranque.

## El archivo

```
[dominio Inventario]
LEER_ACTIVOS
EXPORTAR_REPORTES

[rol SUPER_ADMINISTRADOR]
LEER_ACTIVOS
EXPORTAR_REPORTES

[servicio msvc-maintenance]
LEER_ACTIVOS
```

Reglas de formato:

- Los nombres de privilegio van en mayúsculas con guión bajo, uno por línea
- Las líneas en blanco se ignoran, y los espacios al principio y al final se recortan
- Una línea que empieza con `#` es un comentario
- El orden de las secciones no importa
- Una sección con el encabezado mal escrito corta el arranque con error, sin tocar el realm

## Los tres tipos de sección

### `[dominio ...]` — los privilegios

Cada privilegio del sistema vive dentro de un dominio, y solo uno. La unión de todos los `[dominio ...]` es la lista completa de privilegios que existen.

En Keycloak un privilegio es un rol de realm simple, y el dominio es un atributo de ese rol (`domain`). No hay que crear el dominio en ningún lado: existe apenas le pongas un privilegio debajo.

### `[rol ...]` — quién tiene qué

Un rol agrupa privilegios. En Keycloak es un rol compuesto, y los privilegios de abajo son sus composites.

Un rol queda con **exactamente** los privilegios que diga el archivo: se agregan los que falten y se revocan los que sobren. Revocar solo pasa en desarrollo; más abajo está el detalle.

### `[servicio ...]` — permisos entre microservicios

Son los privilegios que necesita un microservicio para llamar a otro por su cuenta, sin un usuario detrás. El nombre de la sección es el `clientId` del microservicio, y el script se los asigna a su cuenta de servicio (`service-account-<clientid>`).

Requiere que ese cliente tenga *service accounts* habilitado en Keycloak; si no, el arranque falla avisando que no encuentra la cuenta.

## Aplicar los cambios

Editás el archivo y levantás el entorno:

```bash
docker compose -f docker-compose.yml up -d
```

No hace falta build ni `down -v`. El contenedor `keycloak-init` monta el directorio directo del disco, así que lee siempre la versión actual del archivo.

Para que un compañero reciba el cambio le alcanza con hacer el mismo `up -d`.

## Verificar qué hizo

```bash
docker compose -f docker-compose.yml logs keycloak-init
```

Cada línea empieza con un símbolo:

| Símbolo | Qué pasó |
|---|---|
| `+` | Se creó un privilegio o un rol, o se asignó algo |
| `~` | Un privilegio cambió de dominio |
| `-` | Se borró un privilegio, o se revocó una asignación |
| `!` | Error; el contenedor termina con código distinto de 0 |

Al final queda el resumen: cuántos privilegios en cuántos dominios se revisaron, cuántos se agregaron, cuántos dominios se actualizaron y, en desarrollo, cuántos se borraron.

## Recetas

### Agregar un privilegio

1. Ponelo bajo el `[dominio ...]` que le corresponda
2. Agregalo a los `[rol ...]` que deban tenerlo, casi siempre `[rol SUPER_ADMINISTRADOR]`
3. `up -d`

Falta el paso del código: el privilegio recién sirve cuando algún microservicio lo exige. En los servicios Java se declara como constante y se aplica en el `SecurityConfig` del microservicio.

### Quitar un privilegio

Borralo de su dominio **y** de todos los roles y servicios que lo listen. En desarrollo desaparece del realm y, con él, de todos los roles que lo tuvieran.

Si lo sacás del dominio pero lo dejás listado en un rol, tarde o temprano el script intenta asignar un privilegio que ya no existe y el arranque termina con error.

### Mover un privilegio de dominio

Cortá la línea y pegala bajo el otro dominio. En el siguiente arranque el privilegio queda en el dominio nuevo, aunque alguien lo haya cambiado a mano en Keycloak.

No hace falta tocar los roles: mover un privilegio de dominio no cambia quién lo tiene.

### Agregar un dominio

Escribí la sección con los privilegios que la componen:

```
[dominio Transporte]
LEER_CHOFERES
GESTIONAR_CHOFERES
```

Con eso ya funciona y ya aparece en la aplicación, con un estilo neutro. Para darle ícono y color propios, agregalo a `DOMAIN_META` en `app/proseg-app/src/features/security/components/RoleFormModal.jsx`:

```js
Transporte: {
    lightColor: '#0891b2',
    darkColor:  '#22d3ee',
    lightBg:    '#ecfeff',
    darkBg:     'rgba(22,78,99,0.35)',
    icon: LocalShippingIcon,
    description: 'Choferes, vehículos, giras y asignaciones',
},
```

El orden de los paneles en pantalla es el orden de `DOMAIN_META`. Un dominio que no esté ahí se dibuja igual, al final, en gris.

### Quitar un dominio

Repartí sus privilegios entre los dominios que queden y borrá la sección. Un dominio sin privilegios no existe.

### Agregar un rol

```
[rol COORDINADOR_TRANSPORTE]
LEER_CHOFERES
LEER_VEHICULOS
```

Los privilegios que le pongas debajo tienen que existir en algún dominio.

## Qué no se toca nunca

- **Los roles compuestos**, aunque no estén en el archivo. El borrado alcanza solo a los privilegios
- **Los roles internos de Keycloak**: `offline_access`, `uma_authorization` y `default-roles-proseg-realm`
- **Los usuarios y sus asignaciones**. Que un rol pierda un privilegio no le saca el rol a nadie

Un detalle que sorprende: un rol que creaste a mano en el panel y al que **no** le asignaste ningún privilegio no es compuesto para Keycloak, así que en desarrollo se lo borra como si fuera un privilegio suelto. Los roles del archivo nunca caen en eso, porque siempre tienen privilegios debajo.

## El borrado solo ocurre en desarrollo

Lo activa `SYNC_DELETE: "true"`, que únicamente pone el compose de desarrollo. Sin esa variable el script solo agrega y actualiza: nunca borra un privilegio ni revoca una asignación.

Por eso en el servidor sacar un privilegio del archivo no lo saca del realm. Hay que borrarlo a mano desde el panel de Keycloak.

## Cómo llega el dominio a la pantalla

1. `sync-roles.sh` escribe el atributo `domain` sobre el rol en Keycloak
2. `msvc-auth` lo lee de Keycloak y lo devuelve en cada privilegio
3. El modal de roles de la aplicación arma un panel por dominio con lo que venga de la API

Si un privilegio se queda sin dominio, el modal lo muestra en un panel aparte llamado **Sin dominio**. Es la señal de que le falta su sección en el archivo.

## Problemas comunes

| Síntoma | Qué pasó |
|---|---|
| `Sección desconocida: [...]` | El encabezado está mal escrito. El realm no se tocó; corregilo y volvé a levantar |
| `error asignando X a 'ROL'` | El rol lista un privilegio que no está declarado en ningún dominio |
| `no existe la cuenta de servicio` | El cliente del `[servicio ...]` no tiene *service accounts* habilitado |
| Un privilegio aparece en **Sin dominio** | Existe en Keycloak pero no está en el archivo, o el arranque falló antes de aplicarle el dominio |
| El cambio no se ve en la aplicación | La lista de privilegios queda cacheada en el navegador; recargá la página |

En un entorno recién creado con `down -v`, el primer arranque importa el realm desde `realm-export.json` y después el script le aplica los dominios encima. Es normal ver varias líneas `~` en ese primer log.
