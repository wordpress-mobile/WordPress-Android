# frozen_string_literal: true

source 'https://rubygems.org'

# TEMPORARY: point at the dangermattic branch to validate the new string-key guardrail
# (https://github.com/Automattic/dangermattic/pull/126).
# Revert to the released `~> 1.2` constraint once that PR ships in a tagged release.
gem 'danger-dangermattic', git: 'https://github.com/Automattic/dangermattic.git', branch: 'add/strings-immutability-check'
gem 'fastlane', '~> 2'

### Fastlane Plugins

gem 'fastlane-plugin-firebase_app_distribution', '~> 1.0'
gem 'fastlane-plugin-sentry'
gem 'fastlane-plugin-wpmreleasetoolkit', '~> 14.7'
# gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: ''

### Gems needed only for generating Promo Screenshots
group :screenshots, optional: true do
  gem 'rmagick', '~> 7.0'
end

# Security: https://github.com/lostisland/faraday/pull/1665
# Faraday 2.0 is not compatible with Fastlane
gem 'faraday', '~> 1.10', '>= 1.10.5'
