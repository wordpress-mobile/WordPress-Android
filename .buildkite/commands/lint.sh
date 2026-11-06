#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type lint; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

# Use Debug for PR builds (faster), Release for trunk and release-branch builds.
# PRs targeting a release branch use Release to catch release-only issues before merge.
# Merge queue builds have no PR env vars: they count as PR builds, with the base taken from the `gh-readonly-queue/<base>/pr-<N>-<sha>` branch name.
PR_BASE_BRANCH="${BUILDKITE_PULL_REQUEST_BASE_BRANCH:-}"
IS_PR_BUILD=false
if [ "${BUILDKITE_PULL_REQUEST:-false}" != "false" ]; then
  IS_PR_BUILD=true
elif [[ "${BUILDKITE_BRANCH:-}" =~ ^gh-readonly-queue/(.+)/pr-[0-9]+-[0-9a-f]+$ ]]; then
  IS_PR_BUILD=true
  PR_BASE_BRANCH="${BASH_REMATCH[1]}"
fi

if [[ "$IS_PR_BUILD" == true && ! "$PR_BASE_BRANCH" =~ ^release/ ]]; then
  BUILD_VARIANT="Debug"
else
  BUILD_VARIANT="Release"
fi

echo "--- :microscope: Linting (${BUILD_VARIANT})"

if [ "$1" = "wordpress" ]; then
  ./gradlew lintWordpress"${BUILD_VARIANT}"
  exit 0
fi

if [ "$1" = "jetpack" ]; then
  set +e
  ./gradlew lintJetpack"${BUILD_VARIANT}"
  lint_exit_code=$?
  set -e

  upload_sarif_to_github "WordPress/build/reports/lint-results-jetpack${BUILD_VARIANT}.sarif"
  exit $lint_exit_code
fi

if [ "$1" = "library" ]; then
  ./gradlew \
    :libs:analytics:lint"${BUILD_VARIANT}" \
    :libs:editor:lint"${BUILD_VARIANT}" \
    :libs:image-editor:lint"${BUILD_VARIANT}" \
    :libs:mocks:lint"${BUILD_VARIANT}" \
    :libs:networking:lint"${BUILD_VARIANT}" \
    :libs:posttypes:lint"${BUILD_VARIANT}"
  exit 0
fi

echo "No target provided – unable to lint"
exit 1
