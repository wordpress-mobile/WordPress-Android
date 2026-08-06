# frozen_string_literal: true

require 'json'
require 'net/http'
require 'yaml'

# Promotes already-uploaded builds between Play Store tracks without rebuilding.
#
# Beta (internal → beta): `gather_beta_candidates` lists the promotable builds and opens a Buildkite
# block step for a developer to pick one; `promote_to_beta` promotes the chosen version code.
#
# Production (beta → production): `gather_production_candidate` takes the current beta build and opens
# a confirmation block step; `promote_to_production` promotes it to production.
#
# WordPress and Jetpack share a version code from the same build, so one action promotes both apps.

BETA_TRACK = 'beta'
PRODUCTION_TRACK = 'production'

# Beta promotion (picker).
# The most candidates to offer in the picker.
BETA_CANDIDATE_LIMIT = 12
# The block step writes the chosen version code here; the promote step reads it back.
# NOTE: `.buildkite/commands/promote-to-beta.sh` reads this same key as a bare string literal
# (`meta-data get "beta_build_to_promote"`) — keep the two in sync.
BETA_META_DATA_KEY = 'beta_build_to_promote'
# The block step also writes the chosen release-note option here; the promote step reads it back.
# NOTE: `.buildkite/commands/promote-to-beta.sh` reads this same key as a bare string literal
# (`meta-data get "beta_release_notes_option"`) — keep the two in sync.
BETA_RELEASE_NOTES_META_DATA_KEY = 'beta_release_notes_option'
# Matched via the Buildkite API to find the block step's job and build its unblock URL.
BETA_BLOCK_LABEL = ':android: Promote to beta'
BETA_BLOCK_STEP_KEY = 'promote_to_beta_block'

# Production release (confirm).
# The confirmation field's Yes/No value is written here; the release step reads it back.
# NOTE: `.buildkite/commands/promote-to-production.sh` reads this same key as a bare string literal
# (`meta-data get "production_ready_to_release"`) — keep the two in sync.
PRODUCTION_CONFIRM_META_DATA_KEY = 'production_ready_to_release'
# Matched via the Buildkite API to find the block step's job and build its unblock URL.
PRODUCTION_BLOCK_LABEL = ':android: Promote to production'
PRODUCTION_BLOCK_STEP_KEY = 'promote_to_production_block'

# Promoted-release finalize (the git ceremony after the Play submit).
# A continuous build's versionCode packs the Buildkite build number in its low 6 digits (via
# release-toolkit's ContinuousBuildCodeFormatter, build_digits: 6):
#   versionCode = (major * 10 + minor) * 1_000_000 + BUILDKITE_BUILD_NUMBER
# so `versionCode % MODULO` recovers the build number and `versionCode / MODULO` the `major*10+minor`.
# TODO: could move next to ContinuousBuildCodeFormatter in release-toolkit later; fine here for now.
VERSION_CODE_BUILD_MODULO = 1_000_000

# Written verbatim into the generated steps so `buildkite-agent pipeline upload` interpolates it.
CI_TOOLKIT_PLUGIN_REF = '$CI_TOOLKIT'

# Where the gather lane writes the generated block + promote steps (gitignored `build/`). The
# relative form is what `buildkite-agent pipeline upload` receives (it runs from PROJECT_ROOT_FOLDER).
PROMOTION_STEPS_RELATIVE_PATH = File.join('build', 'promote-steps.yml')
PROMOTION_STEPS_FILE = File.join(PROJECT_ROOT_FOLDER, PROMOTION_STEPS_RELATIVE_PATH)

# Buildkite coordinates, used to build the block step's unblock-dialog deep link.
BUILDKITE_ORGANIZATION = 'automattic'
BUILDKITE_PIPELINE = 'wordpress-android'

# Production staged-rollout growth (scheduled bump job).
# The rollout ladder the scheduled job walks — it advances the live production rollout to the next
# step each run. These are a starting point (Play accepts any fraction); adjust freely. "Next step" is
# the smallest value strictly above the current fraction, so an off-ladder manual % still moves
# forward. Past the top step the release is finalized to 100% (status `completed`).
PRODUCTION_ROLLOUT_STEPS = [0.01, 0.02, 0.05, 0.10, 0.20, 0.50].freeze

# Play `TrackRelease.status` values. A paused rollout — the Play Console "pause", or a Play auto-halt
# (newer draft upload, policy/vitals) — is `halted`; there is no separate "paused" status. The growth
# job only ever advances an `inProgress` release, so a scheduled run can never resume a paused one.
ROLLOUT_STATUS_IN_PROGRESS = 'inProgress'
ROLLOUT_STATUS_HALTED = 'halted'
ROLLOUT_STATUS_DRAFT = 'draft'
ROLLOUT_STATUS_COMPLETED = 'completed'

