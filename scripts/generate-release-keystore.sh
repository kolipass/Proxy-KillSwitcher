#!/usr/bin/env bash
# ==============================================================================
# Generate a new Android Release Keystore for ProxyKillSwitcher
# ==============================================================================
set -euo pipefail

KEYSTORE_PATH="${1:-release.keystore}"
KEY_ALIAS="${2:-proxykillswitcher}"

echo "=================================================="
echo " Generating Release Keystore: ${KEYSTORE_PATH}"
echo " Key Alias:                   ${KEY_ALIAS}"
echo "=================================================="

if [[ -f "${KEYSTORE_PATH}" ]]; then
    echo "Error: Keystore file '${KEYSTORE_PATH}' already exists!" >&2
    exit 1
fi

keytool -genkeypair -v \
    -keystore "${KEYSTORE_PATH}" \
    -alias "${KEY_ALIAS}" \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000 \
    -storetype PKCS12

echo ""
echo "=================================================="
echo " Keystore successfully created: ${KEYSTORE_PATH}"
echo " To encode for GitHub Secrets (RELEASE_KEYSTORE_BASE64):"
echo ""
echo "   base64 -i \"${KEYSTORE_PATH}\" | pbcopy"
echo "   (or: base64 -w 0 \"${KEYSTORE_PATH}\" on Linux)"
echo "=================================================="
