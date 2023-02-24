GOOGLE_FIREBASE_SECRETS_PATH = File.join(Dir.home, '.configure', 'wordpress-android', 'secrets', 'firebase.secrets.json')

platform :android do
  #####################################################################################
  # build_and_run_instrumented_test
  # -----------------------------------------------------------------------------------
  # Run instrumented tests in Google Firebase Test Lab
  # -----------------------------------------------------------------------------------
  # Usage:
  # bundle exec fastlane build_and_run_instrumented_test app:wordpress
  # bundle exec fastlane build_and_run_instrumented_test app:jetpack
  #
  #####################################################################################
  desc "Build the application and instrumented tests, then run the tests in Firebase Test Lab"
  lane :build_and_run_instrumented_test do | options |
    app = get_app_name_option!(options)

    gradle(tasks: ["WordPress:assemble#{app.to_s.capitalize}VanillaDebug", "WordPress:assemble#{app.to_s.capitalize}VanillaDebugAndroidTest"])

    # Run the instrumented tests in Firebase Test Lab
    firebase_login(
      key_file: GOOGLE_FIREBASE_SECRETS_PATH
    )

    apk_dir = File.join(PROJECT_ROOT_FOLDER, 'WordPress', 'build', 'outputs', 'apk')

    test_succeeded = android_firebase_test(
      project_id: firebase_secret(name: 'project_id'),
      key_file: GOOGLE_FIREBASE_SECRETS_PATH,
      model: 'Pixel2.arm',
      version: 30,
      test_apk_path: File.join(apk_dir, 'androidTest', "#{app}Vanilla", 'debug', "org.wordpress.android-#{app}-vanilla-debug-androidTest.apk"),
      apk_path: File.join(apk_dir, "#{app}Vanilla", 'debug', "org.wordpress.android-#{app}-vanilla-debug.apk"),
      test_targets: APP_SPECIFIC_VALUES[app][:test_targets],
      results_output_dir: File.join(PROJECT_ROOT_FOLDER, 'build', 'instrumented-tests'),
      crash_on_test_failure: false
    )

    annotation_ctx = "firebase-test-#{app}-vanilla-debug"
    if test_succeeded
      sh("buildkite-agent annotation remove --context '#{annotation_ctx}' || true") if is_ci?
    else
      details_url = lane_context[SharedValues::FIREBASE_TEST_MORE_DETAILS_URL]
      message = "Firebase Tests failed. Failure details can be seen [here in Firebase Console](#{details_url})"
      sh('buildkite-agent', 'annotate', message, '--style', 'error', '--context', annotation_ctx) if is_ci?
      UI.test_failure!(message)
    end
  end
end

def firebase_secret(name:)
  UI.user_error!('Unable to locale Firebase Secrets File – did you run `configure apply`?') unless File.file? GOOGLE_FIREBASE_SECRETS_PATH
  key_file_secrets = JSON.parse(File.read(GOOGLE_FIREBASE_SECRETS_PATH))
  UI.user_error!("Unable to find key `#{name}` in #{GOOGLE_FIREBASE_SECRETS_PATH}") if key_file_secrets[name].nil?
  key_file_secrets[name]
end
