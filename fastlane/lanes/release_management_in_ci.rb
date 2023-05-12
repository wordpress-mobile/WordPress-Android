BUILDKITE_ORGANIZATION = 'automattic'.freeze
BUILDKITE_PIPELINE = 'wordpress-android'.freeze
platform :android do
  #####################################################################################
  # Triggers for Buildkite
  #####################################################################################
  lane :trigger_code_freeze_in_ci do |options|
    buildkite_trigger_build(
      buildkite_organization: BUILDKITE_ORGANIZATION,
      buildkite_pipeline: BUILDKITE_PIPELINE,
      branch: 'trunk',
      pipeline_file: 'code-freeze.yml',
      message: 'Code Freeze'
    )
  end

  lane :trigger_complete_code_freeze_in_ci do |options|
    release_version = options[:release_version]
    buildkite_trigger_build(
      buildkite_organization: BUILDKITE_ORGANIZATION,
      buildkite_pipeline: BUILDKITE_PIPELINE,
      branch: "release/#{release_version}",
      pipeline_file: 'complete-code-freeze.yml',
      message: 'Complete Code Freeze',
      environment: { RELEASE_VERSION: release_version }
    )
  end

  lane :trigger_finalize_release_in_ci do |options|
    release_version = options[:release_version]
    buildkite_trigger_build(
      buildkite_organization: BUILDKITE_ORGANIZATION,
      buildkite_pipeline: BUILDKITE_PIPELINE,
      branch: "release/#{release_version}",
      pipeline_file: 'finalize-release.yml',
      message: 'Finalize Release',
      environment: { RELEASE_VERSION: release_version }
    )
  end

  lane :trigger_new_beta_release_in_ci do |options|
    release_version = options[:release_version]
    buildkite_trigger_build(
      buildkite_organization: BUILDKITE_ORGANIZATION,
      buildkite_pipeline: BUILDKITE_PIPELINE,
      branch: "release/#{release_version}",
      pipeline_file: 'new-beta-release.yml',
      message: 'New Beta Release',
      environment: { RELEASE_VERSION: release_version }
    )
  end

  lane :trigger_update_appstore_strings_in_ci do |options|
    release_version = options[:release_version]
    editorial_branch = options[:editorial_branch]
    buildkite_trigger_build(
      buildkite_organization: BUILDKITE_ORGANIZATION,
      buildkite_pipeline: BUILDKITE_PIPELINE,
      branch: "#{editorial_branch}",
      pipeline_file: 'update-release-notes.yml',
      message: 'Update Release Notes',
      environment: { RELEASE_VERSION: release_version, EDITORIAL_BRANCH: editorial_branch }
    )
  end
end
