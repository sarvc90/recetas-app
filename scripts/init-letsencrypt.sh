#!/usr/bin/env bash
set -euo pipefail

# =====================================
# CONFIG
# =====================================
domains=(rincon-sabores.online www.rincon-sabores.online)
rsa_key_size=4096
email="elrinconlossabores@gmail.com"
staging=0   # 1 = Let's Encrypt staging (untrusted, high rate limit), 0 = production

read -p "Enter project directory: " PROJECT_DIR

while [ ! -f "${PROJECT_DIR}/docker/docker-compose.yml" ]; do
  echo "No docker-compose.yml found in ${PROJECT_DIR}. Please enter the correct path."
  read -p "Enter project directory: " PROJECT_DIR
done

COMPOSE_FILE="${PROJECT_DIR}/docker/docker-compose.yml"
DC=(docker compose -f "${COMPOSE_FILE}")
data_path="${PROJECT_DIR}/docker/frontend/certbot"
primary="${domains[0]}"

if [ -d "$data_path/conf/live/$primary" ]; then
  read -rp "Existing certificate for $primary found. Replace? (y/N) " decision
  [[ "$decision" =~ ^[Yy]$ ]] || exit 0
fi

echo "### Downloading recommended TLS parameters ..."
mkdir -p "$data_path/conf"
if [ ! -e "$data_path/conf/options-ssl-nginx.conf" ] || [ ! -e "$data_path/conf/ssl-dhparams.pem" ]; then
  curl -fsSL https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf \
    > "$data_path/conf/options-ssl-nginx.conf"
  curl -fsSL https://ssl-config.mozilla.org/ffdhe2048.txt \
    > "$data_path/conf/ssl-dhparams.pem"
fi

echo "### Creating dummy certificate for ${primary} ..."
cert_path="/etc/letsencrypt/live/$primary"
mkdir -p "$data_path/conf/live/$primary"
"${DC[@]}" run --rm --entrypoint "\
  openssl req -x509 -nodes -newkey rsa:${rsa_key_size} -days 1 \
    -keyout '${cert_path}/privkey.pem' \
    -out '${cert_path}/fullchain.pem' \
    -subj '/CN=localhost'" certbot

echo "### Starting nginx ..."
"${DC[@]}" up --force-recreate -d frontend

echo "### Removing dummy certificate ..."
"${DC[@]}" run --rm --entrypoint "\
  rm -Rf /etc/letsencrypt/live/${primary} && \
  rm -Rf /etc/letsencrypt/archive/${primary} && \
  rm -Rf /etc/letsencrypt/renewal/${primary}.conf" certbot

echo "### Requesting Let's Encrypt certificate for ${domains[*]} ..."
domain_args=""
for d in "${domains[@]}"; do domain_args="$domain_args -d $d"; done

staging_arg=""
[ "$staging" = "1" ] && staging_arg="--staging"

"${DC[@]}" run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    ${staging_arg} \
    --email ${email} \
    ${domain_args} \
    --rsa-key-size ${rsa_key_size} \
    --agree-tos \
    --non-interactive \
    --force-renewal" certbot

echo "### Reloading nginx ..."
"${DC[@]}" exec frontend nginx -s reload

echo "### Done."
