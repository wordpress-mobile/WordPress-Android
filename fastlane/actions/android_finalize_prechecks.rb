module Fastlane
  module Actions
    module SharedValues
      ANDROID_FINALIZE_PRECHECKS_CUSTOM_VALUE = :ANDROID_FINALIZE_PRECHECKS_CUSTOM_VALUE
    end

    class AndroidFinalizePrechecksAction < Action
      def self.run(params)
        UI.message "Skip confirm: #{params[:skip_confirm]}"
        
        require_relative '../helpers/android_version_helper.rb'
        require_relative '../helpers/android_git_helper.rb'

        UI.user_error!("This is not a release branch. Abort.") unless other_action.git_branch.start_with?("release/")

        version = Fastlane::Helpers::AndroidVersionHelper::get_version_name
        message = "Finalizing release: #{version}\n"
        if (!params[:skip_confirm])
          if (!UI.confirm("#{message}Do you want to continue?"))
            UI.user_error!("Aborted by user request")
          end
        else 
          UI.message(message)
        end

        # Check local repo status
        other_action.ensure_git_status_clean()

        version
      end

      #####################################################
      # @!group Documentation
      #####################################################

      def self.description
        "Runs some prechecks before finalizing a release"
      end

      def self.details
        "Runs some prechecks before finalizing a release"
      end

      def self.available_options
        # Define all options your action supports. 
        
        # Below a few examples
        [
          FastlaneCore::ConfigItem.new(key: :skip_confirm,
                                       env_name: "FL_ANDROID_FINALIZE_PRECHECKS_SKIPCONFIRM",
                                       description: "Skips confirmation",
                                       is_string: false, # true: verifies the input is a string, false: every kind of value
                                       default_value: false) # the default value if the user didn't provide one
        ]
      end

      def self.output
         
      end

      def self.return_value
        "The current app version"
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
