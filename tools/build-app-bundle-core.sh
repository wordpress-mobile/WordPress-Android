#!/bin/sh

# Check for bundletool
command -v bundletool > /dev/null || { echo "bundletool is required to build the APKs. Install it with 'brew install bundletool'" >&2; exit 1; }

# Exit if any command fails
set -eu
# Print the logs on failure
function cleanup {
  cat "$LOGFILE"
}
trap cleanup ERR

# This script defines some shared functions that are used by the build-app-bundle* set.

# Load the Gradle helper functions
source "./tools/gradle-functions.sh"

function build_app_bundle {
  branch=$1
  flavor=$2
  git checkout $branch >> $LOGFILE 2>&1
  version_code=`gradle_version_code "$BUILDFILE"`
  version_name=`gradle_version_name "$BUILDFILE"`
  name="wpandroid-$version_name.aab"
  apk_name="wpandroid-$version_name-universal.apk"
  aab="WordPress.aab"

  echo "Cleaning in branch: $branch" | tee -a $LOGFILE
  ./gradlew clean >> $LOGFILE 2>&1
  echo "Running lint in branch: $branch" | tee -a $LOGFILE
  ./gradlew lint"$flavor"Release >> $LOGFILE 2>&1
  echo "Building $version_name / $version_code - $aab..." | tee -a $LOGFILE
  ./gradlew bundle"$flavor"Release >> $LOGFILE 2>&1
  cp -v $OUTDIR/"$flavor"Release/$aab $BUILDDIR/$name | tee -a $LOGFILE
  echo "Bundle ready: $name" | tee -a $LOGFILE
  extract_universal_apk $BUILDDIR/$name $BUILDDIR/$apk_name
  BUILD_APP_BUNDLE_RET_VALUE=$version_code
}

function extract_universal_apk {
  app_bundle="$1"
  apk_output="$2"
  tmp_dir=$(mktemp -d)

  echo "Extracting universal APK..." | tee -a $LOGFILE
  bundletool build-apks --bundle="$app_bundle" \
                        --output="$tmp_dir/universal.apks" \
                        --mode=universal \
                        --ks="$(get_gradle_property gradle.properties storeFile)" \
                        --ks-pass="pass:$(get_gradle_property gradle.properties storePassword)" \
                        --ks-key-alias="$(get_gradle_property gradle.properties keyAlias)" \
                        --key-pass="pass:$(get_gradle_property gradle.properties keyPassword)" >> $LOGFILE 2>&1
  
  unzip "$tmp_dir/universal.apks" -d "$tmp_dir"  >> $LOGFILE 2>&1
  cp "$tmp_dir/universal.apk" "$apk_output" | tee -a $LOGFILE
}

function check_clean_working_dir {
  if [ "`git status --porcelain`"x \!= x ]; then
    git status
    echo "Your working directory must be clean before you run this script"
    exit 1
  fi
}
