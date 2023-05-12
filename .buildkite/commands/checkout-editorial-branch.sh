#!/bin/bash -eu

# EDITORIAL_BRANCH is passed as an environment variable from fastlane to Buildkite
#
if [[ -z "${EDITORIAL_BRANCH}" ]]; then
    echo "EDITORIAL_BRANCH is not set."
    exit 1
fi

# RELEASE_VERSION is passed as an environment variable from fastlane to Buildkite
# Even though RELEASE_VERSION is not directly used in this script, it's necessary to update
# the app store strings. Having this check here keeps the buildkite pipeline cleaner. Later on,
# if we don't want it here, we can move it to a separate script file.
if [[ -z "${RELEASE_VERSION}" ]]; then
    echo "RELEASE_VERSION is not set."
    exit 1
fi

# Buildkite, by default, checks out a specific commit. When we update the app store strings, we open
# a PR from the current branch. So, we need to checkout the `EDITORIAL_BRANCH`.
git fetch origin "$EDITORIAL_BRANCH"
git checkout "$EDITORIAL_BRANCH"
