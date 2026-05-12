#!/usr/bin/env bash
set -euo pipefail

# =====================================
# CONFIG
# =====================================
domains=(rincon-sabores.online www.rincon-sabores.online)
rsa_key_size=4096
email="elrinconlossabores@gmail.com"
staging=0   # 1 = staging, 0 = producción

read -p "Enter project directory: " PROJECT_DIR
while [ ! -f "${PROJECT_DIR}/docker/docker-compose.yml" ]; do
  echo "No docker-compose.yml found in ${PROJECT_DIR}. Please enter the correct path."
  read -p "Enter project directory: " PROJECT_DIR
done

COMPOSE_FILE="${PROJECT_DIR}/docker/docker-compose.yml"
DC=(docker compose -f "${COMPOSE_FILE}")
data_path="${PROJECT_DIR}/docker/frontend/certbot"
nginx_conf_dir="${PROJECT_DIR}/docker/frontend/nginx/conf"
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
# FASE 1: arrancar nginx SIN SSL
# Renombramos recetas.conf para que nginx
# no lo cargue, y solo use el bootstrap.
# =====================================
echo "### Phase 1: starting nginx in HTTP-only mode ..."
if [ -f "${nginx_conf_dir}/recetas.conf" ]; then
  mv "${nginx_conf_dir}/recetas.conf" "${nginx_conf_dir}/recetas.conf.bak"
fi

# Aseguramos que el bootstrap esté presente
if [ ! -f "${nginx_conf_dir}/recetas-bootstrap.conf" ]; then
  echo "ERROR: recetas-bootstrap.conf not found in ${nginx_conf_dir}"
  exit 1
fi

# Bajamos servicios previos y levantamos solo frontend (sin SSL)
"${DC[@]}" down --remove-orphans 2>/dev/null || true
"${DC[@]}" up --force-recreate --no-deps -d frontend

# Esperamos que nginx esté listo
echo "### Waiting for nginx to be ready ..."
for i in $(seq 1 15); do
  if "${DC[@]}" exec frontend nginx -t 2>/dev/null; then
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

"${DC[@]}" run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    ${staging_arg} \
    --email ${email} \
    ${domain_args} \
    --rsa-key-size ${rsa_key_size} \
    --agree-tos \
    --non-interactive \
    --force-renewal" certbot

# =====================================
# FASE 3: activar SSL en nginx
# =====================================
echo "### Phase 3: enabling SSL config ..."
if [ -f "${nginx_conf_dir}/recetas.conf.bak" ]; then
  mv "${nginx_conf_dir}/recetas.conf.bak" "${nginx_conf_dir}/recetas.conf"
fi

# Recargamos nginx con el conf SSL definitivo
"${DC[@]}" exec frontend nginx -s reload

# Levantamos el resto de servicios
echo "### Starting remaining services ..."
"${DC[@]}" up -d

echo ""
echo "### Done. Certificate issued and nginx running with SSL."
echo "### Verify: https://${primary}"