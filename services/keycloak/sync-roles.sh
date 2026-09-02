#!/bin/bash
set -uo pipefail

KCADM=${KCADM_BIN:-/opt/keycloak/bin/kcadm.sh}
KCADM_CONFIG=/tmp/kcadm.config
ARCHIVO=${ARCHIVO_ROLES:-/config/roles-privilegios.conf}
SERVIDOR=${KEYCLOAK_URL:-http://keycloak:8080/auth}
REALM=${KEYCLOAK_REALM:-proseg-realm}
USUARIO=${KEYCLOAK_ADMIN_USERNAME:-}
CLAVE=${KEYCLOAK_ADMIN_PASSWORD:-}
SINCRONIZAR_BORRADO=${SYNC_DELETE:-false}

PROTEGIDOS="offline_access
uma_authorization
default-roles-${REALM}"

privilegios_creados=0
roles_creados=0
asignaciones_creadas=0
privilegios_borrados=0
asignaciones_revocadas=0
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

es_conflicto() {
  printf '%s' "$1" | grep -qiE "409|conflict|already exists"
}

crear_rol_si_falta() {
  local nombre=$1 tipo=$2 salida
  if salida=$(kc create roles -r "$REALM" -s "name=$nombre" 2>&1); then
    echo "  + $tipo creado: $nombre"
    return 0
  fi
  if es_conflicto "$salida"; then
    return 1
  fi
  echo "  ! error creando $tipo '$nombre': $salida" >&2
  errores=$((errores + 1))
  return 1
}

composites_actuales() {
  local rol=$1 ruta
  ruta="roles/${rol// /%20}/composites"
  kc get "$ruta" -r "$REALM" --fields name --format csv --noquotes 2>/dev/null | tr -d '\r'
}

asignar_privilegio_si_falta() {
  local rol=$1 privilegio=$2 existentes=$3 salida
  if printf '%s\n' "$existentes" | grep -qxF "$privilegio"; then
    return 1
  fi
  if salida=$(kc add-roles -r "$REALM" --rname "$rol" --rolename "$privilegio" 2>&1); then
    echo "  + rol '$rol' gana el privilegio $privilegio"
    return 0
  fi
  echo "  ! error asignando $privilegio a '$rol': $salida" >&2
  errores=$((errores + 1))
  return 1
}

revocar_privilegio() {
  local rol=$1 privilegio=$2 salida
  if salida=$(kc remove-roles -r "$REALM" --rname "$rol" --rolename "$privilegio" 2>&1); then
    echo "  - rol '$rol' pierde el privilegio $privilegio"
    return 0
  fi
  echo "  ! error revocando $privilegio de '$rol': $salida" >&2
  errores=$((errores + 1))
  return 1
}

privilegios_del_realm() {
  kc get roles -r "$REALM" --fields name,composite --format csv --noquotes 2>/dev/null \
    | tr -d '\r' \
    | awk -F, 'NF >= 2 && $2 == "false" { print $1 }'
}

esta_protegido() {
  printf '%s\n' "$PROTEGIDOS" | grep -qxF "$1"
}

borrar_privilegio() {
  local nombre=$1 salida
  if salida=$(kc delete "roles/${nombre// /%20}" -r "$REALM" 2>&1); then
    echo "  - privilegio borrado: $nombre"
    return 0
  fi
  echo "  ! error borrando el privilegio '$nombre': $salida" >&2
  errores=$((errores + 1))
  return 1
}

leer_archivo() {
  local seccion="" linea
  privilegios=()
  roles=()
  while IFS= read -r linea || [ -n "$linea" ]; do
    linea=${linea%$'\r'}
    linea=${linea%"${linea##*[![:space:]]}"}
    linea=${linea#"${linea%%[![:space:]]*}"}
    [ -z "$linea" ] && continue
    case "$linea" in
      \#*) continue ;;
      "[privilegios]")
        seccion="privilegios"
        continue
        ;;
      \[rol\ *\])
        seccion=${linea#\[rol }
        seccion=${seccion%\]}
        roles+=("$seccion")
        seccion="rol:$seccion"
        continue
        ;;
      \[*\])
        echo "Sección desconocida: $linea" >&2
        errores=$((errores + 1))
        continue
        ;;
    esac
    if [ "$seccion" = "privilegios" ]; then
      privilegios+=("$linea")
    elif [ "${seccion#rol:}" != "$seccion" ]; then
      local rol=${seccion#rol:}
      local clave="miembros_${rol// /_}"
      eval "$clave+=(\"\$linea\")"
    fi
  done < "$ARCHIVO"
}

declare -a privilegios roles
for rol_declarado in $(grep -o '^\[rol .*\]$' "$ARCHIVO" | sed -e 's/^\[rol //' -e 's/\]$//' -e 's/ /_/g'); do
  eval "declare -a miembros_${rol_declarado}=()"
done

if [ -z "$USUARIO" ] || [ -z "$CLAVE" ]; then
  echo "Faltan KEYCLOAK_ADMIN_USERNAME o KEYCLOAK_ADMIN_PASSWORD" >&2
  exit 1
fi

if [ ! -f "$ARCHIVO" ]; then
  echo "No existe el archivo $ARCHIVO" >&2
  exit 1
fi

echo "Sincronizando roles y privilegios del realm $REALM"

autenticar || exit 1
leer_archivo

for privilegio in "${privilegios[@]}"; do
  if crear_rol_si_falta "$privilegio" "privilegio"; then
    privilegios_creados=$((privilegios_creados + 1))
  fi
done

for rol in "${roles[@]}"; do
  if crear_rol_si_falta "$rol" "rol"; then
    roles_creados=$((roles_creados + 1))
  fi
done

for rol in "${roles[@]}"; do
  clave="miembros_${rol// /_}"
  eval "miembros=(\"\${${clave}[@]}\")"
  existentes=$(composites_actuales "$rol")
  for privilegio in "${miembros[@]}"; do
    if asignar_privilegio_si_falta "$rol" "$privilegio" "$existentes"; then
      asignaciones_creadas=$((asignaciones_creadas + 1))
    fi
  done
  [ "$SINCRONIZAR_BORRADO" = "true" ] || continue
  declarados=$(printf '%s\n' "${miembros[@]+"${miembros[@]}"}")
  while IFS= read -r sobrante; do
    [ -z "$sobrante" ] && continue
    printf '%s\n' "$declarados" | grep -qxF "$sobrante" && continue
    if revocar_privilegio "$rol" "$sobrante"; then
      asignaciones_revocadas=$((asignaciones_revocadas + 1))
    fi
  done <<< "$existentes"
done

if [ "$SINCRONIZAR_BORRADO" = "true" ]; then
  declarados=$(printf '%s\n' "${privilegios[@]}")
  while IFS= read -r sobrante; do
    [ -z "$sobrante" ] && continue
    esta_protegido "$sobrante" && continue
    printf '%s\n' "$declarados" | grep -qxF "$sobrante" && continue
    if borrar_privilegio "$sobrante"; then
      privilegios_borrados=$((privilegios_borrados + 1))
    fi
  done <<< "$(privilegios_del_realm)"
fi

echo "Listo: ${#privilegios[@]} privilegios y ${#roles[@]} roles revisados"
echo "Agregados: $privilegios_creados privilegios, $roles_creados roles, $asignaciones_creadas asignaciones"
if [ "$SINCRONIZAR_BORRADO" = "true" ]; then
  echo "Borrados: $privilegios_borrados privilegios, $asignaciones_revocadas asignaciones revocadas"
fi

if [ "$errores" -gt 0 ]; then
  echo "Terminó con $errores errores" >&2
  exit 1
fi
