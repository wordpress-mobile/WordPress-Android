# A PR should have at least one label
warn("PR is missing at least one label.") if github.pr_labels.empty?

# Warn when there is a big PR
warn("PR has more than 500 lines of code changing. Consider splitting into smaller PRs if possible.") if git.lines_of_code > 500

# PRs should have a milestone attached
has_milestone = github.pr_json["milestone"] != nil
warn("PR is not assigned to a milestone.", sticky: false) unless has_milestone

# Lint
android_lint.gradle_task = "lintVanillaRelease"
android_lint.lint(inline_mode: true)
