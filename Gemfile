# frozen_string_literal: true

source "https://rubygems.org" do
  gem 'fastlane', "2.146.1"
  gem 'nokogiri'
end

plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)
