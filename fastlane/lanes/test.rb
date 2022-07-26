GOOGLE_FIREBASE_SECRETS_PATH = File.join(Dir.home, '.configure', 'wordpress-android', 'secrets', 'firebase.secrets.json')

platform :android do
  #####################################################################################
  # build_and_instrumented_test
  # -----------------------------------------------------------------------------------
  # Run instrumented tests in Google Firebase Test Lab
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_and_instrumented_test
  #
  #####################################################################################
  desc "Build the application and instrumented tests, then run the tests in Firebase Test Lab"
  lane :build_and_instrumented_test do | options |
   gradle(tasks: ['WordPress:assembleWordPressVanillaDebug', 'WordPress:assembleWordPressVanillaDebugAndroidTest'])

   # Run the instrumented tests in Firebase Test Lab
   firebase_login(
     key_file: GOOGLE_FIREBASE_SECRETS_PATH
   )

   apk_dir = File.join(PROJECT_ROOT_FOLDER, 'WordPress', 'build', 'outputs', 'apk')

   android_firebase_test(
     project_id: firebase_secret(name: 'project_id'),
     key_file: GOOGLE_FIREBASE_SECRETS_PATH,
     model: 'Pixel2',
     version: 28,
     test_apk_path: File.join(apk_dir, 'androidTest', 'wordpressVanilla', 'debug', 'org.wordpress.android-wordpress-vanilla-debug-androidTest.apk'),
     apk_path: File.join(apk_dir, 'wordpressVanilla', 'debug', 'org.wordpress.android-wordpress-vanilla-debug.apk'),
     results_output_dir: File.join(PROJECT_ROOT_FOLDER, 'build', 'instrumented-tests'),
     test-targets: notPackage org.wordpress.android.ui.screenshots
   )
  end
end

def firebase_secret(name:)
  UI.user_error!('Unable to locale Firebase Secrets File – did you run `configure apply`?') unless File.file? GOOGLE_FIREBASE_SECRETS_PATH
  key_file_secrets = JSON.parse(File.read(GOOGLE_FIREBASE_SECRETS_PATH))
  UI.user_error!("Unable to find key `#{name}` in #{GOOGLE_FIREBASE_SECRETS_PATH}") if key_file_secrets[name].nil?
  key_file_secrets[name]
end
