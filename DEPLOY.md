# Deployment Guide

Production model: **Docker Compose on a single EC2 instance**. Spring Boot backend behind an internal nginx static-server, fronted by **`jwilder/nginx-proxy`** with **`jrcs/letsencrypt-nginx-proxy-companion`** for automatic TLS. Prometheus + Grafana sit on the internal network and are reached through the internal nginx with basic auth. RDS MySQL is the only off-host dependency.

GitHub Actions builds the JAR on a runner, ships a bundle to the EC2, and runs `docker compose up -d --build`.

---

## 1. Architecture

```
[ Browser ] ─HTTPS 443──▶ ┌──────────────────────────────────────────────┐
                          │  EC2 (Debian 13, user: admin)                │
                          │  ┌──────────────────────────────────────┐    │
                          │  │ nginx-proxy (jwilder)                │    │
                          │  │  + letsencrypt-companion (jrcs)      │    │
                          │  │  TLS termination, auto-cert via      │    │
                          │  │  VIRTUAL_HOST / LETSENCRYPT_HOST     │    │
                          │  └──────────────┬───────────────────────┘    │
                          │                 │ Docker network              │
                          │  ┌──────────────┴──────────────────────┐     │
                          │  │ frontend (nginx)                    │     │
                          │  │  /            → static HTML         │     │
                          │  │  /api/        → backend:8080        │     │
                          │  │  /grafana/    → grafana:3000        │     │
                          │  │  /prometheus/ → prometheus:9090     │     │
                          │  └──────────────┬──────────────────────┘     │
                          │  ┌──────────────┴──────┐                     │
                          │  │ backend (Spring)    │─────JDBC TLS───▶ AWS RDS MySQL
                          │  │ /actuator/prometheus│                     │
                          │  └─────────────────────┘                     │
                          │  ┌─────────────┐ ┌─────────────────┐         │
                          │  │ prometheus  │ │ grafana         │         │
                          │  └─────────────┘ └─────────────────┘         │
                          └──────────────────────────────────────────────┘
```

Inter-service traffic stays on the `recetas-network` Docker bridge. Only `nginx-proxy` publishes ports 80/443. TLS bootstrap is automatic — no manual certbot steps.

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

Do **not** open `9090` / `3000` / `8080`. Backend, Prometheus and Grafana are reached only through the internal nginx with basic auth, fronted by `nginx-proxy` over TLS.

### 2.3 DNS

Point `rincon-sabores.online` and `www.rincon-sabores.online` (A or AAAA) at the EC2's public IP. Required before the first `docker compose up` so the letsencrypt-companion's HTTP-01 challenge succeeds.

---

## 3. EC2 first-time provisioning

Run as `admin` on the EC2.

### 3.1 Install Docker

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
newgrp docker
```

### 3.2 Layout

```bash
sudo mkdir -p /opt/recetas/docker
sudo chown -R admin:admin /opt/recetas
```

CI rsyncs the full `docker/` tree here on every deploy. No separate static-frontend dir on the host — static files are bundled into `docker/frontend/nginx/www/` by CI and mounted into the `frontend` container.

### 3.3 Place host-only secrets

Copy `recetas-app/recetas.env.example` from the repo to the EC2 as `/opt/recetas/docker/recetas.env`, then fill in real values:

```bash
nano /opt/recetas/docker/recetas.env
chmod 600 /opt/recetas/docker/recetas.env
```

Required keys: `SPRING_PROFILES_ACTIVE=prod`, `DB_*`, `JWT_SECRET`, `SMTP_*`, `CLOUDINARY_*`, `STRIPE_*`, `CORS_ALLOWED_ORIGINS`, `JAVA_OPTS`, `GF_SECURITY_ADMIN_USER`, `GF_SECURITY_ADMIN_PASSWORD`.

### 3.4 Create htpasswd for /grafana and /prometheus

The `frontend` service mounts `/home/admin/.htpasswd` directly from the host (not from the `docker/` tree, so it survives any `rsync --delete`).

```bash
htpasswd -c /home/admin/.htpasswd admin
chmod 640 /home/admin/.htpasswd
```

Use a strong password (different from Grafana's).

### 3.5 First boot

No manual TLS bootstrap. `nginx-proxy` + `letsencrypt-companion` issue and renew certs automatically once DNS resolves to the EC2 and ports 80/443 are open. Just trigger the GitHub Actions workflow (push to `master` or `workflow_dispatch`).

Verify after the first deploy:
```bash
curl -I https://rincon-sabores.online/
```

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

Settings → Environments → New environment → `production`. Add required reviewers. The workflow's `deploy` job already references `environment: production`.

---

## 5. Local development

### 5.1 Backend

```bash
cd recetas-app
cp recetas.env.example recetas.env     # gitignored
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
cd recetas-frontend && python3 -m http.server 8080
```

### 5.3 Full stack via Docker (locally)

```bash
# 1. Build the JAR
cd recetas-app
./mvnw -B clean package -DskipTests
cp target/recetas.jar ../docker/backend/app.jar

