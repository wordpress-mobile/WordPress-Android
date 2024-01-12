#!/bin/bash -eu

echo "--- 🧪 Testing"
set +e
if [ "$1" == "wordpress" ]; then
    test_suite="testWordpressVanillaRelease koverXmlReportWordpressVanillaRelease"
    test_log_dir="WordPress/build/test-results/*/*.xml"
    code_coverage_report="WordPress/build/reports/kover/reportWordpressVanillaRelease.xml"
elif [ "$1" == "processors" ]; then
    test_suite=":libs:processors:test :libs:processors:koverXmlReport"
    test_log_dir="libs/processors/build/test-results/test/*.xml"
    code_coverage_report="libs/processors/build/reports/kover/report.xml"
elif [ "$1" == "image-editor" ]; then
    test_suite=":libs:image-editor:testReleaseUnitTest :libs:image-editor:koverXmlReportRelease"
    test_log_dir="libs/image-editor/build/test-results/testReleaseUnitTest/*.xml"
    code_coverage_report="libs/image-editor/build/reports/kover/reportRelease.xml"
else
    echo "Invalid Test Suite! Expected 'wordpress', 'processors', or 'image-editor', received '$1' instead"
    exit 1
fi

cp gradle.properties-example gradle.properties
./gradlew $test_suite
TESTS_EXIT_STATUS=$?
set -e

if [[ "$TESTS_EXIT_STATUS" -ne 0 ]]; then
  # Keep the (otherwise collapsed) current "Testing" section open in Buildkite logs on error. See https://buildkite.com/docs/pipelines/managing-log-output#collapsing-output
  echo "^^^ +++"
  echo "Unit Tests failed!"
fi

echo "--- 🚦 Report Tests Status"
path_pattern="*/build/test-results/*/*.xml"
results_files=()
while IFS= read -r -d '' file; do
  results_files+=("$file")
done < <(find . -path "$path_pattern" -type f -name "*.xml" -print0)

for file in "${results_files[@]}"; do
  if [[ $BUILDKITE_BRANCH == trunk ]] || [[ $BUILDKITE_BRANCH == release/* ]]; then
    annotate_test_failures "$file" --slack "build-and-ship"
  else
    annotate_test_failures "$file"
  fi
done

echo "--- 🧪 Copying test logs for test collector"
mkdir buildkite-test-analytics
cp $test_log_dir buildkite-test-analytics

echo "--- ⚒️ Uploading code coverage"
.buildkite/commands/upload-code-coverage.sh
.buildkite/commands/upload-code-coverage.sh $code_coverage_report

exit $TESTS_EXIT_STATUS
