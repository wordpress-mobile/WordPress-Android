#!/bin/sh

OUTDIR="WordPress/build/outputs/apk/"
BUILDFILE="WordPress/build.gradle"
LOGFILE="build.log"

if [ x"$3" == x ]; then
  echo "Usage:   $0 release-branch beta-branch alpha-branch"
  echo "Example: $0 5.2 release/5.3 alpha-6"
  exit 1
fi

release_branch=$1
beta_branch=$2
alpha_branch=$3

function gradle_version_name {
  grep -E 'versionName' $BUILDFILE | sed s/versionName// | grep -Eo "[a-zA-Z0-9.-]+"
}

function gradle_version_code {
  grep -E 'versionCode' $BUILDFILE | sed s/versionCode// | grep -Eo "[a-zA-Z0-9.-]+"
}

function build_apk {
  branch=$1
  flavor=$2
  git checkout $branch >> $LOGFILE 2>&1
  version_code=`gradle_version_code`
  version_name=`gradle_version_name`
  name="wpandroid-$version_name.apk"
  apk="WordPress-$flavor-release.apk"

  echo "Cleaning in branch: $branch" | tee -a $LOGFILE
  ./gradlew clean --offline >> $LOGFILE 2>&1
  echo "Building $version_name / $version_code - $apk..." | tee -a $LOGFILE
  ./gradlew assemble"$flavor"Release --offline >> $LOGFILE 2>&1
  cp -v $OUTDIR/$apk $name | tee -a $LOGFILE
  echo "APK ready: $name" | tee -a $LOGFILE
  return $version_code
}

date > $LOGFILE
build_apk $release_branch Vanilla
release_code=$?
build_apk $beta_branch Vanilla
beta_code=$?
build_apk $alpha_branch Zalpha
alpha_code=$?

if [ $release_code -ge $beta_code -o $beta_code -ge $alpha_code ]; then
  echo "(ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ) (ಥ﹏ಥ)"
  echo "Wrong version codes (╯°□°）╯︵ ┻━┻"
  exit 2
fi
echo "Version codes - release: $release_code, beta: $beta_code, alpha: $alpha_code"
echo "Full log in: $LOGFILE"
