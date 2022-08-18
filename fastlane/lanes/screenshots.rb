RAW_SCREENSHOTS_DIR = File.join(Dir.pwd, "screenshots", "raw")
RAW_SCREENSHOTS_PROCESSING_DIR = File.join(Dir.pwd, "screenshots", "raw_tmp")
PROMO_SCREENSHOTS_PROCESSING_DIR = File.join(Dir.pwd, "screenshots", "promo_tmp")
FINAL_METADATA_DIR = File.join(Dir.pwd, "metadata/android")


platform :android do
  #####################################################################################
  # upload_and_replace_screenshots_in_play_store
  # -----------------------------------------------------------------------------------
  # This lane uploads the screenshots in /metadata/android/{locale}/images to Play
  # Store and replaces the existing ones.
  # If a locale doesn't have any screenshots, it'll be skipped.
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane upload_and_replace_screenshots_in_play_store app:<wordpress|jetpack>
  #
  # Example:
  # bundle exec fastlane upload_and_replace_screenshots_in_play_store app:wordpress
  #####################################################################################
  desc 'Upload Screenshots to Play Store and Replaces the existing ones'
  lane :upload_and_replace_screenshots_in_play_store do |options|
    app = get_app_name_option!(options)
    package_name = APP_SPECIFIC_VALUES[app.to_sym][:package_name]
    metadata_dir = File.join('fastlane', APP_SPECIFIC_VALUES[app.to_sym][:metadata_dir], 'android')

    upload_to_play_store(
      package_name: package_name,
      metadata_path: metadata_dir,
      skip_upload_apk: true,
      skip_upload_aab: true,
      skip_upload_metadata: true,
      skip_upload_changelogs: true,
      skip_upload_images: true,
      skip_upload_screenshots: false,
      json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
    )
  end

  #####################################################################################
  # screenshots
  # -----------------------------------------------------------------------------------
  # This lane takes screenshots for the WordPress app across the three device types:
  # phone, sevenInch and tenInch. If device serials are not provided these avds will be
  # used: fastlane_screenshots_phone, fastlane_screenshots_seven_inch,
  # fastlane_screenshots_ten_inch
  # -----------------------------------------------------------------------------------
  # Usage:
  # fastlane screenshots phone_serial:<serial> sevenInch_serial:<serial> tenInch_serial:<serial>
  #
  # Example:
  # fastlane screenshots
  # fastlane screenshots phone_serial:emulator-5444 sevenInch_serial:emulator-5446 tenInch_serial:emulator-5448
  #####################################################################################
  desc "Build and capture screenshots"
  lane :screenshots do |options|
    gradle(task: "assembleWordPressVanillaDebug assembleWordPressVanillaDebugAndroidTest")
    take_screenshots(options)
  end

  desc "Capture screenshots"
  lane :take_screenshots do |options|

  	rebuild_screenshot_devices

    # If you update this, be sure to also update the `rebuild_screenshot_devices` lane down below as well as the corresponding `fastlane/emulators/*.ini` config files.
    # You might also want to mirror the changes of emulators used here with the ones used on Firebase Test Lab (when running screenshots on CI)
    screenshot_devices = [
      {
        screenshot_type: 'phone',
        device_name: 'Pixel_3_API_28',
        device_serial: options[:phone_serial],
      },
      {
        screenshot_type: 'tenInch',
        device_name: 'Nexus_9_API_28',
        device_serial: options[:tenInch_serial],
      }
    ]

    # By default, clear previous screenshots
    should_clear_previous_screenshots = true

    # Allow creating screenshots for just one device type
    if options[:device] != nil
      screenshot_devices.keep_if { |device|
        device[:screenshot_type].casecmp(options[:device]) == 0
      }

      # Don't clear, because we might just be fixing one device type
      should_clear_previous_screenshots = false
    end

    locales = ALL_LOCALES
      .select { |hsh| hsh[:promo_config] != false }
      .map { |hsh| hsh[:google_play] }
      .compact

    # Allow creating screenshots for just one locale
    if options[:locale] != nil
      locales.keep_if { |locale|
        locale.casecmp(options[:locale]) == 0
      }

      # Don't clear, because we might just be fixing one locale
      should_clear_previous_screenshots = false
    end

    puts locales

    screenshot_options = {
      output_directory: RAW_SCREENSHOTS_DIR,
      app_apk_path: "WordPress/build/outputs/apk/wordpressVanilla/debug/org.wordpress.android-wordpress-vanilla-debug.apk",
      tests_apk_path: "WordPress/build/outputs/apk/androidTest/wordpressVanilla/debug/org.wordpress.android-wordpress-vanilla-debug-androidTest.apk",
      use_tests_in_classes: "org.wordpress.android.ui.screenshots.WPScreenshotTest",
      reinstall_app: false,
      clear_previous_screenshots: should_clear_previous_screenshots,
      locales: locales,
      test_instrumentation_runner: "org.wordpress.android.WordPressTestRunner",
      use_adb_root: true
    }

    take_android_emulator_screenshots(devices: screenshot_devices, screenshot_options: screenshot_options)
  end

  #####################################################################################
  # download_promo_strings
  # -----------------------------------------------------------------------------------
  # This lane download the translated promo strings from the translation system
  # -----------------------------------------------------------------------------------
  # Usage:
  # fastlane download_promo_strings
  #
  # Example:
  # fastlane download_promo_strings
  #####################################################################################
  desc "Downloads translated promo strings from the translation system"
  lane :download_promo_strings do |options|
    # "<key in .po file>" => { desc: "<name of txt file>" }
    files = (1..9).map do |n|
      ["play_store_screenshot_#{n}", { desc: "play_store_screenshot_#{n}.txt" }]
    end.to_h

    locales = ALL_LOCALES
      .select { |hsh| hsh[:promo_config] != false }
      .map {| hsh | [ hsh[:glotpress], hsh[:google_play] ]}

    gp_downloadmetadata(project_url: APP_SPECIFIC_VALUES[:wordpress][:glotpress_metadata_project],
      target_files: files,
      locales: locales,
      source_locale: "en-US",
      download_path: File.join(Dir.pwd, "/playstoreres/metadata")
    )
  end



  #####################################################################################
  # download_raw_screenshots
  # -----------------------------------------------------------------------------------
  # This lane downloads the raw screenshots generated by a Firebase Test Lab run.
  # -----------------------------------------------------------------------------------
  # Usage:
  #   fastlane download_raw_screenshots bucket:<gs-url> phone:<model-version> tenInch:<model-version>
  #
  # Example:
  #   fastlane download_raw_screenshots bucket:"gs://test-lab-some-id/yyyy-MM-dd_hh:mm:ss.fff_xyzt/" phone:blueline-28 tenInch:gts3lltevzw-28
  #
  # Notes:
  #   Screenshots generated by Firebase Test Lab are in JPG and have a long prefix in their basename. Those will automatically
  #   be converted to PNG and the long prefix will get removed when they get processed by `create_promo_screenshots` though.
  #####################################################################################
  desc "Download raw screenshots from Firebase Test Lab / Google Storage"
  lane :download_raw_screenshots do |options|
    folder_mapping = { options[:phone] => 'phoneScreenshots', options[:tenInch] => 'tenInchScreenshots' }

    FileUtils.rm_rf(RAW_SCREENSHOTS_DIR)
    FileUtils.mkdir_p(RAW_SCREENSHOTS_DIR)

    bucket_url = options[:bucket]
    device_dirs = `gsutil ls "#{bucket_url}"`
      .split("\n")
      .filter { |f| f.end_with?('/') }
      .sort

    gsutil_cp = ["gsutil", (is_ci? ? '-m' : nil), "cp", "-cU"].compact
    device_dirs.each do |device_dir|
      parts = File.basename(device_dir).split('-') # "model-api-locale-orientation[_rerunN]"
      model_and_api = parts[0..1].join('-')
      subdir = folder_mapping[model_and_api]
      next if subdir.nil?
      locale = parts[2].gsub('_','-')
      dest = File.join( RAW_SCREENSHOTS_DIR, locale, 'images', subdir )
      FileUtils.mkdir_p(dest)
      sh( *gsutil_cp, File.join(device_dir, "artifacts/*.jpg"), dest ) do |ok, res|
        UI.error("Failed to download artifacts for #{subdir} (status: #{res.exitstatus})") unless ok
      end
    end
  end


  #####################################################################################
  # create_promo_screenshots
  # -----------------------------------------------------------------------------------
  # This lane creates the promo screenshot from the original ones that
  # are taken by the screenshot lane
  # -----------------------------------------------------------------------------------
  # Usage:
  # fastlane create_promo_screenshots
  #
  # Example:
  # fastlane create_promo_screenshots
  #####################################################################################
  desc "Creates promo screenshots"
  lane :create_promo_screenshots do |options|
    begin
      require 'rmagick'
    rescue LoadError => e
      UI.user_error!("The rmagick gem doesn't seem to be installed. Be sure to use `bundle install --with screenshots`.")
    end

    # Clean temporary folder from previous runs
    FileUtils.rm_rf(PROMO_SCREENSHOTS_PROCESSING_DIR)

    # Create a copy of the files to work with – this ensures that if we're doing multiple
    # screenshot generation tasks close together, we can keep reusing the same source files
    FileUtils.rm_rf(RAW_SCREENSHOTS_PROCESSING_DIR)
    FileUtils.copy_entry(RAW_SCREENSHOTS_DIR, RAW_SCREENSHOTS_PROCESSING_DIR)

    # For JPG files generated by Firebase Test Lab: Convert them to PNG and get rid of long prefix in name
    Dir.glob(RAW_SCREENSHOTS_PROCESSING_DIR + "/**/*.jpg").each do |jpg_path|
      # Remove long package prefix + digit suffix inserted by FTL in screenshot file names...
      new_filename = File.basename(jpg_path, '.jpg')
      new_filename.gsub!('org.wordpress.android.ui.screenshots.WPScreenshotTest-wPScreenshotTest-', '')
      new_filename.gsub!(/-[0-9]+$/, '')
      new_filename += '.png'
      png_path = File.join( File.dirname(jpg_path), new_filename )
      # Convert to PNG
      UI.message("Converting JPG to PNG: #{png_path}...")
      image = Magick::Image.read(jpg_path).first
      image.format = "PNG"
      image.write(png_path)
      FileUtils.rm(jpg_path)
    end

    # For PNG files generated via screengrab: Remove the timestamps from filenames to make them easier to work with
    Dir.glob(RAW_SCREENSHOTS_PROCESSING_DIR + "/**/*.png").each do |entry|
      ext = File.extname(entry)
      newfilename = File.dirname(entry) + "/" + File.basename(entry, ext).split("_")[0] + ext
      File.rename( entry, newfilename.downcase )
    end

    locales = ALL_LOCALES
      .select { |hsh| hsh[:promo_config] != false }
      .map { |hsh| hsh[:google_play] }

    # Allow creating promo screenshots for just one locale
    if options[:locale] != nil
      locales.keep_if { |locale|
        locale.casecmp(options[:locale]) == 0
      }
    end

    # Remove locales we're not interested in
    Pathname(RAW_SCREENSHOTS_PROCESSING_DIR)
        .children
        .select(&:directory?)
        .reject { |dir| locales.include? File.basename(dir) }
        .each do |dir|
          FileUtils.rm_rf(dir)
        end

    # Run screenshots generator tool
    promo_screenshots(
      orig_folder: RAW_SCREENSHOTS_PROCESSING_DIR,
      metadata_folder: FINAL_METADATA_DIR,
      output_folder: PROMO_SCREENSHOTS_PROCESSING_DIR,
      force: options[:force],
    )

    # Remove old screenshots from FINAL_METADATA_DIR subfolders of the targetted locales
    UI.message("Cleaning old promo screenshots from #{FINAL_METADATA_DIR} for #{locales.count} selected locales...")
    locales.each do |locale|
      screenshot_files = Dir.glob("#{FINAL_METADATA_DIR}/#{locale}/images/*/*.png")
      FileUtils.rm( screenshot_files )
    end

    # Finally move generated screenshots from PROMO_SCREENSHOTS_PROCESSING_DIR to FINAL_METADATA_DIR subfolders
    relative_paths = Dir.chdir(PROMO_SCREENSHOTS_PROCESSING_DIR) { Dir.glob("*/images/*/*.png") }
    UI.message("Moving #{relative_paths.count} new promo screenshots to #{FINAL_METADATA_DIR}...")
    relative_paths.each do |entry|
      old_path = File.join( PROMO_SCREENSHOTS_PROCESSING_DIR, entry )
      new_path = File.join( FINAL_METADATA_DIR, entry )
      FileUtils.mkdir_p( File.dirname(new_path) )
      File.rename( old_path, new_path )
    end

    # Clean up the temp directories
    FileUtils.rm_rf(RAW_SCREENSHOTS_PROCESSING_DIR)
    FileUtils.rm_rf(PROMO_SCREENSHOTS_PROCESSING_DIR)
  end


  #####################################################################################
  # rebuild_screenshot_devices
  # -----------------------------------------------------------------------------------
  # This lane rebuilds all of the emulators used for generating screenshots. Beware – running
  # this action will overwrite the following emulators:
  #     - Nexus 9 API 28
  #     - Pixel 2 XL API 28
  # It will not overwrite any other devices.
  # -----------------------------------------------------------------------------------
  # Usage:
  # fastlane rebuild_screenshot_devices
  #
  # Example:
  # fastlane rebuild_screenshot_devices
  #####################################################################################
  desc "Rebuild screenshot devices"
  lane :rebuild_screenshot_devices do |options|
    emulators = [
        Dir.pwd + "/emulators/Nexus_9_API_28.ini",
        Dir.pwd + "/emulators/Pixel_2_XL_API_28.ini",
        Dir.pwd + "/emulators/Pixel_3_API_28.ini",
    ]

    emulators.each do |emulator_configuration|
        sh("helpers/copy-device.sh '#{emulator_configuration}'")
    end
  end
end
