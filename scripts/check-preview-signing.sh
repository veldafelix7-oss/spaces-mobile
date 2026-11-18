#!/usr/bin/env bash
set -euo pipefail
# Public certificate comparison only. Never exports or prints private signing material.
PREVIOUS="preview-0.1.0-b7891f8"
DIRECTORY="${RUNNER_TEMP:?}/spaces-previous-preview"
mkdir -p "$DIRECTORY"
gh release download "$PREVIOUS" --repo "$GITHUB_REPOSITORY" --pattern spaces-mobile-preview.apk --dir "$DIRECTORY" --clobber
TOOL="$(find "$ANDROID_HOME/build-tools" -mindepth 2 -maxdepth 2 -name apksigner | sort -V | tail -1)"
test -n "$TOOL"
signer() { "$TOOL" verify --print-certs "$1" | sed -n 's/Signer #1 certificate SHA-256 digest: //p' | head -1; }
OLD=$(signer "$DIRECTORY/spaces-mobile-preview.apk")
NEW=$(signer app/build/outputs/apk/debug/app-debug.apk)
if [ -z "$OLD" ] || [ "$OLD" != "$NEW" ]; then
  echo '::error title=Preview signing::Signing identity differs from the installed 0.1.0 preview. Publishing blocked to prevent an incompatible update.'
  exit 1
fi
echo '::notice title=Preview signing::New APK has the same verified signing certificate as the 0.1.0 preview.'
