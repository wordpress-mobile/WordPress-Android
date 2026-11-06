#!/bin/bash -eu

# Usage: should-skip-job.sh --job-type [validation|lint|build|library-lint]
# --job-type library-lint: Skip standalone library lint for app-source-only PR changes.
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
  "*.po"
  "*.pot"
  "*.txt"
  ".gitignore"
  "config/**"
  "fastlane/**"
  "Gemfile"
  "Gemfile.lock"
  "version.properties"
)

# Define constants for job types
VALIDATION="validation"
BUILD="build"
LINT="lint"

# Check if arguments are valid
if [ -z "${1:-}" ] || [ "$1" != "--job-type" ] || [ -z "${2:-}" ]; then
  echo "Error: Must specify --job-type [$VALIDATION|$BUILD|$LINT|library-lint]"
  buildkite-agent step cancel
  exit 15
fi

# Function to display skip message and create annotation
show_skip_message() {
  local job_type=$1
  local message="Skipped ${BUILDKITE_LABEL:-Job} - no relevant files changed"
  local context="skip-$(echo "${BUILDKITE_LABEL:-$job_type}" | sed -E -e 's/[^[:alnum:]]+/-/g' | tr A-Z a-z)"
  
  echo "$message" | buildkite-agent annotate --style "info" --context "$context"
  echo "$message"
}

job_type="$2"
case "$job_type" in
  library-lint)
    # Extend the existing lint exclusions with app-only paths.
    # Library sources and module build files still matter.
    if [[ "${BUILDKITE_SOURCE:-}" != webhook || ! "${BUILDKITE_PULL_REQUEST:-}" =~ ^[0-9]+$ ]]; then
      exit 1
    fi

    # Include both sides of renames, including moves from a library into the app.
    config_index=${GIT_CONFIG_COUNT:-0}
    export GIT_CONFIG_COUNT=$((config_index + 1))
    export "GIT_CONFIG_KEY_${config_index}=diff.renames" "GIT_CONFIG_VALUE_${config_index}=false"

    PATTERNS=(
      "${COMMON_PATTERNS[@]}"
      'WordPress/src/*' 'WordPress/metadata/*' 'WordPress/jetpack_metadata/*'
      'docs/*'
    )
    if pr_changed_files --all-match "${PATTERNS[@]}"; then
      show_skip_message "$job_type"
      exit 0
    fi
    exit 1
    ;;
  $VALIDATION)
    # We should skip if changes are limited to documentation, tooling, non-code files, and localization files
    PATTERNS=("${COMMON_PATTERNS[@]}" "**/strings.xml")
    if pr_changed_files --all-match "${PATTERNS[@]}"; then
      show_skip_message "$job_type"
      exit 0
    fi
    exit 1
    ;;
  $BUILD|$LINT)
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
    echo "Error: Job type must be '$VALIDATION', '$BUILD', '$LINT', or 'library-lint'"
    buildkite-agent step cancel
    exit 15
    ;;
esac
