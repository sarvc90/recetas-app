# Deployment Guide

Production model: **Docker Compose on a single EC2 instance**, Spring Boot backend behind nginx, with Let's Encrypt TLS, Prometheus + Grafana behind nginx basic auth. RDS MySQL is the only off-host dependency.

GitHub Actions builds the JAR on a runner, ships a bundle to the EC2, and runs `docker compose up -d --build`.

---

## 1. Architecture

```
[ Browser ] ─HTTPS 443──▶ ┌──────────────────────────────────────┐
                          │  EC2 (Debian 13, user: admin)        │
                          │  ┌────────────────────────────────┐  │
                          │  │ nginx (recetas-frontend)       │  │
                          │  │  /            → static HTML    │  │
                          │  │  /api/        → backend:8080   │  │
                          │  │  /grafana/    → grafana:3000   │  │
                          │  │  /prometheus/ → prometheus:9090│  │
                          │  └────────────┬───────────────────┘  │
                          │               │ Docker network        │
                          │  ┌────────────┴────────────┐          │
                          │  │ backend (Spring Boot)   │──────────┼─JDBC TLS─▶ AWS RDS MySQL
                          │  │ /actuator/prometheus    │          │
                          │  └─────────────────────────┘          │
                          │  ┌─────────────┐ ┌─────────────────┐  │
                          │  │ prometheus  │ │ grafana         │  │
                          │  └─────────────┘ └─────────────────┘  │
                          │  ┌─────────────┐                      │
                          │  │ certbot     │ (TLS renewal loop)   │
                          │  └─────────────┘                      │
                          └──────────────────────────────────────┘
```

All inter-service traffic stays on the `recetas-network` Docker bridge. Only nginx publishes ports 80/443.

---

## 2. AWS prerequisites

### 2.1 RDS MySQL

| Setting | Value |
|---|---|
| Engine | MySQL 8.0 |
| Public access | No |
| VPC | Same as EC2 |
| Security group | Allow `3306/TCP` **only** from EC2's security group |

Create the schema once:
```bash
# From the EC2 (after RDS SG is wired up)
mysql -h <rds-endpoint> -u <admin-user> -p
mysql> CREATE DATABASE IF NOT EXISTS recetas_db;
```
Hibernate (`ddl-auto=update`) creates tables on first start.

### 2.2 EC2

| Setting | Value |
|---|---|
| AMI | Debian 13 (trixie) |
| User | `admin` |
| Inbound | `22` from your IP, `80` and `443` from `0.0.0.0/0` |
| Outbound | All |
| Egress to RDS | Allowed via SG rule above |

Do **not** open ports `9090`/`3000`. Prometheus/Grafana are reached only through nginx with basic auth.

### 2.3 DNS

Point `rincon-sabores.online` and `www.rincon-sabores.online` (A or AAAA) at the EC2's public IP. Required before TLS bootstrap (certbot's HTTP-01 challenge needs port 80 reachable on those names).

---

## 3. EC2 first-time provisioning

Run as `admin` on the EC2.

