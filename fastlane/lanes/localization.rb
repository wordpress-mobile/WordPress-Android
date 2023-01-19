# NOTE: When updating this list, ensure the locales having `promo_config: {…}` matches the list of locales
# used in the `raw-screenshots` CI job for Firebase Test Lab
#
# NOTE: The `promo_config` hash is used by `fastlane/helpers/android_promo_screenshot_helper.rb` and accepts keys `:text_size` and `:font`.
# When set to `false`, the locale will just not be included during the screenshot generation (see `lanes/screenshots.rb`).
# This setup is likely to disappear soon (we currently don't provide any custom text size and font for any local anyway) when we will:
#  1. Get rid of the local `fastlane/helpers/*` from this repo, to ultimately switch to use the `release-toolkit`'s action instead
#  2. Switch to use the [future `LocaleHelper` once it lands](https://github.com/wordpress-mobile/release-toolkit/pull/296)
#
ALL_LOCALES = [
  # First are the locales which are used for *both* downloading the `strings.xml` files from GlotPress *and* for generating the release notes XML files.
  { glotpress: 'ar', android: 'ar',    google_play: 'ar',     promo_config: {} },
  { glotpress: 'de', android: 'de',    google_play: 'de-DE',  promo_config: {} },
  { glotpress: 'es', android: 'es', google_play: 'es-ES', promo_config: {} },
  { glotpress: 'fr', android: 'fr-rCA', google_play: 'fr-CA', promo_config: false },
  { glotpress: 'fr', android: 'fr',    google_play: 'fr-FR',  promo_config: {} },
  { glotpress: 'he', android: 'he',    google_play: 'iw-IL',  promo_config: {} },
  { glotpress: 'id', android: 'id',    google_play: 'id',     promo_config: {} },
  { glotpress: 'it', android: 'it',    google_play: 'it-IT',  promo_config: {} },
  { glotpress: 'ja', android: 'ja',    google_play: 'ja-JP',  promo_config: {} },
  { glotpress: 'ko', android: 'ko',    google_play: 'ko-KR',  promo_config: {} },
  { glotpress: 'nl', android: 'nl',    google_play: 'nl-NL',  promo_config: {} },
  { glotpress: 'pl', android: 'pl',    google_play: 'pl-PL',  promo_config: {} },
  { glotpress: 'pt-br', android: 'pt-rBR', google_play: 'pt-BR', promo_config: {} },
  { glotpress: 'ru', android: 'ru',    google_play: 'ru-RU',  promo_config: {} },
  { glotpress: 'sr', android: 'sr',    google_play: 'sr',     promo_config: {} },
  { glotpress: 'sv', android: 'sv',    google_play: 'sv-SE',  promo_config: {} },
  { glotpress: 'th', android: 'th',    google_play: 'th',     promo_config: {} },
  { glotpress: 'tr', android: 'tr',    google_play: 'tr-TR',  promo_config: {} },
  { glotpress: 'vi', android: 'vi',    google_play: 'vi',     promo_config: {} },
  { glotpress: 'zh-cn', android: 'zh-rCN', google_play: 'zh-CN',  promo_config: {} },
  { glotpress: 'zh-tw', android: 'zh-rTW', google_play: 'zh-TW',  promo_config: {} },
  # From this point are locales that are still used for downloading `strings.xml`… but not for release notes – and thus don't need a `google_play` key. See `WP_RELEASE_NOTES_LOCALES` below.
  { glotpress: 'az', android: 'az', promo_config: false },
  { glotpress: 'bg', android: 'bg', promo_config: false },
  { glotpress: 'cs', android: 'cs', promo_config: false },
  { glotpress: 'cy', android: 'cy', promo_config: false },
  { glotpress: 'da', android: 'da', promo_config: false },
  { glotpress: 'el', android: 'el', promo_config: false },
  { glotpress: 'en-au', android: 'en-rAU', promo_config: false },
  { glotpress: 'en-ca', android: 'en-rCA', promo_config: false },
  { glotpress: 'en-gb', android: 'en-rGB', promo_config: false },
  { glotpress: 'es-cl', android: 'es-rCL', promo_config: false },
  { glotpress: 'es-co', android: 'es-rCO', promo_config: false },
  { glotpress: 'es-mx', android: 'es-rMX', promo_config: false },
  { glotpress: 'es-ve', android: 'es-rVE', promo_config: false },
  { glotpress: 'eu', android: 'eu', promo_config: false },
  { glotpress: 'gd', android: 'gd', promo_config: false },
  { glotpress: 'gl', android: 'gl', promo_config: false },
  { glotpress: 'hi', android: 'hi', promo_config: false },
  { glotpress: 'hr', android: 'hr', promo_config: false },
  { glotpress: 'hu', android: 'hu', promo_config: false },
  { glotpress: 'is', android: 'is', promo_config: false },
  { glotpress: 'kmr', android: 'kmr', promo_config: false },
  { glotpress: 'lv', android: 'lv', promo_config: false },
  { glotpress: 'mk', android: 'mk', promo_config: false },
  { glotpress: 'ms', android: 'ms', promo_config: false },
  { glotpress: 'nb', android: 'nb', promo_config: false },
  { glotpress: 'ro', android: 'ro', promo_config: false },
  { glotpress: 'sk', android: 'sk', promo_config: false },
  { glotpress: 'sq', android: 'sq', promo_config: false },
  { glotpress: 'uz', android: 'uz', promo_config: false },
  { glotpress: 'zh-tw', android: 'zh-rHK', promo_config: false },
].freeze

