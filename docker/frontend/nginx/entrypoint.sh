#!/bin/sh
set -e

CERT="/etc/letsencrypt/live/rincon-sabores.online/fullchain.pem"

if [ -f "$CERT" ]; then
  echo "Certificate found, starting with SSL config."
else
  echo "Certificate not found, starting with HTTP-only config."
  cp /etc/nginx/conf.d/recetas-bootstrap.conf /etc/nginx/conf.d/recetas.conf
fi

exec "$@"