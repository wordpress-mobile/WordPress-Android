# frozen_string_literal: true

source "https://rubygems.org" do 
  gem 'danger'
  gem 'fastlane', "2.107.0"
end

plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)
