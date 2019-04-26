#!/bin/sh

# Exit if any command fails
set -eu
# Print the logs on failure
function cleanup {
  cat "$LOGFILE"
}
trap cleanup ERR

# This script defines some shared functions that are used by the build-app-bundle* set.

function gradle_version_name {
  grep -E 'versionName' $BUILDFILE | sed s/versionName// | grep -Eo "[a-zA-Z0-9.-]+"
}

function gradle_version_code {
  grep -E 'versionCode' $BUILDFILE | sed s/versionCode// | grep -Eo "[a-zA-Z0-9.-]+"
}

function build_app_bundle {
  branch=$1
  flavor=$2
  git checkout $branch >> $LOGFILE 2>&1
  version_code=`gradle_version_code`
  version_name=`gradle_version_name`
  name="wpandroid-$version_name.aab"
  aab="WordPress.aab"

  echo "Cleaning in branch: $branch" | tee -a $LOGFILE
  ./gradlew clean >> $LOGFILE 2>&1
  echo "Running lint in branch: $branch" | tee -a $LOGFILE
  ./gradlew lint"$flavor"Release >> $LOGFILE 2>&1
  echo "Building $version_name / $version_code - $aab..." | tee -a $LOGFILE
  ./gradlew bundle"$flavor"Release >> $LOGFILE 2>&1
  cp -v $OUTDIR/"$flavor"Release/$aab $BUILDDIR/$name | tee -a $LOGFILE
  echo "Bundle ready: $name" | tee -a $LOGFILE
  BUILD_APP_BUNDLE_RET_VALUE=$version_code
}

function check_clean_working_dir {
  if [ "`git status --porcelain`"x \!= x ]; then
    git status
    echo "Your working directory must be clean before you run this script"
    exit 1
  fi
}
