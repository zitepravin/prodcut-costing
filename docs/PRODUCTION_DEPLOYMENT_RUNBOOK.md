# Production Deployment Runbook

**System:** Product Costing + Supplier Intelligence Platform  
**Stack:** Java 21 / Spring Boot · React + Vite · MySQL 8 / MariaDB 10.11+  
**Profiles:** `local` (dev) · `prod` (production)

---

## 1. Preconditions

| Item | Requirement |
|------|-------------|
| JDK | 21+ (Temurin recommended) |
| Maven | 3.9+ |
| Node | 20+ (frontend build) |
| Database | MySQL 8 or MariaDB 10.11+, UTF8MB4 |
| Reverse proxy | TLS termination (nginx/ALB/etc.) |
| Secrets | Injected via env — never committed |

---

## 2. Database

```bash
# Create schema + app user (example)
CREATE DATABASE product_costing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'costing_app'@'%' IDENTIFIED BY '<strong-password>';
GRANT ALL ON product_costing.* TO 'costing_app'@'%';
FLUSH PRIVILEGES;
```

Flyway runs automatically on application start (`spring.flyway.enabled=true`).  
Migrations: `backend/src/main/resources/db/migration` **V1 → V90** (current).

**Do not** set `ddl-auto=update` in production. Prod profile uses `validate`.

Verify after first start:

```sql
SELECT version, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
-- Expect latest version = 90, success = 1
```

---

## 3. Backend build & run

```bash
cd backend
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:mysql://<host>:3306/product_costing?useSSL=true&requireSSL=true&serverTimezone=UTC&characterEncoding=UTF-8'
export DB_USERNAME=costing_app
export DB_PASSWORD='<strong-password>'
export JWT_SECRET='<random-secret-at-least-32-characters>'
export CORS_ORIGINS='https://app.example.com'
export REQUIRE_STRONG_JWT_SECRET=true
export SWAGGER_PUBLIC=false
export SERVER_PORT=8080

mvn -DskipTests package
java -jar target/product-costing-backend-*.jar
# or: mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Optional notification email

```bash
export NOTIFICATION_EMAIL_ENABLED=true
export SMTP_HOST=smtp.example.com
export SMTP_PORT=587
export SMTP_USERNAME=...
export SMTP_PASSWORD=...
# If email enabled, consider management.health.mail.enabled=true
```

By default **mail health is disabled** so missing SMTP does not mark readiness DOWN.

---

## 4. Frontend build & serve

```bash
cd frontend
npm ci
# Point API via env at build time if used (check vite config / proxy)
npm run build
# Serve dist/ via nginx or CDN; proxy /api → backend
```

Example nginx location:

```nginx
location /api/ {
  proxy_pass http://127.0.0.1:8080/api/;
  proxy_set_header Host $host;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header X-Forwarded-Proto $scheme;
}
location /actuator/health {
  proxy_pass http://127.0.0.1:8080/actuator/health;
}
location / {
  root /var/www/product-costing/dist;
  try_files $uri /index.html;
}
```

---

## 5. Readiness checks

| Check | Expect |
|-------|--------|
| `GET /actuator/health` | **200** `{"status":"UP"}` |
| `POST /api/v1/auth/login` | **200** with accessToken |
| `GET /api/v1/platform/modules` (admin JWT) | **200** module list |
| Module API without `X-Customer-Id` | **403** ACCESS-07 |
| Module API with disabled entitlement | **403** ACCESS-03 |

---

## 6. Central Access (first tenant)

1. Login as seeded `admin` (change password immediately in production).  
2. Platform Admin → Customers: confirm DEFAULT or create tenant.  
3. Enable modules: `PRODUCT_COSTING`, `VENDOR_SELECTION`.  
4. Create users / assign roles; membership ACTIVE.  
5. UI tenant selector sets `X-Customer-Id` for module-scoped APIs.

---

## 7. Security baseline (prod)

See also `docs/PRODUCTION_SECURITY_CHECKLIST.md`.

- Strong `JWT_SECRET` (≥32 chars) — startup fails if weak when `require-strong-jwt-secret=true`
- Explicit `CORS_ORIGINS` (not `*`)
- `swagger-public=false`
- HTTPS at edge
- Actuator: only `health` / `info` exposed; do not expose env/beans publicly
- Rotate admin credentials; enable MFA for privileged users where available

---

## 8. Rollback

1. Stop new application process.  
2. Redeploy previous jar + previous frontend `dist`.  
3. **Do not** reverse Flyway migrations unless a tested down-script exists (none by default — prefer forward fixes).  
4. Restore DB from backup only if a migration corrupted data (standard backup RPO applies).

---

## 9. Docker note

`FlywayMigrationIntegrationTest` uses Testcontainers and requires Docker.  
When Docker is unavailable, validate migrations with **host MySQL/MariaDB** (same as this runbook §2–§5). Host validation through V90 is documented in `docs/FULL_SCALE_QA_REPORT.md` / `QA_MASTER_STATUS.md`.

---

## 10. Smoke script (post-deploy)

```bash
BASE=https://api.example.com
curl -sf "$BASE/actuator/health" | grep -q '"status":"UP"'
TOKEN=$(curl -sf -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<secret>"}' | jq -r .accessToken)
curl -sf -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/platform/modules" | head -c 200
echo OK
```