WP_APP_LOCALES = ALL_LOCALES
JP_APP_LOCALES = ALL_LOCALES
  .select { |h| %w[ar de-DE es-ES fr-FR iw-IL id it-IT ja-JP ko-KR nl-NL pt-BR ru-RU sv-SE tr-TR zh-CN zh-TW].include?(h[:google_play]) }

WP_RELEASE_NOTES_LOCALES = ALL_LOCALES
  .reject { |h| h[:google_play].nil? }
  .map { |h| [h[:glotpress], h[:google_play]] }

JP_RELEASE_NOTES_LOCALES = ALL_LOCALES
  .reject { |h| h[:google_play].nil? }
  .select { |h| %w[ar de-DE es-ES fr-FR iw-IL id it-IT ja-JP ko-KR nl-NL pt-BR ru-RU sv-SE tr-TR zh-CN zh-TW].include?(h[:google_play]) }
  .map { |h| [h[:glotpress], h[:google_play]] }

platform :android do
  ########################################################################
  # PlayStore Metadata
  ########################################################################

  #####################################################################################
  # update_appstore_strings
  # -----------------------------------------------------------------------------------
  # This lane gets the data from the txt files in the `WordPress/metadata/` and
  # `WordPress/jetpack_metadata/` folders and updates the `.po` files that is then
  # picked by GlotPress for translations.
  # -----------------------------------------------------------------------------------
  # Usage:
  # fastlane update_appstore_strings [version:<version>]
  #
  # Example:
  # fastlane update_appstore_strings version:10.3
  #####################################################################################
  desc 'Updates the PlayStoreStrings.po files for WP + JP'
  lane :update_appstore_strings do |options|
    # If no `app:` is specified, call this for both WordPress and Jetpack
    apps = options[:app].nil? ? %i[wordpress jetpack] : Array(options[:app]&.downcase&.to_sym)

    apps.each do |app|
      app_values = APP_SPECIFIC_VALUES[app]

      metadata_folder = File.join(PROJECT_ROOT_FOLDER, 'WordPress', app_values[:metadata_dir])
      version = options.fetch(:version, android_get_app_version)

      # <key in po file> => <path to txt file to read the content from>
      files = {
        release_note: File.join(metadata_folder, 'release_notes.txt'),
        release_note_short: File.join(metadata_folder, 'release_notes_short.txt'),
        play_store_app_title: File.join(metadata_folder, 'title.txt'),
        play_store_promo: File.join(metadata_folder, 'short_description.txt'),
        play_store_desc: File.join(metadata_folder, 'full_description.txt')
      }
      # Add entries for `screenshot_*.txt` files as well
      Dir.glob('screenshot_*.txt', base: metadata_folder).sort.each do |screenshot_file|
        key = "play_store_#{File.basename(screenshot_file, '.txt')}".to_sym
        files[key] = File.join(metadata_folder, screenshot_file)
      end

      update_po_file_for_metadata_localization(
        po_path: File.join(metadata_folder, 'PlayStoreStrings.po'),
        sources: files,
        release_version: version,
        commit_message: "Update #{app_values[:display_name]} `PlayStoreStrings.po` for version #{version}"
      )
    end
  end

  # Updates the metadata in the Play Store (Main store listing) from the content of `fastlane/{metadata|jetpack_metadata}/android/*/*.txt` files
  #
  # @option [String] app The app to update the metadata for. Mandatory. Must be one of `wordpress` or `jetpack`.
  #
  desc 'Uploads the localized metadata to the PlayStore.'
  lane :upload_playstore_localized_metadata do |options|
    app = get_app_name_option!(options)
    package_name = APP_SPECIFIC_VALUES[app.to_sym][:package_name]
    metadata_dir = File.join(FASTLANE_FOLDER, APP_SPECIFIC_VALUES[app.to_sym][:metadata_dir], 'android')
    version_code = android_get_release_version['code']

    upload_to_play_store(
      package_name: package_name,
      track: 'production',
      version_code: version_code, # Apparently required by fastlane… even if the "Main Store Listing" isn't be attached to a specific build ¯\_(ツ)_/¯
      metadata_path: metadata_dir,
      skip_upload_apk: true,
      skip_upload_aab: true,
      skip_upload_metadata: false,
      skip_upload_changelogs: true,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
    )
  end

  # Downloads the translated metadata (release notes, app store strings, title, etc.)
  # from GlotPress, and updates the local `.txt` files with them.
  #
  # @option [String|Symbol] app The app to take screenshots for. Must be `wordpress` or `jetpack`
  # @option [String] version The `versionName` of the app, used to extract the release notes from the right GlotPress key. Defaults to current `versionName`.
  # @option [Int] build_number The `versionCode` of the app, used to save the release notes to the right `changelogs/*.txt` file. Defaults to current `versionCode`.
  # @option [Boolean] skip_release_notes If set to true, will not download release notes. Defaults to `false`. This can be useful when all you want to download
  #         is screenshots translations and metadata not linked to a specific version (in which case `version` and `build_number` parameters are optional).
  # @option [Boolean] skip_commit If set to true, will skip the `git add`, `git commit` and `git push` operations. Default to false.
  # @option [Boolean] skip_git_push If set to true, will skip the `git push` at the end. Default to false. Inferred to `true` if `skip_commit` is `true`.
  #
  desc 'Downloads translated metadata from GlotPress'
  lane :download_metadata_strings do |options|
    skip_release_notes = options.fetch(:skip_release_notes, false)
    version = skip_release_notes ? nil : options.fetch(:version, android_get_app_version)
    build_number = skip_release_notes ? nil : options.fetch(:build_number, android_get_release_version['code'])

    skip_commit = options.fetch(:skip_commit, false)
    skip_git_push = options.fetch(:skip_git_push, false)
    
    # If no `app:` is specified, call this for both WordPress and Jetpack
    apps = options[:app].nil? ? %i[wordpress jetpack] : Array(options[:app]&.to_s&.downcase&.to_sym)

    apps.each do |app|
      app_values = APP_SPECIFIC_VALUES[app]
      metadata_source_dir = File.join(PROJECT_ROOT_FOLDER, 'WordPress', app_values[:metadata_dir])

      files = {
        play_store_app_title: { desc: 'title.txt', max_size: 30 },
        play_store_promo: { desc: 'short_description.txt', max_size: 80 },
        play_store_desc: { desc: 'full_description.txt', max_size: 4000 }
      }
      unless skip_release_notes
        delete_old_changelogs(app: app, build: build_number)
        version_suffix = version.split('.').join
        files["release_note_#{version_suffix}"] = { desc: "changelogs/#{build_number}.txt", max_size: 500, alternate_key: "release_note_short_#{version_suffix}" }
      end
      # Add key mappings for `screenshots_*` files too
      Dir.glob('screenshot_*.txt', base: metadata_source_dir).each do |f|
        key = "play_store_#{File.basename(f, '.txt')}"
        files[key] = { desc: f }
      end

      download_path = File.join(FASTLANE_FOLDER, app_values[:metadata_dir], 'android')
      locales = { wordpress: WP_RELEASE_NOTES_LOCALES, jetpack: JP_RELEASE_NOTES_LOCALES }[app]
      UI.header("Downloading metadata translations for #{app_values[:display_name]}")
      gp_downloadmetadata(
        project_url: app_values[:glotpress_metadata_project],
        target_files: files,
        locales: locales,
        download_path: download_path
      )

      # Copy the source `.txt` files (used as source of truth when we generated the `.po`) to the `fastlane/*metadata/android/en-US` dir,
      # as `en-US` is the source language, and isn't exported from GlotPress during `gp_downloadmetadata`
      files.each do |key, h|
        source_file = key.to_s.start_with?('release_note_') ? 'release_notes.txt' : h[:desc]
        FileUtils.cp(File.join(metadata_source_dir, source_file), File.join(download_path, 'en-US', h[:desc]))
      end
      
      unless skip_commit
        git_add(path: download_path)
        message = "Update #{app_values[:display_name]} metadata translations"
        message += " for #{version}" unless version.nil?
        git_commit(path: download_path, message: message, allow_nothing_to_commit: true)
      end
    end
    push_to_git_remote unless skip_commit || skip_git_push
  end

  ########################################################################
  # In-App Translations
  ########################################################################

  ### Libraries Translation Merging ###

  WORDPRESS_MAIN_STRINGS_PATH = File.join(PROJECT_ROOT_FOLDER, 'WordPress', 'src', 'main', 'res', 'values', 'strings.xml').freeze
  WORDPRESS_FROZEN_STRINGS_DIR_PATH = File.join(FASTLANE_FOLDER, 'resources', 'values').freeze
  JETPACK_MAIN_STRINGS_PATH = File.join(PROJECT_ROOT_FOLDER, 'WordPress', 'src', 'jetpack', 'res', 'values', 'strings.xml').freeze
  JETPACK_FROZEN_STRINGS_DIR_PATH = File.join(FASTLANE_FOLDER, 'jetpack_resources', 'values').freeze
  LOCAL_LIBRARIES_STRINGS_PATHS = [
    # Note: for those we don't set `add_ignore_attr` to true because we currently use `checkDependencies true` in `WordPress/build.gradle`
    # Which will correctly detect strings from the app's `strings.xml` being used by one of the module.
    { library: "Image Editor", strings_path: "./libs/image-editor/src/main/res/values/strings.xml", source_id: 'module:image-editor' },
    { library: "Editor", strings_path: "./libs/editor/src/main/res/values/strings.xml", source_id: 'module:editor' }
  ].freeze
  REMOTE_LIBRARIES_STRINGS_PATHS = [
    {
      name: 'Gutenberg Native',
      import_key: 'gutenbergMobileVersion',
      repository: 'wordpress-mobile/gutenberg-mobile',
      strings_file_path: 'bundle/android/strings.xml',
      source_id: 'gutenberg'
    },
    {
      name: 'Login Library',
      import_key: 'wordPressLoginVersion',
      repository: 'wordpress-mobile/WordPress-Login-Flow-Android',
      strings_file_path: 'WordPressLoginFlow/src/main/res/values/strings.xml',
      exclusions: ['default_web_client_id'],
      source_id: 'login'
    },
    {
      name: "Stories Library",
      import_key: "automatticStoriesVersion",
      repository: "Automattic/stories-android",
      strings_file_path: "stories/src/main/res/values/strings.xml",
      source_id: 'stories'
    },
    {
      name: "About Library",
      import_key: "automatticAboutVersion",
      repository: "Automattic/about-automattic-android",
      strings_file_path: "library/src/main/res/values/strings.xml",
      source_id: 'about'
    },
  ].freeze

  lane :update_frozen_strings_for_translation do
    FileUtils.mkdir_p(WORDPRESS_FROZEN_STRINGS_DIR_PATH)
    FileUtils.cp(WORDPRESS_MAIN_STRINGS_PATH, WORDPRESS_FROZEN_STRINGS_DIR_PATH)
    FileUtils.mkdir_p(JETPACK_FROZEN_STRINGS_DIR_PATH)
    FileUtils.cp(JETPACK_MAIN_STRINGS_PATH, JETPACK_FROZEN_STRINGS_DIR_PATH)

    git_commit(
      path: [File.join(WORDPRESS_FROZEN_STRINGS_DIR_PATH, 'strings.xml'), File.join(JETPACK_FROZEN_STRINGS_DIR_PATH, 'strings.xml')],
      message: 'Freeze strings for translation',
      allow_nothing_to_commit: true
    )
  end

  desc 'Merge libraries strings files into the main app one'
  lane :localize_libraries do
    # Merge `strings.xml` files of libraries that are hosted locally in the repository (in `./libs` folder)
    an_localize_libs(app_strings_path: WORDPRESS_MAIN_STRINGS_PATH, libs_strings_path: LOCAL_LIBRARIES_STRINGS_PATHS)

    # Merge `strings.xml` files of libraries that are hosted in separate repositories (and linked as binary dependencies with the project)
    REMOTE_LIBRARIES_STRINGS_PATHS.each do |lib|
      download_path = android_download_file_by_version(
        library_name: lib[:name],
        import_key: lib[:import_key],
        repository: lib[:repository],
        file_path: lib[:strings_file_path]
      )

      if download_path.nil?
        error_message = <<~ERROR
          Can't download strings file for #{lib[:name]}.
          Strings for this library won't get translated.
          Do you want to continue anyway?
        ERROR
        UI.user_error! 'Abort.' unless UI.confirm(error_message)
      else
        UI.message("`strings.xml` file for #{lib[:name]} downloaded to #{download_path}.")
        lib_to_merge = [{
          library: lib[:name],
          strings_path: download_path,
          exclusions: lib[:exclusions],
          source_id: lib[:source_id],
          add_ignore_attr: true # The linter is not be able to detect if a merged string is actually used by a binary dependency
        }]
        an_localize_libs(app_strings_path: WORDPRESS_MAIN_STRINGS_PATH, libs_strings_path: lib_to_merge)
        File.delete(download_path) if File.exist?(download_path)
      end
    end

    # Commit changes
    git_commit(path: WORDPRESS_MAIN_STRINGS_PATH, message: 'Merge strings from libraries for translation', allow_nothing_to_commit: true)
  end

  #####################################################################################
  # download_translations
  # -----------------------------------------------------------------------------------
  # This lane downloads the string translations from GlotPress
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane download_translations
  #####################################################################################
  lane :download_translations do
    # WordPress strings
    check_declared_locales_consistency(app_flavor: 'wordpress', locales_list: WP_APP_LOCALES)
    android_download_translations(
      res_dir: File.join('WordPress', 'src', 'main', 'res'),
      glotpress_url: APP_SPECIFIC_VALUES[:wordpress][:glotpress_appstrings_project],
      locales: WP_APP_LOCALES
    )

    # Jetpack strings
    check_declared_locales_consistency(app_flavor: 'jetpack', locales_list: JP_APP_LOCALES)
    android_download_translations(
      res_dir: File.join('WordPress', 'src', 'jetpack', 'res'),
      glotpress_url: APP_SPECIFIC_VALUES[:jetpack][:glotpress_appstrings_project],
      locales: JP_APP_LOCALES
    )
  end

  # Updates the `.po` file at the given `po_path` using the content of the `sources` files, interpolating `release_version` where appropriate.
  # Internally, this calls the `an_update_metadata_source` release toolkit action and adds Git management to it.
  #
  def update_po_file_for_metadata_localization(po_path:, sources:, release_version:, commit_message:)
    ensure_git_status_clean

    an_update_metadata_source(
      po_file_path: po_path,
      source_files: sources,
      release_version: release_version
    )

    git_add(path: po_path)
    git_commit(path: po_path, message: commit_message, allow_nothing_to_commit: true)
  end

  # Compares the list of locales declared in the `resourceConfigurations` field of `build.gradle` for a given flavor
  # with the hardcoded list of locales we use in our Fastlane lanes, to ensure they match and we are consistent.
  #
  # @param [String] app_flavor `"wordpress"` or `"jetpack"` — The `productFlavor` to read from in the build.gradle
  # @param [Array<Hash>] locales_list The list of Hash defining the locales to compare that list to.
  #        Typically one of the `WP_APP_LOCALES` or `JP_APP_LOCALES` constants
  def check_declared_locales_consistency(app_flavor:, locales_list:)
    output = gradle(task: 'printResourceConfigurations', flags: '--quiet')
    resource_configs = output.match(/^#{app_flavor}: \[(.*)\]$/)&.captures&.first&.gsub(' ','')&.split(',')&.sort
    if resource_configs.nil? || resource_configs.empty?
      UI.message("No `resourceConfigurations` field set in `build.gradle` for the `#{app_flavor}` flavor. Nothing to check.")
      return
    end

    expected_locales = locales_list.map { |l| l[:android] }.sort
    if resource_configs == expected_locales
      UI.message("The `resourceConfigurations` field set in `build.gradle` for the `#{app_flavor}` flavor matches what is set in our Fastfile. All is good!")
    else
      UI.user_error! <<~ERROR
        The list of `resourceConfigurations` declared in your `build.gradle` for the `#{app_flavor}` flavor
        does not match the list of locales we hardcoded in the `fastlane/lanes/localization.rb` for this app.

        If you recently updated the hardcoded list of locales to include for this app, be sure to apply those
        changes in both places, to keep the Fastlane scripts consistent with the gradle configuration of your app.
      ERROR
    end
  end
end
