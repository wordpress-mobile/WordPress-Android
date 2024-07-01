#!/bin/bash -eu

# This script needs to be source'd as use-bot-for-git exports a variable and this needs to be visible outside
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  echo "This script must be 'source'd (instead of being called directly as an executable) to work properly"
  exit 1
fi

echo '--- :robot_face: Use bot for git operations'
# shellcheck disable=SC1091
source use-bot-for-git
