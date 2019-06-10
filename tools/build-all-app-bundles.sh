#!/bin/sh

# This script builds the bundles for the current release and the beta and alpha versions.

OUTDIR="WordPress/build/outputs/bundle/"
BUILDFILE="WordPress/build.gradle"
BUILDDIR="build"
LOGFILE="$BUILDDIR/build.log"

if [ x"$3" == x ]; then
  echo "Usage:   $0 release-branch beta-branch alpha-branch"
  echo "Example: $0 5.2 release/5.3 alpha-6"
  exit 1
fi

mkdir -p $BUILDDIR
> "$LOGFILE"

current_branch=`git rev-parse --abbrev-ref HEAD`
release_branch=$1
beta_branch=$2
alpha_branch=$3

BUILD_APP_BUNDLE_RET_VALUE=0

source ./tools/build-app-bundle-core.sh

check_clean_working_dir
date > $LOGFILE
build_app_bundle $release_branch Vanilla
release_code=$BUILD_APP_BUNDLE_RET_VALUE
build_app_bundle $beta_branch Vanilla
beta_code=$BUILD_APP_BUNDLE_RET_VALUE
build_app_bundle $alpha_branch Zalpha
alpha_code=$BUILD_APP_BUNDLE_RET_VALUE
git checkout $current_branch

echo "Version codes - release: $release_code, beta: $beta_code, alpha: $alpha_code" | tee -a $LOGFILE
if [ $release_code -ge $beta_code -o $beta_code -ge $alpha_code ]; then
  echo "(ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ)"
  echo "Wrong version codes (╯°□°）╯︵ ┻━┻"
  echo "Full log in: $LOGFILE"
  exit 2
fi
