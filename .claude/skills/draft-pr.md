# Skill: draft-pr

Create a draft pull request for the current branch using `gh`.

## Trigger

When the user asks to create a PR, draft PR, or invokes `/draft-pr`.

## Instructions

### 1. Gather context

Run these commands in parallel:

- `git status` (never use `-uall`)
- `git diff` to see staged and unstaged changes
- `git branch --show-current` to get the current branch name
- Check if the branch tracks a remote and is up to date
- `git log trunk..HEAD --oneline` and `git diff trunk...HEAD` to understand
  all commits on this branch

### 2. Determine base branch

- Default base branch: `trunk`
- If the current branch was cut from a release branch
  (`release/*`), use that as base instead.

### 3. Draft the PR title

Use the format `CMM-XXXX: Short description` where `CMM-XXXX` is a ticket
identifier extracted from the branch name (e.g. branch
`issue/cmm-1234-some-feature` → title `CMM-1234: Some feature`).
If the branch name does not contain a `CMM-XXXX` identifier, omit it and
use a plain descriptive title.

### 4. Draft the PR description

Analyze **all** commits on the branch (not just the latest) and write a PR
description following the repository PR template
(`.github/PULL_REQUEST_TEMPLATE.md`).

#### Format rules

- **Description section**: a brief explanation of what the PR changes and why.
- **Testing instructions section**: concrete steps a reviewer can follow.
  - Use numbered lists for action steps.
  - Use checkboxes (`- [ ]`) for verifications and expected outcomes.
  - Organise by test-case title when there are multiple scenarios.
- **Always output the PR description as raw markdown inside a code block** so
  the user can review and copy it before the PR is created.

### 5. Ask for user confirmation

Present the title and body to the user and wait for approval before
creating the PR.

### 6. Push and create the PR

Once approved, run the following (parallelise independent steps):

- Push the branch with `-u` if it has not been pushed yet.
- Create the PR as a **draft**:

```bash
gh pr create --draft --title "<title>" --body "$(cat <<'EOF'
<body>
EOF
)"
```

### 7. Return the PR URL

Print the new PR URL so the user can open it directly.
