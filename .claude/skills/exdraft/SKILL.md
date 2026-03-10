---
name: exdraft
description: >
  Push current changes and create a draft Pull Request on GitHub with
  the unit-tests-exemption label. This skill should be used when the
  user invokes /exdraft.
---

# Draft Exempt PR

Push the current branch and create a draft Pull Request with the
`unit-tests-exemption` label.

## Steps

### 1. Check current branch and changes

Run `git status` and `git diff` to understand what is staged, unstaged,
and untracked. Run `git log` to review recent commit messages.

### 2. Commit changes (if needed)

If there are uncommitted changes:

- Stage relevant files by name (avoid `git add -A` or `git add .`).
- Do NOT commit files that likely contain secrets.
- Write a concise commit message using a HEREDOC.

### 3. Push to remote

Push the current branch with the `-u` flag if needed.

### 4. Determine the base branch and gather context

Identify the base branch (typically `trunk`). Run:

```bash
git merge-base HEAD trunk
git log <merge-base>..HEAD --oneline
git diff <merge-base>..HEAD
```

Review ALL commits on the branch to understand the full scope.

### 5. Draft the PR title and description

- Keep the PR title short (under 70 characters).
- Read `.github/PULL_REQUEST_TEMPLATE.md` and follow its structure.

### 6. Create the draft PR

Create the PR using `gh pr create --draft --label unit-tests-exemption`:

```bash
gh pr create --draft --label "unit-tests-exemption" \
  --title "the pr title" --body "$(cat <<'EOF'
## Description
...

## Testing instructions
...
EOF
)"
```

### 7. Report the result

Output the PR URL.

## Important Rules

- NEVER push or commit without explicit user approval for THAT
  specific operation.
- NEVER use destructive git commands unless the user explicitly
  requests them.
- NEVER skip hooks unless the user explicitly requests it.
- NEVER push to `main` or `trunk` directly.
- Always create the PR as a **draft** with the
  `unit-tests-exemption` label.