platform :android do
  # Lists the promotable builds — version codes in both apps' Play libraries above the current
  # `beta` release — and opens a Buildkite block step for a developer to pick one.
  #
  # @called_by CI (`.buildkite/commands/gather-beta-candidates.sh`)
  desc 'Gather promotable beta candidates and open the promotion block step'
  lane :gather_beta_candidates do
    ensure_promotion_on_trunk!
    # Fail loudly up front if Slack isn't configured: every step here reports to Slack, and
    # notify_slack otherwise swallows a missing SLACK_WEBHOOK as if it were a transient hiccup.
    get_required_env('SLACK_WEBHOOK')

    candidates = beta_promotion_candidates

    if candidates.empty?
      notify_slack(':android: *Beta promotion* — no builds above the current beta version code. Nothing to promote.')
      UI.important('No promotion candidates found; skipping block step generation.')
      next
    end

    write_beta_promotion_steps_file(candidates: candidates)
    upload_promotion_steps
    post_beta_candidates_to_slack(
      candidates: candidates,
      pick_url: promotion_unblock_dialog_url(block_step_key: BETA_BLOCK_STEP_KEY, block_label: BETA_BLOCK_LABEL)
    )

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
  lane :promote_to_beta do |version_code: nil, release_notes_option: nil|
    # Set once the per-app result has been posted, so the rescue doesn't double-report it.
    result_posted = false
    ensure_promotion_on_trunk!
    # Fail loudly up front if Slack isn't configured (see gather_beta_candidates).
    get_required_env('SLACK_WEBHOOK')

    version_code = version_code.to_s.strip
    UI.user_error!('`version_code` is required, e.g. `version_code:269027172`') if version_code.empty?
    UI.user_error!("`version_code` must be an integer, got #{version_code.inspect}") unless version_code.match?(/\A\d+\z/)

    release_notes_option = validated_release_notes_option(release_notes_option)

    UI.important("Promoting version code #{version_code} to the beta track for WordPress and Jetpack")

    results = distribute_to_beta(version_code: version_code, release_notes_option: release_notes_option)

    post_beta_result_to_slack(version_code: version_code, results: results)
    result_posted = true

    failed = results.reject { |_, result| result[:ok] }.keys
    UI.user_error!("Beta promotion failed for: #{failed.join(', ')}") unless failed.empty?

    UI.success("Promoted #{version_code} to beta for: #{results.keys.join(', ')}")
  rescue StandardError => e
    notify_slack(":x: *Beta promotion* failed — #{e.message}") unless result_posted
    raise
  end

  # Determines the production release candidate — the current beta build, when it's ahead of
  # production — and opens a confirmation block step for a developer to approve the release.
  #
  # @called_by CI (`.buildkite/commands/gather-production-candidate.sh`)
  desc 'Gather the production release candidate and open the confirmation block step'
  lane :gather_production_candidate do
    ensure_promotion_on_trunk!
    # Fail loudly up front if Slack isn't configured (see gather_beta_candidates).
    get_required_env('SLACK_WEBHOOK')

    version_code = production_promotion_candidate

    if version_code.nil?
      notify_slack(':android: *Production release* — the beta build is not ahead of production. Nothing to release.')
      UI.important('No production release candidate; skipping block step generation.')
      next
    end

    write_production_release_steps_file(version_code: version_code)
    upload_promotion_steps
    post_production_candidate_to_slack(
      version_code: version_code,
      confirm_url: promotion_unblock_dialog_url(block_step_key: PRODUCTION_BLOCK_STEP_KEY, block_label: PRODUCTION_BLOCK_LABEL)
    )

    UI.success("Prepared production release candidate #{version_code} and opened the block step.")
  rescue StandardError => e
    notify_slack(":x: *Production release* failed before the confirmation could open — #{e.message}")
    raise
  end

  # Promotes the confirmed build to the `production` track for both WordPress and Jetpack.
  #
  # @param [String] version_code The version code to promote, e.g. `270084231`.
  # @called_by CI (`.buildkite/commands/promote-to-production.sh`)
  desc 'Promote an existing build to the production track (WordPress + Jetpack)'
  lane :promote_to_production do |version_code: nil|
    # Set once the per-app result has been posted, so the rescue doesn't double-report it.
    result_posted = false
    ensure_promotion_on_trunk!
    # Fail loudly up front if Slack isn't configured (see gather_beta_candidates).
    get_required_env('SLACK_WEBHOOK')

    version_code = version_code.to_s.strip
    UI.user_error!('`version_code` is required, e.g. `version_code:270084231`') if version_code.empty?
    UI.user_error!("`version_code` must be an integer, got #{version_code.inspect}") unless version_code.match?(/\A\d+\z/)

    UI.important("Promoting version code #{version_code} to the production track for WordPress and Jetpack")

    results = distribute_to_production(version_code: version_code)

    post_production_result_to_slack(version_code: version_code, results: results)
    result_posted = true

    failed = results.reject { |_, result| result[:ok] }.keys
    UI.user_error!("Production release failed for: #{failed.join(', ')}") unless failed.empty?

    UI.success("Promoted #{version_code} to production for: #{results.keys.join(', ')}")
  rescue StandardError => e
    notify_slack(":x: *Production release* failed — #{e.message}") unless result_posted
    raise
  end

  # Finalizes a promoted release after the Play submit: creates a draft GitHub release tagged at the
  # commit the build came from, and opens the trunk version-name bump PR. Runs as the mac-metal step
  # the promote-to-production pipeline appends after `promote_to_production` (git writes need the bot
  # identity), and is safe to re-run by hand with the same version code.
  #
  # @param [String] version_code The version code that was released, e.g. `270084231`.
  # @called_by CI (`.buildkite/commands/finalize-promoted-release.sh`)
  desc 'Finalize a promoted release: draft GitHub release + trunk version-name bump PR'
  lane :finalize_promoted_release do |version_code: nil|
    ensure_promotion_on_trunk!
    # Fail loudly up front if the environment isn't configured. SLACK_WEBHOOK backs only the failure
    # notification (the rescue below — there's no success post). BUILDKITE_TOKEN is required, not a
    # degradable deep-link nicety: the released commit is resolved from the Buildkite build, and the
    # GitHub release can't be tagged without it.
    get_required_env('SLACK_WEBHOOK')
    get_required_env('GITHUB_TOKEN')
    get_required_env('BUILDKITE_TOKEN')

    version_code = version_code.to_s.strip
    UI.user_error!('`version_code` is required, e.g. `version_code:270084231`') if version_code.empty?
    UI.user_error!("`version_code` must be an integer, got #{version_code.inspect}") unless version_code.match?(/\A\d+\z/)
    # Base 10 so a value with a leading zero (e.g. `0270084231`) isn't parsed as octal.
    version_code = Integer(version_code, 10)

    released_version = marketing_version_for(version_code: version_code)
    UI.important("Finalizing promoted release #{released_version} (build #{version_code})")

    release_url = create_draft_github_release(version_code: version_code, version_name: released_version)
    bump_url = open_version_bump_pull_request(released_version: released_version)

    # No success post to Slack on purpose: the Play-submit step already announced the release, the
    # draft GitHub release is published later, and the version-bump PR pings its reviewers itself.
    # Only failures notify (the rescue below) — anything else would just be noise.
    UI.success("Finalized #{released_version}: release #{release_url}; version bump #{bump_url || 'skipped'}")
  rescue StandardError => e
    notify_slack(":x: *Promoted-release finalize* failed — #{e.message}")
    raise
  end

  # Advances the live production staged rollout one step — reads the current rollout percentage back
  # from Play and bumps WordPress + Jetpack to the next ladder step, finalizing to 100% once past the
  # top. Runs on a daily schedule; stateless (one step per run). Pause-safe: it only ever advances an
  # `inProgress` release, so it never resumes a rollout a developer paused (a paused rollout is
  # `halted`). WordPress and Jetpack must be in the same rollout state — a mismatch stops for a
  # developer to reconcile rather than guessing.
  #
  # @called_by CI (`.buildkite/commands/advance-production-rollout.sh`)
  desc 'Advance the production staged rollout one step (WordPress + Jetpack)'
  lane :advance_production_rollout do
    # Set once the per-app result has been posted, so the rescue doesn't double-report it.
    result_posted = false
    ensure_promotion_on_trunk!
    # Fail loudly up front if Slack isn't configured (see gather_beta_candidates).
    get_required_env('SLACK_WEBHOOK')

    states = %i[wordpress jetpack].to_h do |app|
      [app, current_production_rollout(package_name: APP_SPECIFIC_VALUES[app][:package_name])]
    end
    UI.message("Production rollout states: #{states.inspect}")

    # Both apps promote together, so they must be in the same rollout state. A mismatch means a prior
    # step only half-applied, or one app's rollout was paused/changed — stop and reconcile by hand.
    unless rollout_signature(states[:wordpress]) == rollout_signature(states[:jetpack])
      UI.user_error!(
        'WordPress and Jetpack production rollouts differ ' \
        "(WP=#{states[:wordpress].inspect}, JP=#{states[:jetpack].inspect}); reconcile by hand."
      )
    end

    # Both apps share this state now; act on either.
    state = states[:wordpress]

    if state.nil?
      UI.important('No production rollout in flight (the track is at steady state); nothing to advance.')
      next
    end

    unless state[:status] == ROLLOUT_STATUS_IN_PROGRESS
      # `draft` = not started yet; `halted` = paused (by a developer or auto-halted). Either way, leave
      # it — advancing a non-`inProgress` release is the footgun we refuse (it would resume a pause).
      UI.important("Production rollout is `#{state[:status]}` (not in progress); leaving it untouched.")
      next
    end

    target = next_production_rollout_target(current_fraction: state[:user_fraction].to_f)
    UI.important(
      "Advancing production rollout #{state[:version_code]} from #{state[:user_fraction]} " \
      "to #{rollout_target_label(target)}"
    )

    results = distribute_rollout_advance(target: target)

    post_rollout_result_to_slack(version_code: state[:version_code], target: target, results: results)
    result_posted = true

    failed = results.reject { |_, result| result[:ok] }.keys
    UI.user_error!("Production rollout advance failed for: #{failed.join(', ')}") unless failed.empty?

    UI.success("Advanced production rollout #{state[:version_code]} to #{rollout_target_label(target)}")
  rescue StandardError => e
    notify_slack(":x: *Production rollout* failed — #{e.message}") unless result_posted
    raise
  end

  #################################################
  # Candidate discovery
  #################################################

  # The promotable version codes: present in both apps' Play libraries and above the current beta
  # release, newest first, capped to BETA_CANDIDATE_LIMIT.
  def beta_promotion_candidates
    apps = %i[wordpress jetpack]

    combined_floor = apps.filter_map do |app|
      codes = track_version_codes(package_name: APP_SPECIFIC_VALUES[app][:package_name], track: BETA_TRACK)
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
    common.select { |code| combined_floor.nil? || code > combined_floor }.max(BETA_CANDIDATE_LIMIT)
  end

  # The single production release candidate: the version code both apps share on the `beta` track,
  # when it's ahead of production. Returns nil when there's nothing to release (no beta build, or
  # beta not ahead of production).
  def production_promotion_candidate
    beta_code = common_track_version_code(track: BETA_TRACK, label: 'beta')
    return nil if beta_code.nil?

    production_code = common_track_version_code(track: PRODUCTION_TRACK, label: 'production')
    return nil if production_code && beta_code <= production_code

    beta_code
  end

  # The version code both apps share on the given track (they promote together, so it must match), or
  # nil when neither app has a release there. Raises when the two apps disagree — a mismatch means an
  # earlier promotion only half-completed and must be reconciled by hand before releasing.
  def common_track_version_code(track:, label:)
    codes = %i[wordpress jetpack].map do |app|
      code = track_version_codes(package_name: APP_SPECIFIC_VALUES[app][:package_name], track: track).max
      UI.message("#{app}: #{label} version code = #{code.inspect}")
      code
    end
    return nil if codes.all?(&:nil?)

    UI.user_error!("WordPress and Jetpack #{label} version codes differ (#{codes.inspect}); reconcile by hand before releasing.") unless codes.uniq.one?
    codes.first
  end

  # Reads the version codes currently on the given package's track, as an array of integers.
  # Returns an empty array only when the track legitimately has no releases; a lookup error raises.
  def track_version_codes(package_name:, track:)
    google_play_track_version_codes(
      package_name: package_name,
      track: track,
      json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
    )
  rescue StandardError => e
    # Raise rather than return []: an errored lookup must not read as an empty track.
    UI.user_error!("Unable to read the #{track} track version codes for #{package_name}: #{e.message}")
  end

  # Play intermittently 400s a call with "Google Api Error: ... This Edit has been deleted." Neither
  # the google-apis transport (400s aren't retried) nor supply's call_google_api recovers, and
  # recovery needs a fresh edit — so retry the whole begin_edit…commit/read block, not the single
  # call. Also guards against a concurrent Play edit from another job invalidating ours.
  def with_play_edit_retries(description, attempts: 3, wait: 15)
    tries = 0
    begin
      yield
    rescue FastlaneCore::Interface::FastlaneError => e
      tries += 1
      raise unless e.message.start_with?('Google Api Error') && tries < attempts

      UI.error("#{description} failed (#{e.message}); retrying with a fresh edit (#{tries}/#{attempts - 1}) in #{wait}s")
      sleep(wait)
      retry
    end
  end

  # Lists every AAB version code uploaded for the given package (the Play "app bundle explorer"),
  # as an array of integers. Opens a throwaway Play edit and aborts it, so this only reads.
  def available_aab_version_codes(package_name:)
    require 'supply'
    require 'supply/options'

    Supply.config = FastlaneCore::Configuration.create(
      Supply::Options.available_options,
      { json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY, package_name: package_name }
    )

    codes = with_play_edit_retries("Listing AAB version codes for #{package_name}") do
      client = Supply::Client.make_from_config
      client.begin_edit(package_name: package_name)
      begin
        client.aab_version_codes
      ensure
        # Always discard the throwaway edit, even if the read raises.
        client.abort_current_edit
      end
    end
    Array(codes).compact.map(&:to_i)
  rescue StandardError => e
    # Raise rather than return [], as in track_version_codes.
    UI.user_error!("Unable to list the available AAB version codes for #{package_name}: #{e.message}")
  end

  #################################################
  # Promotion
  #################################################

  # Promotes a version code to beta for each app, returning a per-app `{ ok:, error: }` result.
  # A failure for one app doesn't stop the other.
  def distribute_to_beta(version_code:, release_notes_option:)
    %i[wordpress jetpack].to_h do |app|
      result =
        begin
          promote_version_code_to_beta(
            app: app,
            package_name: APP_SPECIFIC_VALUES[app][:package_name],
            version_code: version_code,
            release_notes_option: release_notes_option
          )
          { ok: true }
        rescue StandardError => e
          UI.error("Failed to promote #{app} (#{version_code}): #{e.message}")
          { ok: false, error: e.message }
        end
      [app, result]
    end
  end

  # Creates a draft `beta` release referencing an already-uploaded version code, via the Play API.
  #
  # `upload_to_play_store` can't do this: it only builds a track release from binaries uploaded in
  # the same run, so a bare `version_code:` with `skip_upload_aab` commits an empty edit. We create
  # the release directly instead, mirroring supply's own `update_track`.
  def promote_version_code_to_beta(app:, package_name:, version_code:, release_notes_option:)
    require 'supply'
    require 'supply/options'

    Supply.config = FastlaneCore::Configuration.create(
      Supply::Options.available_options,
      { json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY, package_name: package_name, track: BETA_TRACK }
    )

    # The picked release notes (needs supply loaded, above). nil when the option has no files.
    release_notes = static_release_notes(app: app, option: release_notes_option)
    release_notes = nil if release_notes.empty?

    with_play_edit_retries("Promoting #{version_code} to beta for #{package_name}") do
      client = Supply::Client.make_from_config
      client.begin_edit(package_name: package_name)

      committed = false
      begin
        release = AndroidPublisher::TrackRelease.new(
          # TODO: switch to 'completed' once the feature is ready to distribute to beta testers.
          status: 'draft',
          # Keep the pinned legacy code(s) on the track, same as the AAB-upload path.
          version_codes: [Integer(version_code), *PLAY_STORE_VERSION_CODES_TO_RETAIN],
          # Carried to production automatically by the promote.
          release_notes: release_notes
        )
        track = client.tracks(BETA_TRACK).first || AndroidPublisher::Track.new(track: BETA_TRACK)
        track.releases = [release]

        client.update_track(BETA_TRACK, track)
        client.commit_current_edit!
        committed = true
      ensure
        # Discard the edit if we bailed before committing (a committed edit can't be aborted).
        client.abort_current_edit unless committed
      end
    end
  end

  # The picked option's notes as Play `LocalizedText`, one per locale that has a
  # `release_notes_static/<option>.txt` file. Missing locales fall back to Play's default language.
  def static_release_notes(app:, option:)
    metadata_dir = File.join(FASTLANE_FOLDER, APP_SPECIFIC_VALUES[app][:metadata_dir], 'android')
    notes = Dir.glob(File.join(metadata_dir, '*', 'release_notes_static', "#{option}.txt")).filter_map do |path|
      text = File.read(path).strip
      next if text.empty?

      # Path is <metadata_dir>/<locale>/release_notes_static/<option>.txt; the locale is two dirs up.
      locale = File.basename(File.dirname(path, 2))
      AndroidPublisher::LocalizedText.new(language: locale, text: text)
    end
    UI.user_error!("No static release notes found for option #{option.inspect} (#{app}).") if notes.empty?
    notes
  end

  # Validates the picked option against the registry, raising on anything unknown.
  def validated_release_notes_option(option)
    option = option.to_s.strip
    valid = STATIC_RELEASE_NOTE_OPTIONS.map { |o| o[:key] }
    UI.user_error!("`release_notes_option` is required, one of: #{valid.join(', ')}") if option.empty?
    return option if valid.include?(option)

    UI.user_error!("Unknown `release_notes_option` #{option.inspect}; expected one of: #{valid.join(', ')}")
  end

  # Promotes a version code to production for each app, returning a per-app `{ ok:, error: }` result.
  # A failure for one app doesn't stop the other.
  def distribute_to_production(version_code:)
    %i[wordpress jetpack].to_h do |app|
      result =
        begin
          promote_version_code_to_production(
            package_name: APP_SPECIFIC_VALUES[app][:package_name],
            version_code: version_code
          )
          { ok: true }
        rescue StandardError => e
          UI.error("Failed to promote #{app} (#{version_code}): #{e.message}")
          { ok: false, error: e.message }
        end
      [app, result]
    end
  end

  # Promotes the existing beta build to production — the scripted equivalent of the Play Console
  # "Promote" button. `track_promote_to` extends the already-reviewed beta release to the production
  # track (same versionCode, no new binary): supply reuses that beta TrackRelease, so its release
  # notes and name carry over automatically, and its version codes (incl. the retained legacy code)
  # come along too.
  def promote_version_code_to_production(package_name:, version_code:)
    upload_to_play_store(
      package_name: package_name,
      json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY,
      track: BETA_TRACK,
      track_promote_to: PRODUCTION_TRACK,
      version_code: Integer(version_code),
      # Promote as a live staged rollout: a `rollout` in (0, 1) makes supply set the promoted
      # release to `inProgress` at that user fraction. Starting tiny (0.1%) exercises the full
      # production flow end to end; `advance_production_rollout` grows it from there.
      rollout: '0.001',
      # Promotion touches only the track release, never a binary — skip every upload path.
      skip_upload_apk: true,
      skip_upload_aab: true,
      skip_upload_metadata: true,
      skip_upload_changelogs: true,
      skip_upload_images: true,
      skip_upload_screenshots: true
    )
  end

  #################################################
  # Production rollout growth
  #################################################

  # Reads the in-flight production release for one app — the release still rolling out (`inProgress`,
  # `halted`, or `draft`), or nil when the track is in steady state (only a `completed` release, or
  # none). Opens a throwaway Play edit and aborts it, so this only reads. Returns
  # `{ status:, user_fraction:, version_code: }`.
  def current_production_rollout(package_name:)
    require 'supply'
    require 'supply/options'

    Supply.config = FastlaneCore::Configuration.create(
      Supply::Options.available_options,
      { json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY, package_name: package_name, track: PRODUCTION_TRACK }
    )

    release = with_play_edit_retries("Reading the production rollout for #{package_name}") do
      client = Supply::Client.make_from_config
      client.begin_edit(package_name: package_name)
      begin
        in_flight_release(client.tracks(PRODUCTION_TRACK).first)
      ensure
        # Always discard the throwaway edit, even if the read raises.
        client.abort_current_edit
      end
    end
    return nil if release.nil?

    {
      status: release.status,
      user_fraction: release.user_fraction,
      # The real code is the largest; `.max` ignores the pinned legacy code kept on the release.
      version_code: Array(release.version_codes).map(&:to_i).max
    }
  rescue StandardError => e
    # Raise rather than return nil: an errored read must not read as "nothing rolling out".
    UI.user_error!("Unable to read the production rollout for #{package_name}: #{e.message}")
  end

  # The release still rolling out on a track — preferring `inProgress`, then a paused `halted`, then a
  # `draft`. nil when the track has only a `completed` release (steady state) or no releases.
  def in_flight_release(track)
    releases = track&.releases || []
    [ROLLOUT_STATUS_IN_PROGRESS, ROLLOUT_STATUS_HALTED, ROLLOUT_STATUS_DRAFT]
      .filter_map { |status| releases.find { |release| release.status == status } }
      .first
  end

  # A comparable signature of an app's rollout state, so WordPress and Jetpack can be checked for
  # lockstep. nil (no in-flight release) collapses to `:steady`; the fraction is rounded so a float
  # round-tripping through JSON can't spuriously fail the comparison.
  def rollout_signature(state)
    return :steady if state.nil?

    [state[:status], state[:version_code], state[:user_fraction]&.round(4)]
  end

  # The next rollout target from the current fraction: the smallest ladder step strictly above it as an
  # `inProgress` bump, or a finalize to 100% (`completed`, no `user_fraction`) once past the top step.
  def next_production_rollout_target(current_fraction:)
    next_step = PRODUCTION_ROLLOUT_STEPS.find { |step| step > current_fraction }
    return { status: ROLLOUT_STATUS_IN_PROGRESS, user_fraction: next_step } if next_step

    { status: ROLLOUT_STATUS_COMPLETED, user_fraction: nil }
  end

  # A readable percentage label for a rollout target, for logs and Slack (e.g. `5%`, `100% (complete)`).
  def rollout_target_label(target)
    return '100% (complete)' if target[:status] == ROLLOUT_STATUS_COMPLETED

    percent = (target[:user_fraction] * 100).round(2)
    percent = percent.to_i if percent == percent.to_i
    "#{percent}%"
  end

  # Applies `target` to each app's in-flight production release, returning a per-app `{ ok:, error: }`
  # result. A failure for one app doesn't stop the other (mirrors distribute_to_production).
  def distribute_rollout_advance(target:)
    %i[wordpress jetpack].to_h do |app|
      result =
        begin
          set_production_rollout(package_name: APP_SPECIFIC_VALUES[app][:package_name], target: target)
          { ok: true }
        rescue StandardError => e
          UI.error("Failed to advance #{app} rollout: #{e.message}")
          { ok: false, error: e.message }
        end
      [app, result]
    end
  end

  # Applies `target` to the app's in-flight production release via the Play API. Re-reads the track
  # inside the edit and mutates the `inProgress` release: a bump only raises `user_fraction`, leaving
  # the rest of the track (the previous version still serving the rollout remainder) intact; a finalize
  # replaces the releases with the single `completed` one, which supersedes the previous version. Bails
  # without committing if the rollout is no longer `inProgress` (e.g. paused between read and write) —
  # never resurrecting a paused rollout.
  def set_production_rollout(package_name:, target:)
    require 'supply'
    require 'supply/options'

    Supply.config = FastlaneCore::Configuration.create(
      Supply::Options.available_options,
      { json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY, package_name: package_name, track: PRODUCTION_TRACK }
    )

    with_play_edit_retries("Advancing #{package_name} production rollout") do
      client = Supply::Client.make_from_config
      client.begin_edit(package_name: package_name)

      committed = false
      begin
        track = client.tracks(PRODUCTION_TRACK).first
        release = (track&.releases || []).find { |candidate| candidate.status == ROLLOUT_STATUS_IN_PROGRESS }
        UI.user_error!("#{package_name} has no in-progress production rollout to advance.") if release.nil?

        if target[:status] == ROLLOUT_STATUS_COMPLETED
          # Finalize to 100%: a single `completed` release supersedes the previous version.
          release.status = ROLLOUT_STATUS_COMPLETED
          release.user_fraction = nil
          track.releases = [release]
        else
          # Bump: only raise the fraction; leave the rest of the track (the remainder) intact.
          release.user_fraction = target[:user_fraction]
        end

        client.update_track(PRODUCTION_TRACK, track)
        client.commit_current_edit!
        committed = true
      ensure
        # Discard the edit if we bailed before committing (a committed edit can't be aborted).
        client.abort_current_edit unless committed
      end
    end
  end

  # Posts the per-app outcome of a rollout advance.
  def post_rollout_result_to_slack(version_code:, target:, results:)
    label = rollout_target_label(target)
    status_lines = results.map do |app, result|
      next "• #{app}: :x: #{result[:error]}" unless result[:ok]

      "• #{app}: :white_check_mark: rollout at #{label}"
    end

    all_ok = results.values.all? { |result| result[:ok] }
    completed = target[:status] == ROLLOUT_STATUS_COMPLETED
    header =
      if all_ok && completed
        ':checkered_flag: *Production rollout complete — now at 100%*'
      elsif all_ok
        ":chart_with_upwards_trend: *Production rollout advanced to #{label}*"
      else
        ':warning: *Production rollout advance finished with errors*'
      end

    notify_slack(
      <<~MSG
        #{header} — `#{version_code}`

        #{status_lines.join("\n")}
      MSG
    )
  end

  #################################################
  # Promoted-release finalize
  #################################################

  # The marketing version (e.g. `26.9`) encoded in a continuous build's versionCode — `major.minor`,
  # read straight from the packed `major*10 + minor` prefix. Hotfix patch numbers aren't encoded, but
  # hotfixes don't flow through this trunk-only path, so `major.minor` is exact here.
  def marketing_version_for(version_code:)
    prefix = version_code / VERSION_CODE_BUILD_MODULO
    "#{prefix / 10}.#{prefix % 10}"
  end

  # Creates a DRAFT GitHub release for the released marketing version, tagged at the commit the build
  # came from. It stays a draft for now — a developer publishes it from the GitHub UI until the whole
  # flow is switched to publishing outright (mirrors the draft Play release). GitHub creates the tag
  # from `target` when the draft is published. The Play-signed universal APKs are attached
  # best-effort. Reuses an existing release for the same commit rather than creating a second draft on
  # a re-run. Returns the release URL.
  def create_draft_github_release(version_code:, version_name:)
    build_number = version_code % VERSION_CODE_BUILD_MODULO
    sha = commit_sha_for_build_number(build_number: build_number)

    existing_url = existing_github_release_url(commit_sha: sha)
    if existing_url
      UI.important("A GitHub release already targets #{sha} (#{existing_url}); skipping creation.")
      return existing_url
    end

    create_github_release(
      repository: GITHUB_REPO,
      version: version_name,
      target: sha,
      release_assets: signed_universal_apks(version_code: version_code, version_name: version_name),
      prerelease: false,
      is_draft: true
    )
  end

  # The URL of an existing GitHub release targeting the given commit, or nil — so a re-run reuses the
  # release for this exact build instead of creating a second draft. Matches on the commit (the build's
  # identity) rather than the marketing version, which many builds share. `client.releases` includes
  # drafts (whose `target_commitish` is the commit we set).
  def existing_github_release_url(commit_sha:)
    github = Fastlane::Helper::GithubHelper.new(github_token: get_required_env('GITHUB_TOKEN'))
    release = github.client.releases(GITHUB_REPO).find { |candidate| candidate.target_commitish == commit_sha }
    release&.html_url
  end

  # Resolves the trunk commit a continuous build ran against, via the Buildkite build whose number is
  # packed into the versionCode. Raises rather than returns nil — the GitHub release needs a target.
  def commit_sha_for_build_number(build_number:)
    org = ENV.fetch('BUILDKITE_ORGANIZATION_SLUG', BUILDKITE_ORGANIZATION)
    pipeline = ENV.fetch('BUILDKITE_PIPELINE_SLUG', BUILDKITE_PIPELINE)
    uri = URI("https://api.buildkite.com/v2/organizations/#{org}/pipelines/#{pipeline}/builds/#{build_number}")

    response = buildkite_api_get(uri)
    UI.user_error!("Unable to look up Buildkite build ##{build_number} (HTTP #{response.code}).") unless response.is_a?(Net::HTTPSuccess)

    build = JSON.parse(response.body)
    sha = build['commit']
    UI.user_error!("Buildkite build ##{build_number} has no commit SHA.") if sha.nil? || sha.to_s.empty?

    UI.message("Build ##{build_number} → #{sha} (branch #{build['branch'].inspect})")
    sha
  end

  # Downloads the Play-signed universal APK for each app at the given version code, best-effort. The
  # original AAB was built in a past CI job and is long gone, but Play still serves a generated
  # universal APK by version code. A download failure just yields fewer/no assets rather than aborting
  # the release. Returns the paths that downloaded.
  def signed_universal_apks(version_code:, version_name:)
    %i[wordpress jetpack].filter_map do |app|
      destination = signed_apk_path(app.to_s, version_name)
      download_universal_apk_from_google_play(
        package_name: APP_SPECIFIC_VALUES[app][:package_name],
        version_code: version_code,
        destination: destination,
        json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
      )
      File.exist?(destination) ? destination : nil
    rescue StandardError => e
      UI.important("Could not download the #{app} universal APK for #{version_code}: #{e.message}; the release will omit it.")
      nil
    end
  end

  # Opens (or reuses) a PR bumping trunk's marketing version to the next line after the released one —
  # e.g. after releasing `26.9`, trunk moves to `27.0` so later builds carry the new line. No-ops when
  # trunk is already at or past that line (a re-run, or a release of an older line). Returns the PR
  # URL, or nil when skipped.
  def open_version_bump_pull_request(released_version:)
    next_version = VERSION_FORMATTER.release_version(
      VERSION_CALCULATOR.next_release_version(version: VERSION_FORMATTER.parse(released_version))
    )

    UI.user_error!("Could not check out and pull #{DEFAULT_BRANCH}.") unless Fastlane::Helper::GitHelper.checkout_and_pull(DEFAULT_BRANCH)

    if version_at_or_past?(current_version_name, next_version)
      UI.important("Trunk is already at #{current_version_name}; skipping the bump to #{next_version}.")
      return nil
    end

    branch = "release/version-bump-#{next_version}"
    Fastlane::Helper::GitHelper.delete_local_branch_if_exists!(branch)
    Fastlane::Helper::GitHelper.create_branch(branch, from: DEFAULT_BRANCH)

    # Only the marketing name advances; the (legacy, unused-on-trunk) version code is written back as-is.
    VERSION_FILE.write_version(version_name: next_version, version_code: current_build_code)
    Fastlane::Helper::GitHelper.commit(message: "Bump version name to #{next_version}", files: VERSION_PROPERTIES_PATH)

    push_to_git_remote(remote_branch: branch, tags: false, force: true, set_upstream: true)

    find_or_create_pull_request(
      repository: GITHUB_REPO,
      title: "Bump version name to #{next_version}",
      body: "Advances the marketing version on `#{DEFAULT_BRANCH}` to `#{next_version}`, following the `#{released_version}` production release.",
      head: branch,
      base: DEFAULT_BRANCH,
      labels: ['Releases']
    )
  end

  # Whether `current_name` is at or past `target_name`, compared on `major.minor` (Array#<=> is
  # element-wise; Array has no `>=`, so compare the spaceship result).
  def version_at_or_past?(current_name, target_name)
    current = VERSION_FORMATTER.parse(current_name)
    target = VERSION_FORMATTER.parse(target_name)
    ([current.major, current.minor] <=> [target.major, target.minor]) >= 0
  end

  #################################################
  # Buildkite block step
  #################################################

  def write_beta_promotion_steps_file(candidates:)
    options = candidates.map { |code| { 'label' => code.to_s, 'value' => code.to_s } }

    steps = {
      'steps' => [
        {
          'block' => BETA_BLOCK_LABEL,
          'key' => BETA_BLOCK_STEP_KEY,
          'prompt' => 'Choose the build to release to beta testers. This promotes the matching WordPress and Jetpack builds.',
          # Keep the build "running" (not green) while it waits for a human.
          'blocked_state' => 'running',
          'fields' => beta_promotion_block_fields(options: options)
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

  # The block step's input fields: pick a build, and pick the release note to publish with it.
  def beta_promotion_block_fields(options:)
    [
      {
        'select' => 'Build to promote',
        'key' => BETA_META_DATA_KEY,
        # Required, no default: an un-actioned unblock can't silently promote a build.
        'required' => true,
        'options' => options
      },
      {
        'select' => 'Release notes',
        'key' => BETA_RELEASE_NOTES_META_DATA_KEY,
        'hint' => 'Which "what\'s new" text to publish. Carries through to production.',
        # Required, no default: the notes are a deliberate choice, not a silent fallback.
        'required' => true,
        'options' => STATIC_RELEASE_NOTE_OPTIONS.map { |o| { 'label' => o[:label], 'value' => o[:key] } }
      }
    ]
  end

  # Writes the confirm → promote → finalize steps. There's no picker — the candidate is baked into the
  # commands; the block step only gates on a Yes/No confirmation.
  def write_production_release_steps_file(version_code:)
    steps = {
      'steps' => [
        production_confirm_block_step(version_code: version_code),
        promote_to_production_step(version_code: version_code),
        # Barrier: finalize only after the promote step succeeds — a failed promote halts the build
        # here. On a "no" confirmation the promote step no-ops and finalize's own guard no-ops too.
        'wait',
        finalize_promoted_release_step(version_code: version_code)
      ]
    }

    FileUtils.mkdir_p(File.dirname(PROMOTION_STEPS_FILE))
    # `line_width: -1` keeps each label on one line (no YAML folding).
    File.write(PROMOTION_STEPS_FILE, steps.to_yaml(line_width: -1))
    UI.message("Wrote production release steps for build #{version_code} to #{PROMOTION_STEPS_FILE}")
  end

  # The Yes/No confirmation block step that gates the production release.
  def production_confirm_block_step(version_code:)
    {
      'block' => PRODUCTION_BLOCK_LABEL,
      'key' => PRODUCTION_BLOCK_STEP_KEY,
      'prompt' => "Release build #{version_code} to production? This releases the matching WordPress and Jetpack builds.",
      # Keep the build "running" (not green) while it waits for a human.
      'blocked_state' => 'running',
      'fields' => [
        {
          'select' => 'Release to production?',
          'key' => PRODUCTION_CONFIRM_META_DATA_KEY,
          # Required, no default: an un-actioned unblock can't silently release a build.
          'required' => true,
          'options' => [
            { 'label' => 'Yes', 'value' => 'yes' },
            { 'label' => 'No', 'value' => 'no' }
          ]
        }
      ]
    }
  end

  # The command step that promotes the confirmed build to production (WordPress + Jetpack).
  def promote_to_production_step(version_code:)
    {
      'label' => ':rocket: Release build to production',
      # The candidate rides along as an argument; the confirmation Yes/No comes from meta-data.
      'command' => ".buildkite/commands/promote-to-production.sh #{version_code}",
      'plugins' => [CI_TOOLKIT_PLUGIN_REF],
      'agents' => { 'queue' => 'android' }
    }
  end

  # The command step that finalizes the release (draft GitHub release + trunk version-bump PR). Runs
  # on mac-metal for the git push identity.
  def finalize_promoted_release_step(version_code:)
    {
      'label' => ':android: Finalize promoted release',
      'command' => ".buildkite/commands/finalize-promoted-release.sh #{version_code}",
      'plugins' => [CI_TOOLKIT_PLUGIN_REF],
      'agents' => { 'queue' => 'mac-metal' }
    }
  end

  # Source `shared-pipeline-vars` first so `$CI_TOOLKIT` is interpolated.
  def upload_promotion_steps
    sh('bash', '-c', "cd '#{PROJECT_ROOT_FOLDER}' && source .buildkite/shared-pipeline-vars && buildkite-agent pipeline upload '#{PROMOTION_STEPS_RELATIVE_PATH}'")
  end

  #################################################
  # Slack
  #################################################

  # Posts the candidate list, linking to the unblock dialog when the job id resolved, otherwise to
  # the build so a degraded link isn't mistaken for the picker.
  def post_beta_candidates_to_slack(candidates:, pick_url: nil)
    build_url = ENV.fetch('BUILDKITE_BUILD_URL', nil)

    candidate_lines = candidates.map { |code| "• `#{code}`" }

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

  # Posts the per-app outcome of a beta promotion.
  def post_beta_result_to_slack(version_code:, results:)
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

  # Posts the candidate, linking to the unblock dialog when the job id resolved, otherwise to the
  # build so a degraded link isn't mistaken for the confirmation.
  def post_production_candidate_to_slack(version_code:, confirm_url: nil)
    build_url = ENV.fetch('BUILDKITE_BUILD_URL', nil)

    confirm_line =
      if confirm_url
        "\n\n:point_right: <#{confirm_url}|Confirm the production release>"
      elsif build_url
        "\n\n:warning: Couldn't deep-link to the confirmation — open <#{build_url}|the build> and unblock the release step."
      else
        ''
      end

    notify_slack(
      <<~MSG
        :android: *Production release* — confirm releasing build `#{version_code}` to production (WordPress + Jetpack).#{confirm_line}
      MSG
    )
  end

  # Posts the per-app outcome of a production release.
  def post_production_result_to_slack(version_code:, results:)
    status_lines = results.map do |app, result|
      next "• #{app}: :x: #{result[:error]}" unless result[:ok]

      "• #{app}: :white_check_mark: draft created on production"
    end

    all_ok = results.values.all? { |result| result[:ok] }
    header = all_ok ? ':rocket: *Production draft created*' : ':warning: *Production release finished with errors*'

    notify_slack(
      <<~MSG
        #{header} — `#{version_code}`

        #{status_lines.join("\n")}
      MSG
    )
  end

  #################################################
  # Buildkite deep link
  #################################################

  # The block step's unblock-dialog URL, or nil (callers fall back to the build URL).
  def promotion_unblock_dialog_url(block_step_key:, block_label:)
    org = ENV.fetch('BUILDKITE_ORGANIZATION_SLUG', BUILDKITE_ORGANIZATION)
    pipeline = ENV.fetch('BUILDKITE_PIPELINE_SLUG', BUILDKITE_PIPELINE)
    build_number = ENV.fetch('BUILDKITE_BUILD_NUMBER', nil)
    return nil if build_number.nil?

    job_id = block_step_job_id(
      org: org, pipeline: pipeline, build_number: build_number,
      block_step_key: block_step_key, block_label: block_label
    )
    return nil if job_id.nil?

    "https://buildkite.com/organizations/#{org}/pipelines/#{pipeline}/builds/#{build_number}/jobs/#{job_id}/unblock_dialog"
  end

  # Polls the build for the just-uploaded block step's job id (it takes a moment to register).
  def block_step_job_id(org:, pipeline:, build_number:, block_step_key:, block_label:)
    uri = URI("https://api.buildkite.com/v2/organizations/#{org}/pipelines/#{pipeline}/builds/#{build_number}")

    5.times do |attempt|
      job = find_promotion_block_job(uri, block_step_key: block_step_key, block_label: block_label)
      return job['id'] if job

      sleep(2) unless attempt == 4
    end

    UI.important('Could not resolve the promotion block step job id; Slack will link to the build instead.')
    nil
  rescue StandardError => e
    UI.important("Error resolving the block step job id (#{e.message}); Slack will link to the build instead.")
    nil
  end

  def find_promotion_block_job(uri, block_step_key:, block_label:)
    response = buildkite_api_get(uri)
    return nil unless response.is_a?(Net::HTTPSuccess)

    manual_jobs = JSON.parse(response.body).fetch('jobs', []).select { |job| job['type'] == 'manual' }

    # Prefer the stable step key; fall back to the label only if the API doesn't surface it.
    manual_jobs.find { |job| job['step_key'] == block_step_key } ||
      manual_jobs.find { |job| job['label'].to_s.include?(block_label) }
  end

  def buildkite_api_get(uri)
    request = Net::HTTP::Get.new(uri)
    # `BUILDKITE_TOKEN` needs the `read_builds` scope. It's only used for the Slack deep link, so a
    # missing token just degrades to linking the build (see `block_step_job_id`'s rescue).
    request['Authorization'] = "Bearer #{get_required_env('BUILDKITE_TOKEN')}"
    # Bound the request so a hung response can't stall the gather lane across all 5 poll attempts.
    Net::HTTP.start(uri.hostname, uri.port, use_ssl: true, open_timeout: 30, read_timeout: 30) do |http|
      http.request(request)
    end
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
end
