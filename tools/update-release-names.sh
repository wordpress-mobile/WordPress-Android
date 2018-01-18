#!/bin/sh

if [ x"$4" == x ]; then
  echo "Usage:   $0 version-code current-version beta-version alpha-version"
  echo "Example: $0 514 9.1 9.2-rc-2 alpha-93"
  exit 1
fi

CURRENT_VERSION_CODE=$1
CURRENT_VERSION=$2
BETA_VERSION=$3
ALPHA_VERSION=$4

BETA_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
ALPHA_VERSION_CODE=$((CURRENT_VERSION_CODE + 2))
BUILD_FILE=WordPress/build.gradle
LOGFILE=/tmp/update-release-names.log
echo > $LOGFILE

function edit_build_file_commit() {
    echo "Editing $BUILD_FILE - name: $2 code: $1"
    perl -pi -e "s/versionCode.*$/versionCode $1/" $BUILD_FILE
    perl -pi -e "s/versionName.*$/versionName \"$2\"/" $BUILD_FILE
    echo "Adding to git"
    git add $BUILD_FILE >> $LOGFILE 2>> $LOGFILE
    git commit -m "$2 / $1 version bump" >> $LOGFILE 2>> $LOGFILE
}

function switch_branch_pull() {
    echo "Switching and pulling: $1"
    git checkout $1 >> $LOGFILE 2>> $LOGFILE
    git pull origin $1 >> $LOGFILE 2>> $LOGFILE
}

switch_branch_pull release/$CURRENT_VERSION
edit_build_file_commit $CURRENT_VERSION_CODE $CURRENT_VERSION

switch_branch_pull release/`echo $BETA_VERSION | cut -d- -f1`
edit_build_file_commit $BETA_VERSION_CODE $BETA_VERSION

switch_branch_pull develop
edit_build_file_commit $ALPHA_VERSION_CODE $ALPHA_VERSION
