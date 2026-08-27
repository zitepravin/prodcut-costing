#!/usr/bin/env bash
# CI / host verification — unit tests + optional host Flyway IT + frontend Vitest
# Usage:
#   ./scripts/ci-verify.sh
#   TEST_DB_URL=jdbc:mysql://127.0.0.1:3306/product_costing_it?... ./scripts/ci-verify.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# --- JDK 21 ---
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/javac" ]]; then
  for candidate in /tmp/jdk-21* "$HOME/.jdks/jdk-21"* /usr/lib/jvm/java-21-openjdk*; do
    if [[ -x "${candidate}/bin/javac" ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/javac" ]]; then
  echo "ERROR: JDK 21 with javac required" >&2
  exit 1
fi
export PATH="$JAVA_HOME/bin:${PATH}"

MVN="${MVN:-mvn}"
if ! command -v "$MVN" >/dev/null 2>&1; then
  if [[ -x /tmp/apache-maven-3.9.9/bin/mvn ]]; then
    MVN=/tmp/apache-maven-3.9.9/bin/mvn
  else
    echo "ERROR: mvn not found" >&2
    exit 1
  fi
fi

echo "=== 1/3 Backend unit tests (exclude docker-only default path) ==="
(
  cd backend
  # Run all tests; Flyway IT auto-skips if no TEST_DB_URL and no Docker
  "$MVN" -B test
)

echo "=== 2/3 Host Flyway IT (if TEST_DB_URL set) ==="
if [[ -n "${TEST_DB_URL:-}" ]]; then
  (
    cd backend
    "$MVN" -B -Dtest=FlywayMigrationIntegrationTest test
  )
  echo "Host Flyway IT: PASS"
else
  echo "SKIP host Flyway IT (set TEST_DB_URL to enable)"
fi

echo "=== 3/3 Frontend Vitest ==="
(
  cd frontend
  if [[ ! -d node_modules ]]; then
    npm ci
  fi
  npx vitest run
)

echo "=== ci-verify COMPLETE ==="
