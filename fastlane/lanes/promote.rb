# frozen_string_literal: true

require 'json'
require 'net/http'
require 'yaml'

# Promotes an already-uploaded build to the Play Store `beta` track without rebuilding.
# `gather_beta_candidates` lists the promotable builds and opens a Buildkite block step for a
# developer to pick one; `promote_to_beta` promotes the chosen version code for both apps.
#
# WordPress and Jetpack share a version code from the same build, so one pick promotes both.

BETA_TRACK = 'beta'

# The most candidates to offer in the picker.
PROMOTION_CANDIDATE_LIMIT = 12

# The block step writes the chosen version code here; the promote step reads it back.
# NOTE: `.buildkite/commands/promote-to-beta.sh` reads this same key as a bare string literal
# (`meta-data get "beta_build_to_promote"`) — keep the two in sync.
PROMOTION_META_DATA_KEY = 'beta_build_to_promote'
# Matched via the Buildkite API to find the block step's job and build its unblock URL.
PROMOTION_BLOCK_LABEL = ':android: Promote to beta'
PROMOTION_BLOCK_STEP_KEY = 'promote_to_beta_block'

# Written verbatim into the generated steps so `buildkite-agent pipeline upload` interpolates it.
CI_TOOLKIT_PLUGIN_REF = '$CI_TOOLKIT'

# Where the gather lane writes the generated block + promote steps (gitignored `build/`). The
# relative form is what `buildkite-agent pipeline upload` receives (it runs from PROJECT_ROOT_FOLDER).
PROMOTION_STEPS_RELATIVE_PATH = File.join('build', 'promote-steps.yml')
PROMOTION_STEPS_FILE = File.join(PROJECT_ROOT_FOLDER, PROMOTION_STEPS_RELATIVE_PATH)

# Buildkite coordinates, used to build the block step's unblock-dialog deep link.
BUILDKITE_ORGANIZATION = 'automattic'
BUILDKITE_PIPELINE = 'wordpress-android'

