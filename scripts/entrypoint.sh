#!/bin/sh
set -eu

DEFAULT_DIR="/tmp/otel"                            # writable on all images
AGENT_DIR="${OTEL_AGENT_DIR:-$DEFAULT_DIR}"
AGENT_PATH="$AGENT_DIR/opentelemetry-javaagent.jar"
DEFAULT_OTEL_URL="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.8.0/opentelemetry-javaagent.jar"
OTEL_URL="${OTEL_JAVA_AGENT_URL:-$DEFAULT_OTEL_URL}"
TZ_VAL="${TZ:-UTC}"

ensure_writable_dir() {
  mkdir -p "$AGENT_DIR" 2>/dev/null || :
  # test writeability
  if ! touch "$AGENT_DIR/.write_test" 2>/dev/null; then
    echo "[entrypoint] WARN: $AGENT_DIR is not writable. Falling back to $DEFAULT_DIR"
    AGENT_DIR="$DEFAULT_DIR"
    AGENT_PATH="$AGENT_DIR/opentelemetry-javaagent.jar"
    mkdir -p "$AGENT_DIR"
    touch "$AGENT_DIR/.write_test" || {
      echo "[entrypoint] ERROR: cannot write to $AGENT_DIR. Check permissions/bind mount/readonly FS." >&2
      exit 1
    }
  fi
  rm -f "$AGENT_DIR/.write_test" 2>/dev/null || :
}

download_agent() {
  echo "[entrypoint] Downloading OTEL agent: $OTEL_URL -> $AGENT_PATH"
  TMP="$AGENT_PATH.part"

  # quick space check (best-effort; requires 'df')
  if command -v df >/dev/null 2>&1; then
    avail_k=$(df -Pk "$AGENT_DIR" | awk 'NR==2{print $4}')
    # need at least ~10 MB
    if [ "${avail_k:-0}" -lt 10240 ]; then
      echo "[entrypoint] ERROR: Not enough space in $AGENT_DIR" >&2
      return 1
    fi
  fi

  if command -v curl >/dev/null 2>&1; then
    # IMPORTANT: write to a temp file in the SAME dir, then mv
    # curl (23) happens if it can’t write; capture stderr for hints
    if ! curl -fL --retry 5 --retry-connrefused --connect-timeout 15 \
      -o "$TMP" "$OTEL_URL" 2>"/tmp/otel_curl_err.$$"; then
      echo "[entrypoint] ERROR: curl failed ($(cat /tmp/otel_curl_err.$$))" >&2
      rm -f "$TMP" 2>/dev/null || :
      return 1
    fi
    rm -f "/tmp/otel_curl_err.$$" 2>/dev/null || :
  elif command -v wget >/dev/null 2>&1; then
    if ! wget -O "$TMP" "$OTEL_URL"; then
      rm -f "$TMP" 2>/dev/null || :
      return 1
    fi
  else
    echo "[entrypoint] ERROR: neither curl nor wget available." >&2
    return 1
  fi

  # sanity size check (>100 KB)
  size=$(wc -c < "$TMP" 2>/dev/null || echo 0)
  if [ "$size" -lt 102400 ]; then
    echo "[entrypoint] ERROR: downloaded file too small ($size bytes). Aborting." >&2
    rm -f "$TMP"
    return 1
  fi

  mv -f "$TMP" "$AGENT_PATH"
  echo "[entrypoint] Saved agent ($size bytes) to $AGENT_PATH"
}

ensure_writable_dir

if [ ! -f "$AGENT_PATH" ]; then
  if ! download_agent; then
    echo "[entrypoint] WARNING: proceeding without OTEL agent" >&2
  fi
fi

# Add -javaagent if the file exists and isn't already present
if [ -f "$AGENT_PATH" ]; then
  case "${JAVA_TOOL_OPTIONS:-}" in
    *-javaagent:*) : ;;
    *) export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -javaagent:$AGENT_PATH" ;;
  esac
fi

echo "[entrypoint] Starting app..."
exec sh -lc "exec java ${JAVA_OPTS:-} -Duser.timezone=$TZ_VAL -jar /app/app.jar"
