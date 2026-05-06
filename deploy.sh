#!/bin/bash
# =============================================================
# deploy.sh - Script de despliegue en AWS EC2
# Incluye: Spring Boot + Prometheus + Grafana
# =============================================================
# Uso: Copiar este script al servidor EC2 y ejecutarlo:
#   chmod +x deploy.sh
#   ./deploy.sh
# =============================================================

set -e

APP_NAME="recetas-app"
APP_DIR="/opt/${APP_NAME}"
JAR_NAME="${APP_NAME}-0.0.1-SNAPSHOT.jar"
SERVICE_NAME="${APP_NAME}"
LOG_FILE="/var/log/${APP_NAME}.log"
PROMETHEUS_VERSION="2.51.0"

echo "=========================================="
echo " Desplegando ${APP_NAME} en EC2 + Monitoreo"
echo "=========================================="

# 1. Instalar Java 21 si no está instalado
if ! java -version 2>&1 | grep -q "21"; then
    echo "[1/8] Instalando Java 21..."
    sudo yum install -y java-21-amazon-corretto-headless 2>/dev/null || \
    sudo apt-get update && sudo apt-get install -y openjdk-21-jre-headless
else
    echo "[1/8] Java 21 ya instalado"
fi

# 2. Crear directorio de la aplicación
echo "[2/8] Creando directorio ${APP_DIR}..."
sudo mkdir -p ${APP_DIR}
sudo chown $USER:$USER ${APP_DIR}

# 3. Copiar el JAR (asume que ya se copió con scp)
if [ ! -f "${APP_DIR}/${JAR_NAME}" ]; then
    echo "ERROR: No se encontró ${APP_DIR}/${JAR_NAME}"
    echo "Copia el JAR al servidor con:"
    echo "  scp -i tu-key.pem target/${JAR_NAME} ec2-user@<EC2_IP>:${APP_DIR}/"
    exit 1
fi
echo "[3/8] JAR encontrado en ${APP_DIR}/${JAR_NAME}"

# 4. Crear archivo de variables de entorno
echo "[4/8] Configurando variables de entorno..."
if [ ! -f "${APP_DIR}/.env" ]; then
    cat > ${APP_DIR}/.env << 'EOF'
# ==========================================
# Variables de entorno para producción
# EDITAR con tus valores reales
# ==========================================
SPRING_PROFILES_ACTIVE=prod

# AWS RDS
RDS_HOSTNAME=recetas-app.crss8egc8xwr.sa-east-1.rds.amazonaws.com
RDS_PORT=3306
RDS_DB_NAME=recetas_db
RDS_USERNAME=recetas_user
RDS_PASSWORD=RecetasApp123*

# Cloudinary (opcional: si quieres sobreescribir los defaults)
# CLOUDINARY_CLOUD_NAME=dgvn4n9ry
# CLOUDINARY_API_KEY=658511773432214
# CLOUDINARY_API_SECRET=MsvQCfUmnxkQtFn3HBa2lLZ7y_c

# CORS (IP pública de tu EC2 o dominio)
# CORS_ALLOWED_ORIGINS=http://<EC2_PUBLIC_IP>,http://tu-dominio.com
EOF
    echo "  -> Archivo ${APP_DIR}/.env creado. EDÍTALO con tus credenciales reales."
else
    echo "  -> Archivo .env ya existe, se mantiene."
fi

# 5. Crear servicio systemd
echo "[5/8] Configurando servicio systemd..."
sudo tee /etc/systemd/system/${SERVICE_NAME}.service > /dev/null << EOF
[Unit]
Description=Recetas App - Spring Boot
After=network.target

[Service]
User=$USER
WorkingDirectory=${APP_DIR}
EnvironmentFile=${APP_DIR}/.env
ExecStart=/usr/bin/java -jar ${APP_DIR}/${JAR_NAME}
SuccessExitStatus=143
Restart=always
RestartSec=10
StandardOutput=append:${LOG_FILE}
StandardError=append:${LOG_FILE}

[Install]
WantedBy=multi-user.target
EOF

