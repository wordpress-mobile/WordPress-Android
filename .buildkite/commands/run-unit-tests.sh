#!/bin/bash -eu

echo "--- 🧪 Testing"
set +e
if [ "$1" == "wordpress" ]; then
    test_suite="testWordpressVanillaRelease"
elif [ "$1" == "processors" ]; then
    test_suite=":libs:processors:test"
elif [ "$1" == "image-editor" ]; then
    test_suite=":libs:image-editor:test"
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

exit $TESTS_EXIT_STATUS
