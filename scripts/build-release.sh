#!/usr/bin/env bash
# ==============================================================================
# ProxyKillSwitcher Release Build Script
# Builds minified, optimized Release APK and AAB locally.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

VERSION_NAME="${VERSION_NAME:-"0.1.0"}"
VERSION_CODE="${VERSION_CODE:-"1"}"
SKIP_TESTS=false
BUILD_TARGET="all" # all, apk, bundle

print_help() {
    cat <<EOF
Usage: ./scripts/build-release.sh [OPTIONS]

Options:
  --version-name <name>  Set release version name (default: \$VERSION_NAME or 0.1.0)
  --version-code <code>  Set release version code (default: \$VERSION_CODE or 1)
  --apk                  Build Release APK only
  --bundle               Build Release AAB (App Bundle) only
  --skip-tests           Skip ktlint, detekt and unit test checks
  -h, --help             Show this help message

Environment variables for signing:
  RELEASE_KEYSTORE_PATH      Path to release keystore (.jks/.keystore)
  RELEASE_KEYSTORE_PASSWORD  Keystore password
  RELEASE_KEY_ALIAS          Key alias
  RELEASE_KEY_PASSWORD       Key password
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version-name)
            VERSION_NAME="$2"
            shift 2
            ;;
        --version-code)
            VERSION_CODE="$2"
            shift 2
            ;;
        --apk)
            BUILD_TARGET="apk"
            shift
            ;;
        --bundle)
            BUILD_TARGET="bundle"
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -h|--help)
            print_help
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            print_help
            exit 1
            ;;
    esac
done

cd "${PROJECT_ROOT}"

echo "=================================================="
echo " ProxyKillSwitcher - Building Release Artifacts"
echo " Version Name: ${VERSION_NAME}"
echo " Version Code: ${VERSION_CODE}"
echo " Target:       ${BUILD_TARGET}"
echo "=================================================="

# Ensure workspace-local Gradle user home if outside standard setup
if [[ -z "${GRADLE_USER_HOME:-}" && ! -w "${HOME}/.gradle" ]]; then
    export GRADLE_USER_HOME="${PROJECT_ROOT}/.gradle-home"
fi

# Verification
if [[ "${SKIP_TESTS}" == "false" ]]; then
    echo "==> Running quality checks & tests..."
    ./gradlew ktlintCheck detekt test
    echo "==> All checks passed!"
else
    echo "==> Skipping verification tests (--skip-tests specified)"
fi

# Build tasks
TASKS=()
case "${BUILD_TARGET}" in
    apk)
        TASKS+=(":app:assembleRelease")
        ;;
    bundle)
        TASKS+=(":app:bundleRelease")
        ;;
    all)
        TASKS+=(":app:assembleRelease" ":app:bundleRelease")
        ;;
esac

echo "==> Executing Gradle build: ${TASKS[*]}..."
VERSION_NAME="${VERSION_NAME}" VERSION_CODE="${VERSION_CODE}" ./gradlew "${TASKS[@]}"

# Destination output folder
OUT_DIR="${PROJECT_ROOT}/build/release-dist"
mkdir -p "${OUT_DIR}"

echo "==> Collecting release artifacts into ${OUT_DIR}..."

if [[ "${BUILD_TARGET}" == "apk" || "${BUILD_TARGET}" == "all" ]]; then
    APK_SRC="${PROJECT_ROOT}/app/build/outputs/apk/release/app-release.apk"
    if [[ -f "${APK_SRC}" ]]; then
        APK_DEST="${OUT_DIR}/proxykillswitcher-v${VERSION_NAME}-release.apk"
        cp "${APK_SRC}" "${APK_DEST}"
        echo "  [APK]    ${APK_DEST} ($(du -h "${APK_DEST}" | cut -f1))"
    fi
fi

if [[ "${BUILD_TARGET}" == "bundle" || "${BUILD_TARGET}" == "all" ]]; then
    AAB_SRC="${PROJECT_ROOT}/app/build/outputs/bundle/release/app-release.aab"
    if [[ -f "${AAB_SRC}" ]]; then
        AAB_DEST="${OUT_DIR}/proxykillswitcher-v${VERSION_NAME}-release.aab"
        cp "${AAB_SRC}" "${AAB_DEST}"
        echo "  [Bundle] ${AAB_DEST} ($(du -h "${AAB_DEST}" | cut -f1))"
    fi
fi

MAPPING_SRC="${PROJECT_ROOT}/app/build/outputs/mapping/release/mapping.txt"
if [[ -f "${MAPPING_SRC}" ]]; then
    MAPPING_DEST="${OUT_DIR}/mapping-v${VERSION_NAME}.txt"
    cp "${MAPPING_SRC}" "${MAPPING_DEST}"
    echo "  [Mapping] ${MAPPING_DEST}"
fi

# Generate Checksums
cd "${OUT_DIR}"
if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 proxykillswitcher-* > SHA256SUMS.txt 2>/dev/null || true
elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum proxykillswitcher-* > SHA256SUMS.txt 2>/dev/null || true
fi

echo "=================================================="
echo " Release Build Succeeded!"
echo " Artifacts located in: ${OUT_DIR}"
ls -lh "${OUT_DIR}"
echo "=================================================="
