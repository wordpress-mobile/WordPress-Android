#!/bin/sh

if [ x"$4" == x ]; then
  echo "Usage:   $0 version-code current-version beta-version alpha-version [current-branch] [beta-branch] [alpha-branch]"
  echo "Example: $0 514 9.1 9.2-rc-2 alpha-93"
  echo "Example: $0 517 9.1.1 9.2-rc-3 alpha-94 hotfix/9.1.1 release/9.2 develop"
  exit 1
fi

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

function check_version_code() {
    git checkout develop >> $LOGFILE 2>> $LOGFILE
    previous_alpha_version_code=`grep -E 'versionCode' $BUILD_FILE | grep -Eo "[0-9]+"`
    if [ $CURRENT_VERSION_CODE -gt $previous_alpha_version_code ]; then
        echo "Current version code: $CURRENT_VERSION_CODE - previous alpha version code: $previous_alpha_version_code"
    else
        echo "Current version code ($CURRENT_VERSION_CODE) should be greater than previous alpha version code ($previous_alpha_version_code)"
        exit 2
    fi
    git checkout - >> $LOGFILE 2>> $LOGFILE
}

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

check_version_code

switch_branch_pull $CURRENT_BRANCH
edit_build_file_commit $CURRENT_VERSION_CODE $CURRENT_VERSION

switch_branch_pull $BETA_BRANCH
edit_build_file_commit $BETA_VERSION_CODE $BETA_VERSION

switch_branch_pull $ALPHA_BRANCH
edit_build_file_commit $ALPHA_VERSION_CODE $ALPHA_VERSION
