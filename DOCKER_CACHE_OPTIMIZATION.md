# Docker build cache optimization (MASTER-V253)

## Backend
| Layer | When it rebuilds |
|-------|------------------|
| `pom.xml` + `dependency:go-offline` | Only when pom dependencies change |
| `src/main` + `package` | When production Java/resources change |
| Runtime image | When jar or JRE base changes |

- **BuildKit cache mount** `/root/.m2` — dependencies not re-downloaded across builds
- **Tests excluded** from image context (`src/test` in `.dockerignore`)
- Runtime: non-root `app` user + container RAM percentage

## Frontend
| Layer | When it rebuilds |
|-------|------------------|
| `package*.json` + `npm ci` | Only when lockfile/deps change |
| `src` + vite build | When app source changes |

- **BuildKit cache mount** `/root/.npm`
- Context excludes `e2e`, tests, `node_modules`, `dist`

## Host
```bash
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
docker compose build
# or
docker compose build --parallel
```

Requires Docker BuildKit (Docker 23+ default on most installs).
