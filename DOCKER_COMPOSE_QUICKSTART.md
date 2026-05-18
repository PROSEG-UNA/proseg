# Docker Compose Quickstart

Guia minima para compilar y levantar los servicios definidos en `docker-compose.yml`.

## 1. Compilar las imagenes

Desde la carpeta `services`:

```bash
docker-compose build
```

## 2. Levantar los contenedores

Desde la carpeta `services`:

```bash
docker-compose up
```

Si quieres dejarlo en segundo plano, usa:

```bash
docker-compose up -d
```