platform :android do
  # Lists the promotable builds and opens a Buildkite block step for a developer to pick one.
  # A build is promotable when its version code is present in both apps' Play libraries and higher
  # than the current `beta` release. Uploads the block + promote steps here (not from the CI
  # script) so it can read back the block step's job id for the Slack unblock-dialog link.
  #
  # @called_by CI (`.buildkite/commands/gather-beta-candidates.sh`)
  desc 'Gather promotable beta candidates and open the promotion block step'
  lane :gather_beta_candidates do
    ensure_promotion_on_trunk!

    candidates = beta_promotion_candidates

    if candidates.empty?
      notify_slack(':android: *Beta promotion* — no builds above the current beta version code. Nothing to promote.')
      UI.important('No promotion candidates found; skipping block step generation.')
      next
    end

    write_promotion_steps_file(candidates: candidates)
    upload_promotion_steps
    post_candidates_to_slack(candidates: candidates, pick_url: promotion_unblock_dialog_url)

    UI.success("Prepared #{candidates.count} promotion candidate(s) and opened the block step.")
  rescue StandardError => e
    notify_slack(":x: *Beta promotion* failed before the picker could open — #{e.message}")
    raise
  end

  # Promotes the chosen build to the `beta` track for both WordPress and Jetpack.
  #
  # @param [String] version_code The version code to promote, e.g. `269027172`.
  # @called_by CI (`.buildkite/commands/promote-to-beta.sh`)
  desc 'Promote an existing build to the beta track (WordPress + Jetpack)'
  lane :promote_to_beta do |version_code: nil|
    # Set once the per-app result has been posted, so the rescue doesn't double-report it.
    result_posted = false
    ensure_promotion_on_trunk!

    version_code = version_code.to_s.strip
    UI.user_error!('`version_code` is required, e.g. `version_code:269027172`') if version_code.empty?

    UI.important("Promoting version code #{version_code} to the beta track for WordPress and Jetpack")

    results = distribute_to_beta(version_code: version_code)

    post_promotion_result_to_slack(version_code: version_code, results: results)
    result_posted = true

    failed = results.reject { |_, result| result[:ok] }.keys
    UI.user_error!("Beta promotion failed for: #{failed.join(', ')}") unless failed.empty?

    UI.success("Promoted #{version_code} to beta for: #{results.keys.join(', ')}")
  rescue StandardError => e
    notify_slack(":x: *Beta promotion* failed — #{e.message}") unless result_posted
    raise
  end

  #################################################
  # Candidate discovery
  #################################################

  # The promotable builds: version codes present in both apps' Play libraries and above the
  # current beta release, newest first, capped to PROMOTION_CANDIDATE_LIMIT.
  def beta_promotion_candidates
    apps = %i[wordpress jetpack]

    combined_floor = apps.filter_map do |app|
      codes = beta_track_version_codes(package_name: APP_SPECIFIC_VALUES[app][:package_name])
      UI.message("#{app}: beta track version codes = #{codes.sort.inspect}")
      codes.max
    end.max
    UI.message("Combined beta floor = #{combined_floor.inspect}")

    available_per_app = apps.map do |app|
      codes = available_aab_version_codes(package_name: APP_SPECIFIC_VALUES[app][:package_name])
      UI.message("#{app}: available AAB version codes = #{codes.sort.inspect}")
      codes
    end

    common = available_per_app.reduce(:&) || []
    codes = common.select { |code| combined_floor.nil? || code > combined_floor }.sort.reverse.first(PROMOTION_CANDIDATE_LIMIT)

    codes.map { |code| candidate_for(code) }
  end

  # Builds a candidate hash from a version code, decoding the version name and build number the
  # continuous formatter encoded: `versionCode = (major * 10 + minor) * 1_000_000 + build_number`.
  def candidate_for(version_code)
    prefix = version_code / 1_000_000
    {
      version_code: version_code,
      version_name: "#{prefix / 10}.#{prefix % 10}",
      build_number: version_code % 1_000_000
    }
  end

  # Reads the version codes currently on the given package's `beta` track, as an array of integers.
  # Returns an empty array only when the track legitimately has no releases; a lookup error raises.
  def beta_track_version_codes(package_name:)
    google_play_track_version_codes(
      package_name: package_name,
      track: BETA_TRACK,
      json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
    )
  rescue StandardError => e
    # Fail loudly: swallowing this into [] would let a transient failure read as "nothing to
    # promote" (the intersection collapses) or "promote everything" (the floor becomes nil).
    UI.user_error!("Unable to read the beta track version codes for #{package_name}: #{e.message}")
  end

  # Lists every AAB version code uploaded for the given package (the Play "app bundle explorer"),
  # as an array of integers. Opens a throwaway Play edit and aborts it, so this only reads.
  # Returns an empty array if the lookup fails.
  def available_aab_version_codes(package_name:)
    require 'supply'
    require 'supply/options'

    Supply.config = FastlaneCore::Configuration.create(
      Supply::Options.available_options,
      { json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY, package_name: package_name }
    )

    client = Supply::Client.make_from_config
    client.begin_edit(package_name: package_name)
    codes =
      begin
        client.aab_version_codes
      ensure
        # Always discard the throwaway edit, even if the read raises.
        client.abort_current_edit
      end
    Array(codes).compact.map(&:to_i)
  rescue StandardError => e
    # Fail loudly, like beta_track_version_codes: a swallowed [] collapses the candidate set.
    UI.user_error!("Unable to list the available AAB version codes for #{package_name}: #{e.message}")
  end

  #################################################
  # Promotion
  #################################################

  # Promotes a version code to beta for each app, returning a per-app `{ ok:, error: }` result.
  # A failure for one app doesn't stop the other. Reuses upload_build_to_play_store (build.rb),
  # which owns the actual Play upload — the retry, the pinned retained codes, and the skip flags.
  def distribute_to_beta(version_code:)
    %i[wordpress jetpack].to_h do |app|
      result =
        begin
          upload_build_to_play_store(
            app: app.to_s,
            track: BETA_TRACK,
            version_code: version_code,
            # TODO: switch to 'completed' once the feature is ready to distribute to beta testers.
            release_status: 'draft'
          )
          { ok: true }
        rescue StandardError => e
          UI.error("Failed to promote #{app} (#{version_code}): #{e.message}")
          { ok: false, error: e.message }
        end
      [app, result]
    end
  end

  #################################################
  # Buildkite block step
  #################################################

  def write_promotion_steps_file(candidates:)
    options = candidates.map { |candidate| { 'label' => candidate_label(candidate), 'value' => candidate[:version_code].to_s } }

    steps = {
      'steps' => [
        {
          'block' => PROMOTION_BLOCK_LABEL,
          'key' => PROMOTION_BLOCK_STEP_KEY,
          'prompt' => 'Choose the build to release to beta testers. This promotes the matching WordPress and Jetpack builds.',
          # Keep the build "running" (not green) while it waits for a human.
          'blocked_state' => 'running',
          'fields' => [
            {
              'select' => 'Build to promote',
              'key' => PROMOTION_META_DATA_KEY,
              # No default, and required: an un-actioned unblock must not silently promote
              # whatever sorted newest — force an explicit human choice.
              'required' => true,
              'options' => options
            }
          ]
        },
        {
          'label' => ':rocket: Promote selected build to beta',
          'command' => '.buildkite/commands/promote-to-beta.sh',
          'plugins' => [CI_TOOLKIT_PLUGIN_REF],
          'agents' => { 'queue' => 'android' }
        }
      ]
    }

    FileUtils.mkdir_p(File.dirname(PROMOTION_STEPS_FILE))
    # `line_width: -1` keeps each label on one line (no YAML folding).
    File.write(PROMOTION_STEPS_FILE, steps.to_yaml(line_width: -1))
    UI.message("Wrote promotion steps for #{candidates.count} build(s) to #{PROMOTION_STEPS_FILE}")
  end

  # Source `shared-pipeline-vars` first so `$CI_TOOLKIT` is interpolated.
  def upload_promotion_steps
    sh('bash', '-c', "cd '#{PROJECT_ROOT_FOLDER}' && source .buildkite/shared-pipeline-vars && buildkite-agent pipeline upload '#{PROMOTION_STEPS_RELATIVE_PATH}'")
  end

  def candidate_label(candidate)
    "#{candidate[:version_name]} (#{candidate[:version_code]}) · build ##{candidate[:build_number]}"
  end

  #################################################
  # Slack
  #################################################

  # Posts the candidate list. Links straight to the unblock dialog when we resolved the block
  # step's job id; otherwise points at the build so a degraded link isn't mistaken for the picker.
  def post_candidates_to_slack(candidates:, pick_url: nil)
    build_url = ENV.fetch('BUILDKITE_BUILD_URL', nil)

    candidate_lines = candidates.map { |candidate| slack_candidate_line(candidate) }

    choose_line =
      if pick_url
        "\n\n:point_right: <#{pick_url}|Choose a build to promote>"
      elsif build_url
        "\n\n:warning: Couldn't deep-link to the picker — open <#{build_url}|the build> and unblock the promotion step to choose a build."
      else
        ''
      end

    notify_slack(
      <<~MSG
        :android: *Beta promotion* — choose a build to release to beta testers (WordPress + Jetpack).#{choose_line}

        *Candidates:*
        #{candidate_lines.join("\n")}
      MSG
    )
  end

  def slack_candidate_line(candidate)
    "• `#{candidate[:version_code]}` — #{candidate[:version_name]} · build ##{candidate[:build_number]}"
  end

  # Posts the per-app outcome of a promotion.
  def post_promotion_result_to_slack(version_code:, results:)
    status_lines = results.map do |app, result|
      next "• #{app}: :x: #{result[:error]}" unless result[:ok]

      "• #{app}: :white_check_mark: submitted to beta"
    end

    all_ok = results.values.all? { |result| result[:ok] }
    header = all_ok ? ':rocket: *Promoted to beta*' : ':warning: *Beta promotion finished with errors*'

    notify_slack(
      <<~MSG
        #{header} — `#{version_code}`

        #{status_lines.join("\n")}
      MSG
    )
  end

  # Posts to Slack without letting a webhook hiccup abort the caller.
  def notify_slack(message)
    send_slack_message(message: message)
  rescue StandardError => e
    UI.important("Slack notification failed (#{e.message}); continuing without it.")
  end

  # Sends a Slack message to the given channel via the incoming webhook.
  def send_slack_message(message:, channel: '#build-and-ship')
    slack(
      username: 'WordPress Release Bot',
      icon_url: 'https://s.w.org/style/images/about/WordPress-logotype-wmark.png',
      slack_url: get_required_env('SLACK_WEBHOOK'),
      channel: channel,
      message: message,
      default_payloads: []
    )
  end

  #################################################
  # Buildkite deep link
  #################################################

  # The block step's unblock-dialog URL, or nil (callers fall back to the build URL).
  def promotion_unblock_dialog_url
    org = ENV.fetch('BUILDKITE_ORGANIZATION_SLUG', BUILDKITE_ORGANIZATION)
    pipeline = ENV.fetch('BUILDKITE_PIPELINE_SLUG', BUILDKITE_PIPELINE)
    build_number = ENV.fetch('BUILDKITE_BUILD_NUMBER', nil)
    return nil if build_number.nil?

    job_id = block_step_job_id(org: org, pipeline: pipeline, build_number: build_number)
    return nil if job_id.nil?

    "https://buildkite.com/organizations/#{org}/pipelines/#{pipeline}/builds/#{build_number}/jobs/#{job_id}/unblock_dialog"
  end

  # Polls the build for the just-uploaded block step's job id (it takes a moment to register).
  def block_step_job_id(org:, pipeline:, build_number:)
    uri = URI("https://api.buildkite.com/v2/organizations/#{org}/pipelines/#{pipeline}/builds/#{build_number}")

    5.times do |attempt|
      job = find_promotion_block_job(uri)
      return job['id'] if job

      sleep(2) unless attempt == 4
    end

    UI.important('Could not resolve the promotion block step job id; Slack will link to the build instead.')
    nil
  rescue StandardError => e
    UI.important("Error resolving the block step job id (#{e.message}); Slack will link to the build instead.")
    nil
  end

  def find_promotion_block_job(uri)
    response = buildkite_api_get(uri)
    return nil unless response.is_a?(Net::HTTPSuccess)

    manual_jobs = JSON.parse(response.body).fetch('jobs', []).select { |job| job['type'] == 'manual' }

    # Match on the stable step key the block was generated with; fall back to the label only if the
    # API didn't surface a step key, so a second manual step whose label contains ours can't shadow it.
    manual_jobs.find { |job| job['step_key'] == PROMOTION_BLOCK_STEP_KEY } ||
      manual_jobs.find { |job| job['label'].to_s.include?(PROMOTION_BLOCK_LABEL) }
  end

  def buildkite_api_get(uri)
    request = Net::HTTP::Get.new(uri)
    request['Authorization'] = "Bearer #{buildkite_api_token}"
    # Bound the request so a hung response can't stall the gather lane across all 5 poll attempts.
    Net::HTTP.start(uri.hostname, uri.port, use_ssl: true, open_timeout: 10, read_timeout: 10) do |http|
      http.request(request)
    end
  end

  # A Buildkite API token with `read_builds` scope — `BUILDKITE_API_TOKEN` on CI, `BUILDKITE_TOKEN`
  # locally. Only needed for the Slack deep link; the flow still works without it (links to the build).
  def buildkite_api_token
    token = ENV.fetch('BUILDKITE_API_TOKEN', nil) || ENV.fetch('BUILDKITE_TOKEN', nil)
    UI.user_error!('No Buildkite API token found. Set `BUILDKITE_API_TOKEN` (or `BUILDKITE_TOKEN`).') if token.to_s.empty?
    token
  end

  #################################################
  # Utils
  #################################################

  # The promote lanes distribute to real testers and can be run by hand, so they must only ever run
  # from the scheduled `trunk` jobs. Any other branch — or a local run — is a mistake: fail loudly.
  def ensure_promotion_on_trunk!
    branch = ENV.fetch('BUILDKITE_BRANCH', '')
    return if branch == DEFAULT_BRANCH

    UI.user_error!("Promotion only runs on `#{DEFAULT_BRANCH}` (current branch: #{branch.empty? ? 'none' : "`#{branch}`"}). Refusing to proceed.")
  end

  # Returns an env var's value, failing loudly if it's unset.
  def get_required_env(key)
    UI.user_error!("Environment variable '#{key}' is not set.") unless ENV.key?(key)
    ENV.fetch(key, nil)
  end
end
