# Auditoría de permisos Keycloak vs backend

Realm: `proseg-realm`. Los "permisos" son roles del realm (nombre en `MAYUSCULAS_SNAKE`, en español) que llegan al backend como `GrantedAuthority` mediante `KeycloakJwtConverter`.

## 1. Fuentes y método

| Fuente | Uso |
|---|---|
| Admin API de Keycloak (73 roles; export temporal, no versionado en el repo) | Estado real del realm: nombres, descripciones, composite |
| `services/keycloak/realm-export.json` | Definición inicial del realm (68 roles) |
| `services/keycloak/roles-privilegios.conf` | Fuente de `sync-roles.sh`: dominios, roles compuestos y cuentas de servicio |
| `SecurityConfig` + `Privileges.java` de cada microservicio | Reglas `hasAuthority` / `hasAnyAuthority` |
| Controllers (`@*Mapping`) | Endpoints reales, para cruzar contra las reglas |
| Servicios (`TicketServiceImpl`, `MaintenanceRequestServiceImpl`, `MaintenanceRegisterController`, `UserService`) | Chequeos de permiso fuera de `SecurityConfig` |

Hallazgos de método:

- No hay `@PreAuthorize`, `@Secured` ni `@RolesAllowed` en el repo. Toda la protección está en `SecurityConfig` (Spring evalúa la primera regla que coincide; si ninguna, aplica `anyRequest().authenticated()`).
- `msvc-gateway` hace `permitAll` a todas las rutas de negocio y delega la autorización a cada microservicio.
- Cada microservicio duplica su propio `Privileges.java` y `KeycloakJwtConverter`.

## 2. Inventario de Keycloak

| Concepto | Cantidad |
|---|---|
| Roles totales en el realm vivo | 73 |
| Roles internos de Keycloak (`default-roles-proseg-realm`, `offline_access`, `uma_authorization`) | 3 |
| Roles compuestos de negocio (`SUPER_ADMINISTRADOR`, `ADMINISTRADOR_ACTIVOS`, `TECNICO_EMPRESA`) | 3 |
| Privilegios (roles simples) | 67 |
| Privilegios sin descripción | 31 |
| Roles compuestos sin descripción | 2 (`ADMINISTRADOR_ACTIVOS`, `TECNICO_EMPRESA`) |

Los 67 privilegios del realm vivo coinciden con `realm-export.json` y `roles-privilegios.conf`. `ADMINISTRADOR_ACTIVOS` y `TECNICO_EMPRESA` existen en el realm vivo y en el `.conf`, pero no en `realm-export.json` (el export está desactualizado).

## 3. Hallazgos críticos (endpoints sin permiso donde debería haberlo)

Prioridad alta. Cada caso cae en `anyRequest().authenticated()`: cualquier usuario autenticado puede invocarlo.

**Prioridad inmediata: hallazgo 1.** Asignar un rol es la operación que otorga el resto de los permisos; mientras la regla no coincida con el `PUT`, un usuario autenticado podría asignarse o asignar a otros roles como `SUPER_ADMINISTRADOR`. Debe tratarse y comunicarse por separado, sin esperar al cierre del resto de hallazgos.

Nota: el informe de cierre agrupa los hallazgos 4, 5, 6 y 7 (modelos, tipos, componentes y pisos de inventario) en un solo punto.

