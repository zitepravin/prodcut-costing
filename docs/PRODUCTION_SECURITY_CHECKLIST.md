# Production Security Checklist

## Profile
- Activate: `SPRING_PROFILES_ACTIVE=prod`
- `application-prod.yml`: ddl-auto=validate, require-strong-jwt-secret=true, swagger-public=false

## Required environment
| Variable | Requirement |
|----------|-------------|
| `JWT_SECRET` | ≥32 chars, non-placeholder (startup fails if weak when require-strong-jwt-secret=true) |
| `CORS_ORIGINS` | Explicit frontend origin(s), comma-separated — not `*` |
| `DB_URL` / credentials | Production MySQL with TLS as required by ops |
| `HTTPS` | Terminate TLS at reverse proxy; backend may listen HTTP internally |

## Central Access
- Module APIs require `X-Customer-Id` + entitlement
- Frontend tenant selector sets `pcs.activeCustomerId` → `X-Customer-Id` header
- FE hide is not security; backend `@RequiresModule` + RBAC enforce

## Swagger
- Production: swagger-public=false (UI not publicly open)
- Local profile may enable swagger for developers

## Auth
- JWT access + refresh; token_version revocation
- MFA available for elevated accounts
- Portal uses separate portal JWT; supplierId from token only

## Do not
- Ship with default/local JWT secret
- Use CORS `*` in production
- Expose actuator beyond health without auth
