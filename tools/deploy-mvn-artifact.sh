#!/bin/sh +v

. tools/deploy-mvn-artifact.conf
PROJECT=WordPressUtils
VERSION=`grep -E 'versionName' $PROJECT/build.gradle \
		| sed s/versionName// \
		| grep -Eo "[a-zA-Z0-9.-]+"`
GROUPID=org.wordpress
ARTIFACTID=wordpress-utils
AARFILE=$PROJECT/build/outputs/aar/WordPressUtils.aar

# Deploy release build
mvn deploy:deploy-file -Dfile=$AARFILE \
	-Durl=$LOCAL_GH_PAGES -DgroupId=$GROUPID \
	-DartifactId=$ARTIFACTID -Dversion=$VERSION

echo ========================================
echo
echo \"$GROUPID:$ARTIFACTID:$VERSION\" deployed