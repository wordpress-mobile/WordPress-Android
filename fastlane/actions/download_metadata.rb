module Fastlane
  module Actions
    module SharedValues
      DOWNLOAD_METADATA_CUSTOM_VALUE = :DOWNLOAD_METADATA_CUSTOM_VALUE
    end

    class DownloadMetadataAction < Action
      def self.run(params)
        # fastlane will take care of reading in the parameter and fetching the environment variable:
        UI.message "Project URL: #{params[:project_url]}"
        UI.message "Locales: #{params[:locales].inspect}"
        
        # Init
        require_relative '../helpers/metadata_download_helper.rb'

        # Download
        downloader = Fastlane::Helpers::MetadataDownloader.new(Dir.pwd + "/fastlane/metadata/android", params[:target_files])

        params[:locales].each do | loc |
          puts "Downloading language: #{loc[:glotpress]}"
          complete_url = "#{params[:project_url]}#{loc[:glotpress]}/default/export-translations?filters[status]=current&format=json"
          downloader.download(loc[:google_play], complete_url)
        end
      end

      #####################################################
      # @!group Documentation
      #####################################################

      def self.description
        "Download translated metadata"
      end

      def self.details
        "Downloads tranlated metadata from GlotPress and updates local files"
      end

      def self.available_options
        # Define all options your action supports. 
        
        # Below a few examples
        [
          FastlaneCore::ConfigItem.new(key: :project_url,
                                       env_name: "FL_DOWNLOAD_METADATA_PROJECT_URL", # The name of the environment variable
                                       description: "GlotPress project URL"),
          FastlaneCore::ConfigItem.new(key: :target_files,
                                        env_name: "FL_DOWNLOAD_METADATA_TARGET_FILES",
                                        description: "The hash with the path to the target files and the key to use to extract their content",
                                        is_string: false),
          FastlaneCore::ConfigItem.new(key: :locales,
                                          env_name: "FL_DOWNLOAD_METADATA_LOCALES",
                                          description: "The hash with the GLotPress locale and the project locale association",
                                          is_string: false)
        ]
      end

      def self.output
        
      end

      def self.return_value
        # If your method provides a return value, you can describe here what it does
      end

      def self.authors
        ["loremattei"]
      end

      def self.is_supported?(platform)
        [:ios, :android].include?(platform)
      end
    end
  end
end