### 3.1 Install Docker + helpers

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg apache2-utils rsync git
# Docker Engine + Compose v2 (Debian official)
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker admin
# log out/in for group change to take effect, OR:
newgrp docker
```

### 3.2 Layout

```bash
sudo mkdir -p /opt/recetas/docker /opt/recetas/recetas-frontend
sudo chown -R admin:admin /opt/recetas
```

### 3.3 Place host-only secrets

Copy `recetas-app/recetas.env.example` from the repo to the EC2 as `/opt/recetas/docker/recetas.env`, then fill in real values:

```bash
nano /opt/recetas/docker/recetas.env
chmod 600 /opt/recetas/docker/recetas.env
```

Required keys: `SPRING_PROFILES_ACTIVE=prod`, `DB_*`, `JWT_SECRET`, `SMTP_*`, `CLOUDINARY_*`, `STRIPE_*`, `CORS_ALLOWED_ORIGINS`, `JAVA_OPTS`, `GF_SECURITY_ADMIN_USER`, `GF_SECURITY_ADMIN_PASSWORD`.

### 3.4 Create htpasswd for /grafana and /prometheus

```bash
sudo mkdir -p /opt/recetas/docker/frontend/nginx
htpasswd -c /opt/recetas/docker/frontend/nginx/.htpasswd <admin-username>
chmod 640 /opt/recetas/docker/frontend/nginx/.htpasswd
```
Use a strong password (different from Grafana's).

### 3.5 First-run TLS bootstrap

nginx fails to start without certs, but certbot needs nginx to serve the HTTP-01 challenge. The `init-letsencrypt.sh` script breaks the chicken-and-egg by issuing a self-signed dummy first.

This must run **once before the first GitHub Actions deploy**:

```bash
# Pull just the docker/ + scripts/ trees onto the host one time
cd /tmp && git clone https://github.com/sarvc90/recetas-app.git recetas-bootstrap
cd recetas-bootstrap
# Copy docker/ skeleton + script
sudo rsync -a docker/ /opt/recetas/docker/ \
  --exclude 'backend/app.jar' \
  --exclude '.htpasswd' \
  --exclude 'recetas.env'
sudo cp scripts/init-letsencrypt.sh /opt/recetas/scripts/init-letsencrypt.sh
sudo chmod +x /opt/recetas/scripts/init-letsencrypt.sh

# Confirm staging=0 in the script (production cert), then run:
cd /opt/recetas
sudo bash scripts/init-letsencrypt.sh
```

Verify `https://rincon-sabores.online/` shows the static page (TLS green). After this, GH Actions takes over.

The `certbot` service in compose renews automatically every 12h.

---

## 4. GitHub repository configuration

### 4.1 Required secrets

Settings → Secrets and variables → Actions → New repository secret:

| Name | Value |
|---|---|
| `EC2_HOST` | EC2 public DNS or IP |
| `EC2_USER` | `admin` |
| `EC2_SSH_KEY` | Contents of the SSH private key (full PEM, including `BEGIN`/`END` lines) |
| `EC2_PORT` | `22` |

### 4.2 Optional: production environment with approval gate

Settings → Environments → New environment → `production`. Add required reviewers. The workflow's `deploy` job already references `environment: production`, so PRs to master will pause for approval.

---

## 5. Local development

### 5.1 Backend

```bash
cd recetas-app
cp recetas.env.example recetas.env     # gitignored
# Fill JWT_SECRET, DB creds (local MySQL/MariaDB), test Stripe keys, etc.
./mvnw spring-boot:run                  # picks up dev profile by default
```

`recetas.env` is loaded automatically via `spring.config.import=optional:file:./recetas.env[.properties]` declared in `application.properties`.

To exercise the prod profile against a local MySQL:
```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

### 5.2 Frontend

Static HTML/CSS/JS in `recetas-frontend/`. Open `index.html` directly, or:
```bash
cd recetas-frontend && python3 -m http.server 80
```

### 5.3 Full stack via Docker (locally)

```bash
# 1. Build the JAR
cd recetas-app
./mvnw -B clean package -DskipTests
cp target/recetas.jar ../docker/backend/app.jar

# 2. Provide a local recetas.env at docker/recetas.env (NOT the one inside recetas-app/)
cp recetas-app/recetas.env.example docker/recetas.env
nano docker/recetas.env

