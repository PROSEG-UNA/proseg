# Docker + Kafka Setup

## 1. Instalacion de Docker (Windows)

1. Descargar Docker Desktop desde: https://www.docker.com/products/docker-desktop/
2. Ejecutar el instalador con las siguientes opciones:
    - Marcar la opcion **Use WSL 2**
    - Completar la instalacion
3. Instalar o actualizar WSL desde PowerShell con permisos de administrador:

```bash
wsl --install
wsl --update
wsl --set-default-version 2
```

4. Reiniciar el equipo.
5. Verificar la instalacion:

```bash
docker --version
docker run hello-world
```

---

## 3. Levantar Kafka

Desde la raiz del proyecto, ejecutar:

```bash
docker-compose up -d
```

---

## 4. Verificar contenedores activos

```bash
docker ps
```

El resultado debe mostrar los siguientes contenedores en ejecucion:

- `kafka`
- `zookeeper`

---

## 5. Ver logs del servicio Kafka

```bash
docker logs kafka
```

---

## 6. Detener los servicios

```bash
docker-compose down
```

---

## 7. Reiniciar los servicios

```bash
docker-compose up -d
```

---

## Notas

- Docker Desktop debe estar en ejecucion antes de lanzar cualquier comando.
- Se recomienda el uso de WSL2 para mayor rendimiento.
- Puerto Kafka: `9092`
- Puerto Zookeeper: `2181`

---

## Resultado esperado

Kafka operando de forma local para la comunicacion entre microservicios bajo un esquema orientado a eventos (
event-driven architecture).
