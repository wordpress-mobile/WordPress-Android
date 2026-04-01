#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type lint; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :microscope: Linting"

if [ "$1" = "wordpress" ]; then
  ./gradlew lintWordpressDebug
  exit 0
fi

if [ "$1" = "jetpack" ]; then
  set +e
  ./gradlew lintJetpackDebug
  lint_exit_code=$?
  set -e

  upload_sarif_to_github "WordPress/build/reports/lint-results-jetpackDebug.sarif"
  exit $lint_exit_code
fi

echo "No target provided – unable to lint"
exit 1
