# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', '~> 1.2'
gem 'fastlane', '~> 2'
gem 'nokogiri'

### Fastlane Plugins

gem 'fastlane-plugin-firebase_app_distribution', '~> 0.10'
gem 'fastlane-plugin-sentry'
gem 'fastlane-plugin-wpmreleasetoolkit', '~> 14.0'
# gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: ''

### Gems needed only for generating Promo Screenshots
group :screenshots, optional: true do
  gem 'rmagick', '~> 4.1'
end

# Security: https://github.com/lostisland/faraday/pull/1665
# Faraday 2.0 is not compatible with Fastlane
gem 'faraday', '~> 1.10', '>= 1.10.5'