| # | Microservicio | Método | Endpoint | Problema | Permiso esperado |
|---|---|---|---|---|---|
| 1 | `msvc-auth` | PUT | `/api/user/{userId}/roles/{roleId}` | La regla de `SecurityConfig` protege `POST /api/user/*/roles/*`, pero el controller y el frontend (`assignSingleRoleToUser`) usan `PUT`. Asignar roles solo exige autenticación. El `POST` del frontend (`assignRoleToUser`) no tiene handler. | `ASIGNAR_ROL_USUARIO` |
| 2 | `msvc-auth` | POST | `/api/user/{userId}/resend-invitation` | Sin regla. | `CREAR_USUARIO` (a definir) |
| 3 | `msvc-maintenance` | POST | `/api/v1/maintenance/companies/{id}/users/batch` | Las reglas POST cubren `/companies/*/users` pero no `/users/batch`. | `GESTIONAR_EMPRESAS` |
| 4 | `msvc-inventory` | GET/POST/PUT/DELETE | `/api/v1/inventory/models/**` | `SecurityConfig` protege `/asset-models`, pero el controller es `/models`. | `LEER/GESTIONAR/ELIMINAR_ACTIVOS` |
| 5 | `msvc-inventory` | GET/POST/PUT/DELETE | `/api/v1/inventory/types/**` | `SecurityConfig` protege `/asset-types`, pero el controller es `/types`. | `LEER/GESTIONAR/ELIMINAR_ACTIVOS` |
| 6 | `msvc-inventory` | POST/PUT/DELETE | `/api/v1/inventory/assets/{assetId}/components`, `/api/v1/inventory/components/{id}` | Sin regla para componentes (`AssetComponentController`). El GET sí queda cubierto por `assets/**`. | `GESTIONAR_ACTIVOS` / `ELIMINAR_ACTIVOS` |
| 7 | `msvc-inventory` | GET | `/api/v1/inventory/floors/**` | Sin regla. | `LEER_UBICACIONES` |
| 8 | `msvc-transport` | POST | `/api/v1/transport/assignments/integrity/orphans/cleanup` | Operación de limpieza sin regla específica. | `ACTUALIZAR_ASIGNACIONES` |

Prioridad media (lectura sin permiso):

| Microservicio | Método | Endpoint | Nota |
|---|---|---|---|
| `msvc-maintenance` | GET | `/api/v1/maintenance/locations/**` (13 endpoints de `MaintenanceLocationController`: campuses, buildings, floors, locations, assets, emails) | Sin regla. En inventory, los datos equivalentes exigen `LEER_UBICACIONES` / `LEER_ACTIVOS`. |
| `msvc-auth` | GET | `/api/user/statuses` | Catálogo de estados. Bajo riesgo. |
| `msvc-document-processor` | GET/POST | `/imports/{documentType}/**`, `/exports/{documentType}` | Solo `assets`, `tickets` y `maintenance-requests` tienen regla. Cualquier otro `documentType` solo exige autenticación. Verificar qué tipos existen. |
| `msvc-email` | GET | `/email/test` | Sin `SecurityConfig` propio (solo `oauth2-client`), por lo que aparentemente no exige JWT de Keycloak. No comprobado en ejecución: verificar comportamiento real. |

Deliberadamente solo autenticados (aceptable): `GET /api/auth`, `GET /api/auth/me`, `POST /api/auth/change-password`, `PATCH /api/user/me/profile-image`, `GET /api/v1/maintenance/user-companies/{id}/has-company` (lo usa `msvc-auth`), `/api/v1/maintenance/ws/**`.

Públicos (`permitAll`): `/api/auth/login|register|refresh|logout|forgot-password|reset-password`, `POST /api/user/set-password`, `GET /api/user/invitation-info`, Swagger y `/v3/api-docs`, `GET /api/v1/transport/status`.

## 4. Matriz permiso vs endpoint

Las rutas son las que expone el gateway. Estado: **En uso** (regla y endpoint reales), **En uso (lógica)** (se evalúa dentro del código del servicio), **Regla sin endpoint** (referenciado en `SecurityConfig` pero sin controller en el repo), **Inconsistente** (la regla existe pero no coincide con el método o la ruta del controller, por lo que no protege), **Sin uso**. Cada tabla incluye la columna Microservicio.

### 4.1 `msvc-auth`

