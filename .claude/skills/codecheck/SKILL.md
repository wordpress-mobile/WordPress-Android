---
name: codecheck
description: >
  Run all three code quality checks (detekt, checkstyle, lint),
  read the reports, and propose fixes for any issues found.
---

# Code Check

Run all three code quality checks, read the reports, and propose
fixes for any issues found.

## Steps

### 1. Detect the main branch

Detect the main/default branch (typically `trunk` or `main`):

```bash
git symbolic-ref refs/remotes/origin/HEAD 2>/dev/null \
  | sed 's@^refs/remotes/origin/@@'
```

If that fails, check if `trunk` or `main` exists and use whichever
is found. Store this as `<main-branch>` for later steps.

### 2. Run all three checks in parallel

Run these Gradle tasks:

```bash
./gradlew detekt
./gradlew checkstyle
./gradlew lintWordPressRelease
```

Run all three in parallel to save time. Allow each to complete even
if others fail.

### 3. Read the reports

After the checks complete, read the generated report files:

- **Detekt**: `WordPress/build/reports/detekt/detekt.html`
- **Checkstyle**: `WordPress/build/reports/checkstyle/checkstyle.html`
- **Lint**: `WordPress/build/reports/lint-results-wordpressRelease.html`

Parse each report to extract the list of issues (file, line, rule,
message).

### 4. Filter to relevant issues

Focus on issues in files that were changed on the current branch.
To determine changed files:

```bash
git diff --name-only $(git merge-base <main-branch> HEAD)..HEAD
```

If no branch changes are detected (e.g., on trunk), report all
issues found.

### 5. Present a summary

Output a summary table or grouped list of issues organized by tool:

- **Detekt issues** (count and details)
- **Checkstyle issues** (count and details)
- **Lint issues** (count and details)

For each issue include the file path, line number, rule name, and
the message.

### 6. Propose fixes

For each issue, propose a specific fix. Group fixes by file. Show
the current code and what it should be changed to.

### 7. Ask for approval

Present the proposed fixes and ask the user which ones to apply
before making any changes.

## Important Rules

- Do NOT make any code changes until the user approves.
- Respect the project's code style (120-char line limit, Kotlin
  conventions, no empty lines after opening braces).
- Use TODO instead of FIXME in any comments.
- Do not suppress warnings unless the user explicitly approves.
