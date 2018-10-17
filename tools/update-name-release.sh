#!/bin/sh

if [ x"$2" == x ]; then
  echo "This script updates the version name and code on the release branch"
  echo "Usage:   $0 version-code current-version [current-branch]"
  echo "Example: $0 514 9.1"
  echo "Example: $0 517 9.1.1 hotfix/9.1.1"
  exit 1
fi

source ./tools/update-name-core.sh

CURRENT_VERSION_CODE=$1
CURRENT_VERSION=$2

if [ x"$3" == x ]; then
    CURRENT_BRANCH=release/$CURRENT_VERSION
else
    CURRENT_BRANCH=$3
fi

BUILD_FILE=WordPress/build.gradle
LOGFILE=/tmp/update-release-names.log
echo > $LOGFILE

check_version_code

switch_branch_pull $CURRENT_BRANCH
edit_build_file_commit $CURRENT_VERSION_CODE $CURRENT_VERSION
