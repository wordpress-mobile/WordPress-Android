#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type build; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- :hammer_and_wrench: Building"
if [ "$1" = "wordpress" ]; then
  ./gradlew assembleWordpressDebug
fi

if [ "$1" = "jetpack" ]; then
  ./gradlew assembleJetpackDebug
fi
