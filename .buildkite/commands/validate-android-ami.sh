#!/usr/bin/env bash

set -euo pipefail

EXPECTED_AMI="ami-0db49da8f2bcc776d"

fail() {
  echo "^^^ +++"
  echo "❌ $*"
  exit 1
}

assert_ruby() {
  local expected=$1 actual
  actual=$(ruby -e 'print RUBY_VERSION')
  [[ "$actual" == "$expected" ]] || fail "Expected Ruby $expected, got $actual (RBENV_VERSION=${RBENV_VERSION:-unset})"
  echo "✅ Ruby $actual (RBENV_VERSION=${RBENV_VERSION:-unset})"
}

echo "--- :aws: Instance runs the target AMI"
token=$(curl -sf -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')
ami=$(curl -sf -H "X-aws-ec2-metadata-token: $token" http://169.254.169.254/latest/meta-data/ami-id)
[[ "$ami" == "$EXPECTED_AMI" ]] || fail "Expected AMI $EXPECTED_AMI, got $ami"
echo "✅ AMI $ami"

echo "--- :buildkite: Agent is v4"
agent_version=$(buildkite-agent --version)
echo "$agent_version"
[[ "$agent_version" =~ version\ 4\. ]] || fail "Expected agent v4, got: $agent_version"

echo "--- :ruby: Rubies"
rbenv versions
assert_ruby "$(cat .ruby-version)"
for v in 3.4.13 3.3.14 4.0.10; do
  RBENV_VERSION=$v assert_ruby "$v"
done

echo "--- :ruby: Dropped Ruby 3.2.2 fails through rbenv"
if output=$(RBENV_VERSION=3.2.2 ruby -v 2>&1); then
  fail "Ruby 3.2.2 unexpectedly available: $output"
fi
grep -q "version \`3.2.2' is not installed" <<<"$output" || fail "3.2.2 failed for an unexpected reason: $output"
echo "✅ $output"

echo "--- :rubygems: Bundler and fastlane on the pinned Ruby"
install_gems
bundle exec fastlane check_declared_locales_consistency app:wordpress

echo "--- :gradle: Gradle, forced to execute"
java -version
./gradlew checkstyle --rerun-tasks
