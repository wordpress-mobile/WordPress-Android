#!/bin/sh

# This script installs an Android App Bundle (.aab) file on a device or emulator, using the code signing from gradle.properties

# Check for bundletool
command -v bundletool > /dev/null || { echo "bundletool is required to build the APKs. Install it with 'brew install bundletool'" >&2; exit 1; }

# Exit if any command fails
set -eu

function get_gradle_property {
    PROP_KEY=$1
    PROP_VALUE=`cat "gradle.properties" | grep "$PROP_KEY" | cut -d'=' -f2`
    echo $PROP_VALUE
}

APP_BUNDLE="$1"
TMP_DIR=$(mktemp -d)

echo "Generating APKs..."
bundletool build-apks --bundle="$APP_BUNDLE" \
                      --output="$TMP_DIR/output.apks" \
                      --ks="$(get_gradle_property storeFile)" \
                      --ks-pass="pass:$(get_gradle_property storePassword)" \
                      --ks-key-alias="$(get_gradle_property keyAlias)" \
                      --key-pass="pass:$(get_gradle_property keyPassword)"
echo "Installing..."
bundletool install-apks --apks="$TMP_DIR/output.apks"
