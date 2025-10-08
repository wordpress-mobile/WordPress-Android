#!/bin/bash -eu

# Check if unit tests failed (matches both hard_failed and soft_failed outcomes)
if buildkite-agent step get outcome --step unit-tests | grep -q "failed"; then
  comment_on_pr --id claude-test-analysis "$(cat <<EOF

## 🤖 Test Failure Analysis

Your tests failed. Claude has analyzed the failures - <a href="${BUILDKITE_BUILD_URL}/annotations#annotation-claude-analysis-${BUILDKITE_BUILD_ID}" target="_blank">check the annotation</a> for details.
EOF
)"

else
  # Remove the comment if tests are now passing
  comment_on_pr --id claude-test-analysis --if-exist delete
fi