| Permiso Keycloak | Microservicio | Descripción | Método | Endpoint | Acción habilitada | Estado |
|---|---|---|---|---|---|---|
| `LEER_ROLES_BASE` | `msvc-auth` | Consultar los roles base definidos en el sistema. | GET | `/api/role/base` | Listar roles base | En uso |
| `LEER_ROLES_COMPUESTOS` | `msvc-auth` | Consultar roles compuestos. | GET | `/api/role/composite` | Listar roles compuestos | En uso |
| `LEER_COMPOSITES_ROL` | `msvc-auth` | Consultar los permisos asociados a un rol. | GET | `/api/role/{roleName}/composites` | Ver privilegios de un rol | En uso |
| `CREAR_ROL` | `msvc-auth` | Crear nuevos roles. | POST | `/api/role` | Crear rol | En uso |
| `EDITAR_ROL` | `msvc-auth` | Modificar la configuración o permisos de un rol. | PUT | `/api/role/{roleName}` | Editar rol | En uso |
| `ELIMINAR_ROL` | `msvc-auth` | Eliminar roles del sistema. | DELETE | `/api/role/{roleName}` | Eliminar rol | En uso |
| `LEER_USUARIOS_POR_ROL` | `msvc-auth` | Consultar usuarios asociados a un rol. | GET | `/api/role/{roleName}/users` | Listar usuarios de un rol | En uso |
| `LEER_USUARIOS` | `msvc-auth` | Consultar todos los usuarios del sistema. | GET | `/api/user` | Listar usuarios (paginado). También lo acepta `LEER_USUARIO` | En uso |
| `LEER_USUARIO` | `msvc-auth` | Consultar la información de un usuario. | GET | `/api/user` (alternativo), `/api/user/keycloak/{id}` | Ver usuario | En uso |
| `LEER_USUARIO` | `msvc-auth` | idem | POST | `/api/user/keycloak/batch` | Obtener usuarios por lista de ids | En uso |
| `CREAR_USUARIO` | `msvc-auth` | Crear nuevos usuarios. | POST | `/api/user` | Crear usuario administrado | En uso |
| `LEER_ROLES_USUARIO` | `msvc-auth` | Consultar los roles de un usuario. | GET | `/api/user/{userId}/roles` | Ver roles de usuario | En uso |
| `APROBAR_USUARIO` | `msvc-auth` | Aprobar o cambiar el estado de un usuario pendiente. | PATCH | `/api/user/approval/{id}` | Aprobar/rechazar usuario | En uso |
| `ASIGNAR_ROL_USUARIO` | `msvc-auth` | Asignar un rol a un usuario. | POST (regla) / PUT (controller) | `/api/user/{userId}/roles/{roleId}` | Asignar rol | **Inconsistente** (ver hallazgo 1) |
| `ELIMINAR_ROL_USUARIO` | `msvc-auth` | Eliminar un rol asignado a un usuario. | DELETE | `/api/user/{userId}/roles/{roleId}` | Quitar rol | En uso |
| `LEER_INVITACIONES_PENDIENTES` | `msvc-auth` | Consulta los usuarios invitados. | GET | `/api/invitations/**` | Ver invitaciones pendientes | Regla sin endpoint |
| `SOLICITAR_MANTENIMIENTO` / `SELECCIONAR_EMPRESA_EN_SOLICITUD_MANTENIMIENTO` | `msvc-auth` | ver 4.2 | n/a | n/a | `UserService` valida que quien reciba `SOLICITAR_MANTENIMIENTO` tenga empresa asociada, salvo que también tenga `SELECCIONAR_EMPRESA...` | En uso (lógica) |

### 4.2 `msvc-maintenance`

Base: `/api/v1/maintenance`.

