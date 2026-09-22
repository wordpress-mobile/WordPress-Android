#!/usr/bin/env bash

set -euo pipefail

# Secrets are mid-migration from configure to a8c-secrets, so both decrypt here until the
# last file has moved over.

echo "--- :closed_lock_with_key: Installing Secrets"

a8c_secrets_bin_dir="$HOME/.local/bin"
install_a8c-secrets_binary --install-dir "$a8c_secrets_bin_dir"
export PATH="$a8c_secrets_bin_dir:$PATH"

a8c-secrets decrypt --non-interactive

bundle exec fastlane run configure_apply
