#!/bin/bash -eu

echo "--- :git: Checkout Release Branch"

RELEASE_VERSION="${1?Please provide a release version as an argument.}"

# Buildkite, by default, checks out a specific commit. For many release actions, we need to be
# on a release branch instead.
BRANCH_NAME="release/${RELEASE_VERSION}"
git fetch origin "$BRANCH_NAME"
git checkout "$BRANCH_NAME"
# Buildkite can reuse a working copy where "$BRANCH_NAME" was left at an older commit by a previous job,
# so force the local branch to the fetched commit. `reset --hard` rather than
# `git pull`, to avoid merging if the two diverged.
git reset --hard "origin/$BRANCH_NAME"
