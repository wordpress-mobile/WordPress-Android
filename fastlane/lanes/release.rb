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
    android_codefreeze_prechecks(skip_confirm: options[:skip_confirm])

    android_bump_version_release()
    new_version = android_get_app_version()

    extract_release_notes_for_version(
      version: new_version,
      release_notes_file_path: "#{ENV['PROJECT_ROOT_FOLDER']}RELEASE-NOTES.txt",
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

    android_update_release_notes(new_version: new_version) # Adds empty section for next version
    setbranchprotection(repository: GHHELPER_REPO, branch: "release/#{new_version}")
    setfrozentag(repository: GHHELPER_REPO, milestone: new_version)

    UI.message("Jetpack release notes were based on the same ones as WordPress. Don't forget to check #{release_notes_path('jetpack')} and amend them as necessary if any item does not apply for Jetpack before sending them to Editorial.")
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
    android_completecodefreeze_prechecks(skip_confirm: options[:skip_confirm])

    localize_libraries
    update_frozen_strings_for_translation

    ensure_git_status_clean
    push_to_git_remote

    new_version = android_get_app_version()
    trigger_beta_build(branch_to_build: "release/#{new_version}")
  end

  #####################################################################################
  # new_beta_release
  # -----------------------------------------------------------------------------------
  # This lane updates the release branch for a new beta release. It will update the
  # current release branch by default. If you want to update a different branch
  # (i.e. hotfix branch) pass the related version with the 'base_version' param
  # (example: base_version:10.6.1 will work on the 10.6.1 branch)
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane new_beta_release [skip_confirm:<skip confirm>] [base_version:<version>]
  #
  # Example:
  # bundle exec fastlane new_beta_release
  # bundle exec fastlane new_beta_release skip_confirm:true
  # bundle exec fastlane new_beta_release base_version:10.6.1
  #####################################################################################
  desc 'Updates a release branch for a new beta release'
  lane :new_beta_release do |options|
    android_betabuild_prechecks(base_version: options[:base_version], skip_confirm: options[:skip_confirm])
    update_frozen_strings_for_translation
    download_translations()
    android_bump_version_beta()
    next unless UI.confirm('Ready for CI build')

    new_version = android_get_app_version()
    trigger_beta_build(branch_to_build: "release/#{new_version}")
  end

  #####################################################################################
  # new_hotfix_release
  # -----------------------------------------------------------------------------------
  # This lane updates the release branch for a new hotfix release.
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane new_hotfix_release [skip_confirm:<skip confirm>] [version_name:<x.y.z>] [version_code:<nnnn>]
  #
  # Example:
  # bundle exec fastlane new_hotfix_release version_name:10.6.1 version_code:1070
  #####################################################################################
  desc 'Prepare a new hotfix branch cut from the previous tag, and bump the version'
  lane :new_hotfix_release do |options|
    hotfix_version = options[:version_name] || UI.input('Version number for the new hotfix?')
    previous_tag = android_hotfix_prechecks(version_name: hotfix_version, skip_confirm: options[:skip_confirm])
    android_bump_version_hotfix(previous_version_name: previous_tag, version_name: hotfix_version, version_code: options[:version_code])
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
    new_version = android_get_app_version()
    trigger_release_build(branch_to_build: "release/#{new_version}")
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
    UI.user_error!('Please use `finalize_hotfix_release` lane for hotfixes') if android_current_branch_is_hotfix()

    android_finalize_prechecks(skip_confirm: options[:skip_confirm])
    configure_apply(force: is_ci)

    check_translations_coverage()
    download_translations()

    android_bump_version_final_release()
    version = android_get_release_version()
    download_metadata_strings(version: version['name'], build_number: version['code'])

    # Wrap up
    removebranchprotection(repository: GHHELPER_REPO, branch: "release/#{version['name']}")
    setfrozentag(repository: GHHELPER_REPO, milestone: version['name'], freeze: false)
    create_new_milestone(repository: GHHELPER_REPO)
    close_milestone(repository: GHHELPER_REPO, milestone: version['name'])

    # Trigger release build
    trigger_release_build(branch_to_build: "release/#{version['name']}")
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
  # bundle exec fastlane create_gh_release [app:<wordpress|jetpack>] [version:<Hash{name,code}>] [prerelease:<true|false>]
  #
  # Examples:
  # bundle exec fastlane create_gh_release     # Guesses prerelease status based on version name. Includes existing assets for WPBeta+JPBeta
  # bundle exec fastlane create_gh_release app:wordpress prerelease:true                        # Includes existing assets for WPBeta
  # bundle exec fastlane create_gh_release version:{name:12.3-rc-4} prerelease:true             # Includes existing assets for WPBeta+JPBeta 12.3-rc-4
  # bundle exec fastlane create_gh_release app:jetpack version:{name:12.3-rc-4} prerelease:true # Includes only existing asset for JPBeta 12.3-rc-4
  #####################################################################################
  lane :create_gh_release do |options|
    apps = options[:app].nil? ? ['wordpress', 'jetpack'] : [get_app_name_option!(options)]
    versions = options[:version].nil? ? [android_get_release_version()] : [options[:version]]

    release_assets = apps.flat_map do |app|
      versions.flat_map { |vers| bundle_file_path(app, vers) }
    end.select { |f| File.exist?(f) }

    release_title = versions.last['name']
    set_prerelease_flag = options[:prerelease].nil? ? /-rc-|alpha-/.match?(release_title) : options[:prerelease]

    UI.message("Creating release for #{release_title} with the following assets: #{release_assets.inspect}")

    app_titles = { 'wordpress' => 'WordPress', 'jetpack' => 'Jetpack' }
    tmp_file = File.absolute_path('unified-release-notes.txt')
    unified_notes = apps.map do |app|
      notes = File.read(release_notes_path(app))
      "\#\# #{app_titles[app]}\n\n#{notes}"
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

  private_lane :delete_old_changelogs do |options|
    app = get_app_name_option!(options)
    app_values = APP_SPECIFIC_VALUES[app.to_sym]
    Dir.glob(File.join(app_values[:metadata_dir], 'android', '*', 'changelogs', '*')).each do |file|
      if Integer(File.basename(file, '.*')) < Integer(options[:build])
        File.delete(file)
      end rescue puts "Cannot delete file #{file}"
    end
  end

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
    UI.user_error!("Missing 'app' parameter. Expected 'app:wordpress' or 'app:jetpack'") if app.nil?
    unless ['wordpress', 'jetpack'].include?(app)
      UI.user_error!("Invalid 'app' parameter #{app.inspect}. Expected 'wordpress' or 'jetpack'")
    end
    return app
  end

  def release_notes_path(app)
    paths = {
      wordpress: File.join(ENV['PROJECT_ROOT_FOLDER'], 'WordPress', 'metadata', 'release_notes.txt'),
      jetpack: File.join(ENV['PROJECT_ROOT_FOLDER'], 'WordPress', 'jetpack_metadata', 'release_notes.txt')
    }
    paths[app.to_sym] || paths[:wordpress]
  end

  def release_notes_short_paths
    [
      File.join(ENV['PROJECT_ROOT_FOLDER'], 'WordPress', 'metadata', 'release_notes_short.txt'),
      File.join(ENV['PROJECT_ROOT_FOLDER'], 'WordPress', 'jetpack_metadata', 'release_notes_short.txt')
    ]
  end

  def bundle_file_path(app, version)
    prefix = APP_SPECIFIC_VALUES[app.to_sym][:bundle_name_prefix]
    File.join(ENV['PROJECT_ROOT_FOLDER'], 'build', "#{prefix}-#{version['name']}.aab")
  end
end