# 3. Skip TLS locally — for a smoke test, point a curl at backend directly:
cd docker
docker compose build
docker compose up -d backend prometheus grafana
docker compose exec backend curl -s http://127.0.0.1:8080/actuator/health
```
Running the `frontend` service locally requires either swapping `recetas.conf` for an HTTP-only variant or supplying dummy certs. For local dev prefer running nginx outside Docker against the static files directly.

---

## 6. Deploy flow (GH Actions)

`.github/workflows/deploy.yml` triggers on push to `master` (or manually via `workflow_dispatch`).

1. **build** job (Ubuntu runner)
   - Set up JDK 21 with maven cache.
   - `./mvnw clean package -DskipTests` → `recetas-app/target/recetas.jar`.
   - Stages a bundle:
     ```
     release/
     ├── docker/             (full docker/ tree from repo)
     │   └── backend/app.jar (built JAR placed here)
     └── recetas-frontend/   (static assets)
     ```
   - Uploads `release.tar.gz` artifact.

2. **deploy** job
   - Downloads the bundle.
   - SCPs it to `/tmp/recetas-deploy/release.tar.gz` on EC2.
   - SSH script:
     - Asserts `/opt/recetas/docker/recetas.env` and `frontend/nginx/.htpasswd` exist (provisioned in §3, never shipped by CI).
     - Extracts to a temp dir.
     - `rsync -a --delete` into `/opt/recetas/docker/` and `/opt/recetas/recetas-frontend/`, **excluding** `recetas.env`, `.htpasswd`, `certbot/`, `prometheus-data/`, `grafana-data/`. Stateful host data survives.
     - `docker compose build --pull && docker compose up -d --remove-orphans`.
     - Polls `actuator/health` inside the backend container for up to ~60s.
     - Dumps backend logs and exits non-zero on failure.
   - Always cleans `/tmp/recetas-deploy`.

`concurrency.group: deploy-ec2` serializes overlapping runs.

---

## 7. Configuration matrix

| File | Tracked | Purpose |
|---|---|---|
| `recetas-app/src/main/resources/application.properties` | yes | Common config; references `${VAR}` for all secrets, no fallbacks |
| `application-dev.properties` | yes | Local DB on `localhost:3306`, verbose logs |
| `application-prod.properties` | yes | DB / CORS from env; actuator + Prometheus exposed |
| `recetas-app/recetas.env.example` | yes | Template — copy to `recetas.env` locally |
| `recetas-app/recetas.env` | **no** | Local dev secrets (gitignored) |
| `/opt/recetas/docker/recetas.env` | **no** | Production secrets on EC2 (manual, §3.3) |
| `docker/frontend/nginx/.htpasswd` | **no** | Basic-auth users for `/grafana` and `/prometheus` (manual, §3.4) |
| `docker/frontend/certbot/conf/` | **no** | Let's Encrypt cert state (managed by certbot container) |

---

## 8. Operations cheatsheet

```bash
# SSH in
ssh admin@<EC2>

# View running services
cd /opt/recetas/docker && docker compose ps

# Tail logs
docker compose logs -f backend
docker compose logs -f frontend

# Restart one service after a config change
docker compose up -d --force-recreate frontend

# Force-renew TLS (debug)
docker compose run --rm certbot renew --force-renewal
docker compose exec frontend nginx -s reload

# Rotate a secret
nano recetas.env
docker compose up -d backend     # picks up new env

# Add a Grafana/Prometheus user
htpasswd /opt/recetas/docker/frontend/nginx/.htpasswd <user>
docker compose exec frontend nginx -s reload
```

---

## 9. Security notes

- Secrets never enter Git history. `recetas.env` is gitignored at both possible locations; CI does not ship it.
- Backend port `8080` is `expose:` only — not published. nginx is the only ingress.
- Prometheus and Grafana likewise never published; nginx fronts them with basic auth + TLS.
- Backend container runs as non-root (`recetas` user, alpine).
- Container does not use `privileged` mode; no systemd inside containers.
- TLS termination at nginx with Mozilla Intermediate cipher list, HSTS preload, HTTP/2.
- RDS is reachable only from the EC2's security group, and JDBC enforces `useSSL=true`.
- Rotate the secrets that were committed in earlier `application.properties` revisions (Cloudinary, SMTP app password, Stripe test keys, JWT secret) — assume they are public.
