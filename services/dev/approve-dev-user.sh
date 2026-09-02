#!/bin/bash
set -uo pipefail

export PGPASSWORD="${POSTGRES_PASSWORD:-}"
HOST=${POSTGRES_HOST:-postgres}
USUARIO_BD=${POSTGRES_USER:-}
BD_APP=${POSTGRES_DB:-proseg-bd}
BD_KEYCLOAK=${KEYCLOAK_DB_NAME:-keycloak_db}
REALM=${KEYCLOAK_REALM:-proseg-realm}
USUARIO=${DEV_USER_USERNAME:-dev}

consultar() {
  psql -h "$HOST" -U "$USUARIO_BD" -d "$1" -tAc "$2" 2>/dev/null | tr -d '[:space:]'
}

if [ -z "$USUARIO_BD" ] || [ -z "$PGPASSWORD" ]; then
  echo "Faltan POSTGRES_USER o POSTGRES_PASSWORD" >&2
  exit 1
fi

echo "Aprobando el usuario de desarrollo '$USUARIO' en la base de la aplicación"

id_keycloak=""
for intento in $(seq 1 60); do
  id_keycloak=$(consultar "$BD_KEYCLOAK" \
    "select u.id from user_entity u join realm r on u.realm_id = r.id where r.name = '$REALM' and u.username = '$USUARIO'")
  [ -n "$id_keycloak" ] && break
  sleep 5
done

if [ -z "$id_keycloak" ]; then
  echo "No se encontró el usuario '$USUARIO' en Keycloak" >&2
  exit 1
fi

tabla=""
for intento in $(seq 1 60); do
  tabla=$(consultar "$BD_APP" "select to_regclass('public.user_approbation_table')")
  [ -n "$tabla" ] && break
  if [ "$intento" -eq 1 ]; then
    echo "  esperando a que msvc-auth cree sus tablas..."
  fi
  sleep 5
done

if [ -z "$tabla" ]; then
  echo "La tabla user_approbation_table no apareció; ¿arrancó msvc-auth?" >&2
  exit 1
fi

psql -h "$HOST" -U "$USUARIO_BD" -d "$BD_APP" -v ON_ERROR_STOP=1 -q <<SQL
INSERT INTO user_approbation_table (id, id_usuario, status, created_at, updated_at, is_deleted)
VALUES ('$id_keycloak'::uuid, '$id_keycloak', 'APPROVED', now(), now(), false)
ON CONFLICT (id) DO UPDATE SET status = 'APPROVED', is_deleted = false, updated_at = now();
SQL

estado=$(consultar "$BD_APP" "select status from user_approbation_table where id = '$id_keycloak'::uuid")
echo "Usuario de desarrollo aprobado (estado: $estado)"
