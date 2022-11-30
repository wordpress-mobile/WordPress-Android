# frozen_string_literal: true

source 'https://rubygems.org'

gem 'fastlane', '~> 2'
gem 'nokogiri'

### Fastlane Plugins

# gem 'fastlane-plugin-wpmreleasetoolkit', '~> 6.0'
#gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: 'firebase-test-return-url'


### Gems needed only for generating Promo Screenshots
group :screenshots, optional: true do
    gem 'rmagick', '~> 4.1'
end
