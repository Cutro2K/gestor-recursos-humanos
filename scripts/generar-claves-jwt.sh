#!/usr/bin/env sh
# Genera la clave privada RSA con la que la API firma los JWT de sesión (CU 01).
# Uso: scripts/generar-claves-jwt.sh [ruta]   (por defecto api/keys/jwt-private.pem)
# La clave pública no se guarda: la API la deriva de la privada y la publica en
# GET /api/auth/jwks.json. La carpeta keys/ está en .gitignore: nunca subas la clave.
set -eu

destino="${1:-$(dirname "$0")/../api/keys/jwt-private.pem}"

if [ -e "$destino" ]; then
	echo "Ya existe $destino. Borralo primero si querés reemplazarlo (invalida las sesiones abiertas)." >&2
	exit 1
fi

mkdir -p "$(dirname "$destino")"
umask 077
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$destino"
echo "Clave generada en $destino"
