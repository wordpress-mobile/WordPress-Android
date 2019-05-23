#!/bin/sh

# This script defines some shared functions that are used by the app bundles scipts.

function get_gradle_property {
  GRADLE_PROPERTIES=$1
  PROP_KEY=$2
  PROP_VALUE=`cat "$GRADLE_PROPERTIES" | grep "$PROP_KEY" | cut -d'=' -f2`
  echo $PROP_VALUE
}

function gradle_version_name {
  BUILDFILE=$1
  grep -E 'versionName' $BUILDFILE | sed s/versionName// | grep -Eo "[a-zA-Z0-9.-]+"
}

function gradle_version_code {
  BUILDFILE=$1
  grep -E 'versionCode' $BUILDFILE | sed s/versionCode// | grep -Eo "[a-zA-Z0-9.-]+"
}
