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
    android_build_prechecks(
      skip_confirm: options[:skip_confirm],
      alpha: false,
      beta: false,
      final: true
    )
    android_build_preflight() unless options[:skip_prechecks]

    # Create the file names
    app = get_app_name_option!(options)
    version = android_get_release_version()
    aab_path = build_bundle(app: app, version: version, flavor: 'Vanilla', buildType: 'Release')

    upload_build_to_play_store(app: app, version: version, track: 'production')

    create_gh_release(app: app, version: version) if options[:create_release]

    android_send_app_size_metrics(
      api_url: ENV['APPMETRICS_BASE_URL'] = File.join('file://localhost/', ENV['PROJECT_ROOT_FOLDER'], 'build', "#{app}-app-size-metrics.json"),
      use_gzip_content_encoding: false,
      app_name: 'WordPress',
      app_version_name: version['name'],
      app_version_code: version['code'],
      product_flavor: 'Vanilla',
      build_type: 'Release',
      source: 'Final Build',
      aab_path: aab_path
    )
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
    android_build_prechecks(
      skip_confirm: options[:skip_confirm],
      alpha: true,
      beta: true,
      final: false
    )
    android_build_preflight() unless options[:skip_prechecks]
    app = get_app_name_option!(options)
    build_alpha(app: app, skip_prechecks: true, skip_confirm: options[:skip_confirm], upload_to_play_store: true, create_release: options[:create_release])
    build_beta(app: app, skip_prechecks: true, skip_confirm: options[:skip_confirm], upload_to_play_store: true, create_release: options[:create_release])
  end

  #####################################################################################
  # build_alpha
  # -----------------------------------------------------------------------------------
  # This lane builds the app for internal testing and optionally uploads it
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_alpha app:<wordpress|jetpack> [skip_confirm:<true|false>] [upload_to_play_store:<true|false>] [create_release:<true|false>]
  #
  # Example:
  # bundle exec fastlane build_alpha app:wordpress create_release:true
  # bundle exec fastlane build_alpha app:wordpress skip_confirm:true upload_to_play_store:true
  # bundle exec fastlane build_alpha app:jetpack
  #####################################################################################
  desc 'Builds and updates for distribution'
  lane :build_alpha do |options|
    android_build_prechecks(skip_confirm: options[:skip_confirm], alpha: true) unless options[:skip_prechecks]
    android_build_preflight() unless options[:skip_prechecks]

    # Create the file names
    app = get_app_name_option!(options)
    version = android_get_alpha_version()
    aab_path = build_bundle(app: app, version: version, flavor: 'Zalpha', buildType: 'Release')

    upload_build_to_play_store(app: app, version: version, track: 'alpha') if options[:upload_to_play_store]

    create_gh_release(app: app, version: version, prerelease: true) if options[:create_release]

    android_send_app_size_metrics(
      api_url: ENV['APPMETRICS_BASE_URL'] = File.join('file://localhost/', ENV['PROJECT_ROOT_FOLDER'], 'build', "#{app}-app-size-metrics.json"),
      use_gzip_content_encoding: false,
      app_name: 'WordPress',
      app_version_name: version['name'],
      app_version_code: version['code'],
      product_flavor: 'Zalpha',
      build_type: 'Release',
      source: 'Alpha',
      aab_path: aab_path
    )
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
    android_build_prechecks(skip_confirm: options[:skip_confirm], beta: true) unless options[:skip_prechecks]
    android_build_preflight() unless options[:skip_prechecks]

    # Create the file names
    app = get_app_name_option!(options)
    version = android_get_release_version()
    aab_path = build_bundle(app: app, version: version, flavor: 'Vanilla', buildType: 'Release')

    upload_build_to_play_store(app: app, version: version, track: 'beta') if options[:upload_to_play_store]

    create_gh_release(app: app, version: version, prerelease: true) if options[:create_release]

    android_send_app_size_metrics(
      api_url: ENV['APPMETRICS_BASE_URL'] = File.join('file://localhost/', ENV['PROJECT_ROOT_FOLDER'], 'build', "#{app}-app-size-metrics.json"),
      use_gzip_content_encoding: false,
      app_name: 'WordPress',
      app_version_name: version['name'],
      app_version_code: version['code'],
      product_flavor: 'Vanilla',
      build_type: 'Release',
      source: 'Beta',
      aab_path: aab_path
    )
  end

  #####################################################################################
  # build_internal
  # -----------------------------------------------------------------------------------
  # This lane builds the app for restricted internal testing, and optionally uploads it to PlayStore's Internal track
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_internal app:<wordpress|jetpack> [skip_confirm:<true|false>] [upload_to_play_store:<true|false>] [create_release:<true|false>]
  #
  # Example:
  # bundle exec fastlane build_internal app:wordpress
  # bundle exec fastlane build_internal app:wordpress skip_confirm:true upload_to_play_store:true
  # bundle exec fastlane build_internal app:jetpack create_release:true
  #####################################################################################
  desc 'Builds and updates for internal testing'
  lane :build_internal do |options|
    android_build_prechecks(skip_confirm: options[:skip_confirm]) unless options[:skip_prechecks]
    android_build_preflight() unless options[:skip_prechecks]

    # Create the file names
    app = get_app_name_option!(options)
    version = android_get_release_version()
    aab_path = build_bundle(app: app, version: version, flavor: 'Zalpha', buildType: 'Debug')

    upload_build_to_play_store(app: app, version: version, track: 'internal') if options[:upload_to_play_store]

    create_gh_release(app: app, version: version, prerelease: true) if options[:create_release]

    android_send_app_size_metrics(
      api_url: ENV['APPMETRICS_BASE_URL'] = File.join('file://localhost/', ENV['PROJECT_ROOT_FOLDER'], 'build', "#{app}-app-size-metrics.json"),
      use_gzip_content_encoding: false,
      app_name: 'WordPress',
      app_version_name: version['name'],
      app_version_code: version['code'],
      product_flavor: 'Zalpha',
      build_type: 'Debug',
      source: 'Internal',
      aab_path: aab_path
    )
  end

  #####################################################################################
  # upload_build_to_play_store
  # -----------------------------------------------------------------------------------
  # This lane uploads the build to Play Store for the given version to the given track
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane upload_build_to_play_store app:<wordpress|jetpack> version:<version> track:<track>
  #
  # Example:
  # bundle exec fastlane upload_build_to_play_store app:wordpress version:15.0 track:production
  # bundle exec fastlane upload_build_to_play_store app:wordpress version:alpha-228 track:alpha
  # bundle exec fastlane upload_build_to_play_store app:jetpack version:15.0-rc-1 track:beta
  #####################################################################################
  desc 'Upload Build to Play Store'
  lane :upload_build_to_play_store do |options|

    app = get_app_name_option!(options)
    package_name = APP_SPECIFIC_VALUES[app.to_sym][:package_name]
    metadata_dir = File.join('fastlane', APP_SPECIFIC_VALUES[app.to_sym][:metadata_dir], 'android')

    version = options[:version]

    if version.nil?
      UI.message("No version available for #{options[:track]} track for #{app}")
      next
    end

    aab_file_path = bundle_file_path(app, version)

    if File.exist? aab_file_path then
      retry_count = 2
      begin
        upload_to_play_store(
          package_name: package_name,
          aab: aab_file_path,
          track: options[:track],
          release_status: 'draft',
          metadata_path: metadata_dir,
          skip_upload_metadata: (options[:track] != 'production'), # Only update app title/description/etc. if uploading for Production, skip for alpha/beta tracks
          skip_upload_changelogs: false,
          skip_upload_images: true,
          skip_upload_screenshots: true,
          json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
        )
      rescue FastlaneCore::Interface::FastlaneError => ex
        # Sometimes the upload fails randomly with a "Google Api Error: Invalid request - This Edit has been deleted.".
        # It seems one reason might be a race condition when we do multiple edits at the exact same time (WP alpha, WP beta, JP beta). Retrying usually fixes it
        if ex.message.start_with?('Google Api Error') && (retry_count -= 1) > 0
          UI.error "Upload failed with Google API error. Retrying in 2mn..."
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
  # build_bundle
  # -----------------------------------------------------------------------------------
  # This lane builds an app bundle
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_bundle app:<wordpress|jetpack> version:<version> flavor:<flavor> buildType:<debug|release> [skip_lint:<true|false>]
  #####################################################################################
  desc 'Builds an app bundle'
  lane :build_bundle do |options|
    # Create the file names
    version = options[:version]
    app = get_app_name_option!(options)

    if version.nil?
      UI.message("Version specified for #{app} bundle is nil. Skipping ahead")
      next
    end

    prefix = APP_SPECIFIC_VALUES[app.to_sym][:bundle_name_prefix]
    name = "#{prefix}-#{version['name']}.aab"

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

      UI.message("Building #{version['name']} / #{version['code']} - #{aab_file}...")
      sh("echo \"Building #{version['name']} / #{version['code']} - #{aab_file}...\" >> #{logfile_path}")
      sh("./gradlew bundle#{app}#{options[:flavor]}#{options[:buildType]} >> #{logfile_path} 2>&1")

      UI.crash!("Unable to find a bundle at #{bundle_path}") unless File.file?(bundle_path)

      sh("cp -v #{bundle_path} #{build_dir}#{name} | tee -a #{logfile_path}")
      UI.message("Bundle ready: #{name}")
      sh("echo \"Bundle ready: #{name}\" >> #{logfile_path}")
    end
    "#{build_dir}#{name}"
  end

  # Send Universal APK app size metrics for Installable Builds
  # @called_by CI
  #
  # @option [String,Symbol] apk The path to the Universal APK to send metrics for
  # @option [String] app_name The display name of the app to send metrics for. "WordPress" or "Jetpack"
  # @option [String] version_name The `versionName` of the APK
  #
  lane :send_installable_build_app_metrics do |options|
    basename = File.basename(options[:apk], '.apk')
    android_send_app_size_metrics(
      api_url: ENV['APPMETRICS_BASE_URL'] = File.join('file://localhost/', ENV['PROJECT_ROOT_FOLDER'], 'Artifacts', "#{basename}-app-size-metrics.json"),
      use_gzip_content_encoding: false,
      app_name: options[:app_name],
      app_version_name: options[:version_name],
      product_flavor: 'Jalapeno',
      build_type: 'Debug',
      source: 'Installable Build',
      universal_apk_path: options[:apk]
    )
  end
end