| Permiso Keycloak | Microservicio | Descripción | Método | Endpoint | Acción habilitada | Estado |
|---|---|---|---|---|---|---|
| `LEER_EMPRESAS` | `msvc-maintenance` | Consulta de empresas registradas. | GET | `/companies`, `/companies/{id}`, `/companies/{id}/users`, `/companies/{id}/technicians` | Consultar empresas, sus usuarios y técnicos | En uso |
| `GESTIONAR_EMPRESAS` | `msvc-maintenance` | Creación y actualización de empresas. | POST | `/companies`, `/companies/users`, `/companies/{id}/users` | Crear empresa, vincular usuarios | En uso |
| `GESTIONAR_EMPRESAS` | `msvc-maintenance` | idem | PUT | `/companies/{id}` | Actualizar empresa | En uso |
| `ELIMINAR_EMPRESAS` | `msvc-maintenance` | Eliminación de empresas. | DELETE | `/companies/{id}`, `/companies/{id}/users/{userId}` | Eliminar empresa; desvincular usuario | En uso |
| `SOLICITAR_MANTENIMIENTO` o `EDITAR_SOLICITUDES_MANTENIMIENTO` | `msvc-maintenance` | Crear / modificar solicitudes de mantenimiento. | GET | `/companies/me` | Ver la empresa del usuario | En uso |
| `LEER_SOLICITUDES_MANTENIMIENTO` | `msvc-maintenance` | Consulta de solicitudes. | GET | `/requests`, `/requests/{id}`, `/requests/assets`, `/requests/company/{companyId}` | Consultar solicitudes | En uso |
| `SOLICITAR_MANTENIMIENTO` | `msvc-maintenance` | Permite crear solicitudes de mantenimiento. | POST | `/requests` | Crear solicitud | En uso |
| `EDITAR_SOLICITUDES_MANTENIMIENTO` | `msvc-maintenance` | Actualizar solicitudes existentes. | PUT | `/requests/{id}` | Editar solicitud | En uso |
| `CANCELAR_SOLICITUDES_MANTENIMIENTO` | `msvc-maintenance` | Permite cancelar solicitudes. | PATCH | `/requests/{id}/cancel` | Cancelar solicitud | En uso |
| `ELIMINAR_SOLICITUDES_MANTENIMIENTO` | `msvc-maintenance` | Eliminación de solicitudes. | DELETE | `/requests/{id}` | Eliminar solicitud | En uso |
| `SELECCIONAR_EMPRESA_EN_SOLICITUD_MANTENIMIENTO` | `msvc-maintenance` | Elegir cualquier empresa al solicitar. | n/a | n/a | `MaintenanceRequestServiceImpl` permite elegir cualquier empresa al crear | En uso (lógica) |
| `LEER_HISTORIAL_REGISTROS_MANTENIMIENTO` | `msvc-maintenance` | Historial de registros de la empresa. | GET | `/records` | Consultar historial. En `/registers/history` el controller lo usa para ver todo el historial de la empresa | En uso |
| `LEER_REGISTROS_MANTENIMIENTO` | `msvc-maintenance` | Consulta de registros. | GET | `/registers`, `/registers/assigned`, `/registers/history`, `/registers/by-request/{id}`, `/registers/{id}/assets`, `/registers/{id}/records` | Consultar registros | En uso |
| `GESTIONAR_REGISTROS_MANTENIMIENTO` | `msvc-maintenance` | Modificar registros, bitácoras y finalizarlos. | PUT, POST | `/registers/{id}`, `/registers/{id}/finalize`, `/registers/{id}/records` | Editar, finalizar, registrar bitácora | En uso |
| `LEER_TECNICOS_MANTENIMIENTO` | `msvc-maintenance` | Consulta de técnicos. | GET | `/technicians/**` | n/a | Regla sin endpoint |
| `GESTIONAR_TECNICOS_MANTENIMIENTO` | `msvc-maintenance` | Creación y actualización de técnicos. | POST, PUT | `/technicians/**` | n/a | Regla sin endpoint |
| `ELIMINAR_TECNICOS_MANTENIMIENTO` | `msvc-maintenance` | Eliminación de técnicos. | DELETE | `/technicians/**` | n/a | Regla sin endpoint |
| `LEER_USUARIOS_EMPRESAS` | `msvc-maintenance` | Consulta de asignaciones usuario-empresa. | GET | `/user-companies`, `/user-companies/**` | n/a | Regla sin endpoint |
| `GESTIONAR_USUARIOS_EMPRESAS` | `msvc-maintenance` | Creación y actualización de asignaciones. | POST, PUT | `/user-companies/**` | n/a | Regla sin endpoint |
| `ELIMINAR_USUARIOS_EMPRESAS` | `msvc-maintenance` | Eliminación de asignaciones. | DELETE | `/user-companies/**` | n/a | Regla sin endpoint |
| `LEER_TICKET_MANTENIMIENTO` | `msvc-maintenance` | Consultar tickets, historial, fotos y dashboard. | GET | `/tickets`, `/tickets/{id}`, `/tickets/{id}/history`, `/tickets/dashboard`, `/tickets/{id}/photos` | Consultar tickets | En uso |
| `CREAR_TICKETS_MANTENIMIENTO` | `msvc-maintenance` | Crear tickets. | POST | `/tickets` | Crear ticket | En uso |
| `EDITAR_TICKETS_MANTENIMIENTO` | `msvc-maintenance` | Modificar tickets y su estado. | PUT, PATCH | `/tickets/{id}`, `/tickets/{id}/status`, `/tickets/{id}/priority` (también `ASIGNAR_PRIORIDAD...`) | Editar ticket, cambiar estado | En uso |
| `ASIGNAR_PRIORIDAD_TICKETS_MANTENIMIENTO` | `msvc-maintenance` | Asignar o cambiar prioridad. | PATCH | `/tickets/{id}/priority` | Cambiar prioridad. También se valida en servicio al crear/editar | En uso |
| `ASIGNAR_TICKETS_MANTENIMIENTO` | `msvc-maintenance` | Asignar ticket a un responsable. | GET, PATCH | `/tickets/assignees`, `/tickets/{id}/assigned-to` | Listar asignables; asignar ticket | En uso |
| `COMENTAR_TICKETS_MANTENIMIENTO` | `msvc-maintenance` | Agregar, editar y eliminar comentarios. | POST, PUT, DELETE | `/tickets/{id}/comments`, `/tickets/{id}/comments/{commentId}` | Gestionar comentarios | En uso |
| `LEER_TODOS_TICKETS_MANTENIMIENTO` | `msvc-maintenance` | Ver todos los tickets, no solo los propios. | n/a | n/a | `TicketServiceImpl`: sin este permiso solo se ven los tickets creados por el usuario | En uso (lógica) |
| `ELIMINAR_TICKETS_MANTENIMIENTO` | `msvc-maintenance` | Permite eliminar tickets de mantenimiento. | n/a | n/a | No hay `DELETE /tickets` ni regla; hoy no habilita ninguna acción | Sin uso |
| `LEER_TICKETS_MANTENIMIENTO` | `msvc-maintenance` | Permite consultar tickets de mantenimiento. | n/a | n/a | No abre ningún endpoint: duplicado en plural de `LEER_TICKET_MANTENIMIENTO`, el backend solo valida el singular | Sin uso |
| `ACEPTAR_SOLICITUDES_MANTENIMIENTO` | `msvc-maintenance` | Permite aceptar solicitudes de mantenimiento. | n/a | n/a | Solo aparece en `roles-privilegios.conf`; hoy no habilita ninguna acción | Sin uso |

