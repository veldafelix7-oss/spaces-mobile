#!/usr/bin/env bash
# A changed certificate is permitted ONLY for the exact user-approved reset version.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
POLICY="${IDENTITY_POLICY:-config/preview-identity.json}"
BASELINE=$(python3 "$SCRIPT_DIR/preview-policy.py" "$POLICY")
DIRECTORY="${RUNNER_TEMP:?}/spaces-update-baseline"
mkdir -p "$DIRECTORY"
gh release download "$BASELINE" --repo "$GITHUB_REPOSITORY" --pattern spaces-mobile-preview.apk --dir "$DIRECTORY" --clobber
TOOLS="$(find "$ANDROID_HOME/build-tools" -mindepth 2 -maxdepth 2 -name apksigner | sort -V | tail -1 | xargs dirname)"
OLD="$DIRECTORY/spaces-mobile-preview.apk"
NEW="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
old_info=$("$TOOLS/apksigner" verify --print-certs "$OLD")
new_info=$("$TOOLS/apksigner" verify --print-certs "$NEW")
old_cert=$(printf '%s\n' "$old_info" | awk '/^Signer |^V[0-9]+ Signer:/ && /certificate SHA-256 digest:/ {sub(/^.*certificate SHA-256 digest: */, ""); sub(/\r$/, ""); print}' | tr 'A-F' 'a-f' | sort -u)
new_cert=$(printf '%s\n' "$new_info" | awk '/^Signer |^V[0-9]+ Signer:/ && /certificate SHA-256 digest:/ {sub(/^.*certificate SHA-256 digest: */, ""); sub(/\r$/, ""); print}' | tr 'A-F' 'a-f' | sort -u)
[[ "$old_cert" =~ ^[a-f0-9]{64}$ ]] && [[ "$new_cert" =~ ^[a-f0-9]{64}$ ]] || { echo '::error::Expected one SHA-256 signer per APK'; exit 1; }
echo "::notice title=Installed preview certificate::$old_cert"
echo "::notice title=Candidate certificate::$new_cert"
"$TOOLS/aapt" dump badging "$OLD" > "$DIRECTORY/baseline-badging.txt"
"$TOOLS/aapt" dump badging "$NEW" > "$DIRECTORY/candidate-badging.txt"
python3 "$SCRIPT_DIR/preview-policy.py" "$POLICY" "$old_cert" "$new_cert" "$DIRECTORY/baseline-badging.txt" "$DIRECTORY/candidate-badging.txt"
