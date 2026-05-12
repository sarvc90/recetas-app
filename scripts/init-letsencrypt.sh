#!/usr/bin/env bash
set -euo pipefail

# =====================================
# CONFIG
# =====================================
domains=(rincon-sabores.online www.rincon-sabores.online)
rsa_key_size=4096
email="elrinconlossabores@gmail.com"
staging=0

read -p "Enter project directory: (if you are following the DEPLOY.md instructions: /tmp/recetas-boostrap ) " PROJECT_DIR
while [ ! -f "${PROJECT_DIR}/docker/docker-compose.yml" ]; do
  echo "No docker-compose.yml found in ${PROJECT_DIR}. Please enter the correct path."
  read -p "Enter project directory: " PROJECT_DIR
done

COMPOSE_FILE="${PROJECT_DIR}/docker/docker-compose.yml"
BOOTSTRAP_FILE="${PROJECT_DIR}/docker/docker-compose.bootstrap.yml"
DC=(docker compose -f "${COMPOSE_FILE}")
DC_BOOT=(docker compose -f "${BOOTSTRAP_FILE}")
data_path="${PROJECT_DIR}/docker/frontend/certbot"
primary="${domains[0]}"

# =====================================
# VERIFICAR CERTIFICADO EXISTENTE
# =====================================
if [ -d "$data_path/conf/live/$primary" ]; then
  read -rp "Existing certificate for $primary found. Replace? (y/N) " decision
  [[ "$decision" =~ ^[Yy]$ ]] || exit 0
fi

# =====================================
# PARÁMETROS TLS
# =====================================
echo "### Downloading recommended TLS parameters ..."
mkdir -p "$data_path/conf"
if [ ! -e "$data_path/conf/options-ssl-nginx.conf" ] || [ ! -e "$data_path/conf/ssl-dhparams.pem" ]; then
  curl -fsSL https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf \
    > "$data_path/conf/options-ssl-nginx.conf"
  curl -fsSL https://ssl-config.mozilla.org/ffdhe2048.txt \
    > "$data_path/conf/ssl-dhparams.pem"
fi

# =====================================
# FASE 1: nginx HTTP-only con bootstrap
# =====================================
echo "### Phase 1: starting nginx in HTTP-only mode ..."
"${DC[@]}" down --remove-orphans 2>/dev/null || true
"${DC_BOOT[@]}" up -d --no-deps --force-recreate frontend

echo "### Waiting for nginx to be ready ..."
for i in $(seq 1 15); do
  if "${DC_BOOT[@]}" exec frontend nginx -t 2>/dev/null; then
    echo "nginx is ready."
    break
  fi
  echo "  attempt $i/15 ..."
  sleep 2
done

# =====================================
# FASE 2: obtener certificado real
# =====================================
echo "### Requesting Let's Encrypt certificate for ${domains[*]} ..."
domain_args=""
for d in "${domains[@]}"; do domain_args="$domain_args -d $d"; done

staging_arg=""
[ "$staging" = "1" ] && staging_arg="--staging"

"${DC_BOOT[@]}" run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    ${staging_arg} \
    --email ${email} \
    ${domain_args} \
    --rsa-key-size ${rsa_key_size} \
    --agree-tos \
    --non-interactive \
    --force-renewal" certbot

# =====================================
# FASE 3: bajar bootstrap, levantar prod
# =====================================
echo "### Phase 3: switching to production stack ..."
"${DC_BOOT[@]}" down --remove-orphans

echo "### Starting full production stack ..."
"${DC[@]}" up -d

echo ""
echo "### Done. Certificate issued and full stack running."
echo "### Verify: https://${primary}"