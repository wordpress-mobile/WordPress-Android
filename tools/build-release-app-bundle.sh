#!/bin/sh

# This script builds the release bundle

OUTDIR="WordPress/build/outputs/bundle/"
BUILDFILE="WordPress/build.gradle"
BUILDDIR="build"
LOGFILE="$BUILDDIR/build.log"

if [ x"$1" == x ]; then
  echo "Usage:   $0 release-branch"
  echo "Example: $0 5.2"
  exit 1
fi

mkdir -p $BUILDDIR
> "$LOGFILE"

current_branch=`git rev-parse --abbrev-ref HEAD`
release_branch=$1

BUILD_APP_BUNDLE_RET_VALUE=0

source ./tools/build-app-bundle-core.sh

check_clean_working_dir
date > $LOGFILE
build_app_bundle $release_branch Vanilla
release_code=$BUILD_APP_BUNDLE_RET_VALUE
git checkout $current_branch

echo "Version codes - release: $release_code" | tee -a $LOGFILE