# 6. Instalar y configurar Prometheus
echo "[6/8] Instalando Prometheus..."
if ! command -v prometheus &> /dev/null; then
    cd /tmp
    wget -q https://github.com/prometheus/prometheus/releases/download/v${PROMETHEUS_VERSION}/prometheus-${PROMETHEUS_VERSION}.linux-amd64.tar.gz
    tar xf prometheus-${PROMETHEUS_VERSION}.linux-amd64.tar.gz
    sudo mv prometheus-${PROMETHEUS_VERSION}.linux-amd64/prometheus /usr/local/bin/
    sudo mv prometheus-${PROMETHEUS_VERSION}.linux-amd64/promtool /usr/local/bin/
    sudo mkdir -p /etc/prometheus /var/lib/prometheus
    sudo mv prometheus-${PROMETHEUS_VERSION}.linux-amd64/consoles /etc/prometheus/
    sudo mv prometheus-${PROMETHEUS_VERSION}.linux-amd64/console_libraries /etc/prometheus/
    rm -rf /tmp/prometheus-${PROMETHEUS_VERSION}.linux-amd64*
    cd -
fi

sudo tee /etc/prometheus/prometheus.yml > /dev/null << 'EOF'
global:
  scrape_interval: 30s
  evaluation_interval: 30s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'recetas-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
    scrape_interval: 30s
EOF

sudo tee /etc/systemd/system/prometheus.service > /dev/null << 'EOF'
[Unit]
Description=Prometheus
After=network.target

[Service]
User=root
ExecStart=/usr/local/bin/prometheus \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/var/lib/prometheus \
  --storage.tsdb.retention.time=7d \
  --storage.tsdb.retention.size=200MB \
  --web.listen-address=0.0.0.0:9090
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 7. Instalar y configurar Grafana
echo "[7/8] Instalando Grafana..."
if ! command -v grafana-server &> /dev/null; then
    if command -v yum &> /dev/null; then
        sudo tee /etc/yum.repos.d/grafana.repo > /dev/null << 'EOF'
[grafana]
name=grafana
baseurl=https://rpm.grafana.com
repo_gpgcheck=1
enabled=1
gpgcheck=1
gpgkey=https://rpm.grafana.com/gpg.key
sslverify=1
sslcacert=/etc/pki/tls/certs/ca-bundle.crt
EOF
        sudo yum install -y grafana
    else
        sudo apt-get install -y apt-transport-https software-properties-common
        wget -q -O - https://packages.grafana.com/gpg.key | sudo apt-key add -
        echo "deb https://packages.grafana.com/oss/deb stable main" | sudo tee /etc/apt/sources.list.d/grafana.list
        sudo apt-get update && sudo apt-get install -y grafana
    fi
fi

sudo mkdir -p /etc/grafana/provisioning/datasources
sudo tee /etc/grafana/provisioning/datasources/prometheus.yml > /dev/null << 'EOF'
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://localhost:9090
    isDefault: true
    editable: true
EOF

# 8. Iniciar todos los servicios
echo "[8/8] Iniciando servicios..."
sudo systemctl daemon-reload

sudo systemctl enable prometheus
sudo systemctl restart prometheus

sudo systemctl enable grafana-server
sudo systemctl restart grafana-server

sudo systemctl enable ${SERVICE_NAME}
sudo systemctl restart ${SERVICE_NAME}

echo ""
echo "=========================================="
echo " Despliegue completado!"
echo "=========================================="
EC2_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo '<EC2_PUBLIC_IP>')
echo ""
echo " Estado:    sudo systemctl status ${SERVICE_NAME}"
echo " Logs:      sudo tail -f ${LOG_FILE}"
echo " Parar:     sudo systemctl stop ${SERVICE_NAME}"
echo " Reiniciar: sudo systemctl restart ${SERVICE_NAME}"
echo ""
echo " App:        http://${EC2_IP}:8080"
echo " Grafana:    http://${EC2_IP}:3000  (admin / admin123)"
echo " Prometheus: http://${EC2_IP}:9090"
echo ""