# 2. Stage static files where the frontend container expects them
mkdir -p ../docker/frontend/nginx/www
cp -r ../recetas-frontend/. ../docker/frontend/nginx/www/

# 3. Provide a local recetas.env at docker/recetas.env
cp recetas.env.example ../docker/recetas.env
nano ../docker/recetas.env

# 4. Stub the htpasswd path (compose mounts /home/admin/.htpasswd)
sudo touch /home/admin/.htpasswd
htpasswd -bc /home/admin/.htpasswd admin admin

# 5. Run backend + observability only (skip nginx-proxy/letsencrypt for HTTP smoke test)
cd ../docker
docker compose build
docker compose up -d backend prometheus grafana
docker compose exec backend wget -qO- http://127.0.0.1:8080/actuator/health
```

For a full local run with TLS, point a local DNS entry (`/etc/hosts`) at `127.0.0.1` and let `letsencrypt-companion` use a staging endpoint, or terminate TLS upstream.

---

## 6. Deploy flow (GH Actions)

`.github/workflows/deploy.yml` triggers on push to `master` (or manually via `workflow_dispatch`).

1. **build** job (Ubuntu runner)
   - JDK 21 with maven cache.
   - `./mvnw clean package -DskipTests` → `recetas-app/target/recetas.jar`.
   - Stages a single bundle:
     ```
     release/
     └── docker/                       (full docker/ tree from repo, minus stateful dirs)
         ├── backend/app.jar           (built JAR placed here)
         └── frontend/nginx/www/       (recetas-frontend/ copied here)
     ```
   - Uploads `release.tar.gz` artifact.

2. **deploy** job
   - Downloads the bundle.
   - SCPs it to `/tmp/recetas-deploy/release.tar.gz` on EC2.
   - SSH script:
     - Asserts `/opt/recetas/docker/recetas.env` and `/home/admin/.htpasswd` exist (provisioned in §3, never shipped by CI).
     - Extracts to a temp dir.
     - `rsync -a --delete` into `/opt/recetas/docker/`, **excluding** `recetas.env`, `frontend/nginx/certs/`, `observability/prometheus/prometheus-data/`, `observability/grafana/grafana-data/`. TLS state and named-volume seed dirs survive.
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
| `/home/admin/.htpasswd` | **no** | Basic-auth users for `/grafana` and `/prometheus` (manual, §3.4) |
| `docker/frontend/nginx/certs/` | **no** | Let's Encrypt cert state (managed by `letsencrypt-companion`) |
| `prometheus-data` / `grafana-data` (named volumes) | **no** | Persistent metrics + dashboard state |

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
docker compose logs -f nginx-proxy
docker compose logs -f letsencrypt

# Restart one service after a config change
docker compose up -d --force-recreate frontend

# Force TLS renewal (debug)
docker compose exec letsencrypt /app/force_renew

# Rotate a secret
nano recetas.env
docker compose up -d backend     # picks up new env

# Add a Grafana/Prometheus user
sudo htpasswd /home/admin/.htpasswd <user>
docker compose exec frontend nginx -s reload
```

---

## 9. Security notes

- Secrets never enter Git history. `recetas.env` is gitignored at both possible locations; CI does not ship it.
- Backend port `8080` is `expose:` only — not published. The internal `frontend` nginx is the only ingress to the backend; `nginx-proxy` is the only ingress to the host.
- Prometheus and Grafana are likewise never published; the internal `frontend` nginx fronts them with basic auth, and `nginx-proxy` fronts that with TLS.
- Backend container runs as non-root (`recetas` user, alpine).
- No container uses `privileged` mode; no systemd inside containers.
- `nginx-proxy` and `letsencrypt-companion` need `/var/run/docker.sock` (read-only) to discover services via `VIRTUAL_HOST`. Treat the host as part of the trust boundary.
- TLS termination at `nginx-proxy` with auto-renewed Let's Encrypt certs.
- Internal `frontend` nginx adds HSTS, CSP, X-Frame-Options, Referrer-Policy, Permissions-Policy.
- RDS is reachable only from the EC2's security group, and JDBC enforces `useSSL=true`.
- Rotate any secrets that were committed in earlier `application.properties` revisions (Cloudinary, SMTP app password, Stripe test keys, JWT secret) — assume they are public.
