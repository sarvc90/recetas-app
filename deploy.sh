#!/bin/bash
# =============================================================
# deploy.sh - Script de despliegue en AWS EC2
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

echo "=========================================="
echo " Desplegando ${APP_NAME} en EC2"
echo "=========================================="

# 1. Instalar Java 21 si no está instalado
if ! java -version 2>&1 | grep -q "21"; then
    echo "[1/6] Instalando Java 21..."
    sudo yum install -y java-21-amazon-corretto-headless 2>/dev/null || \
    sudo apt-get update && sudo apt-get install -y openjdk-21-jre-headless
else
    echo "[1/6] Java 21 ya instalado"
fi

# 2. Crear directorio de la aplicación
echo "[2/6] Creando directorio ${APP_DIR}..."
sudo mkdir -p ${APP_DIR}
sudo chown $USER:$USER ${APP_DIR}

# 3. Copiar el JAR (asume que ya se copió con scp)
if [ ! -f "${APP_DIR}/${JAR_NAME}" ]; then
    echo "ERROR: No se encontró ${APP_DIR}/${JAR_NAME}"
    echo "Copia el JAR al servidor con:"
    echo "  scp -i tu-key.pem target/${JAR_NAME} ec2-user@<EC2_IP>:${APP_DIR}/"
    exit 1
fi
echo "[3/6] JAR encontrado en ${APP_DIR}/${JAR_NAME}"

# 4. Crear archivo de variables de entorno
echo "[4/6] Configurando variables de entorno..."
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
echo "[5/6] Configurando servicio systemd..."
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

# 6. Iniciar servicio
echo "[6/6] Iniciando servicio..."
sudo systemctl daemon-reload
sudo systemctl enable ${SERVICE_NAME}
sudo systemctl restart ${SERVICE_NAME}

echo ""
echo "=========================================="
echo " Despliegue completado!"
echo "=========================================="
echo ""
echo " Estado:  sudo systemctl status ${SERVICE_NAME}"
echo " Logs:    sudo tail -f ${LOG_FILE}"
echo " Parar:   sudo systemctl stop ${SERVICE_NAME}"
echo " Reiniciar: sudo systemctl restart ${SERVICE_NAME}"
echo ""
echo " La app está corriendo en: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo '<EC2_PUBLIC_IP>'):8080"
echo ""
