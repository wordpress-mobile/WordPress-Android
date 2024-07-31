#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 🧪 Testing"
set +e
bundle exec fastlane build_and_run_instrumented_test app:$1
TESTS_EXIT_STATUS=$?
set -e

if [[ "$TESTS_EXIT_STATUS" -ne 0 ]]; then
  # Keep the (otherwise collapsed) current "Testing" section open in Buildkite logs on error. See https://buildkite.com/docs/pipelines/managing-log-output#collapsing-output
  echo "^^^ +++"
  echo "Instrumented Tests failed!"
fi

echo "--- 🚦 Report Tests Status"
results_file=$(find "build/instrumented-tests" -type f -name "*.xml" -print -quit)

if [[ $BUILDKITE_BRANCH == trunk ]] || [[ $BUILDKITE_BRANCH == release/* ]]; then
    annotate_test_failures "$results_file" --slack "build-and-ship"
else
    annotate_test_failures "$results_file"
fi

echo "--- 🧪 Copying test logs for test collector"
mkdir buildkite-test-analytics && cp -r build/instrumented-tests/matrix_* buildkite-test-analytics

exit $TESTS_EXIT_STATUS
