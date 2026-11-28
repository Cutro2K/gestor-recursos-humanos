#!/usr/bin/env sh
# Activa los Git hooks versionados en .githooks/ para este repositorio local.
# Uso: ./scripts/configurar-githooks.sh
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

chmod +x .githooks/pre-push
git config core.hooksPath .githooks

echo "Git hooks configurados correctamente (core.hooksPath = .githooks)."
echo "A partir de ahora, antes de cada 'git push' se validarán automáticamente rama, backend y frontend."
