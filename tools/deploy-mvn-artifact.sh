#!/bin/sh +v

. tools/deploy-mvn-artifact.conf

VERSION=`grep -E 'versionName' build.gradle \
		| sed s/versionName// \
		| grep -Eo "[a-zA-Z0-9.-]+"`
GROUPID=org.wordpress
ARTIFACTID=graphview
AARFILE=build/outputs/aar/GraphView-wpmobile.aar

# Deploy release build
mvn deploy:deploy-file -Dfile=$AARFILE \
	-Durl=$LOCAL_GH_PAGES -DgroupId=$GROUPID \
	-DartifactId=$ARTIFACTID -Dversion=$VERSION

echo ========================================
echo
echo \"$GROUPID:$ARTIFACTID:$VERSION\" deployed