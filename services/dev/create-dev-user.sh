#!/bin/bash
set -uo pipefail

KCADM=${KCADM_BIN:-/opt/keycloak/bin/kcadm.sh}
KCADM_CONFIG=/tmp/kcadm.config
SERVIDOR=${KEYCLOAK_URL:-http://keycloak:8080/auth}
REALM=${KEYCLOAK_REALM:-proseg-realm}
ADMIN=${KEYCLOAK_ADMIN_USERNAME:-}
ADMIN_CLAVE=${KEYCLOAK_ADMIN_PASSWORD:-}
USUARIO=${DEV_USER_USERNAME:-dev}
CORREO=${DEV_USER_EMAIL:-dev@proseg.local}
CLAVE=${DEV_USER_PASSWORD:-}
ROL=${DEV_USER_ROLE:-SUPER_ADMINISTRADOR}

kc() {
  "$KCADM" "$@" --config "$KCADM_CONFIG"
}

es_conflicto() {
  printf '%s' "$1" | grep -qiE "409|conflict|already exists|exists with same"
}

if [ -z "$ADMIN" ] || [ -z "$ADMIN_CLAVE" ]; then
  echo "Faltan KEYCLOAK_ADMIN_USERNAME o KEYCLOAK_ADMIN_PASSWORD" >&2
  exit 1
fi

if [ -z "$CLAVE" ]; then
  echo "Falta DEV_USER_PASSWORD" >&2
  exit 1
fi

for intento in $(seq 1 30); do
  if kc config credentials --server "$SERVIDOR" --realm master \
       --user "$ADMIN" --password "$ADMIN_CLAVE" >/dev/null 2>&1; then
    break
  fi
  if [ "$intento" -eq 30 ]; then
    echo "No se pudo autenticar contra $SERVIDOR" >&2
    exit 1
  fi
  sleep 5
done

echo "Preparando el usuario de desarrollo '$USUARIO' en el realm $REALM"

recien_creado=0
if salida=$(kc create users -r "$REALM" \
     -s "username=$USUARIO" \
     -s "email=$CORREO" \
     -s "firstName=Desarrollo" \
     -s "lastName=Local" \
     -s "enabled=true" \
     -s "emailVerified=true" 2>&1); then
  recien_creado=1
  echo "  + usuario creado"
elif es_conflicto "$salida"; then
  echo "  = el usuario ya existía, no se toca su contraseña"
else
  echo "  ! error creando el usuario: $salida" >&2
  exit 1
fi

if [ "$recien_creado" -eq 1 ]; then
  if salida=$(kc set-password -r "$REALM" --username "$USUARIO" \
       --new-password "$CLAVE" 2>&1); then
    echo "  + contraseña establecida"
  else
    echo "  ! error estableciendo la contraseña: $salida" >&2
    exit 1
  fi
fi

if salida=$(kc add-roles -r "$REALM" --uusername "$USUARIO" --rolename "$ROL" 2>&1); then
  echo "  + rol $ROL asignado"
else
  echo "  ! error asignando el rol $ROL: $salida" >&2
  exit 1
fi

echo "Usuario de desarrollo listo en Keycloak"
