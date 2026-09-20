#!/usr/bin/env bash
set -euo pipefail

# Genera (y opcionalmente ejecuta) el INSERT para crear un usuario del
# Sistema Hotel directo en la base de datos, con la contraseña ya
# encriptada en bcrypt (BCryptPasswordEncoder, el mismo que usa la app,
# acepta hashes $2a$/$2b$/$2y$ sin distinción).
#
# Uso:
#   ./scripts/crear-usuario.sh <username> <contraseña> <nombres> <apellidos> <correo> [admin]
#
# El sexto argumento es opcional: pasá "admin" para marcarlo como
# administrador. Si se omite, el usuario queda como staff normal.
#
# Ejemplo:
#   ./scripts/crear-usuario.sh gerardo 12345 Gerardo "" gerardo@hoteltunki.com admin

if [ "$#" -lt 5 ]; then
  echo "Uso: $0 <username> <contraseña> <nombres> <apellidos> <correo> [admin]" >&2
  exit 1
fi

if ! command -v htpasswd >/dev/null 2>&1; then
  echo "Falta el comando 'htpasswd' (viene con apache2-utils en Linux, o ya" >&2
  echo "está en macOS por defecto)." >&2
  exit 1
fi

USERNAME="$1"
PASSWORD="$2"
NOMBRES="$3"
APELLIDOS="$4"
CORREO="$5"
ES_ADMIN="false"
[ "${6:-}" = "admin" ] && ES_ADMIN="true"

escapar() {
  printf '%s' "$1" | sed "s/'/''/g"
}

HASH=$(htpasswd -bnBC 10 "" "$PASSWORD" | cut -d: -f2)

QUERY="insert into usuario (activo, apellidos, contrasena, correo, es_admin, nombres, username)
values (true, '$(escapar "$APELLIDOS")', '${HASH}', '$(escapar "$CORREO")', ${ES_ADMIN}, '$(escapar "$NOMBRES")', '$(escapar "$USERNAME")');"

echo "--- Query generado ---"
echo "$QUERY"
echo "----------------------"

read -r -p "¿Ejecutar este INSERT contra la base 'alquiler' ahora? (s/N) " CONFIRMAR
if [ "$CONFIRMAR" = "s" ] || [ "$CONFIRMAR" = "S" ]; then
  mysql -u root alquiler -e "$QUERY"
  echo "Usuario '$USERNAME' creado."
else
  echo "No se ejecutó nada. Copiá el query de arriba si lo querés correr manualmente."
fi
