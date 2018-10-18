#!/bin/sh

# This script defines some shared functions that are used by the update-name* set.

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