### 4.3 `msvc-inventory`

Base: `/api/v1/inventory`.

| Permiso Keycloak | Microservicio | Descripción | Método | Endpoint | Acción habilitada | Estado |
|---|---|---|---|---|---|---|
| `LEER_ACTIVOS` | `msvc-inventory` | Consultar activos del inventario. | GET | `/assets/**`, `/asset-archives/**`, `/network-interfaces/**`, `/brands/**`, `/executing-units/**`, `/employees/**` | Consultar activos y catálogos asociados | En uso |
| `GESTIONAR_ACTIVOS` | `msvc-inventory` | Crear y editar activos. | POST, PUT | `/assets`, `/assets/{id}`, `/assets/alarm-sensors/**`, `/asset-archives/**`, `/network-interfaces/**`, `/brands/**`, `/executing-units/**`, `/employees/**` | Crear y editar activos y catálogos | En uso |
| `ELIMINAR_ACTIVOS` | `msvc-inventory` | Eliminar activos. | DELETE | `/assets/{id}`, `/asset-archives/**`, `/network-interfaces/**`, `/brands/**`, `/executing-units/**`, `/employees/**` | Eliminar activos y catálogos | En uso |
| `IMPORTAR_ACTIVOS` | `msvc-inventory` | Importar activos desde archivo. | POST, GET | `/assets/import/preview`, `/assets/import/confirm`, `/assets/schema` | Importar activos | En uso |
| `LEER_UBICACIONES` | `msvc-inventory` | Consultar sedes y ubicaciones. | GET | `/campuses/**`, `/buildings/**`, `/locations/**`, `/buildings/{id}/emails`, `/campuses/{id}/emails` | Consultar ubicaciones y correos | En uso |
| `GESTIONAR_UBICACIONES` | `msvc-inventory` | Crear y editar ubicaciones. | POST, PUT | `/campuses`, `/buildings`, `/locations`, `/buildings/{id}/emails` | Crear y editar ubicaciones y correos | En uso |
| `ELIMINAR_UBICACIONES` | `msvc-inventory` | Eliminar sedes y ubicaciones. | DELETE | `/campuses/{id}`, `/buildings/{id}`, `/locations/{id}`, `/emails/{emailId}` | Eliminar ubicaciones y correos | En uso |

