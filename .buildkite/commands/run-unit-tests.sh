#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type validation; then
  mkdir -p buildkite-test-analytics && touch buildkite-test-analytics/empty.xml
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "+++ 🧪 Testing"
set +e
./gradlew \
  testJetpackJalapenoDebugUnitTest \
  testWordpressJalapenoDebugUnitTest \
  :libs:processors:test \
  :libs:image-editor:testDebugUnitTest \
  :libs:fluxc:testDebugUnitTest \
  :libs:login:testDebugUnitTest \
  koverXmlReportJetpackJalapenoDebug \
  koverXmlReportWordpressJalapenoDebug \
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

MODULES=(WordPress:jetpack WordPress:wordpress processors image-editor fluxc login)
for module in "${MODULES[@]}"; do
    echo "--- 🚦 Report Tests Status (Module: ${module})"

    # Define possible directories for merging JUnit reports
    if [[ "$module" == "WordPress:jetpack" ]]; then
        junit_test_results_dir="WordPress/build/test-results/testJetpackJalapenoDebugUnitTest"
    elif [[ "$module" == "WordPress:wordpress" ]]; then
        junit_test_results_dir="WordPress/build/test-results/testWordpressJalapenoDebugUnitTest"
    elif [[ "$module" == "processors" ]]; then
        junit_test_results_dir="libs/processors/build/test-results/test"
    elif [[ "$module" == "image-editor" ]]; then
        junit_test_results_dir="libs/image-editor/build/test-results/testDebugUnitTest"
    elif [[ "$module" == "fluxc" ]]; then
        junit_test_results_dir="libs/fluxc/build/test-results/testDebugUnitTest"
    elif [[ "$module" == "login" ]]; then
        junit_test_results_dir="libs/login/build/test-results/testDebugUnitTest"
    fi

    # Determine which directory exists
    if [ -d "$junit_test_results_dir" ]; then
        merge_dir="$junit_test_results_dir"
    else
        echo "$junit_test_results_dir does not exist for module $module. Skipping..."
        continue
    fi

    results_file="${merge_dir}/../merged-test-results.xml"
    # Merge JUnit results into a single file (for performance reasons with reporting)
    merge_junit_reports -d "$merge_dir" -o "$results_file"

    if [[ $BUILDKITE_BRANCH == trunk ]] || [[ $BUILDKITE_BRANCH == release/* ]]; then
        annotate_test_failures "$results_file" --module "$module" --slack "build-and-ship"
    else
        annotate_test_failures "$results_file" --module "$module"
    fi

    echo "--- 🧪 Copying Test Logs for Test Collector (Module: ${module})"
    mkdir -p buildkite-test-analytics
    cp "$results_file" "buildkite-test-analytics/${module}-merged-test-results.xml"
done

echo "--- 📊 Tests Status"
exit $TESTS_EXIT_STATUS
