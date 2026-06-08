#!/usr/bin/env bash
#
# One command to build, sign, and publish the plugin to the JetBrains Marketplace.
# Secrets are read from an env file (default: publish.env) that is git-ignored.
#
# Usage:
#   cp publish.env.example publish.env   # then fill it in
#   ./publish.sh                         # uses ./publish.env
#   ./publish.sh path/to/other.env       # or a different env file
#
set -euo pipefail
cd "$(dirname "$0")"

ENV_FILE="${1:-publish.env}"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: env file '$ENV_FILE' not found." >&2
  echo "Copy publish.env.example to publish.env and fill in your values." >&2
  exit 1
fi

# Load the env file, exporting everything it defines.
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

# Allow signing material to be given either inline (PRIVATE_KEY / CERTIFICATE_CHAIN)
# or as file paths (PRIVATE_KEY_FILE / CERTIFICATE_CHAIN_FILE).
if [[ -z "${PRIVATE_KEY:-}" && -n "${PRIVATE_KEY_FILE:-}" ]]; then
  PRIVATE_KEY="$(cat "$PRIVATE_KEY_FILE")"; export PRIVATE_KEY
fi
if [[ -z "${CERTIFICATE_CHAIN:-}" && -n "${CERTIFICATE_CHAIN_FILE:-}" ]]; then
  CERTIFICATE_CHAIN="$(cat "$CERTIFICATE_CHAIN_FILE")"; export CERTIFICATE_CHAIN
fi

# Validate required values up front.
missing=()
[[ -z "${PUBLISH_TOKEN:-}" ]]        && missing+=("PUBLISH_TOKEN")
[[ -z "${PRIVATE_KEY:-}" ]]          && missing+=("PRIVATE_KEY (or PRIVATE_KEY_FILE)")
[[ -z "${CERTIFICATE_CHAIN:-}" ]]    && missing+=("CERTIFICATE_CHAIN (or CERTIFICATE_CHAIN_FILE)")
[[ -z "${PRIVATE_KEY_PASSWORD:-}" ]] && missing+=("PRIVATE_KEY_PASSWORD")
if (( ${#missing[@]} > 0 )); then
  echo "ERROR: missing required values in '$ENV_FILE': ${missing[*]}" >&2
  exit 1
fi

# Gradle must run on JDK 17-21 (not 25). Let the env file point at a suitable JDK.
if [[ -n "${PUBLISH_JAVA_HOME:-}" ]]; then
  export JAVA_HOME="$PUBLISH_JAVA_HOME"
fi
[[ -n "${JAVA_HOME:-}" ]] && echo "Using JAVA_HOME=$JAVA_HOME"

echo "==> Building, testing, signing, and publishing to the JetBrains Marketplace..."
./gradlew clean test verifyPluginProjectConfiguration publishPlugin
echo "==> Done. Check https://plugins.jetbrains.com/author/me for the upload."
