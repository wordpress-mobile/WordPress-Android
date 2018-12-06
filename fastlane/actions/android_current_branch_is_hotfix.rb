module Fastlane
  module Actions
    module SharedValues
      ANDROID_CURRENT_BRANCH_IS_HOTFIX_CUSTOM_VALUE = :ANDROID_CURRENT_BRANCH_IS_HOTFIX_CUSTOM_VALUE
    end

    class AndroidCurrentBranchIsHotfixAction < Action
      def self.run(params)
        require_relative '../helpers/android_version_helper.rb'
        Fastlane::Helpers::AndroidVersionHelper::is_hotfix(Fastlane::Helpers::AndroidVersionHelper::get_version_name)
      end

      #####################################################
      # @!group Documentation
      #####################################################

      def self.description
        "Checks if the current branch is for a hotfix"
      end

      def self.details
        "Checks if the current branch is for a hotfix"
      end

      def self.available_options
        
      end

      def self.output
        
      end

      def self.return_value
        "True if the branch is for a hotfix, false otherwise"
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
