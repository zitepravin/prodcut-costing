# Production JWT hardening

## Local (default)
- `spring.profiles.default=local` in `application.yml`
- `application-local.yml`: `app.security.require-strong-jwt-secret: false`
- Placeholder secret allowed for local demos only

## Production / staging
1. **Do not** use profile `local`.
2. Set environment:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod   # or omit local
   export JWT_SECRET='<cryptographically random string, >= 32 chars, ideally 64+>'
   export REQUIRE_STRONG_JWT_SECRET=true
   ```
3. `JwtSecretStartupValidator` fails fast if secret is missing, &lt;32 chars, or still the default placeholder when strong JWT is required.
4. Rotate `JWT_SECRET` via controlled change management; existing access tokens become invalid after rotation (clients must re-login).
5. Prefer secret injection via vault/KMS — never commit production secrets to the repository.
6. Keep MFA available for privileged roles (SYSTEM_ADMINISTRATOR, COSTING_MANAGER) per SEC policy.

## Checklist
- [ ] `JWT_SECRET` unique per environment
- [ ] `REQUIRE_STRONG_JWT_SECRET=true` outside local
- [ ] HTTPS only at reverse proxy
- [ ] CORS origins restricted (`app.security.cors-allowed-origins`)
- [ ] Swagger not public in production (`app.security.swagger-public=false`)

## application-prod.yml

Profile file ships with:
- `require-strong-jwt-secret: true`
- `swagger-public: false`
- `ddl-auto: validate`
- CORS from `CORS_ORIGINS` (default restrictive)

```bash
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET='...'
export CORS_ORIGINS='https://app.example.com'
```
