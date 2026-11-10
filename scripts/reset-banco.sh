#!/usr/bin/env bash
# Apaga o schema public e recria o banco com database/*.sql. TODOS os dados se perdem.
set -euo pipefail

cd "$(dirname "$0")/.."

export PGHOST="${DB_HOST:-localhost}"
export PGPORT="${DB_PORT:-5432}"
export PGDATABASE="${DB_NAME:-vgrfinance}"
export PGUSER="${DB_USER:-vgr}"
export PGPASSWORD="${DB_PASSWORD:?Defina DB_PASSWORD (veja .devcontainer/.env.example)}"

psql -v ON_ERROR_STOP=1 -q -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

for arquivo in database/*.sql; do
    echo "Rodando $arquivo"
    psql -v ON_ERROR_STOP=1 -q -f "$arquivo"
done

echo "Banco recriado."
