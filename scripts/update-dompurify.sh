#!/usr/bin/env bash
# Resolve the latest DOMPurify release, vendor purify.es.mjs, and bump sw.js
# VERSION when the file changes. Safe to run locally or from CI.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VENDOR_DIR="${ROOT}/src/main/resources/META-INF/resources/scripts/vendor"
VENDOR_FILE="${VENDOR_DIR}/purify.es.mjs"
VERSION_PIN="${VENDOR_DIR}/dompurify.version"
SW_FILE="${ROOT}/src/main/resources/META-INF/resources/sw.js"

mkdir -p "${VENDOR_DIR}"

OLD_VERSION=""
if [[ -f "${VERSION_PIN}" ]]; then
  OLD_VERSION="$(tr -d '[:space:]' < "${VERSION_PIN}")"
fi

echo "Resolving latest dompurify version from npm registry..."
REGISTRY_JSON="$(curl -fsSL "https://registry.npmjs.org/dompurify/latest")" || {
  echo "error: failed to fetch npm registry metadata for dompurify" >&2
  exit 1
}
LATEST="$(
  printf '%s' "${REGISTRY_JSON}" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])'
)"

if [[ -z "${LATEST}" ]]; then
  echo "error: could not resolve latest dompurify version" >&2
  exit 1
fi

echo "Current pin: ${OLD_VERSION:-<none>}"
echo "Latest:      ${LATEST}"

TMP="$(mktemp)"
trap 'rm -f "${TMP}"' EXIT

URL="https://cdn.jsdelivr.net/npm/dompurify@${LATEST}/dist/purify.es.mjs"
echo "Downloading ${URL}"
curl -fsSL "${URL}" -o "${TMP}"

if ! grep -q "export { purify as default }" "${TMP}"; then
  echo "error: downloaded file is missing expected ES module export" >&2
  exit 1
fi

if ! grep -q "DOMPurify.version = '${LATEST}'" "${TMP}"; then
  echo "error: downloaded file version does not match ${LATEST}" >&2
  exit 1
fi

if [[ -f "${VENDOR_FILE}" ]] && cmp -s "${TMP}" "${VENDOR_FILE}"; then
  # Keep pin in sync even if the file was already correct.
  printf '%s\n' "${LATEST}" > "${VERSION_PIN}"
  echo "DOMPurify ${LATEST} is already vendored; nothing to update."
  exit 0
fi

cp "${TMP}" "${VENDOR_FILE}"
printf '%s\n' "${LATEST}" > "${VERSION_PIN}"

# Bump service worker cache version so PWA clients fetch the new vendor file.
TODAY="$(date -u +%Y-%m-%d)"
CURRENT_SW="$(
  sed -n 's/^const VERSION = "\([^"]*\)";$/\1/p' "${SW_FILE}" | head -n1
)"

if [[ -z "${CURRENT_SW}" ]]; then
  echo "error: could not parse VERSION from ${SW_FILE}" >&2
  exit 1
fi

SUFFIX=1
if [[ "${CURRENT_SW}" == "${TODAY}"-* ]]; then
  EXISTING_SUFFIX="${CURRENT_SW##*-}"
  if [[ "${EXISTING_SUFFIX}" =~ ^[0-9]+$ ]]; then
    SUFFIX=$((EXISTING_SUFFIX + 1))
  fi
fi
NEW_SW="$(printf '%s-%03d' "${TODAY}" "${SUFFIX}")"

# Portable in-place edit (GNU/BSD sed).
TMP_SW="$(mktemp)"
sed "s/^const VERSION = \"${CURRENT_SW}\";$/const VERSION = \"${NEW_SW}\";/" \
  "${SW_FILE}" > "${TMP_SW}"
mv "${TMP_SW}" "${SW_FILE}"

echo "Updated DOMPurify ${OLD_VERSION:-<none>} -> ${LATEST}"
echo "Bumped sw.js VERSION ${CURRENT_SW} -> ${NEW_SW}"
echo "Source: ${URL}"