Endpoints sin cobertura: ver hallazgos 4 a 7 de la sección 3. Las reglas `/asset-models` y `/asset-types` no corresponden a ningún controller.

### 4.4 `msvc-transport`

Base: `/api/v1/transport`.

| Permiso Keycloak | Microservicio | Descripción | Método | Endpoint | Acción habilitada | Estado |
|---|---|---|---|---|---|---|
| `LEER_CHOFERES` / `GESTIONAR_CHOFERES` / `ELIMINAR_CHOFERES` | `msvc-transport` | Consultar / registrar y modificar / eliminar choferes. | GET / POST, PUT / DELETE | `/drivers`, `/drivers/{id}` | CRUD de choferes | En uso |
| `LEER_VEHICULOS` / `GESTIONAR_VEHICULOS` / `ELIMINAR_VEHICULOS` | `msvc-transport` | Consultar / registrar y modificar / eliminar vehículos. | GET / POST, PUT / DELETE | `/vehicles`, `/vehicles/{id}` | CRUD de vehículos | En uso |
| `LEER_MANTENIMIENTO_VEHICULOS` / `GESTIONAR_MANTENIMIENTO_VEHICULOS` / `ELIMINAR_MANTENIMIENTO_VEHICULOS` | `msvc-transport` | Consultar / registrar y modificar / eliminar mantenimientos de vehículos. | GET / POST, PUT / DELETE | `/maintenance`, `/maintenance/{id}` | CRUD de mantenimientos de vehículos | En uso |
| `LEER_GIRAS` / `GESTIONAR_GIRAS` / `ELIMINAR_GIRAS` | `msvc-transport` | Consultar / registrar y modificar / eliminar giras. | GET / POST, PUT / DELETE | `/tours`, `/tours/{id}` | CRUD de giras | En uso |
| `GENERAR_ASIGNACIONES` | `msvc-transport` | Generar asignaciones. | POST | `/assignments/generate` | Generar asignaciones | En uso |
| `GENERAR_ASIGNACIONES` o `ACTUALIZAR_ASIGNACIONES` | `msvc-transport` | idem | GET | `/assignments`, `/assignments/{id}`, `/assignments/integrity/orphans` | Consultar asignaciones | En uso |
| `GENERAR_ASIGNACIONES` o `ACTUALIZAR_ASIGNACIONES` | `msvc-transport` | idem | GET | `/reports/overtime`, `/reports/overtime/export`, `/reports/final-commission`, `/reports/final-commission/export` | Reportes | En uso |
| `GENERAR_ASIGNACIONES` o `ACTUALIZAR_ASIGNACIONES` | `msvc-transport` | idem | POST | `/cleaning/preview`, `/cleaning/finalize`, `/cleaning/register` | Procesos de limpieza | En uso |
| `GENERAR_ASIGNACIONES`, `ACTUALIZAR_ASIGNACIONES`, `LEER_GIRAS`, `GESTIONAR_GIRAS` o `ELIMINAR_GIRAS` | `msvc-transport` | idem | GET | `/cleaning/history`, `/cleaning/history/{id}` | Historial de limpieza | En uso |
| `ACTUALIZAR_ASIGNACIONES` | `msvc-transport` | Crear, modificar y eliminar asignaciones. | POST, PUT, DELETE | `/assignments`, `/assignments/{id}` | Gestionar asignaciones | En uso |

### 4.5 `msvc-archive`

| Permiso Keycloak | Microservicio | Descripción | Método | Endpoint | Acción habilitada | Estado |
|---|---|---|---|---|---|---|
| `SUBIR_ARCHIVOS` | `msvc-archive` | Subir archivos al sistema. | POST | `/api/v1/archive/files/initiate`, `/part`, `/complete` | Subida por partes | En uso |
| `LEER_ARCHIVOS` | `msvc-archive` | Descargar archivos del sistema. | GET | `/api/v1/archive/files/presigned`, `/api/v1/archive/files/**` | Descargar archivos | En uso |

