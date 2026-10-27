#!/usr/bin/env bash

set -euo pipefail

echo "--- :closed_lock_with_key: Installing Secrets"

a8c_secrets_bin_dir="$HOME/.local/bin"
install_a8c-secrets_binary --install-dir "$a8c_secrets_bin_dir"
export PATH="$a8c_secrets_bin_dir:$PATH"

a8c-secrets decrypt --non-interactive
