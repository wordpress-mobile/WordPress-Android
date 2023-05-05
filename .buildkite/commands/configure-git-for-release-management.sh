#!/bin/bash -eu

# Git command line client is not configured in Buildkite. Temporarily, we configure it in each step.
# Later on, we should be able to configure the agent instead.
curl -L https://api.github.com/meta | jq -r '.ssh_keys | .[]' | sed -e 's/^/github.com /' >> ~/.ssh/known_hosts
git config --global user.email "mobile+wpmobilebot@automattic.com"
git config --global user.name "Buildkite"