### 4.6 `msvc-document-processor`

Base: `/api/v1/document-processor`.

| Permiso Keycloak | Microservicio | Descripción | Método | Endpoint | Acción habilitada | Estado |
|---|---|---|---|---|---|---|
| `IMPORTAR_ACTIVOS` | `msvc-document-processor` | Importar activos desde archivo. | POST, GET | `/imports/assets/{preview,confirm,template}` | Importación de activos | En uso |
| `EXPORTAR_ACTIVOS` o `IMPORTAR_ACTIVOS` | `msvc-document-processor` | Exportar activos a documento. | GET | `/exports/assets` | Exportar activos | En uso |
| `LEER_TICKET_MANTENIMIENTO` | `msvc-document-processor` | Consultar tickets. | GET | `/exports/tickets` | Exportar tickets | En uso |
| `LEER_SOLICITUDES_MANTENIMIENTO` | `msvc-document-processor` | Consultar solicitudes. | GET | `/exports/maintenance-requests` | Exportar solicitudes | En uso |

### 4.7 Cuentas de servicio (`roles-privilegios.conf`)

| Cliente | Privilegios | Uso |
|---|---|---|
| `msvc-maintenance` | `LEER_ACTIVOS`, `LEER_UBICACIONES`, `LEER_USUARIO`, `LEER_USUARIOS`, `CREAR_USUARIO` | Llamadas Feign hacia inventory y auth |
| `msvc-email` | `LEER_USUARIO`, `LEER_USUARIOS_POR_ROL`, `LEER_EMPRESAS` | Llamadas Feign hacia auth y maintenance |

Estas llamadas no se pueden quitar de los permisos de usuario sin romper esas integraciones.

## 5. Permisos sin uso o sin endpoint

| Permiso | Situación | Acción sugerida (no ejecutar sin validar impacto) |
|---|---|---|
| `ACEPTAR_SOLICITUDES_MANTENIMIENTO` | No existe en ningún `Privileges.java` ni regla. Solo está en `roles-privilegios.conf` y en 3 roles compuestos. | Confirmar con negocio y evaluar eliminación |
| `LEER_TICKETS_MANTENIMIENTO` | Duplicado del singular `LEER_TICKET_MANTENIMIENTO`. Está en `roles-privilegios.conf` y en `seed-role-domains.ps1`. | Consolidar en uno solo |
| `ELIMINAR_TICKETS_MANTENIMIENTO` | No hay endpoint de borrado de tickets. | Mantener si se prevé la función; si no, evaluar eliminación |
| `LEER/GESTIONAR/ELIMINAR_TECNICOS_MANTENIMIENTO` | Reglas en `SecurityConfig` pero no existe `TechnicianController`. El frontend (`techniciansService.js`) y las colecciones Bruno aún llaman a `/maintenance/technicians`. | Verificar si el controller se eliminó o si el frontend es código muerto |
| `LEER/GESTIONAR/ELIMINAR_USUARIOS_EMPRESAS` | `UserCompanyController` solo expone `has-company`. La vinculación usuario-empresa real usa `GESTIONAR_EMPRESAS` / `ELIMINAR_EMPRESAS`. | Decidir cuál es el modelo (permisos propios o los de empresa) |
| `LEER_INVITACIONES_PENDIENTES` | Regla `GET /api/invitations/**` sin controller ni llamadas del frontend. | Verificar si se eliminó el endpoint |

## 6. Inconsistencias y permisos demasiado generales

Nomenclatura y definiciones:

