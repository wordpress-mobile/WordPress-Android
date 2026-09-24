#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type validation; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

BUILD_VARIANT=$1

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- 💾 Diff Merged Manifest (Module: WordPress, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "WordPress" ${BUILD_VARIANT}
