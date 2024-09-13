# frozen_string_literal: true

platform :android do
  # Creates a new release branch from the current trunk for code freeze.
  #
  # @param [Boolean] skip_confirm Whether to skip the confirmation prompt
  #
  # @example
  #   bundle exec fastlane code_freeze
  #   bundle exec fastlane code_freeze skip_confirm:true
  #
  lane :code_freeze do |skip_confirm: false|
    ensure_git_status_clean
    Fastlane::Helper::GitHelper.checkout_and_pull(DEFAULT_BRANCH)
    ensure_git_branch(branch: DEFAULT_BRANCH)

    message = <<-MESSAGE

      Code Freeze:
      • New release branch from #{DEFAULT_BRANCH}: release/#{next_release_version}
      • Current release version and build code: #{current_release_version} (#{current_build_code}).
      • New release version and build code: #{code_freeze_beta_version} (#{next_build_code}).

      Do you want to continue?

    MESSAGE

    UI.important(message)

    UI.user_error!('Aborted by user request') unless skip_confirm || UI.confirm('Do you want to continue?')

    release_branch_name = "release/#{next_release_version}"
    ensure_branch_does_not_exist!(release_branch_name)

    # Create the release branch
    UI.message 'Creating release branch...'
    Fastlane::Helper::GitHelper.create_branch(release_branch_name, from: DEFAULT_BRANCH)
    ensure_git_branch(branch: '^release/')
    UI.success "Done! New release branch is: #{git_branch}"

    # Bump the version and build code
    UI.message 'Bumping beta version and build code...'
    VERSION_FILE.write_version(
      version_name: code_freeze_beta_version,
      version_code: next_build_code
    )
    commit_version_bump
    UI.success "Done! New Beta Version: #{current_beta_version}. New Build Code: #{current_build_code}"

    new_version = current_release_version

    extract_release_notes_for_version(
      version: new_version,
      release_notes_file_path: RELEASE_NOTES_SOURCE_PATH,
      extracted_notes_file_path: release_notes_path('wordpress')
    )
    # Jetpack Release notes are based on WP Release notes
    begin
      # FIXME: Move this logic to release-toolkit?
      FileUtils.cp(release_notes_path('wordpress'), release_notes_path('jetpack'))
      sh('git', 'add', release_notes_path('jetpack'))
      sh('git', 'commit', '-m', "Update draft release notes for Jetpack #{new_version}.")
    end
    cleanup_release_files(files: release_notes_short_paths)
    # Adds empty section for next version
    android_update_release_notes(
      new_version: new_version,
      release_notes_file_path: RELEASE_NOTES_SOURCE_PATH
    )

    UI.message("Jetpack release notes were based on the same ones as WordPress. Don't forget to check #{release_notes_path('jetpack')} and amend them" \
               'as necessary if any item does not apply for Jetpack before sending them to Editorial.')

    push_to_git_remote(tags: false)

    copy_branch_protection(
      repository: GHHELPER_REPO,
      from_branch: DEFAULT_BRANCH,
      to_branch: "release/#{new_version}"
    )

    begin
      # Add ❄️ marker to milestone title to indicate we entered code-freeze
      set_milestone_frozen_marker(
        repository: GHHELPER_REPO,
        milestone: new_version
      )
    rescue StandardError => e
      report_milestone_error(error_title: "Error freezing milestone `#{new_version}`: #{e.message}")
    end
  end

  #####################################################################################
  # complete_code_freeze
  # -----------------------------------------------------------------------------------
  # This lane executes the initial steps planned on code freeze
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane complete_code_freeze [skip_confirm:<skip confirm>]
  #
  # Example:
  # bundle exec fastlane complete_code_freeze
  # bundle exec fastlane complete_code_freeze skip_confirm:true
  #####################################################################################
  desc 'Trigger a release build for a given app after code freeze'
  lane :complete_code_freeze do |options|
    ensure_git_branch(branch: '^release/')
    ensure_git_status_clean

    new_version = current_release_version

    UI.important("Completing code freeze for: #{new_version}")

    UI.user_error!('Aborted by user request') unless options[:skip_confirm] || UI.confirm('Do you want to continue?')

    localize_libraries
    update_frozen_strings_for_translation

    ensure_git_status_clean
    push_to_git_remote(tags: false)

    trigger_beta_build(branch_to_build: "release/#{new_version}")

    create_backmerge_pr
  end

  # Updates a release branch for a new beta release, triggering a beta build using the `.buildkite/beta-builds.yml` pipeline.
  #
  # @param skip_confirm [Boolean] Whether to skip the confirmation prompt
  #
  # @example
  #   bundle exec fastlane new_beta_release skip_confirm:true
  #
  lane :new_beta_release do |skip_confirm: false|
    # Checkout default branch and update
    Fastlane::Helper::GitHelper.checkout_and_pull(DEFAULT_BRANCH)

    # Check local repo status
    ensure_git_status_clean

    # Check branch
    UI.user_error!("Release branch for version #{current_release_version} doesn't exist.") unless Fastlane::Helper::GitHelper.checkout_and_pull(release: current_release_version)

    # Check versions
    message = <<-MESSAGE

      Current beta version: #{current_beta_version}
      New beta version: #{next_beta_version}

      Current build code: #{current_build_code}
      New build code: #{next_build_code}

    MESSAGE

    UI.important(message)

    UI.user_error!('Aborted by user request') unless skip_confirm || UI.confirm('Do you want to continue?')

    update_frozen_strings_for_translation
    download_translations

    # Bump the release version and build code
    UI.message 'Bumping beta version and build code...'
    VERSION_FILE.write_version(
      version_name: next_beta_version,
      version_code: next_build_code
    )
    commit_version_bump
    UI.success "Done! New Beta Version: #{current_beta_version}. New Build Code: #{current_build_code}"

    push_to_git_remote(tags: false)

    trigger_beta_build(branch_to_build: "release/#{current_release_version}")

    create_backmerge_pr
  end

  # Prepares a new hotfix branch cut from the previous tag and bumps the version.
  #
  # @param version_name [String] The version number for the new hotfix (e.g., "10.6.1")
  # @param build_code [String] The build code for the new hotfix (e.g., "1070")
  # @param skip_confirm [Boolean] Whether to skip the confirmation prompt
  #
  # @example
  #   bundle exec fastlane new_hotfix_release version_name:10.6.1 build_code:1070 skip_confirm:true
  #
  lane :new_hotfix_release do |version_name: nil, build_code: nil, skip_confirm: false|
    new_version = version_name || UI.input('Version number for the new hotfix?')
    new_build_code = build_code || UI.input('Version code for the new hotfix?')

    ensure_git_status_clean

    # Parse the provided version into an AppVersion object
    parsed_version = VERSION_FORMATTER.parse(new_version)
    previous_version = VERSION_FORMATTER.release_version(VERSION_CALCULATOR.previous_patch_version(version: parsed_version))

    # Check versions
    message = <<-MESSAGE

      Current release version: #{current_release_version}
      New hotfix version: #{new_version}

      Current build code: #{current_build_code}
      New build code: #{new_build_code}

      Branching from tag: #{previous_version}

    MESSAGE

    UI.important(message)

    UI.user_error!('Aborted by user request') unless skip_confirm || UI.confirm('Do you want to continue?')

    # Check tags
    UI.user_error!("The version `#{new_version}` tag already exists!") if git_tag_exists(tag: new_version)
    UI.user_error!("Version #{previous_version} is not tagged! A hotfix branch cannot be created.") unless git_tag_exists(tag: previous_version)

    # Create the hotfix branch
    UI.message 'Creating hotfix branch...'
    Fastlane::Helper::GitHelper.create_branch("release/#{new_version}", from: previous_version)
    UI.success "Done! New hotfix branch is: #{git_branch}"

    # Bump the hotfix version and build code and write it to the `version.properties` file
    UI.message 'Bumping hotfix version and build code...'
    VERSION_FILE.write_version(
      version_name: new_version,
      version_code: new_build_code
    )
    commit_version_bump
    UI.success "Done! New Release Version: #{current_release_version}. New Build Code: #{current_build_code}"

    push_to_git_remote(tags: false)
  end

  # This lane finalizes the hotfix branch, triggering a release build and closing the milestone.
  #
  # @param skip_confirm [Boolean] Whether to skip the confirmation prompt
  #
  # @example
  #   bundle exec fastlane finalize_hotfix_release skip_confirm:true
  #
  lane :finalize_hotfix_release do |skip_confirm: false|
    ensure_git_branch(branch: '^release/')
    ensure_git_status_clean unless is_ci

    UI.important("Triggering hotfix build for version: #{current_release_version}")

    UI.user_error!('Aborted by user request') unless skip_confirm || UI.confirm('Do you want to continue?')

    trigger_release_build(branch_to_build: "release/#{current_release_version}")

    create_backmerge_pr

    # Close hotfix milestone
    begin
      close_milestone(
        repository: GHHELPER_REPO,
        milestone: current_release_version
      )
    rescue StandardError => e
      report_milestone_error(error_title: "Error closing milestone `#{current_release_version}`: #{e.message}")
    end
  end

  # This lane finalizes a release by updating store metadata and running release checks.
  #
  # @param skip_confirm [Boolean] Whether to skip the confirmation prompt
  #
  # @example
  #   bundle exec fastlane finalize_release(skip_confirm: true)
  #
  lane :finalize_release do |skip_confirm: false|
    UI.user_error!('Please use `finalize_hotfix_release` lane for hotfixes') if android_current_branch_is_hotfix(version_properties_path: VERSION_PROPERTIES_PATH)

    ensure_git_status_clean
    ensure_git_branch(branch: '^release/')

    UI.important("Finalizing release: #{current_release_version}")
    UI.user_error!('Aborted by user request') unless skip_confirm || UI.confirm('Do you want to continue?')

    configure_apply(force: is_ci)

    release_branch = "release/#{current_release_version}"

    # Don't check translation coverage for now since we are finalizing the release in CI
    # check_translations_coverage
    download_translations

    # Bump the release version and build code
    UI.message 'Bumping final release version and build code...'
    VERSION_FILE.write_version(
      version_name: current_release_version,
      version_code: next_build_code
    )
    commit_version_bump
    UI.success "Done! New Release Version: #{current_release_version}. New Build Code: #{current_build_code}"

    version_name = current_release_version
    download_metadata_strings(version: version_name)

    push_to_git_remote(tags: false)

    trigger_release_build(branch_to_build: "release/#{version_name}")

    create_backmerge_pr

    remove_branch_protection(repository: GHHELPER_REPO, branch: release_branch)

    # Close milestone
    begin
      set_milestone_frozen_marker(
        repository: GHHELPER_REPO,
        milestone: version_name,
        freeze: false
      )
      close_milestone(
        repository: GHHELPER_REPO,
        milestone: version_name
      )
    rescue StandardError => e
      report_milestone_error(error_title: "Error closing milestone `#{version}`: #{e.message}")
    end
  end

  lane :check_translations_coverage do |options|
    UI.message('Checking WordPress app strings translation status...')
    check_translation_progress(
      glotpress_url: APP_SPECIFIC_VALUES[:wordpress][:glotpress_appstrings_project],
      abort_on_violations: false,
      skip_confirm: options[:skip_confirm] || false
    )

    UI.message('Checking Jetpack app strings translation status...')
    check_translation_progress(
      glotpress_url: APP_SPECIFIC_VALUES[:jetpack][:glotpress_appstrings_project],
      abort_on_violations: false,
      skip_confirm: options[:skip_confirm] || false
    )

    UI.message('Checking WordPress release notes strings translation status...')
    check_translation_progress(
      glotpress_url: APP_SPECIFIC_VALUES[:wordpress][:glotpress_metadata_project],
      abort_on_violations: false,
      skip_confirm: options[:skip_confirm] || false
    )

    UI.message('Checking Jetpack release notes strings translation status...')
    check_translation_progress(
      glotpress_url: APP_SPECIFIC_VALUES[:jetpack][:glotpress_metadata_project],
      abort_on_violations: false,
      skip_confirm: options[:skip_confirm] || false
    )
  end

  # Triggers a beta build using the `.buildkite/beta-builds.yml` pipeline.
  #
  # @param branch_to_build [String] The branch to build. Defaults to the current git branch.
  #
  # @example
  #   bundle exec fastlane trigger_beta_build branch_to_build:"release/1.2.3"
  lane :trigger_beta_build do |branch_to_build: git_branch|
    trigger_buildkite_release_build(
      branch: branch_to_build,
      beta: true
    )
  end

  # Triggers a release build using the `.buildkite/release-builds.yml` pipeline.
  #
  # @param branch_to_build [String] The branch to build. Defaults to the current git branch.
  #
  # @example
  #   bundle exec fastlane trigger_release_build branch_to_build:"release/1.2.3"
  lane :trigger_release_build do |branch_to_build: git_branch|
    trigger_buildkite_release_build(
      branch: branch_to_build,
      beta: false
    )
  end

  # Creates a GitHub release for the current version.
  #
  # This lane creates a GitHub release for the specified version, attaching existing .aab files
  # in the `build/` directory as release assets. It uses the WordPress and Jetpack release notes
  # as the description. If the `prerelease` parameter is not provided, the pre-release status
  # will be inferred from the version name.
  #
  # @param [String] app The app to create the release for. Can be 'wordpress' or 'jetpack'. If not specified, both apps are included.
  # @param [String] version_name The version name for the release. Defaults to the current version name.
  # @param [Boolean] prerelease Whether this is a pre-release. If not specified, it's inferred from the version name.
  #
  # @example Create a release for both WordPress and Jetpack, inferring pre-release status
  #   bundle exec fastlane create_gh_release
  #
  # @example Create a pre-release for WordPress
  #   bundle exec fastlane create_gh_release app:wordpress prerelease:true
  #
  # @example Create a release for both apps with a specific version
  #   bundle exec fastlane create_gh_release version_name:12.3-rc-4 prerelease:true
  #
  # @example Create a release for Jetpack with a specific version
  #   bundle exec fastlane create_gh_release app:jetpack version_name:12.3-rc-4 prerelease:true
  #
  lane :create_gh_release do |app: nil, version_name: nil, prerelease: nil|
    apps = app.nil? ? %w[wordpress jetpack] : [get_app_name_option!(app: app)]
    versions = version_name.nil? ? [current_version_name] : [version_name]

    download_signed_apks_from_google_play(app: app)

    release_assets = apps.flat_map do |current_app|
      versions.flat_map { |vers| [bundle_file_path(current_app, vers), signed_apk_path(current_app, vers)] }
    end.select { |f| File.exist?(f) }

    release_title = versions.last
    set_prerelease_flag = prerelease.nil? ? /-rc-|alpha-/.match?(release_title) : prerelease

    UI.message("Creating release for #{release_title} with the following assets: #{release_assets.inspect}")

    app_titles = { 'wordpress' => 'WordPress', 'jetpack' => 'Jetpack' }
    tmp_file = File.absolute_path('unified-release-notes.txt')
    unified_notes = apps.map do |current_app|
      notes = File.read(release_notes_path(current_app))
      "## #{app_titles[current_app]}\n\n#{notes}"
    end.join("\n\n")
    File.write(tmp_file, unified_notes)

    create_github_release(
      repository: GHHELPER_REPO,
      version: release_title,
      release_notes_file_path: tmp_file,
      prerelease: set_prerelease_flag,
      release_assets: release_assets.join(',')
    )

    FileUtils.rm(tmp_file)
  end

  #####################################################################################
  # Private lanes
  #####################################################################################

  private_lane :cleanup_release_files do |options|
    files = options[:files]

    files.each do |f|
      File.open(f, 'w') {}
      sh("git add #{f}")
    end

    sh('git diff-index --quiet HEAD || git commit -m "Clean up release files."')
  end

  #####################################################################################
  # Utils
  #####################################################################################
  def get_app_name_option!(options)
    app = options[:app]&.downcase
    validate_app_name!(app)
    app
  end

  def validate_app_name!(app)
    UI.user_error!("Missing 'app' parameter. Expected 'app:wordpress' or 'app:jetpack'") if app.nil?
    UI.user_error!("Invalid 'app' parameter #{app.inspect}. Expected 'wordpress' or 'jetpack'") unless %i[wordpress jetpack].include?(app.to_sym)
  end

  def release_notes_path(app)
    paths = {
      wordpress: File.join(PROJECT_ROOT_FOLDER, 'WordPress', 'metadata', 'release_notes.txt'),
      jetpack: File.join(PROJECT_ROOT_FOLDER, 'WordPress', 'jetpack_metadata', 'release_notes.txt')
    }
    paths[app.to_sym] || paths[:wordpress]
  end

  def release_notes_short_paths
    [
      File.join(PROJECT_ROOT_FOLDER, 'WordPress', 'metadata', 'release_notes_short.txt'),
      File.join(PROJECT_ROOT_FOLDER, 'WordPress', 'jetpack_metadata', 'release_notes_short.txt')
    ]
  end

  def bundle_file_path(app, version_name)
    prefix = APP_SPECIFIC_VALUES[app.to_sym][:bundle_name_prefix]
    File.join(PROJECT_ROOT_FOLDER, 'build', "#{prefix}-#{version_name}.aab")
  end

  def signed_apk_path(app, version_name)
    bundle_file_path(app, version_name).sub('.aab', '.apk')
  end

  def commit_version_bump
    Fastlane::Helper::GitHelper.commit(
      message: 'Bump version number',
      files: VERSION_PROPERTIES_PATH
    )
  end

  def trigger_buildkite_release_build(branch:, beta:)
    pipeline_file = beta ? 'beta-builds.yml' : 'release-builds.yml'
    message = beta ? 'Beta Builds' : 'Release Builds'

    build_url = buildkite_trigger_build(
      buildkite_organization: 'automattic',
      buildkite_pipeline: 'wordpress-android',
      branch: branch,
      pipeline_file: pipeline_file,
      message: message
    )

    message = "This build triggered a build on <code>#{branch}</code>:<br>- #{build_url}"
    buildkite_annotate(style: 'info', context: 'trigger-release-build', message: message) if is_ci
  end

  def create_backmerge_pr
    version = current_release_version

    pr_url = create_release_backmerge_pull_request(
      repository: GHHELPER_REPO,
      source_branch: "release/#{version}",
      labels: ['Releases'],
      milestone_title: next_release_version
    )
  rescue StandardError => e
    error_message = <<-MESSAGE
      Error creating backmerge pull request:

      #{e.message}

      If this is not the first time you are running the release task, the backmerge PR for the version `#{version}` might have already been previously created.
      Please close any previous backmerge PR for `#{version}`, delete the previous merge branch, then run the release task again.
    MESSAGE

    buildkite_annotate(style: 'error', context: 'error-creating-backmerge', message: error_message) if is_ci

    UI.user_error!(error_message)

    pr_url
  end

  def ensure_branch_does_not_exist!(branch_name)
    return unless Fastlane::Helper::GitHelper.branch_exists_on_remote?(branch_name: branch_name)

    error_message = "The branch `#{branch_name}` already exists. Please check first if there is an existing Pull Request that needs to be merged or closed first, " \
                    'or delete the branch to then run again the release task.'

    buildkite_annotate(style: 'error', context: 'error-checking-branch', message: error_message) if is_ci

    UI.user_error!(error_message)
  end

  def report_milestone_error(error_title:)
    error_message = <<-MESSAGE
      #{error_title}

      - If this is not the first time you are running the release task (e.g. retrying because it failed on first attempt), the milestone might have already been closed and this error is expected.
      - Otherwise if this is the first you are running the release task for this version, please investigate the error.
    MESSAGE

    UI.error(error_message)

    buildkite_annotate(style: 'warning', context: 'error-with-milestone', message: error_message) if is_ci
  end
end
