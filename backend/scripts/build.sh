#!/usr/bin/env bash
set -euo pipefail
# Prefer explicit JAVA_HOME; otherwise discover a JDK 21 with javac.
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/javac" ]]; then
  for candidate in /tmp/jdk-21* "$HOME/.jdks/jdk-21"* /usr/lib/jvm/java-21-openjdk*; do
    if [[ -x "${candidate}/bin/javac" ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/javac" ]]; then
  echo "ERROR: JDK 21 with javac not found. Install Temurin 21 and set JAVA_HOME." >&2
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"
echo "Using JAVA_HOME=$JAVA_HOME ($("$JAVA_HOME/bin/javac" -version 2>&1))"
exec mvn "$@"
