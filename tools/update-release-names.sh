#!/bin/sh

if [ x"$4" == x ]; then
  echo "This script updates the version name and code on the three relevant branches: current, beta and alpha"
  echo "Usage:   $0 version-code current-version beta-version alpha-version [current-branch] [beta-branch] [alpha-branch]"
  echo "Example: $0 514 9.1 9.2-rc-2 alpha-93"
  echo "Example: $0 517 9.1.1 9.2-rc-3 alpha-94 hotfix/9.1.1 release/9.2 develop"
  exit 1
fi

source ./tools/update-name-core.sh

CURRENT_VERSION_CODE=$1
CURRENT_VERSION=$2
BETA_VERSION=$3
ALPHA_VERSION=$4

if [ x"$5" == x ]; then
    CURRENT_BRANCH=release/$CURRENT_VERSION
else
    CURRENT_BRANCH=$5
fi

if [ x"$6" == x ]; then
    BETA_BRANCH=release/`echo $BETA_VERSION | cut -d- -f1`
else
    BETA_BRANCH=$6
fi

if [ x"$7" == x ]; then
    ALPHA_BRANCH=develop
else
    ALPHA_BRANCH=$7
fi

BETA_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
ALPHA_VERSION_CODE=$((CURRENT_VERSION_CODE + 2))
BUILD_FILE=WordPress/build.gradle
LOGFILE=/tmp/update-release-names.log
echo > $LOGFILE

check_version_code

switch_branch_pull $CURRENT_BRANCH
edit_build_file_commit $CURRENT_VERSION_CODE $CURRENT_VERSION

switch_branch_pull $BETA_BRANCH
edit_build_file_commit $BETA_VERSION_CODE $BETA_VERSION

switch_branch_pull $ALPHA_BRANCH
edit_build_file_commit $ALPHA_VERSION_CODE $ALPHA_VERSION
