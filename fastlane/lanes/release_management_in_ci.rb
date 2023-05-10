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
      message: 'Code Freeze in CI'
    )
  end

  lane :trigger_complete_code_freeze_in_ci do |options|
    release_version = options[:release_version]
    buildkite_trigger_build(
      buildkite_organization: BUILDKITE_ORGANIZATION,
      buildkite_pipeline: BUILDKITE_PIPELINE,
      branch: "release/#{release_version}",
      pipeline_file: 'complete-code-freeze.yml',
      message: 'Complete Code Freeze in CI',
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
      message: 'Finalize release',
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
      message: 'New beta release',
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
      message: 'Update release notes',
      environment: { RELEASE_VERSION: release_version, EDITORIAL_BRANCH: editorial_branch }
    )
  end

  #####################################################################################
  # Release Management Utils
  #####################################################################################
  def create_release_management_pull_request(base_branch, title)
    create_pull_request(
        api_token: ENV['GITHUB_TOKEN'],
        repo: GHHELPER_REPO,
        title: title,
        head: Fastlane::Helper::GitHelper.current_git_branch,
        base: base_branch,
        labels: 'Releases'
    )
  end
end
