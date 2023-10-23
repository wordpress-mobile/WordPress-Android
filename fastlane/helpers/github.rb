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
