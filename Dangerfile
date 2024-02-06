# frozen_string_literal: true

github.dismiss_out_of_range_messages

# `files: []` forces rubocop to scan all files, not just the ones modified in the PR
rubocop.lint(files: [], force_exclusion: true, inline_comment: true, fail_on_inline_comment: true, include_cop_names: true)

manifest_pr_checker.check_gemfile_lock_updated

android_release_checker.check_release_notes_and_play_store_strings

android_strings_checker.check_strings_do_not_refer_resource

# skip remaining checks if we're during the release process
if github.pr_labels.include?('Releases')
  message('This PR has the `Releases` label: some checks will be skipped.')
  return
end

common_release_checker.check_internal_release_notes_changed(report_type: :message)

android_release_checker.check_modified_strings_on_release

labels_checker.check(
  do_not_merge_labels: ['Do Not Merge'],
  required_labels: [//],
  required_labels_error: 'PR requires at least one label.'
)

view_changes_checker.check

pr_size_checker.check_diff_size(
  max_size: 300,
  file_selector: ->(path) { !path.include?('/src/test') }
)

android_unit_test_checker.check_missing_tests

# skip check for draft PRs and for WIP features unless the PR is against the main branch or release branch
unless github.pr_draft? || (github_utils.wip_feature? && !(github_utils.release_branch? || github_utils.main_branch?))
  milestone_checker.check_milestone_due_date(days_before_due: 4)
end
