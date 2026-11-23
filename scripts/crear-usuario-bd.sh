#!/usr/bin/env bash
set -euo pipefail

# Crea el usuario de MySQL que usa la aplicación (defecto D5: antes se
# conectaba con root). Solo tiene permisos sobre la base "alquiler": leer y
# escribir datos y los permisos de esquema que necesita Hibernate
# (ddl-auto=update) para crear o ajustar tablas e índices. No puede crear
# usuarios, tocar otras bases ni administrar el servidor.
#
# La contraseña se toma de la variable DB_PASSWORD (o del archivo .env) y
# nunca se escribe en el repositorio.
#
# Uso (como administrador de MySQL):
#   DB_PASSWORD='una-clave-segura' ./scripts/crear-usuario-bd.sh [host]
#
# El argumento host es opcional: "localhost" por defecto (también crea
# 127.0.0.1). En contenedores o en el pipeline se usa "%".
# Variables opcionales: DB_USER (estadia_app), MYSQL_ADMIN (root),
# MYSQL_HOST (127.0.0.1), MYSQL_PORT (3306).

if [ -f .env ] && [ -z "${DB_PASSWORD:-}" ]; then
  # shellcheck disable=SC1091
  set -a; . ./.env; set +a
fi

: "${DB_PASSWORD:?Definí DB_PASSWORD (o creá el archivo .env a partir de .env.example)}"
DB_USER="${DB_USER:-estadia_app}"
HOST_PATTERN="${1:-localhost}"
ADMIN="${MYSQL_ADMIN:-root}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"

hosts=("$HOST_PATTERN")
if [ "$HOST_PATTERN" = "localhost" ]; then
  hosts+=("127.0.0.1")
fi

sql=""
for h in "${hosts[@]}"; do
  sql+="CREATE USER IF NOT EXISTS '${DB_USER}'@'${h}' IDENTIFIED BY '${DB_PASSWORD}';"
  sql+="ALTER USER '${DB_USER}'@'${h}' IDENTIFIED BY '${DB_PASSWORD}';"
  sql+="GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON alquiler.* TO '${DB_USER}'@'${h}';"
done
sql+="FLUSH PRIVILEGES;"

mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$ADMIN" ${MYSQL_ADMIN_PASSWORD:+-p"$MYSQL_ADMIN_PASSWORD"} -e "$sql"
echo "Usuario ${DB_USER} listo para ${hosts[*]} con permisos solo sobre la base alquiler."
