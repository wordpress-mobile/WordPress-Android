#!/bin/bash -eu

# Usage: should-skip-job.sh --job-type [validation|build|lint]
# --job-type validation: For jobs like unit/instrumented tests, manifest validation…
#     Skip when changes are limited to documentation, tooling, non-code files, and localization files
# --job-type lint: For jobs doing linting, especially ones that might include linting of localization files
#     Skip when changes are limited to documentation, tooling, and non-code files.
#     Does not skip if localization files changed (so that linter can run to report translation inconsistencies)
# --job-type build: For jobs building an apk binary, e.g. Assemble, Prototype Builds…
#     Skip when changes are limited to documentation, tooling, and non-code files
#
# Return codes:
# 0 - Job should be skipped (script also handles displaying the message and annotation)
# 1 - Job should not be skipped
# 15 - Error in script parameters

COMMON_PATTERNS=(
  "*.md"
  "*.pot"
  "*.txt"
  ".gitignore"
  "config/**"
  "fastlane/**"
  "Gemfile"
  "Gemfile.lock"
  "gradle/**"
  "version.properties"
)

# Check if arguments are valid
if [ -z "${1:-}" ] || [ "$1" != "--job-type" ] || [ -z "${2:-}" ]; then
  echo "Error: Must specify --job-type [validation|build|lint]"
  buildkite-agent step cancel
  exit 15
fi

# Function to display skip message and create annotation
show_skip_message() {
  local job_type=$1
  local message="Skipping ${BUILDKITE_LABEL:-Job} - no relevant files changed"
  local context="skip-$(echo "${BUILDKITE_LABEL:-$job_type}" | sed -E -e 's/[^[:alnum:]]+/-/g' | tr A-Z a-z)"
  
  echo "$message" | buildkite-agent annotate --style "info" --context "$context"
  echo "$message"
}

job_type="$2"
case "$job_type" in
  "validation")
    # We should skip if changes are limited to documentation, tooling, non-code files, and localization files
    PATTERNS=("${COMMON_PATTERNS[@]}" "**/strings.xml")
    if pr_changed_files --all-match "${PATTERNS[@]}"; then
      show_skip_message "$job_type"
      exit 0
    fi
    exit 1
    ;;
  "build"|"lint")
    # We should skip if changes are limited to documentation, tooling, and non-code files
    # We'll let the job run (won't skip) if PR includes changes in localization files though
    PATTERNS=("${COMMON_PATTERNS[@]}")
    if pr_changed_files --all-match "${PATTERNS[@]}"; then
      show_skip_message "$job_type"
      exit 0
    fi
    exit 1
    ;;
  *)
    echo "Error: Job type must be either 'validation', 'build', or 'lint'"
    buildkite-agent step cancel
    exit 15
    ;;
esac
