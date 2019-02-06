# A PR should have at least one label
warn("PR is missing at least one label.") if github.pr_labels.empty?

# Warn when there is a big PR
warn("PR has more than 500 lines of code changing. Consider splitting into smaller PRs if possible.") if git.lines_of_code > 500

# PRs should have a milestone attached
has_milestone = github.pr_json["milestone"] != nil
warn("PR is not assigned to a milestone.", sticky: false) unless has_milestone

# If changes were made to the release notes, there must also be changes to the PlayStoreStrings file.

made_release_notes_change = git.modified_files.include?("WordPress/metadata/release_notes.txt")
made_translation_changes_for_release_notes = git.modified_files.include?("WordPress/metadata/PlayStoreStrings.po")

if made_release_notes_change
    fail("The PlayStoreStrings.po file must be updated any time changes are made to release notes") if ! made_translation_changes_for_release_notes
end