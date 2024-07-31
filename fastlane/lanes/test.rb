# frozen_string_literal: true

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
  desc 'Build the application and instrumented tests, then run the tests in Firebase Test Lab'
  lane :build_and_run_instrumented_test do |options|
    app = get_app_name_option!(options)

    gradle(tasks: ["WordPress:assemble#{app.to_s.capitalize}VanillaDebug", "WordPress:assemble#{app.to_s.capitalize}VanillaDebugAndroidTest"])

    test_succeeded = gradle(task: "runFlank#{app.to_s.capitalize}")

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
