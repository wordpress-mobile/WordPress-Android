require 'find'
require 'pathname'
require 'fileutils'

module Fastlane
  module Actions
    class AndroidPromoScreenshotInitAction < Action
      def self.run(params)
        UI.message "Source folder: #{params[:orig_folder]}"
        UI.message "Target folder: #{params[:target_folder]}"
        

        Dir.mkdir(params[:target_folder]) unless File.exists?(params[:target_folder])

        png_file_paths = []
        Find.find(params[:orig_folder]) do | path |
          png_file_paths << path if path =~ /.*\.png$/
        end

        orig_path = Pathname.new(params[:orig_folder])
        png_file_paths.each do | path |
          target_file_path = File.join(params[:target_folder], Pathname.new(path).relative_path_from(orig_path).to_s)
          FileUtils.mkdir_p(File.dirname(target_file_path))
          FileUtils.cp(path, target_file_path)
        end
      end

      #####################################################
      # @!group Documentation
      #####################################################

      def self.description
        "Init a source repository for the orig screenshots"
      end

      def self.details
        "Init a source repository for the orig screenshots"
      end

      def self.available_options
        [
          FastlaneCore::ConfigItem.new(key: :orig_folder,
                                       env_name: "FL_ANDROID_PROMO_SCREENSHOT_INIT_ORIG_FOLDER", # The name of the environment variable
                                       description: "The folder that contains the screenshots", # a short description of this parameter
                                       is_string: true),
          FastlaneCore::ConfigItem.new(key: :target_folder,
                                       env_name: "FL_ANDROID_PROMO_SCREENSHOT_INIT_TARGET_FOLDER",
                                       description: "The new repository",
                                       is_string: true) 
        ]
      end

      def self.output
        
      end

      def self.return_value
        
      end

      def self.authors
        ["loremattei"]
      end

      def self.is_supported?(platform)
        platform == :android
      end
    end
  end
end
