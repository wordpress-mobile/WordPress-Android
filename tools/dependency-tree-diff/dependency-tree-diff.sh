#!/bin/bash -eu

TARGET_BRANCH_DEPENDENCIES_FILE="target_branch_dependencies.txt"
CURRENT_TARGET_BRANCH_DEPENDENCIES_FILE="current_branch_dependencies.txt"
DIFF_DEPENDENCIES_FOLDER="./build/reports/diff"
DIFF_DEPENDENCIES_FILE="$DIFF_DEPENDENCIES_FOLDER/diff_dependencies.txt"
CONFIGURATION="wordpressVanillaReleaseRuntimeClasspath"
DEPENDENCY_TREE_VERSION="1.2.0"


git config --global user.email '$( git log --format='%ae' $CIRCLE_SHA1^! )'
git config --global user.name '$( git log --format='%an' $CIRCLE_SHA1^! )'

prNumber="${CIRCLE_PULL_REQUEST##*/}"
githubUrl="https://api.github.com/repos/$CIRCLE_PROJECT_USERNAME/$CIRCLE_PROJECT_REPONAME/pulls/$prNumber"
githubResponse="$(curl "$githubUrl" -H "Authorization: token $GITHUB_API_TOKEN")"
targetBranch=$(echo "$githubResponse" | tr '\r\n' ' ' | jq '.base.ref' | tr -d '"')

git merge "origin/$targetBranch" --no-edit

if [[ $(git diff --name-status "origin/$targetBranch" | grep ".gradle") ]]; then
    echo ".gradle files have been changed. Looking for caused dependency changes"
  else
    echo ".gradle files haven't been changed. There is no need to run the diff"
    ./gradlew dependencyTreeDiffCommentToGitHub -DGITHUB_PULLREQUESTID="${CIRCLE_PULL_REQUEST##*/}" -DGITHUB_OAUTH2TOKEN="$GITHUB_API_TOKEN"
    exit 0
fi

mkdir -p "$DIFF_DEPENDENCIES_FOLDER"

./gradlew :WordPress:dependencies --configuration $CONFIGURATION > $CURRENT_TARGET_BRANCH_DEPENDENCIES_FILE

git checkout "$targetBranch"
./gradlew :WordPress:dependencies --configuration $CONFIGURATION > $TARGET_BRANCH_DEPENDENCIES_FILE

# https://github.com/JakeWharton/dependency-tree-diff
curl -L "https://github.com/JakeWharton/dependency-tree-diff/releases/download/$DEPENDENCY_TREE_VERSION/dependency-tree-diff.jar" -o dependency-tree-diff.jar
sha=($(sha1sum dependency-tree-diff.jar))
if [[ $sha != "949394274f37c06ac695b5d49860513e4d16e847" ]]; then
  echo "dependency-tree-diff.jar file has unexpected sha1"
  exit 1
fi
chmod +x dependency-tree-diff.jar
./dependency-tree-diff.jar $TARGET_BRANCH_DEPENDENCIES_FILE $CURRENT_TARGET_BRANCH_DEPENDENCIES_FILE > $DIFF_DEPENDENCIES_FILE

if [ -s $DIFF_DEPENDENCIES_FILE ]; then
  echo "There are changes in dependencies of the project"
  cat "$DIFF_DEPENDENCIES_FILE"
else
  echo "There are no changes in dependencies of the project"
  rm "$DIFF_DEPENDENCIES_FILE"
fi
./gradlew dependencyTreeDiffCommentToGitHub -DGITHUB_PULLREQUESTID="${CIRCLE_PULL_REQUEST##*/}" -DGITHUB_OAUTH2TOKEN="$GITHUB_API_TOKEN" --info
