#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type validation; then
  mkdir -p buildkite-test-analytics && touch buildkite-test-analytics/empty.xml
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "+++ 🧪 Testing"
set +e
./gradlew \
  testWordpressWasabiDebugUnitTest \
  :libs:processors:test \
  :libs:image-editor:testDebugUnitTest \
  :libs:fluxc:testDebugUnitTest \
  :libs:login:testDebugUnitTest \
  koverXmlReportWordpressWasabiDebug \
  :libs:processors:koverXmlReportJvm \
  :libs:image-editor:koverXmlReportDebug \
  :libs:fluxc:koverXmlReportDebug \
  :libs:login:koverXmlReportDebug
TESTS_EXIT_STATUS=$?
set -e
echo ""

if [[ "$TESTS_EXIT_STATUS" -ne 0 ]]; then
  # Keep the (otherwise collapsed) current "Testing" section open in Buildkite logs on error. See https://buildkite.com/docs/pipelines/managing-log-output#collapsing-output
  echo "^^^ +++"
  echo "Unit Tests failed!"
fi

if [[ "$TESTS_EXIT_STATUS" -eq 0 ]]; then
  echo "--- ⚒️ Uploading code coverage"
  .buildkite/commands/upload-code-coverage.sh
fi

echo "--- 🚦 Collecting Test Results"
# Create temporary directory for collecting all test results
temp_test_results_dir=$(mktemp -d)

# Define test result directories for each module
declare -A TEST_RESULT_DIRS=(
  ["WordPress:wordpress"]="WordPress/build/test-results/testWordpressWasabiDebugUnitTest"
  ["processors"]="libs/processors/build/test-results/test"
  ["image-editor"]="libs/image-editor/build/test-results/testDebugUnitTest"
  ["fluxc"]="libs/fluxc/build/test-results/testDebugUnitTest"
  ["login"]="libs/login/build/test-results/testDebugUnitTest"
)

# Copy all XML test results to temporary directory
for module in "${!TEST_RESULT_DIRS[@]}"; do
    test_results_dir="${TEST_RESULT_DIRS[$module]}"

    if [ -d "$test_results_dir" ]; then
        echo "Collecting test results from ${module}..."
        cp "$test_results_dir"/*.xml "$temp_test_results_dir/" 2>/dev/null || true
    else
        echo "Test results directory $test_results_dir does not exist for module $module. Skipping..."
    fi
done

echo "--- 🚦 Report Tests Status"
results_file="WordPress/build/test-results/merged-test-results.xml"
# Merge JUnit results into a single file (for performance reasons with reporting)
# See https://github.com/Automattic/a8c-ci-toolkit-buildkite-plugin/pull/103
merge_junit_reports -d "$temp_test_results_dir" -o "$results_file"

# Clean up temporary directory
rm -rf "$temp_test_results_dir"

if [[ $BUILDKITE_BRANCH == trunk ]] || [[ $BUILDKITE_BRANCH == release/* ]]; then
    annotate_test_failures "$results_file" --slack "build-and-ship"
else
    annotate_test_failures "$results_file"
fi

echo "--- 🧪 Copying Test Logs for Test Collector"
mkdir -p buildkite-test-analytics
cp "$results_file" "buildkite-test-analytics/merged-test-results.xml"

echo "--- 📊 Tests Status"
exit $TESTS_EXIT_STATUS
