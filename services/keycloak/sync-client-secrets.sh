#!/bin/bash
set -uo pipefail

KCADM=${KCADM_BIN:-/opt/keycloak/bin/kcadm.sh}
KCADM_CONFIG=/tmp/kcadm.config
SERVIDOR=${KEYCLOAK_URL:-http://keycloak:8080/auth}
REALM=${KEYCLOAK_REALM:-proseg-realm}
USUARIO=${KEYCLOAK_ADMIN_USERNAME:-}
CLAVE=${KEYCLOAK_ADMIN_PASSWORD:-}

CLIENTES="${KEYCLOAK_CLIENT_ID:-proseg-app}=${KEYCLOAK_CLIENT_SECRET:-}
msvc-email=${KEYCLOAK_EMAIL_CLIENT_SECRET:-}
msvc-maintenance=${KEYCLOAK_MAINTENANCE_CLIENT_SECRET:-}"

actualizados=0
errores=0

kc() {
  "$KCADM" "$@" --config "$KCADM_CONFIG"
}

autenticar() {
  local intento
  for intento in $(seq 1 30); do
    if kc config credentials --server "$SERVIDOR" --realm master \
         --user "$USUARIO" --password "$CLAVE" >/dev/null 2>&1; then
      return 0
    fi
    sleep 5
  done
  echo "No se pudo autenticar contra $SERVIDOR tras 30 intentos" >&2
  return 1
}

id_del_cliente() {
  kc get clients -r "$REALM" -q "clientId=$1" --fields id --format csv --noquotes 2>/dev/null \
    | tr -d '\r' | head -1
}

secret_actual() {
  kc get "clients/$1/client-secret" -r "$REALM" --fields value --format csv --noquotes 2>/dev/null \
    | tr -d '\r' | head -1
}

if [ -z "$USUARIO" ] || [ -z "$CLAVE" ]; then
  echo "Faltan KEYCLOAK_ADMIN_USERNAME o KEYCLOAK_ADMIN_PASSWORD" >&2
  exit 1
fi

echo "Sincronizando los client secrets del realm $REALM"

autenticar || exit 1

while IFS='=' read -r cliente esperado; do
  [ -z "$cliente" ] && continue
  if [ -z "$esperado" ]; then
    echo "  ! sin valor para el secret de '$cliente'" >&2
    errores=$((errores + 1))
    continue
  fi
  id=$(id_del_cliente "$cliente")
  if [ -z "$id" ]; then
    echo "  ! no existe el cliente '$cliente' en el realm" >&2
    errores=$((errores + 1))
    continue
  fi
  if [ "$(secret_actual "$id")" = "$esperado" ]; then
    continue
  fi
  if salida=$(kc update "clients/$id" -r "$REALM" -s "secret=$esperado" 2>&1); then
    echo "  + secret actualizado: $cliente"
    actualizados=$((actualizados + 1))
  else
    echo "  ! error actualizando el secret de '$cliente': $salida" >&2
    errores=$((errores + 1))
  fi
done <<< "$CLIENTES"

echo "Listo: $actualizados secrets actualizados"

if [ "$errores" -gt 0 ]; then
  echo "Terminó con $errores errores" >&2
  exit 1
fi
