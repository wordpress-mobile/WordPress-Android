#!/bin/bash -eu

# Usage: should-skip-job.sh [validation|lint|build|library-lint]
# Prints a skip reason, or false to run. Both exit 0; invalid arguments exit 15.
# shared-pipeline-vars uses this output for pipeline skip values before jobs get an agent.
# Only webhook PRs can skip. WORDPRESS_CI_FULL_BUILD=1 forces a full run.

run_job() { echo false; exit 0; }

job="${1:-}"
case "$job" in
  validation|lint|build|library-lint) ;;
  *) echo "Unknown job type: $job" >&2; exit 15 ;;
esac

if [[ "${WORDPRESS_CI_FULL_BUILD:-}" == 1 || "${BUILDKITE_SOURCE:-}" != webhook ||
      -n "${BUILDKITE_TAG:-}" || ! "${BUILDKITE_PULL_REQUEST:-}" =~ ^[0-9]+$ ]]; then
  run_job
fi

# Preserve the existing documentation, tooling and version exclusions.
PATTERNS=(
  '*.md' '*.po' '*.pot' '*.txt' '.gitignore' 'config/**'
  'fastlane/**' 'Gemfile' 'Gemfile.lock' 'version.properties'
)
case "$job" in
  validation) PATTERNS+=('**/strings.xml') ;;
  library-lint)
    # Library sources and module build files still run lint.
    PATTERNS+=('WordPress/src/*' 'WordPress/metadata/*' 'WordPress/jetpack_metadata/*' 'docs/*')
    ;;
esac

# Empty diffs, unknown paths and errors keep the job.
if pr_changed_files --all-match "${PATTERNS[@]}"; then
  echo "No relevant files changed"
else
  run_job
fi
