require 'fastlane/action'
require_relative '../helpers/android_promo_screenshot_helper.rb'

module Fastlane
  module Actions
    class AndroidPromoScreenshotAction < Action
      def self.run(params)
        UI.message "Origin Folder: #{params[:orig_folder]}"
        UI.message "Target Folder: #{params[:output_folder]}"
        UI.message "Locales: #{params[:locales].inspect}"
        UI.message "Configuration: #{params[:device_config].inspect}"
        UI.message "Default locale: #{params[:default_locale]}"
        UI.message "Metadata folder path: #{params[:metadata_folder]}"

        Fastlane::Helpers::AndroidPromoScreenshots::require_font()
        screenshot_gen = Fastlane::Helpers::AndroidPromoScreenshots.new(params[:locales],
          params[:default_locale],
          params[:orig_folder],
          params[:output_folder],
          params[:metadata_folder])

        device_config = params[:device_config]
        device_config.each do | device |
          screenshot_gen.generate_device(device)
        end
      end

      def self.description
        "Generate promo screenshots"
      end

      def self.authors
        ["Lorenzo Mattei"]
      end

      def self.return_value
        # If your method provides a return value, you can describe here what it does
      end

      def self.details
        # Optional:
        "Creates promo screenshots starting from standard ones"
      end

      def self.available_options
        [
          FastlaneCore::ConfigItem.new(key: :orig_folder,
                                  env_name: "PROMOSS_ORIG",
                                description: "The path of the original screenshots",
                                  optional: false,
                                  is_string: true),
          FastlaneCore::ConfigItem.new(key: :output_folder,
                                        env_name: "PROMOSS_OUTPUT",
                                    description: "The path of the folder to save the promo screenshots",
                                        optional: false,
                                      is_string: true),
          FastlaneCore::ConfigItem.new(key: :locales,
                                        env_name: "PROMOSS_LOCALES",
                                    description: "The list of locales to generate",
                                        optional: false,
                                      is_string: false),
          FastlaneCore::ConfigItem.new(key: :device_config,
                                        env_name: "PROMOSS_DEVICE_CONFIGHASH",
                                    description: "A hash with the configuration data",
                                        optional: false,
                                      is_string: false),
          FastlaneCore::ConfigItem.new(key: :default_locale,
                                        env_name: "PROMOSS_DEFAULT_LOCALE",
                                    description: "The default locale to use in case of missing translations",
                                        optional: false,
                                      is_string: true),
          FastlaneCore::ConfigItem.new(key: :metadata_folder,
                                        env_name: "PROMOSS_METADATA_FOLDER",
                                    description: "The default locale to use in case of missing translations",
                                        optional: false,
                                      is_string: true),
        ]
      end

      def self.is_supported?(platform)
        true
      end
    end
  end
end
