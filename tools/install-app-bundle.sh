#!/bin/sh

# This script installs an Android App Bundle (.aab) file on a device or emulator, using the code signing from gradle.properties

# Check for bundletool
command -v bundletool > /dev/null || { echo "bundletool is required to build the APKs. Install it with 'brew install bundletool'" >&2; exit 1; }

# Exit if any command fails
set -eu

APP_BUNDLE="$1"
TMP_DIR=$(mktemp -d)

echo "Generating APKs..."
bundletool build-apks --bundle="$APP_BUNDLE" \
                      --output="$TMP_DIR/output.apks"
echo "Installing..."
bundletool install-apks --apks="$TMP_DIR/output.apks"
