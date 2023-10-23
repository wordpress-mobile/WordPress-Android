#!/bin/bash -eu

# Git command line client is not configured in Buildkite. Temporarily, we configure it in each step.
# Later on, we should be able to configure the agent instead.
curl -L https://api.github.com/meta | jq -r '.ssh_keys | .[]' | sed -e 's/^/github.com /' >> ~/.ssh/known_hosts
git config --global user.email "mobile+wpmobilebot@automattic.com"
git config --global user.name "Automattic Release Bot"

# Buildkite is currently using the https url to checkout. We need to override it to be able to use the deploy key.
git remote set-url origin git@github.com:wordpress-mobile/WordPress-Android.git
