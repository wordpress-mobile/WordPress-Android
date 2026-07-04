# frozen_string_literal: true

platform :android do
  # Gathers the internal builds that can be promoted to the `beta` track.
  #
  # Builds three things and logs each so we can confirm the Play API access and the data shape
  # before wiring up the picker:
  #   1. the beta floor — the highest version code already on each app's `beta` track,
  #   2. the pool — every AAB version code uploaded for each app (the Play "app bundle explorer"),
  #   3. the candidates — codes present for both apps and above the floor, newest first.
  #
  # Read-only for now: it doesn't open the picker or promote anything.
  #
  # Usage:
  #   bundle exec fastlane gather_beta_candidates
  desc 'Gather and log promotable beta candidates (beta floor + available AABs)'
  lane :gather_beta_candidates do
    apps = %i[wordpress jetpack]

    # 1. The floor: the highest version code already on each app's beta track.
    combined_floor = apps.filter_map do |app|
      codes = beta_track_version_codes(package_name: APP_SPECIFIC_VALUES[app][:package_name])
      UI.message("#{app}: beta track version codes = #{codes.sort.inspect}")
      codes.max
    end.max
    UI.message("Combined beta floor (max across both apps) = #{combined_floor.inspect}")

    # 2. The pool: every AAB version code uploaded for each app.
    available_per_app = apps.map do |app|
      codes = available_aab_version_codes(package_name: APP_SPECIFIC_VALUES[app][:package_name])
      UI.message("#{app}: available AAB version codes = #{codes.sort.inspect}")
      codes
    end

    # 3. Promotable candidates: present for both apps and above the beta floor, newest first.
    common = available_per_app.reduce(:&) || []
    candidates = common.select { |code| combined_floor.nil? || code > combined_floor }.sort.reverse
    UI.success("Promotable candidates (in both apps, > #{combined_floor.inspect}) = #{candidates.inspect}")
  end

  # Reads the version codes currently on the given package's `beta` track, as an array of integers.
  # Returns an empty array if the track has no releases or the lookup fails.
  def beta_track_version_codes(package_name:)
    google_play_track_version_codes(
      package_name: package_name,
      track: 'beta',
      json_key: UPLOAD_TO_PLAY_STORE_JSON_KEY
    )
  rescue StandardError => e
    UI.error("Failed to fetch beta track version codes for #{package_name}: #{e.message}")
    []
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
    codes = client.aab_version_codes
    client.abort_current_edit
    Array(codes).compact.map(&:to_i)
  rescue StandardError => e
    UI.error("Failed to list available AAB version codes for #{package_name}: #{e.message}")
    []
  end
end
