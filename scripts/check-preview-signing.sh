#!/usr/bin/env bash
set -euo pipefail
# Verify public APK certificates and forbid accidentally replacing the old preview with a different key.
PREVIOUS="preview-0.1.0-b7891f8"
DIRECTORY="${RUNNER_TEMP:?}/spaces-previous-preview"
mkdir -p "$DIRECTORY"
gh release download "$PREVIOUS" --repo "$GITHUB_REPOSITORY" --pattern spaces-mobile-preview.apk --dir "$DIRECTORY" --clobber
TOOLS="$(find "$ANDROID_HOME/build-tools" -mindepth 2 -maxdepth 2 -name apksigner | sort -V | tail -1 | xargs dirname)"
"$TOOLS/apksigner" verify "$DIRECTORY/spaces-mobile-preview.apk"
"$TOOLS/apksigner" verify app/build/outputs/apk/debug/app-debug.apk
PACKAGE=$("$TOOLS/aapt" dump badging app/build/outputs/apk/debug/app-debug.apk | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
if [ "$PACKAGE" != id.veldafelix.spaces.preview2 ]; then
  echo '::error title=Preview isolation::Expected isolated preview2 application ID. Publishing blocked to protect the old preview and its data.'
  exit 1
fi
echo '::notice title=Preview installation::APK signatures verified. This is an isolated side-by-side preview2, NOT an in-place update of 0.1.0. Stable private release signing remains unconfigured.'
