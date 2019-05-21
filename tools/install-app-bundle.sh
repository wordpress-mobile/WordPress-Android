#!/bin/sh

# This script installs an Android App Bundle (.aab) file on a device or emulator, using the code signing from gradle.properties

# Check for bundletool
command -v bundletool > /dev/null || { echo "bundletool is required to build the APKs. Install it with 'brew install bundletool'" >&2; exit 1; }

# Exit if any command fails
set -eu

# Load the Gradle helper functions
source "./tools/gradle-functions.sh"

APP_BUNDLE="$1"
TMP_DIR=$(mktemp -d)

echo "Generating APKs..."
bundletool build-apks --bundle="$APP_BUNDLE" \
                      --output="$TMP_DIR/output.apks" \
                      --ks="$(get_gradle_property gradle.properties storeFile)" \
                      --ks-pass="pass:$(get_gradle_property gradle.properties storePassword)" \
                      --ks-key-alias="$(get_gradle_property gradle.properties keyAlias)" \
                      --key-pass="pass:$(get_gradle_property gradle.properties keyPassword)"
echo "Installing..."
bundletool install-apks --apks="$TMP_DIR/output.apks"
