#!/bin/sh

if [ x"$3" == x ]; then
  echo "This script updates the version name and code on the beta and alpha branches"
  echo "Usage:   $0 version-code beta-version alpha-version [beta-branch] [alpha-branch]"
  echo "Example: $0 514 9.2-rc-2 alpha-93"
  echo "Example: $0 517 9.2-rc-3 alpha-94 release/9.2 develop"
  exit 1
fi

source ./tools/update-name-core.sh

BETA_VERSION_CODE=$1
BETA_VERSION=$2
ALPHA_VERSION=$3

if [ x"$4" == x ]; then
    BETA_BRANCH=release/`echo $BETA_VERSION | cut -d- -f1`
else
    BETA_BRANCH=$4
fi

if [ x"$5" == x ]; then
    ALPHA_BRANCH=develop
else
    ALPHA_BRANCH=$5
fi

ALPHA_VERSION_CODE=$((BETA_VERSION_CODE + 1))
BUILD_FILE=WordPress/build.gradle
LOGFILE=/tmp/update-release-names.log
echo > $LOGFILE

CURRENT_VERSION_CODE=$BETA_VERSION_CODE
check_version_code

switch_branch_pull $BETA_BRANCH
edit_build_file_commit $BETA_VERSION_CODE $BETA_VERSION

switch_branch_pull $ALPHA_BRANCH
edit_build_file_commit $ALPHA_VERSION_CODE $ALPHA_VERSION
