# SSSI - Sistema de Sección de Seguridad Institucional

Monorepo del Sistema de Sección de Seguridad Institucional (SSSI), desarrollado con una arquitectura de microservicios utilizando Spring Boot y React.

---

## Arquitectura del Proyecto

Este repositorio sigue una estructura de **monorepo**, donde se agrupan múltiples aplicaciones y microservicios en un solo repositorio para facilitar la gestión, integración y despliegue del sistema.

La estructura principal se divide en dos conceptos clave:

- `apps/`
- `services/`

---

## apps/

La carpeta `apps/` contiene las **aplicaciones completas** del sistema.  
Estas son las partes que interactúan directamente con el usuario o actúan como punto de entrada al sistema.

### Características:
- Son ejecutables por sí mismas
- Representan interfaces o gateways
- Consumen los microservicios

---

## services/

La carpeta `services/` contiene los **microservicios** del sistema.

Cada microservicio está diseñado bajo el principio de responsabilidad única y se encarga de una funcionalidad específica del dominio.

### Características:
- Independientes entre sí
- Desplegables de forma individual
- Manejan lógica de negocio
- Se comunican entre sí mediante APIs

### Convención de nombres:
Se utiliza el prefijo `msvc-` para identificar microservicios:

```text
services/
├── msvc-auth/ # Autenticación y autorización
├── msvc-eureka/ # Service discovery (Eureka Server)
├── msvc-config/ # Configuración centralizada (Config Server)
├── msvc-gateway/ # API Gateway (punto de entrada)
```

## Tecnologías principales

- Backend: Spring Boot
- Frontend: React
- Arquitectura: Microservicios

---

## Objetivo

Centralizar y modernizar la gestión de activos de seguridad de la Universidad Nacional mediante una arquitectura escalable, mantenible y basada en buenas prácticas de ingeniería de software.

---

## Notas

- Este proyecto sigue una arquitectura distribuida basada en microservicios
- Cada servicio puede evolucionar de forma independiente
- Se promueve el uso de buenas prácticas como separación de responsabilidades y escalabilidad
