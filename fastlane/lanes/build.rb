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
      Fastlane::Helper::GitHelper.ensure_on_branch!('release') unless is_ci

      UI.user_error!("Can't build a final release out of this branch because it's configured as a beta release!") if current_version_name.include? '-rc-'

      ensure_git_status_clean unless is_ci

      message = "Building version #{current_release_version} (#{current_build_code}) for upload to Release Channel\n"

      if options[:skip_confirm]
        UI.message(message)
      else
        UI.user_error!('Aborted by user request') unless UI.confirm("#{message}Do you want to continue?")
      end
    end

    android_build_preflight unless options[:skip_prechecks]

    # Create the file names
    app = get_app_name_option!(options)
    version = current_version_name
    build_bundle(app: app, version: version, build_code: current_build_code, flavor: 'Vanilla', buildType: 'Release')

    upload_build_to_play_store(app: app, version: version, track: 'production')

    create_gh_release(app: app, version: version) if options[:create_release]
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
      Fastlane::Helper::GitHelper.ensure_on_branch!('release') unless is_ci

      ensure_git_status_clean unless is_ci

      message = "Building version #{current_version_name} (#{current_build_code}) for upload to Beta Channel\n"

      if options[:skip_confirm]
        UI.message(message)
      else
        UI.user_error!('Aborted by user request') unless UI.confirm("#{message}Do you want to continue?")
      end
    end

    android_build_preflight unless options[:skip_prechecks]

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
    Fastlane::Helper::GitHelper.ensure_on_branch!('release') unless is_ci

    ensure_git_status_clean unless is_ci

    message = "Building version #{current_version_name} (#{current_build_code}) for upload to Beta Channel\n"

    if options[:skip_confirm]
      UI.message(message)
    else
      UI.user_error!('Aborted by user request') unless UI.confirm("#{message}Do you want to continue?")
    end

    android_build_preflight unless options[:skip_prechecks]

    # Create the file names
    app = get_app_name_option!(options)
    version = current_version_name
    build_bundle(app: app, version: version, build_code: current_build_code, flavor: 'Vanilla', buildType: 'Release')

    upload_build_to_play_store(app: app, version: version, track: 'beta') if options[:upload_to_play_store]

    create_gh_release(app: app, version: version, prerelease: true) if options[:create_release]
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
  # bundle exec fastlane upload_build_to_play_store app:jetpack version:15.0-rc-1 track:beta
  #####################################################################################
  desc 'Upload Build to Play Store'
  lane :upload_build_to_play_store do |options|
    app = get_app_name_option!(options)
    package_name = APP_SPECIFIC_VALUES[app.to_sym][:package_name]
    metadata_dir = File.join(FASTLANE_FOLDER, APP_SPECIFIC_VALUES[app.to_sym][:metadata_dir], 'android')

    version = options[:version]

    if version.nil?
      UI.message("No version available for #{options[:track]} track for #{app}")
      next
    end

    aab_file_path = bundle_file_path(app, version)

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
          json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
        )
      rescue FastlaneCore::Interface::FastlaneError => e
        # Sometimes the upload fails randomly with a "Google Api Error: Invalid request - This Edit has been deleted.".
        # It seems one reason might be a race condition when we do multiple edits at the exact same time (WP beta, JP beta). Retrying usually fixes it
        if e.message.start_with?('Google Api Error') && (retry_count -= 1) > 0
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
  # bundle exec fastlane download_signed_apks_from_google_play app:<wordpress|jetpack> version:versionCode
  #####################################################################################
  lane :download_signed_apks_from_google_play do |options|
    # If no `app:` is specified, call this for both WordPress and Jetpack
    apps = options[:app].nil? ? %i[wordpress jetpack] : Array(options[:app]&.downcase&.to_sym)
    build_code = options[:version] || current_build_code

    apps.each do |app|
      package_name = APP_SPECIFIC_VALUES[app.to_sym][:package_name]

      download_universal_apk_from_google_play(
          package_name: package_name,
          version_code: build_code,
          destination: signed_apk_path(app, version),
          json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
      )
    end
  end

  #####################################################################################
  # build_and_upload_wordpress_prototype_build
  # -----------------------------------------------------------------------------------
  # Build a WordPress Prototype Build and make it available for download
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_and_upload_wordpress_prototype_build
  #####################################################################################
  desc 'Build a WordPress Prototype Build and make it available for download'
  lane :build_and_upload_wordpress_prototype_build do
    UI.user_error!("'BUILDKITE_ARTIFACTS_S3_BUCKET' must be defined as an environment variable.") unless ENV['BUILDKITE_ARTIFACTS_S3_BUCKET']

    versionName = generate_prototype_build_number
    gradle(
      task: 'assemble',
      flavor: "WordPress#{PROTOTYPE_BUILD_FLAVOR}",
      build_type: PROTOTYPE_BUILD_TYPE,
      properties: { prototypeBuildVersionName: versionName }
    )

    upload_prototype_build(product: 'WordPress', versionName: versionName)
  end

  #####################################################################################
  # build_and_upload_jetpack_prototype_build
  # -----------------------------------------------------------------------------------
  # Build a Jetpack Prototype Build and make it available for download
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_and_upload_jetpack_prototype_build
  #####################################################################################
  desc 'Build a Jetpack Prototype Build and make it available for download'
  lane :build_and_upload_jetpack_prototype_build do
    UI.user_error!("'BUILDKITE_ARTIFACTS_S3_BUCKET' must be defined as an environment variable.") unless ENV['BUILDKITE_ARTIFACTS_S3_BUCKET']

    versionName = generate_prototype_build_number
    gradle(
      task: 'assemble',
      flavor: "Jetpack#{PROTOTYPE_BUILD_FLAVOR}",
      build_type: PROTOTYPE_BUILD_TYPE,
      properties: { prototypeBuildVersionName: versionName }
    )

    upload_prototype_build(product: 'Jetpack', versionName: versionName)
  end

  #####################################################################################
  # build_bundle
  # -----------------------------------------------------------------------------------
  # This lane builds an app bundle
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_bundle app:<wordpress|jetpack> version:string, build_code:string flavor:<flavor> buildType:<debug|release> [skip_lint:<true|false>]
  #####################################################################################
  desc 'Builds an app bundle'
  lane :build_bundle do |options|
    # Create the file names
    version = options[:version]
    build_code = options[:build_code]
    app = get_app_name_option!(options)

    if version.nil?
      UI.message("Version specified for #{app} bundle is nil. Skipping ahead")
      next
    end

    prefix = APP_SPECIFIC_VALUES[app.to_sym][:bundle_name_prefix]
    name = "#{prefix}-#{version}.aab"

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

      UI.message("Building #{version} / #{build_code} - #{aab_file}...")
      sh("echo \"Building #{version} / #{build_code} - #{aab_file}...\" >> #{logfile_path}")
      sh("./gradlew bundle#{app}#{options[:flavor]}#{options[:buildType]} >> #{logfile_path} 2>&1")

      UI.crash!("Unable to find a bundle at #{bundle_path}") unless File.file?(bundle_path)

      sh("cp -v #{bundle_path} #{build_dir}#{name} | tee -a #{logfile_path}")
      UI.message("Bundle ready: #{name}")
      sh("echo \"Bundle ready: #{name}\" >> #{logfile_path}")
    end
    "#{build_dir}#{name}"
  end

  # Uploads the apk built by the `gradle` (i.e. `SharedValues::GRADLE_APK_OUTPUT_PATH`) to S3 then comment on the PR to provide the download link
  #
  # @param [String] product the display name of the app to upload to S3. 'WordPress' or 'Jetpack'
  #
  def upload_prototype_build(product:, versionName:)
    filename = "#{product.downcase}-prototype-build-#{versionName}.apk"

    upload_path = upload_to_s3(
      bucket: 'a8c-apps-public-artifacts',
      key: filename,
      file: lane_context[SharedValues::GRADLE_APK_OUTPUT_PATH],
      if_exists: :skip
    )

    return if ENV['BUILDKITE_PULL_REQUEST'].nil?

    install_url = "#{PROTOTYPE_BUILD_DOMAIN}/#{upload_path}"
    comment_body = prototype_build_details_comment(
      app_display_name: product,
      app_icon: ":#{product.downcase}:", # Use Buildkite emoji based on product name
      download_url: install_url,
      metadata: { Flavor: PROTOTYPE_BUILD_FLAVOR, 'Build Type': PROTOTYPE_BUILD_TYPE, 'Version': versionName },
      footnote: '<em>Note: Google Login is not supported on these builds.</em>',
      fold: true
    )

    comment_on_pr(
      project: GHHELPER_REPO,
      pr_number: Integer(ENV['BUILDKITE_PULL_REQUEST']),
      reuse_identifier: "#{product.downcase}-prototype-build-link",
      body: comment_body
    )

    if ENV['BUILDKITE']
      message = "#{product} Prototype Build: [#{filename}](#{install_url})"
      buildkite_annotate(style: 'info', context: "prototype-build-#{product}", message: message)
      buildkite_metadata(set: { versionName: versionName, 'build:flavor': PROTOTYPE_BUILD_FLAVOR, 'build:type': PROTOTYPE_BUILD_TYPE })
    end
  end

  # This function is Buildkite-specific
  def generate_prototype_build_number
    if ENV['BUILDKITE']
      commit = ENV['BUILDKITE_COMMIT'][0, 7]
      branch = ENV['BUILDKITE_BRANCH'].parameterize
      pr_num = ENV['BUILDKITE_PULL_REQUEST']

      pr_num == 'false' ? "#{branch}-#{commit}" : "pr#{pr_num}-#{commit}"
    else
      repo = Git.open(PROJECT_ROOT_FOLDER)
      commit = repo.current_branch.parameterize
      branch = repo.revparse('HEAD')[0, 7]

      "#{branch}-#{commit}"
    end
  end
end
