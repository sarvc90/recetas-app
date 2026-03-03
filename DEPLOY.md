# Guia de Despliegue: EC2 + RDS

## Arquitectura

```
  [Tu PC / IDE]                    [AWS Cloud]
  ─────────────                    ───────────────────────────────
  │ Spring Boot │   desarrollo     │  EC2 (Ubuntu/Amazon Linux)  │
  │ Profile: dev│──── MySQL ───>   │  ┌─────────────────────┐    │
  │ localhost DB │   local         │  │ Spring Boot          │    │
  └──────────────┘                 │  │ Profile: prod        │    │
                                   │  │ Puerto 8080          │    │
                                   │  └──────────┬──────────┘    │
                                   │             │ JDBC           │
                                   │  ┌──────────▼──────────┐    │
                                   │  │ RDS MySQL            │    │
                                   │  │ Puerto 3306          │    │
                                   │  └─────────────────────┘    │
                                   ───────────────────────────────
```

---

## Paso 1: Configurar AWS RDS

### 1.1 Crear la instancia RDS (si no la tienes)
1. Ve a **AWS Console > RDS > Create database**
2. Configuracion:
   - Engine: **MySQL 8.0**
   - Template: **Free tier**
   - DB instance identifier: `recetas-app`
   - Master username: `recetas_user`
   - Master password: (tu password)
   - DB name: `recetas_db`
   - Region: `sa-east-1`

### 1.2 Configurar Security Group de RDS
El Security Group de RDS debe permitir trafico **solo desde el Security Group del EC2**:

| Tipo  | Protocolo | Puerto | Origen                        |
|-------|-----------|--------|-------------------------------|
| MySQL | TCP       | 3306   | sg-XXXX (Security Group EC2)  |

> **IMPORTANTE**: NO abrir el puerto 3306 a 0.0.0.0/0. Solo al SG del EC2.

---

## Paso 2: Configurar AWS EC2

### 2.1 Crear la instancia EC2
1. Ve a **AWS Console > EC2 > Launch Instance**
2. Configuracion:
   - AMI: **Amazon Linux 2023** o **Ubuntu 22.04**
   - Instance type: **t2.micro** (Free tier)
   - Key pair: Crear o seleccionar una (descargar `.pem`)
   - Red: Misma VPC y subred que RDS (o subredes que se comuniquen)

### 2.2 Configurar Security Group de EC2

| Tipo  | Protocolo | Puerto | Origen      | Nota                |
|-------|-----------|--------|-------------|---------------------|
| SSH   | TCP       | 22     | Tu IP/32    | Para conectarte     |
| HTTP  | TCP       | 8080   | 0.0.0.0/0   | App Spring Boot     |
| HTTP  | TCP       | 80     | 0.0.0.0/0   | (Opcional, con nginx)|

---

## Paso 3: Crear la base de datos

Conéctate desde EC2 a RDS para crear el schema:

```bash
# Desde EC2 (ya que tiene acceso a RDS por Security Groups)
mysql -h recetas-app.crss8egc8xwr.sa-east-1.rds.amazonaws.com -u recetas_user -p

# Dentro de MySQL:
CREATE DATABASE IF NOT EXISTS recetas_db;
```

> Hibernate con `ddl-auto=update` creara las tablas automaticamente.

---

## Paso 4: Compilar el JAR

Desde tu maquina local:

```bash
cd recetas-app/recetas-app
./mvnw clean package -DskipTests
```

Esto genera: `target/recetas-app-0.0.1-SNAPSHOT.jar`

---

## Paso 5: Desplegar en EC2

### 5.1 Copiar archivos al servidor
```bash
# Copiar el JAR
scp -i tu-key.pem target/recetas-app-0.0.1-SNAPSHOT.jar ec2-user@<EC2_IP>:/opt/recetas-app/

# Copiar el script de despliegue
scp -i tu-key.pem deploy.sh ec2-user@<EC2_IP>:~/
```

### 5.2 Conectarse al EC2
```bash
ssh -i tu-key.pem ec2-user@<EC2_IP>
```

### 5.3 Ejecutar el despliegue
```bash
chmod +x deploy.sh
./deploy.sh
```

### 5.4 Editar variables de entorno
```bash
nano /opt/recetas-app/.env
```
Ajusta `RDS_PASSWORD` y `CORS_ALLOWED_ORIGINS` con los valores reales, luego:
```bash
sudo systemctl restart recetas-app
```

---

## Paso 6: Verificar

```bash
# Ver estado del servicio
sudo systemctl status recetas-app

# Ver logs en tiempo real
sudo tail -f /var/log/recetas-app.log

# Probar endpoint
curl http://localhost:8080/api/test
```

Desde tu navegador: `http://<EC2_PUBLIC_IP>:8080`

---

## Ejecucion local (desarrollo)

No necesitas cambiar nada. Por defecto usa el perfil `dev`:

```bash
# Desde el IDE o terminal
./mvnw spring-boot:run
```

Esto conecta a `localhost:3306/recetas_db` con `root/root`.

Si quieres probar el perfil de produccion localmente:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## Resumen de archivos de configuracion

| Archivo                         | Proposito                              |
|---------------------------------|----------------------------------------|
| `application.properties`        | Config comun (ambos perfiles)          |
| `application-dev.properties`    | BD local, logs detallados              |
| `application-prod.properties`   | BD RDS, logs minimos, CORS produccion  |
| `deploy.sh`                     | Script para desplegar en EC2           |
| `/opt/recetas-app/.env`         | Variables de entorno en el servidor    |

