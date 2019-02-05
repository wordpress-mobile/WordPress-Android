module Fastlane
  module Actions
    module SharedValues
    end

    class AndroidCodefreezeAction < Action
      def self.run(params)
        # fastlane will take care of reading in the parameter and fetching the environment variable:
        UI.message "Skip confirm on code freeze: #{params[:skip_confirm]}"
        UI.message "Code freezing release: #{params[:codefreeze_version]}"
        UI.message "Update release branch version: #{params[:update_release_branch_version]}"

        require_relative '../helpers/android_version_helper.rb'
        require_relative '../helpers/android_git_helper.rb'

        # Checkout develop and update
        Fastlane::Helpers::AndroidGitHelper.git_checkout_and_pull("develop")

        # Get current versions
        current_version = Fastlane::Helpers::AndroidVersionHelper.get_version_name
        current_build_version_code = Fastlane::Helpers::AndroidVersionHelper.get_build_version_code.to_i()

        # Check is an alpha version
        if (!Fastlane::Helpers::AndroidVersionHelper.is_alpha_version(current_version))
          UI.user_error!("Develop branch is not on an Alpha version. Can't continue with the standard flow.")
        end

        # Check the release branch
        current_release_version = Fastlane::Helpers::AndroidVersionHelper.calc_prev_release_version(params[:codefreeze_version])
        current_release_version_code = current_build_version_code
        Fastlane::Helpers::AndroidGitHelper.git_checkout_and_pull("release/#{current_release_version}") unless !params[:update_release_branch_version] 
        current_release_version_code = current_release_version_code + 1 unless !params[:update_release_branch_version] 

        # Create new versions
        new_beta_version = "#{params[:codefreeze_version]}-rc-1"
        new_beta_version_code = current_release_version_code + 1
        new_alpha_version = Fastlane::Helpers::AndroidVersionHelper.calc_next_alpha_version_name(current_version)
        new_alpha_version_code = new_beta_version_code + 1

        # Ask user confirmation
        message = "Building a new release branch starting from develop.\n"
        message << "Version in develop is #{current_version} (code: #{current_build_version_code}).\n"
        message << "After code freeze:\n"
        message << "#{current_release_version} on branch release/#{current_release_version}: version name: #{current_release_version} - version code: #{current_release_version_code}.\n" unless !params[:update_release_branch_version] 
        message << "New #{params[:codefreeze_version]} on branch release/#{params[:codefreeze_version]}: version name: #{new_beta_version} - version code: #{new_beta_version_code}.\n"
        message << "New alpha on branch develop: version name: #{new_alpha_version} - version code: #{new_alpha_version_code}.\n"
        message << "Do you want to continue?\n"
        
        if (!params[:skip_confirm])
          if (!UI.confirm(message))
            UI.user_error!("Aborted by user request")
          end
        end

        # Check local repo status
        other_action.ensure_git_status_clean()

        # Creates the new branch
        Fastlane::Helpers::AndroidGitHelper.get_create_codefreeze_branch("release/#{params[:codefreeze_version]}")

        # Updates the version codes
        if (params[:update_release_branch_version])
          Action.sh("./tools/update-release-names.sh #{current_release_version_code} #{current_release_version} #{new_beta_version} #{new_alpha_version}")
        else
          Action.sh("./tools/update-name-alpha-beta.sh #{new_beta_version_code} #{new_beta_version} #{new_alpha_version}")
        end 

        current_release_version
      end

      #####################################################
      # @!group Documentation
      #####################################################

      def self.description
        "Runs some prechecks before code freeze"
      end

      def self.details
        "Updates the develop branch, checks the app version and ensure the branch is clean"
      end

      def self.available_options
        # Define all options your action supports. 
        
        # Below a few examples
        [
          FastlaneCore::ConfigItem.new(key: :skip_confirm,
                                       env_name: "FL_ANDROID_CODEFREEZE_PRECHECKS_SKIPCONFIRM",
                                       description: "Skips confirmation before codefreeze",
                                       is_string: false, # true: verifies the input is a string, false: every kind of value
                                       default_value: false), # the default value if the user didn't provide one
          FastlaneCore::ConfigItem.new(key: :codefreeze_version,
                                       env_name: "FL_ANDROID_CODEFREEZE_PRECHECKS_NEW_VERSION", 
                                       description: "The version to freeze", # a short description of this parameter
                                       is_string: true,
                                       optional: false),
          FastlaneCore::ConfigItem.new(key: :update_release_branch_version, # true: Update the version for the code frozen branch
                                       env_name: "FL_ANDROID_CODEFREEZE_PRECHECKS_UPDATERELEASEBRANCH",
                                       description: "true: update the version code on the releasing branch; false: only develop and the new code freeze branch",
                                       is_string: false, 
                                       default_value: true), # the default value if the user didn't provide one

        ]
      end

      def self.output

      end

      def self.return_value
        "Version of the app before code freeze"
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
