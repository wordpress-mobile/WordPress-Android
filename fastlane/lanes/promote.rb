# frozen_string_literal: true

platform :android do
  # Fetches the current `beta` track version codes for WordPress and Jetpack and logs them.
  #
  # This is the first step of the beta promotion picker: the highest code already on `beta` is the
  # floor that a promotable `internal` build has to exceed. For now the lane only reads and logs, so
  # we can confirm Play API access and see the shape of the data before building the rest of the flow.
  #
  # Usage:
  #   bundle exec fastlane gather_beta_candidates
  desc 'Fetch and log the current beta track version codes for WordPress and Jetpack'
  lane :gather_beta_candidates do
    floors = %i[wordpress jetpack].map do |app|
      package_name = APP_SPECIFIC_VALUES[app][:package_name]
      codes = beta_track_version_codes(package_name: package_name)

      UI.message("#{app} (#{package_name}): beta track version codes = #{codes.inspect}")
      floor = codes.max
      UI.message("#{app}: beta floor = #{floor.inspect}")
      floor
    end

    combined_floor = floors.compact.max
    UI.success("Combined beta floor (max across both apps) = #{combined_floor.inspect}")
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
end
