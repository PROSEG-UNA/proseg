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

## 3. Si se actualiza el código fuente

Si realizas cambios en el código fuente de los servicios, es necesario recompilar las imágenes para que los cambios se reflejen en los contenedores. Para hacerlo, ejecuta:

```bash
docker compose up -d --build msvc-"nombre del servicio"
```

Reemplaza `"nombre del servicio"` con el nombre del servicio que deseas actualizar, por ejemplo, `msvc-usuarios` o `msvc-productos`. Esto recompilará solo la imagen del servicio especificado y levantará el contenedor actualizado en segundo plano.