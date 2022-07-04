#!/bin/bash -eu

# This script aims to rename any `.apk` or `.aab` file to `[wpandroid|jpandroid]-{version}[-Signed].apk/aab`
# depending on the APK/AAB package name, version name and package signature.
#
# The main use of this tool is to easily rename the signed `.apk` and/or original `.aab` downloaded from the PlayStore
# (and re-signed by Google depending on the context), as when you download those, their basename is usually just the
# versionCode and doesn't distinguish from a binary corresponding to WordPress vs Jetpack.
#
# By running this script on the APK or AAB files provided – defaulting to all the APK and AAB files in `~/Downloads` if
# no parameter provided – the files will be renamed with a basename appropriate to the ones we use when attaching
# those files to the GitHub release.

set +o pipefail

### Input Parameters ###

# Usage:
#   rename_apk_aab.sh [file1.apk] [file2.aab] [...]
#
# Without any parameter, applies the rename to every APK/AAB file found at the top level of the ~/Downloads directory
#
INPUT_FILES=( "$@" )
if [[ $# -lt 1 ]]; then
  shopt -s nullglob
  INPUT_FILES=(~/Downloads/*.{aab,apk})
fi


##################################
### Path to Android tools used ###
##################################
[[ -d "${ANDROID_SDK_ROOT:-$ANDROID_HOME}" ]] || ( echo "You need to have either \`ANDROID_SDK_ROOT\` or \`ANDROID_HOME\` env var defined, and pointing to your Android SDK installation directory." && exit 1 )

AAPT2=$(ls -1t "${ANDROID_SDK_ROOT:-$ANDROID_HOME}"/build-tools/*/aapt2 | head -n1)
APKSIGNER=$(ls -1t "${ANDROID_SDK_ROOT:-$ANDROID_HOME}"/build-tools/*/apksigner | head -n1)
BUNDLETOOL=$(command -v bundletool || true)

[[ -x "$AAPT2" ]] || ( echo "Failed to find the \`aapt2\` tool in your \$ANDROID_SDK_ROOT" && exit 1 )
[[ -x "$APKSIGNER" ]] || ( echo "Failed to find the \`apksigner\` tool in your \$ANDROID_SDK_ROOT" && exit 1 )
[[ -x "$BUNDLETOOL" ]] || ( echo "Failed to find the \`bundletool\` executable; install it using \`brew install bundletool\`" && exit 1 )

######################
### Helper Methods ###
######################

### Extract info from a single APK file
# Sets PKG, VNAME, VCODE and SIGNED_SUFFIX
info_for_apk() {
  # Use aapt2 to extract package name, versionCode and versionName
  INFO_LINE=$("$AAPT2" dump badging "$1" | head -n 1)
  [[ "$INFO_LINE" =~ name=\'([^\']*)\'[[:blank:]]versionCode=\'([^\']*)\'[[:blank:]]versionName=\'([^\']*)\' ]] && PKG=${BASH_REMATCH[1]} && VCODE=${BASH_REMATCH[2]} && VNAME=${BASH_REMATCH[3]}
  SIGNED_SUFFIX=$("$APKSIGNER" verify --print-certs "$1" | grep -qE "(CN=Android, OU=Android, O=Google Inc.)|(O=Automattic Inc.)" && echo "-Signed")
}

### Extract info from a single AAB file
# Sets PKG, VNAME, VCODE
info_for_aab() {
  PKG=$("$BUNDLETOOL" dump manifest --bundle "$1" --xpath /manifest/@package)
  VCODE=$("$BUNDLETOOL" dump manifest --bundle "$1" --xpath /manifest/@android:versionCode)
  VNAME=$("$BUNDLETOOL" dump manifest --bundle "$1" --xpath /manifest/@android:versionName)
}

### Extract info from a single APK or AAB file
info_for_file() {
  echo "== $1 =="
  unset PKG
  unset VNAME
  unset VCODE
  unset SIGNED_SUFFIX

  ext="${1##*.}"
  case $ext in
    apk)
      info_for_apk "$1"
      ;;
    aab)
      info_for_aab "$1"
      ;;
    *)
      echo "Error: Unsupported extension ${ext} for ${1}"
      ;;
  esac
}

### Rename a APK or AAB file based on its package name, versionName, signature and extension
auto_rename_file() {
  ext="${1##*.}"
  info_for_file "$1"

  case $PKG in
    org.wordpress.android)
      BASENAME=wpandroid-$VNAME
      ;;
    com.jetpack.android)
      BASENAME=jpandroid-$VNAME
      ;;
    *)
      echo "Unrecognized package name: ${PKG}"
      ;;
  esac

  if [[ "$ext" == "apk" ]]; then
    NEW_NAME="${BASENAME}${SIGNED_SUFFIX}.apk"
  else
    NEW_NAME="$BASENAME.aab"
  fi

  echo "$(basename "$1") ==> $NEW_NAME (versionCode: $VCODE)"
  mv "$1" "$(dirname "$1")/$NEW_NAME"
}


#### MAIN ####

for f in "${INPUT_FILES[@]}"; do
  auto_rename_file "$f"
done
