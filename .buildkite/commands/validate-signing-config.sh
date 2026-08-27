#!/usr/bin/env bash

set -euo pipefail

# [DO NOT MERGE] Validation-only step, deleted with the branch it lives on.

SECRETS_DIR="$HOME/.configure/wordpress-android/secrets"
UPLOAD_KEYSTORE="$SECRETS_DIR/upload.jks"
DEBUG_KEYSTORE="$SECRETS_DIR/debug.keystore"
SECRET_PROPERTIES="$SECRETS_DIR/secrets.properties"
REPORT_DIR="build/signing-validation"

failures=0

pass() {
  echo "PASS: $*"
}

fail() {
  echo "FAIL: $*"
  failures=$((failures + 1))
}

# The negative cases run against a throwaway home so the agent's real secrets
# directory is never mutated. Gradle's own home has to be pinned or it would
# follow user.home into the throwaway and re-download every dependency.
signing_report() {
  local destination=$1
  shift
  mkdir -p "$REPORT_DIR"
  # `signingConfigs {}` is evaluated at configuration time, so a reused configuration
  # cache entry would let the negative cases pass without re-reading the disk.
  GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}" \
    ./gradlew :WordPress:signingReport --no-configuration-cache --console=plain "$@" | tee "$destination"
}

variant_block() {
  awk -v variant="$1" '$1 == "Variant:" { keep = ($2 == variant) } keep' "$2"
}

field() {
  sed -n "s/^$1: *//p" | head -1
}

# Symlinks rather than copies: no decrypted secret is duplicated anywhere.
fake_home_with() {
  local home=$1
  shift
  mkdir -p "$home/.configure/wordpress-android/secrets"
  ln -s "$SECRET_PROPERTIES" "$home/.configure/wordpress-android/secrets/secrets.properties"
  for keystore in "$@"; do
    ln -s "$SECRETS_DIR/$keystore" "$home/.configure/wordpress-android/secrets/$keystore"
  done
}

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :file_folder: Where configure_apply put the keystores"
for keystore in "$UPLOAD_KEYSTORE" "$DEBUG_KEYSTORE"; do
  if [ -f "$keystore" ]; then
    pass "configure_apply wrote $keystore"
  else
    fail "configure_apply did not write $keystore"
  fi
done

if [ -e WordPress/upload.jks ]; then
  echo "WARN: WordPress/upload.jks is present in the checkout — stale from a pre-move configure_apply on a reused agent, or the destination change did not take effect"
else
  pass "No upload keystore inside the checkout"
fi

echo "--- :mag: signingReport with the keystores where the build expects them"
signing_report "$REPORT_DIR/with-keystores.txt"

for variant in wordpressRelease jetpackRelease; do
  block=$(variant_block "$variant" "$REPORT_DIR/with-keystores.txt")
  if [ -z "$block" ]; then
    fail "$variant is missing from the report"
    continue
  fi

  config=$(printf '%s\n' "$block" | field Config)
  store=$(printf '%s\n' "$block" | field Store)
  fingerprint=$(printf '%s\n' "$block" | field SHA-256)

  if [ "$config" = "release" ]; then
    pass "$variant is signed by the release config"
  else
    fail "$variant reports 'Config: $config' — the release signing config was not applied"
  fi

  if [ "$store" = "$UPLOAD_KEYSTORE" ]; then
    pass "$variant reads the upload keystore from the out-of-repo secrets directory"
  else
    fail "$variant reads its keystore from '$store', expected '$UPLOAD_KEYSTORE'"
  fi

  if [ -n "$fingerprint" ]; then
    pass "$variant certificate read out of the keystore: SHA-256 $fingerprint"
  else
    fail "$variant reported no certificate — the store password or the key alias did not resolve"
  fi
done

for variant in wordpressDebug jetpackDebug; do
  store=$(variant_block "$variant" "$REPORT_DIR/with-keystores.txt" | field Store)

  if [ "$store" = "$DEBUG_KEYSTORE" ]; then
    pass "$variant reads the shared debug keystore under its new name"
  else
    fail "$variant reads its keystore from '$store', expected '$DEBUG_KEYSTORE'"
  fi
done

scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT

echo "--- :test_tube: signingReport with the credentials present but no upload keystore"
fake_home_with "$scratch/no-upload-keystore" debug.keystore
signing_report "$REPORT_DIR/without-upload-keystore.txt" -Duser.home="$scratch/no-upload-keystore"

if grep -q "^Variant: wordpressRelease$" "$REPORT_DIR/without-upload-keystore.txt"; then
  pass "The report still ran, so the assertions below are about the gate rather than about an empty report"
else
  fail "The report did not list wordpressRelease at all — this negative case proves nothing"
fi

for variant in wordpressRelease jetpackRelease; do
  block=$(variant_block "$variant" "$REPORT_DIR/without-upload-keystore.txt")
  config=$(printf '%s\n' "$block" | field Config)

  if printf '%s\n' "$block" | grep -q "upload.jks"; then
    fail "$variant still points at an upload keystore that is not there"
  elif [ "$config" = "none" ]; then
    pass "$variant drops to 'Config: none' with the credentials still readable, so the keystore file is what gates signing"
  else
    fail "$variant reports 'Config: $config' without the keystore, expected 'none'"
  fi
done

echo "--- :test_tube: signingReport with no shared debug keystore"
fake_home_with "$scratch/no-debug-keystore"
signing_report "$REPORT_DIR/without-debug-keystore.txt" -Duser.home="$scratch/no-debug-keystore"

debug_fallback=$(variant_block wordpressDebug "$REPORT_DIR/without-debug-keystore.txt" | field Store)
if [ "$debug_fallback" = "$DEBUG_KEYSTORE" ]; then
  fail "wordpressDebug still reports the shared debug keystore when it is not there"
else
  pass "wordpressDebug silently falls back to '$debug_fallback', which is why a green prototype build is not on its own evidence that the shared key was used"
fi

echo "--- :white_check_mark: Result"
if [ "$failures" -gt 0 ]; then
  echo "$failures assertion(s) failed"
  exit 1
fi

echo "Every signing-config assertion passed"
