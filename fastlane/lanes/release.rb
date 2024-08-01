# frozen_string_literal: true

platform :android do
  #####################################################################################
  # code_freeze
  # -----------------------------------------------------------------------------------
  # This lane executes the steps planned on code freeze
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane code_freeze [update_release_branch_version:<update flag>] [skip_confirm:<skip confirm>]
  #
  # Example:
  # bundle exec fastlane code_freeze
  # bundle exec fastlane code_freeze update_release_branch_version:false
  # bundle exec fastlane code_freeze skip_confirm:true
  #####################################################################################
  desc 'Creates a new release branch from the current trunk'
  lane :code_freeze do |options|
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

    UI.user_error!('Aborted by user request') unless options[:skip_confirm] || UI.confirm('Do you want to continue?')

    # Create the release branch
    UI.message 'Creating release branch...'
    Fastlane::Helper::GitHelper.create_branch("release/#{next_release_version}", from: DEFAULT_BRANCH)
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
    set_milestone_frozen_marker(repository: GHHELPER_REPO, milestone: new_version)
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

    # Create an intermediate branch
    Fastlane::Helper::GitHelper.create_branch("merge/#{new_version}-code-freeze-into-trunk")
    push_to_git_remote(tags: false)
    create_release_management_pull_request('trunk', "Merge #{new_version} code freeze into trunk")
  end

  #####################################################################################
  # new_beta_release
  # -----------------------------------------------------------------------------------
  # This lane updates the release branch for a new beta release. It will update the
  # current release branch by default.
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane new_beta_release [skip_confirm:<skip confirm>]
  #
  # Example:
  # bundle exec fastlane new_beta_release
  # bundle exec fastlane new_beta_release skip_confirm:true
  #####################################################################################
  desc 'Updates a release branch for a new beta release'
  lane :new_beta_release do |options|
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

    UI.user_error!('Aborted by user request') unless options[:skip_confirm] || UI.confirm('Do you want to continue?')

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

    release_branch = "release/#{current_release_version}"
    release_version = current_version_name

    # Create an intermediate branch
    new_beta_branch_name = "new_beta/#{release_version}"
    Fastlane::Helper::GitHelper.create_branch(new_beta_branch_name)

    push_to_git_remote(tags: false)

    trigger_beta_build(branch_to_build: new_beta_branch_name)

    create_release_management_pull_request(release_branch, "Merge #{release_version} to #{release_branch}")
  end

  #####################################################################################
  # new_hotfix_release
  # -----------------------------------------------------------------------------------
  # This lane updates the release branch for a new hotfix release.
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane new_hotfix_release [skip_confirm:<skip confirm>] [version_name:<x.y.z>] [build_code:<nnnn>]
  #
  # Example:
  # bundle exec fastlane new_hotfix_release version_name:10.6.1 build_code:1070
  #####################################################################################
  desc 'Prepare a new hotfix branch cut from the previous tag, and bump the version'
  lane :new_hotfix_release do |options|
    new_version = options[:version_name] || UI.input('Version number for the new hotfix?')
    new_build_code = options[:build_code] || UI.input('Version code for the new hotfix?')

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

    UI.user_error!('Aborted by user request') unless options[:skip_confirm] || UI.confirm('Do you want to continue?')

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

  #####################################################################################
  # finalize_hotfix_release
  # -----------------------------------------------------------------------------------
  # This lane finalizes the hotfix branch.
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane finalize_hotfix_release
  #
  # Example:
  # bundle exec fastlane finalize_hotfix_release
  #####################################################################################
  desc 'Finalizes a hotfix release by triggering a release build'
  lane :finalize_hotfix_release do |options|
    ensure_git_branch(branch: '^release/')
    ensure_git_status_clean unless is_ci

    UI.important("Triggering hotfix build for version: #{current_release_version}")

    UI.user_error!('Aborted by user request') unless options[:skip_confirm] || UI.confirm('Do you want to continue?')

    trigger_release_build(branch_to_build: "release/#{current_release_version}")
  end

  #####################################################################################
  # finalize_release
  # -----------------------------------------------------------------------------------
  # This lane finalize a release: updates store metadata and runs the release checks
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane finalize_release [skip_confirm:<skip confirm>]
  #
  # Example:
  # bundle exec fastlane finalize_release
  # bundle exec fastlane finalize_release skip_confirm:true
  #####################################################################################
  desc 'Updates store metadata and runs the release checks'
  lane :finalize_release do |options|
    UI.user_error!('Please use `finalize_hotfix_release` lane for hotfixes') if android_current_branch_is_hotfix(version_properties_path: VERSION_PROPERTIES_PATH)

    ensure_git_status_clean
    ensure_git_branch(branch: '^release/')

    UI.important("Finalizing release: #{current_release_version}")

    UI.user_error!('Aborted by user request') unless options[:skip_confirm] || UI.confirm('Do you want to continue?')

    configure_apply(force: is_ci)

    release_branch = "release/#{current_release_version}"

    # Remove branch protection first, so that we can push the final commits directly to the release branch
    remove_branch_protection(repository: GHHELPER_REPO, branch: release_branch)

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

    # Wrap up
    set_milestone_frozen_marker(repository: GHHELPER_REPO, milestone: version_name, freeze: false)
    create_new_milestone(repository: GHHELPER_REPO)
    close_milestone(repository: GHHELPER_REPO, milestone: version_name)

    # Trigger release build
    trigger_release_build(branch_to_build: "release/#{version_name}")

    # Create an intermediate branch
    Fastlane::Helper::GitHelper.create_branch("merge/#{version_name}-final-into-trunk")
    push_to_git_remote(tags: false)
    create_release_management_pull_request('trunk', "Merge #{version_name} final into trunk")
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

  #####################################################################################
  # trigger_beta_build
  # -----------------------------------------------------------------------------------
  # This lane triggers a beta build using the `.buildkite/beta-builds.yml` pipeline.
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane trigger_beta_build branch_to_build:<branch_name>
  #
  #####################################################################################
  lane :trigger_beta_build do |options|
    buildkite_trigger_build(
      buildkite_organization: 'automattic',
      buildkite_pipeline: 'wordpress-android',
      branch: options[:branch_to_build] || git_branch,
      pipeline_file: 'beta-builds.yml',
      message: 'Beta Builds'
    )
  end

  #####################################################################################
  # trigger_release_build
  # -----------------------------------------------------------------------------------
  # This lane triggers a release build using the `.buildkite/release-builds.yml`
  # pipeline.
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane trigger_release_build branch_to_build:<branch_name>
  #
  #####################################################################################
  lane :trigger_release_build do |options|
    buildkite_trigger_build(
      buildkite_organization: 'automattic',
      buildkite_pipeline: 'wordpress-android',
      branch: options[:branch_to_build] || git_branch,
      pipeline_file: 'release-builds.yml',
      message: 'Release Builds'
    )
  end

  #####################################################################################
  # create_gh_release
  # -----------------------------------------------------------------------------------
  # This lane creates a GitHub release for the current version
  #   - Attaching the existing .aab files in `build/` as the release assets
  #   - Using the WP & JP release_notes.txt as description
  #   - If `prerelease:<true|false>`` is not provided, the pre-release status will be inferred from the version name
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane create_gh_release [app:<wordpress|jetpack>] [version_name:string] [prerelease:<true|false>]
  #
  # Examples:
  # bundle exec fastlane create_gh_release     # Guesses prerelease status based on version name. Includes existing assets for WPBeta+JPBeta
  # bundle exec fastlane create_gh_release app:wordpress prerelease:true                        # Includes existing assets for WPBeta
  # bundle exec fastlane create_gh_release version_name:12.3-rc-4 prerelease:true             # Includes existing assets for WPBeta+JPBeta 12.3-rc-4
  # bundle exec fastlane create_gh_release app:jetpack version_name:12.3-rc-4 prerelease:true # Includes only existing asset for JPBeta 12.3-rc-4
  #####################################################################################
  lane :create_gh_release do |options|
    apps = options[:app].nil? ? %w[wordpress jetpack] : [get_app_name_option!(options)]
    versions = options[:version_name].nil? ? [current_version_name] : [options[:version_name]]

    download_signed_apks_from_google_play(app: options[:app])

    release_assets = apps.flat_map do |app|
      versions.flat_map { |vers| [bundle_file_path(app, vers), signed_apk_path(app, vers)] }
    end.select { |f| File.exist?(f) }

    release_title = versions.last
    set_prerelease_flag = options[:prerelease].nil? ? /-rc-|alpha-/.match?(release_title) : options[:prerelease]

    UI.message("Creating release for #{release_title} with the following assets: #{release_assets.inspect}")

    app_titles = { 'wordpress' => 'WordPress', 'jetpack' => 'Jetpack' }
    tmp_file = File.absolute_path('unified-release-notes.txt')
    unified_notes = apps.map do |app|
      notes = File.read(release_notes_path(app))
      "## #{app_titles[app]}\n\n#{notes}"
    end.join("\n\n")
    File.write(tmp_file, unified_notes)

    create_release(
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
end
