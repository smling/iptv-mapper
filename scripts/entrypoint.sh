#!/usr/bin/env sh
set -eu

AGENT_DIR=${OTEL_AGENT_DIR:-/otel}
AGENT_PATH="$AGENT_DIR/opentelemetry-javaagent.jar"
DEFAULT_OTEL_URL="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.8.0/opentelemetry-javaagent.jar"
OTEL_URL=${OTEL_JAVA_AGENT_URL:-$DEFAULT_OTEL_URL}

mkdir -p "$AGENT_DIR" || true

if [ ! -f "$AGENT_PATH" ]; then
  echo "[entrypoint] OpenTelemetry agent not found at $AGENT_PATH; downloading..."
  if command -v curl >/dev/null 2>&1; then
    if curl -fsSL "$OTEL_URL" -o "$AGENT_PATH"; then
      echo "[entrypoint] OpenTelemetry agent downloaded to $AGENT_PATH"
    else
      echo "[entrypoint] WARNING: Failed to download OTEL agent from $OTEL_URL; continuing without agent" >&2
    fi
  else
    echo "[entrypoint] WARNING: curl not available; cannot download OTEL agent; continuing without agent" >&2
  fi
fi

# Ensure -javaagent is present if agent exists and not already set
if [ -f "$AGENT_PATH" ]; then
  case "${JAVA_TOOL_OPTIONS:-}" in
    *-javaagent:*) : ;; # already present
    *) export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -javaagent:$AGENT_PATH" ;;
  esac
fi

exec sh -lc "exec java $JAVA_OPTS -Duser.timezone=$TZ -jar /app/app.jar"