- Singular vs plural: `LEER_TICKET_MANTENIMIENTO` (usado) vs `LEER_TICKETS_MANTENIMIENTO` (sin uso). `LEER_USUARIO` y `LEER_USUARIOS` se aceptan indistintamente en `GET /api/user`.
- `SOLICITAR_MANTENIMIENTO` no sigue el patrón `<VERBO>_SOLICITUDES_MANTENIMIENTO` del resto del dominio.
- `*_MANTENIMIENTO_VEHICULOS` (mantenimiento de vehículos, transporte) se confunde con el dominio de mantenimiento.
- `LEER_COMPOSITES_ROL` mezcla español e inglés y se parece a `LEER_ROLES_COMPUESTOS`; son endpoints distintos.
- `TicketServiceImpl` acepta alias que no existen en Keycloak: `admin`, `administrador`, `MAINTENANCE_TICKETS_VIEW_ALL`, `maintenance.tickets.view_all`, `maintenance:tickets:view_all` y las variantes de `set_priority` y `assign`.
- Dominios: `roles-privilegios.conf` tiene un dominio `Mantenimiento` (`ACEPTAR_...`, `CANCELAR_...`), pero `seed-role-domains.ps1` pone `CANCELAR_...` en "Solicitud de mantenimiento". El script no incluye los 14 privilegios de Transporte, `EXPORTAR_ACTIVOS` ni `ACEPTAR_SOLICITUDES_MANTENIMIENTO`.
- `realm-export.json` no incluye `ADMINISTRADOR_ACTIVOS` ni `TECNICO_EMPRESA`.
- `ELIMINAR_SOLICITUDES_MANTENIMIENTO` tiene un salto de línea al final de la descripción.

Permisos demasiado generales (revisar):

| Permiso | Qué cubre |
|---|---|
| `GESTIONAR_ACTIVOS` / `LEER_ACTIVOS` / `ELIMINAR_ACTIVOS` | Activos más marcas, unidades ejecutoras, empleados, interfaces de red, archivos de activos y sensores de alarma |
| `GESTIONAR_EMPRESAS` | CRUD de empresas y vinculación de usuarios a empresa (existe `GESTIONAR_USUARIOS_EMPRESAS`, sin uso) |
| `ELIMINAR_EMPRESAS` | Eliminar empresa y también desvincular un usuario (existe `ELIMINAR_USUARIOS_EMPRESAS`, sin uso) |
| `LEER_EMPRESAS` | Empresas, usuarios de la empresa y técnicos de la empresa |
| `ACTUALIZAR_ASIGNACIONES` / `GENERAR_ASIGNACIONES` | Crear/editar/borrar asignaciones, reportes, limpieza e historial. `GENERAR` se usa también como permiso de lectura |
| `EDITAR_TICKETS_MANTENIMIENTO` | Editar ticket, cambiar estado y (alternativo) prioridad |
| `GESTIONAR_REGISTROS_MANTENIMIENTO` | Editar, finalizar y registrar bitácoras (POST y PUT en `registers/**`) |
| `SUPER_ADMINISTRADOR` | Compuesto con todos los privilegios |

## 7. Descripciones a agregar en Keycloak

Las 33 descripciones propuestas (31 privilegios y 2 roles compuestos) están en `scripts/keycloak/role-descriptions.json`. Se aplican con `scripts/keycloak/apply-role-descriptions.ps1`:

- Sin `-Apply` solo muestra el plan (dry-run).
- Con `-Apply` solo escribe en roles cuya descripción está vacía. No sobrescribe descripciones existentes ni toca composites, atributos o asignaciones.

```powershell
# Dry-run
.\scripts\keycloak\apply-role-descriptions.ps1
# Aplicar
.\scripts\keycloak\apply-role-descriptions.ps1 -Apply
```

Estado: Se aplicaron el 1 de octubre de 2026 en el realm proseg-realm, para que el documento coincida con lo que quedó en Keycloak.

## 8. Pendientes

- Validar y corregir los hallazgos críticos de la sección 3 (cambian accesos, requieren validar impacto en frontend e integraciones).
- Confirmar con negocio los permisos sin uso de la sección 5 antes de eliminarlos o consolidarlos.
- Ajustar en Keycloak la descripción de `ACEPTAR_SOLICITUDES_MANTENIMIENTO`, `LEER_TICKETS_MANTENIMIENTO` y `ELIMINAR_TICKETS_MANTENIMIENTO`: hoy describen una acción que ningún endpoint habilita (por ejemplo, agregar "sin uso actual en el backend").
- Actualizar `realm-export.json` para que incluya los roles compuestos reales.
- Verificar en runtime el comportamiento de `msvc-email` y los `documentType` del `document-processor`.