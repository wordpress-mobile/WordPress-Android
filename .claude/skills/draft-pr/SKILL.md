---
name: draft-pr
description: >
  Push current changes and create a draft Pull Request on GitHub.
  This skill should be used when the user asks to create a PR,
  draft PR, or invokes /draft-pr.
---

# Draft PR

Push current changes and create a draft Pull Request on GitHub.

## Steps

### 1. Check current branch and changes

Run `git status` and `git diff` to understand what is staged, unstaged,
and untracked. Run `git log` to review recent commit messages and the
repository's commit message style.

### 2. Confirm with the user

Before committing or pushing, present a summary of the changes and ask
the user for explicit permission to proceed with the commit and push.

### 3. Commit changes (if needed)

If there are uncommitted changes:

- Stage relevant files by name (avoid `git add -A` or `git add .` to
  prevent accidentally including secrets or large binaries).
- Do NOT commit files that likely contain secrets (`.env`,
  `credentials.json`, etc.). Warn the user if they specifically request
  to commit those files.
- Write a concise commit message (1-2 sentences) that focuses on the
  "why" rather than the "what". End it with:
  `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>`
- Use a HEREDOC to pass the commit message to ensure correct formatting.

### 4. Push to remote

Push the current branch to the remote with the `-u` flag to set up
tracking if needed.

### 5. Determine the base branch and gather context

Identify the base branch (typically `trunk`). Run `git merge-base` to
find the common ancestor. Then run:

```bash
git log <merge-base>..HEAD --oneline
git diff <merge-base>..HEAD
```

Review ALL commits on the branch (not just the latest) to understand the
full scope of changes.

### 6. Read changed files for context

For each file changed on the branch, read the full file to understand
the surrounding context beyond just the diff hunks.

### 7. Draft the PR title and description

Follow these rules:

- Keep the PR title short (under 70 characters). Use the
  description/body for details.
- Try to extract a Linear issue identifier from the branch name (e.g.,
  `cmm-1234` from `feature/cmm-1234-some-description`). If found,
  prefix the PR title with it in uppercase: `CMM-1234: Title here`. If
  the branch name does not contain a recognizable Linear identifier,
  omit the prefix.
- Read `.github/PULL_REQUEST_TEMPLATE.md` and follow its structure and
  formatting instructions for the PR body.

### 8. Present the PR for approval

Output the PR title and full description as raw markdown inside a code
block so the user can review and copy it. Ask the user to confirm or
request changes before creating the PR.

### 9. Create the draft PR

Once approved, create the PR using `gh pr create --draft` with a HEREDOC
for the body:

```bash
gh pr create --draft --title "the pr title" --body "$(cat <<'EOF'
## Description
...

## Testing instructions
...
EOF
)"
```

### 10. Report the result

Output the PR URL so the user can access it directly.

## Important Rules

- NEVER push or commit without explicit user approval for THAT specific
  operation.
- NEVER use destructive git commands (`push --force`, `reset --hard`,
  etc.) unless the user explicitly requests them.
- NEVER skip hooks (`--no-verify`) unless the user explicitly requests
  it.
- NEVER amend previous commits unless the user explicitly requests it.
  If a pre-commit hook fails, fix the issue and create a NEW commit.
- NEVER push to `main` or `trunk` directly.
- Always create the PR as a **draft** (`--draft` flag).
