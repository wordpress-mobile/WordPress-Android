# frozen_string_literal: true

platform :android do
  #####################################################################################
  # build_and_upload_release
  # -----------------------------------------------------------------------------------
  # This lane builds the final release of the app and uploads it
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_and_upload_release app:<wordpress|jetpack> [skip_confirm:<skip confirm>] [skip_prechecks:<skip prechecks>] [create_release:<Create release on GH> ]
  #
  # Example:
  # bundle exec fastlane build_and_upload_release app:wordpress
  # bundle exec fastlane build_and_upload_release app:wordpress skip_confirm:true
  # bundle exec fastlane build_and_upload_release app:jetpack skip_prechecks:true
  # bundle exec fastlane build_and_upload_release app:wordpress create_release:true
  #####################################################################################
  desc 'Builds and updates for distribution'
  lane :build_and_upload_release do |options|
    unless options[:skip_prechecks]
      ensure_git_branch(branch: '^release/') unless is_ci

      UI.user_error!("Can't build a final release out of this branch because it's configured as a beta release!") if current_version_name.include? '-rc-'

      ensure_git_status_clean unless is_ci

      UI.important("Building version #{current_release_version} (#{current_build_code}) for upload to Release Channel")

      UI.user_error!('Aborted by user request') unless options[:skip_confirm] || UI.confirm('Do you want to continue?')

      android_build_preflight
    end

    # Create the file names
    app = get_app_name_option!(options)
    version_name = current_version_name
    build_bundle(app: app, version_name: version_name, build_code: current_build_code, flavor: 'Vanilla', buildType: 'Release')

    upload_build_to_play_store(app: app, version_name: version_name, track: 'production')
    upload_gutenberg_sourcemaps(app: app, release_version: version_name)

    create_gh_release(app: app, version_name: version_name) if options[:create_release]
  end

  #####################################################################################
  # build_and_upload_pre_releases
  # -----------------------------------------------------------------------------------
  # This lane builds the app for both internal and external distribution and uploads them
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_and_upload_pre_releases app:<wordpress|jetpack> [skip_confirm:<true|false>] [skip_prechecks:<true|false>] <[create_release:<true|false>]
  #
  # Example:
  # bundle exec fastlane build_and_upload_pre_releases
  # bundle exec fastlane build_and_upload_pre_releases skip_confirm:true
  # bundle exec fastlane build_and_upload_beta create_release:true
  #####################################################################################
  desc 'Builds and updates for distribution'
  lane :build_and_upload_pre_releases do |options|
    unless options[:skip_prechecks]
      ensure_git_branch(branch: '^release/') unless is_ci

      ensure_git_status_clean unless is_ci

      UI.important("Building version #{current_version_name} (#{current_build_code}) for upload to Beta Channel")

      UI.user_error!('Aborted by user request') unless options[:skip_confirm] || UI.confirm('Do you want to continue?')

      android_build_preflight
    end

    app = get_app_name_option!(options)
    build_beta(app: app, skip_prechecks: true, skip_confirm: options[:skip_confirm], upload_to_play_store: true, create_release: options[:create_release])
  end

  #####################################################################################
  # build_beta
  # -----------------------------------------------------------------------------------
  # This lane builds the app for internal testing and optionally uploads it
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_beta app:<wordpress|jetpack> [skip_confirm:<true|false>] [upload_to_play_store:<true|false>] [create_release:<true|false>]
  #
  # Example:
  # bundle exec fastlane build_beta app:wordpress create_release:true
  # bundle exec fastlane build_beta app:wordpress skip_confirm:true upload_to_play_store:true
  # bundle exec fastlane build_beta app:jetpack create_release:true
  #####################################################################################
  desc 'Builds and updates for distribution'
  lane :build_beta do |options|
    unless options[:skip_prechecks]
      ensure_git_branch(branch: '^release/') unless is_ci

      ensure_git_status_clean unless is_ci

      UI.important("Building version #{current_version_name} (#{current_build_code}) for upload to Beta Channel")

      UI.user_error!('Aborted by user request') unless options[:skip_confirm] || UI.confirm('Do you want to continue?')

      android_build_preflight
    end

    # Create the file names
    app = get_app_name_option!(options)
    version_name = current_version_name
    build_bundle(app: app, version_name: version_name, build_code: current_build_code, flavor: 'Vanilla', buildType: 'Release')

    upload_build_to_play_store(app: app, version_name: version_name, track: 'beta') if options[:upload_to_play_store]
    upload_gutenberg_sourcemaps(app: app, release_version: version_name)

    create_gh_release(app: app, version_name: version_name, prerelease: true) if options[:create_release]
  end

  #####################################################################################
  # upload_build_to_play_store
  # -----------------------------------------------------------------------------------
  # This lane uploads the build to Play Store for the given version to the given track
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane upload_build_to_play_store app:<wordpress|jetpack> version_name:<version_name> track:<track>
  #
  # Example:
  # bundle exec fastlane upload_build_to_play_store app:wordpress version_name:15.0 track:production
  # bundle exec fastlane upload_build_to_play_store app:jetpack version_name:15.0-rc-1 track:beta
  #####################################################################################
  desc 'Upload Build to Play Store'
  lane :upload_build_to_play_store do |options|
    app = get_app_name_option!(options)
    package_name = APP_SPECIFIC_VALUES[app.to_sym][:package_name]
    metadata_dir = File.join(FASTLANE_FOLDER, APP_SPECIFIC_VALUES[app.to_sym][:metadata_dir], 'android')

    version_name = options[:version_name]

    if version_name.nil?
      UI.message("No version available for #{options[:track]} track for #{app}")
      next
    end

    aab_file_path = bundle_file_path(app, version_name)

    if File.exist? aab_file_path
      retry_count = 2
      begin
        upload_to_play_store(
          package_name: package_name,
          aab: aab_file_path,
          track: options[:track],
          release_status: 'draft',
          metadata_path: metadata_dir,
          skip_upload_metadata: (options[:track] != 'production'), # Only update app title/description/etc. if uploading for Production, skip for beta tracks
          skip_upload_changelogs: false,
          skip_upload_images: true,
          skip_upload_screenshots: true,
          json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY,
          version_codes_to_retain: [1440]
        )
      rescue FastlaneCore::Interface::FastlaneError => e
        # Sometimes the upload fails randomly with a "Google Api Error: Invalid request - This Edit has been deleted.".
        # It seems one reason might be a race condition when we do multiple edits at the exact same time (WP beta, JP beta). Retrying usually fixes it
        if e.message.start_with?('Google Api Error') && (retry_count -= 1).positive?
          UI.error 'Upload failed with Google API error. Retrying in 2mn...'
          sleep(120)
          retry
        end
        raise
      end
    else
      UI.error("Unable to find a build artifact at #{aab_file_path}")
    end
  end

  #####################################################################################
  # download_signed_apks_from_google_play
  # -----------------------------------------------------------------------------------
  # This lane downloads the signed apks from Play Store for the given app and version
  #
  # If no argument is provided, it'll download both WordPress & Jetpack apks using the version from version.properties
  # If only 'app' argument is provided, it'll download the apk for the given app using the version from version.properties
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane download_signed_apks_from_google_play # Download WordPress & Jetpack apks using the version from version.properties
  # bundle exec fastlane download_signed_apks_from_google_play app:<wordpress|jetpack> # Download given app's apk using the version from version.properties
  # bundle exec fastlane download_signed_apks_from_google_play app:<wordpress|jetpack> build_code:build_code
  #####################################################################################
  lane :download_signed_apks_from_google_play do |options|
    # If no `app:` is specified, call this for both WordPress and Jetpack
    apps = options[:app].nil? ? %i[wordpress jetpack] : Array(options[:app]&.downcase&.to_sym)
    build_code = options[:build_code] || current_build_code

    apps.each do |app|
      package_name = APP_SPECIFIC_VALUES[app.to_sym][:package_name]

      download_universal_apk_from_google_play(
        package_name: package_name,
        version_code: build_code,
        destination: signed_apk_path(app, current_version_name),
        json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
      )
    end
  end

  #####################################################################################
  # build_and_upload_wordpress_prototype_build
  # -----------------------------------------------------------------------------------
  # Build a WordPress Prototype Build and upload it to Firebase App Distribution
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_and_upload_wordpress_prototype_build
  #####################################################################################
  desc 'Build a WordPress Prototype Build and upload it to Firebase App Distribution'
  lane :build_and_upload_wordpress_prototype_build do
    UI.user_error!("'FIREBASE_APP_DISTRIBUTION_ACCOUNT_KEY' must be defined as an environment variable.") unless ENV['FIREBASE_APP_DISTRIBUTION_ACCOUNT_KEY']

    version_name = generate_prototype_build_number
    gradle(
      task: 'assemble',
      flavor: "WordPress#{PROTOTYPE_BUILD_FLAVOR}",
      build_type: PROTOTYPE_BUILD_TYPE,
      properties: { prototypeBuildVersionName: version_name }
    )

    upload_prototype_build(app: :wordpress, version_name: version_name)
    upload_gutenberg_sourcemaps(app: 'Wordpress', release_version: version_name)
  end

  #####################################################################################
  # build_and_upload_jetpack_prototype_build
  # -----------------------------------------------------------------------------------
  # Build a Jetpack Prototype Build and upload it to Firebase App Distribution
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_and_upload_jetpack_prototype_build
  #####################################################################################
  desc 'Build a Jetpack Prototype Build and upload it to Firebase App Distribution'
  lane :build_and_upload_jetpack_prototype_build do
    UI.user_error!("'FIREBASE_APP_DISTRIBUTION_ACCOUNT_KEY' must be defined as an environment variable.") unless ENV['FIREBASE_APP_DISTRIBUTION_ACCOUNT_KEY']

    version_name = generate_prototype_build_number
    gradle(
      task: 'assemble',
      flavor: "Jetpack#{PROTOTYPE_BUILD_FLAVOR}",
      build_type: PROTOTYPE_BUILD_TYPE,
      properties: { prototypeBuildVersionName: version_name }
    )

    upload_prototype_build(app: :jetpack, version_name: version_name)
    upload_gutenberg_sourcemaps(app: 'Jetpack', release_version: version_name)
  end

  #####################################################################################
  # build_bundle
  # -----------------------------------------------------------------------------------
  # This lane builds an app bundle
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_bundle app:<wordpress|jetpack> version_name:string, build_code:string flavor:<flavor> buildType:<debug|release> [skip_lint:<true|false>]
  #####################################################################################
  desc 'Builds an app bundle'
  lane :build_bundle do |options|
    # Create the file names
    version_name = options[:version_name]
    build_code = options[:build_code]
    app = get_app_name_option!(options)

    if version_name.nil?
      UI.message("Version name specified for #{app} bundle is nil. Skipping ahead")
      next
    end

    prefix = APP_SPECIFIC_VALUES[app.to_sym][:bundle_name_prefix]
    name = "#{prefix}-#{version_name}.aab"

    aab_file = "org.wordpress.android-#{app}-#{options[:flavor]}-#{options[:buildType]}.aab".downcase
    output_dir = 'WordPress/build/outputs/bundle/'
    build_dir = 'build/'
    logfile_path = "#{build_dir}build.log"

    # Intermediate Variables
    bundle_path = "#{output_dir}#{app}#{options[:flavor].capitalize}#{options[:buildType].capitalize}/#{aab_file}"

    # Build
    Dir.chdir('..') do
      sh("mkdir -p #{build_dir}")

      UI.message('Cleaning branch...')
      sh("echo \"Cleaning branch\" >> #{logfile_path}")
      sh("./gradlew clean >> #{logfile_path} 2>&1")

      sh("mkdir -p #{build_dir}")
      if options[:skip_lint].nil?
        UI.message('Running lint...')
        sh("echo \"Running lint...\" >> #{logfile_path}")
        sh("./gradlew lint#{app}#{options[:flavor]}#{options[:buildType]} >> #{logfile_path} 2>&1") unless is_ci
      else
        UI.message('Skipping lint...')
      end

      UI.message("Building #{version_name} / #{build_code} - #{aab_file}...")
      sh("echo \"Building #{version_name} / #{build_code} - #{aab_file}...\" >> #{logfile_path}")
      sh("./gradlew bundle#{app}#{options[:flavor]}#{options[:buildType]} >> #{logfile_path} 2>&1")

      UI.crash!("Unable to find a bundle at #{bundle_path}") unless File.file?(bundle_path)

      annotate_sentry_mapping_uuid(app: app, aab_file: bundle_path)

      sh("cp -v #{bundle_path} #{build_dir}#{name} | tee -a #{logfile_path}")
      UI.message("Bundle ready: #{name}")
      sh("echo \"Bundle ready: #{name}\" >> #{logfile_path}")
    end
    "#{build_dir}#{name}"
  end

  # Uploads the APK built by `gradle` to Firebase App Distribution and comments on the PR
  #
  # @param [Symbol] app the app identifier (:wordpress or :jetpack)
  # @param [String] version_name the version name for the build
  #
  def upload_prototype_build(app:, version_name:)
    app_specific_values = APP_SPECIFIC_VALUES[app]
    app_display_name = app_specific_values[:display_name]
    firebase_settings = app_specific_values[:firebase]

    release_notes = <<~NOTES
      App: #{app_display_name} Android
      Branch: `#{ENV.fetch('BUILDKITE_BRANCH', 'N/A')}`
      Commit: #{ENV.fetch('BUILDKITE_COMMIT', 'N/A')[0, 7]}
      Build Type: #{PROTOTYPE_BUILD_TYPE}
      Version: #{version_name}
    NOTES

    firebase_app_distribution(
      app: firebase_settings[:app_id],
      service_credentials_json_data: ENV.fetch('FIREBASE_APP_DISTRIBUTION_ACCOUNT_KEY', nil),
      release_notes: release_notes,
      groups: firebase_settings[:testers_group]
    )

    return unless is_ci

    comment_on_pr_with_prototype_build_install_link(
      project: GITHUB_REPO,
      app_display_name: "#{app_display_name} Android",
      app_icon: ":#{app}:",
      metadata: {
        Flavor: PROTOTYPE_BUILD_FLAVOR,
        'Build Type': PROTOTYPE_BUILD_TYPE,
        Version: version_name
      }
    )

    annotate_ci_build_with_prototype_build_install_link(app_display_name: app_display_name)
  end

  # Adds an install link for prototype build via PR comment
  #
  # @param [String] project the GitHub repository (e.g., 'wordpress-mobile/WordPress-Android')
  # @param [String] app_display_name the display name of the app (e.g., 'WordPress Android')
  # @param [String] app_icon the Buildkite emoji for the app (e.g., ':wordpress:')
  # @param [Hash] metadata additional metadata to display in the comment
  #
  def comment_on_pr_with_prototype_build_install_link(project:, app_display_name:, app_icon: nil, metadata: {})
    pr_number = ENV.fetch('BUILDKITE_PULL_REQUEST', nil)
    return unless pr_number && pr_number != 'false'

    comment_on_pr(
      project: project,
      pr_number: Integer(pr_number),
      reuse_identifier: "#{app_display_name.downcase.gsub(' ', '-')}-prototype-build-link",
      body: prototype_build_details_comment(
        app_display_name: app_display_name,
        app_icon: app_icon,
        metadata: metadata,
        footnote: '<em>Note: Google Login is not supported on these builds.</em>'
      )
    )
  end

  # If running in Buildkite, annotates the current build with prototype build info
  #
  # @param [String] app_display_name the display name of the app
  #
  def annotate_ci_build_with_prototype_build_install_link(app_display_name:)
    return unless ENV['BUILDKITE']

    install_link = 'Firebase App Distribution'
    install_url = lane_context[SharedValues::FIREBASE_APP_DISTRO_RELEASE]&.dig(:testingUri)
    install_link = "[#{install_link}](#{install_url})" unless install_url.nil?

    buildkite_annotate(
      style: 'success',
      context: "prototype-build-#{app_display_name.downcase.gsub(' ', '-')}",
      message: "#{app_display_name} Prototype Build uploaded to #{install_link}"
    )
  end

  # This function is Buildkite-specific
  def generate_prototype_build_number
    if ENV['BUILDKITE']
      commit = ENV.fetch('BUILDKITE_COMMIT', nil)[0, 7]
      branch = ENV['BUILDKITE_BRANCH'].parameterize
      pr_num = ENV.fetch('BUILDKITE_PULL_REQUEST', nil)

      pr_num == 'false' ? "#{branch}-#{commit}" : "pr#{pr_num}-#{commit}"
    else
      repo = Git.open(PROJECT_ROOT_FOLDER)
      commit = repo.current_branch.parameterize
      branch = repo.revparse('HEAD')[0, 7]

      "#{branch}-#{commit}"
    end
  end

  # Uploads the React Native JavaScript bundle and source map files.
  # These files are provided by the Gutenberg Mobile library.
  #
  # @param [String] app App name, e.g. 'WordPress' or 'Jetpack'.
  # @param [String] release_version Release version name to attach the files to in Sentry.
  #
  def upload_gutenberg_sourcemaps(app:, release_version:)
    # Load Sentry properties
    sentry_path = File.join(PROJECT_ROOT_FOLDER, 'WordPress', 'src', app.downcase, 'sentry.properties')
    sentry_properties = JavaProperties.load(sentry_path)
    sentry_token = sentry_properties[:'auth.token']
    project_slug = sentry_properties[:'defaults.project']
    org_slug = sentry_properties[:'defaults.org']

    # Bundle and source map files are copied to a specific folder as part of the build process.
    bundle_source_map_path = File.join(PROJECT_ROOT_FOLDER, 'WordPress', 'build', 'react-native-bundle-source-map')

    sentry_upload_sourcemap(
      auth_token: sentry_token,
      org_slug: org_slug,
      project_slug: project_slug,
      version: release_version,
      dist: current_build_code,
      # When the React native bundle is generated, the source map file references include the local machine path;
      # With the `rewrite` and `strip_common_prefix` options, Sentry automatically strips this part.
      rewrite: true,
      strip_common_prefix: true,
      sourcemap: bundle_source_map_path
    )
  end

  def annotate_sentry_mapping_uuid(app:, aab_file:)
    sentry_props = sh('unzip', '-p', aab_file, 'base/assets/sentry-debug-meta.properties', step_name: 'Extract sentry-debug-meta.properties') do |status, result, _|
      status.success? ? result : nil
    end
    if sentry_props.nil?
      UI.important("Unable to extract Sentry properties file from #{aab_file}. Skipping annotating the build with the mapping UUID.")
      return
    end

    line = sentry_props.split("\n").find { |l| l.start_with?('io.sentry.ProguardUuids=') }
    if line.nil?
      UI.error("`io.sentry.ProguardUuids` line not found in #{aab_file}'s `sentry-debug-meta.properties` file")
      return
    end

    uuid = line.split('=', 2).last.strip
    buildkite_annotate(style: 'info', context: "sentry-mapping-uuid-#{app}", message: "Sentry mapping UUID for #{app}: #{uuid}")
  end
end
