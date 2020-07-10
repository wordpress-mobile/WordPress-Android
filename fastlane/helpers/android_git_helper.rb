module Fastlane
  module Helpers
    module AndroidGitHelper

      def self.git_checkout_and_pull(branch)
        Action.sh("git checkout #{branch}")
        Action.sh("git pull")
      end

      def self.get_create_codefreeze_branch(branch)
        Action.sh("git checkout develop")
        Action.sh("git pull")
        Action.sh("git checkout -b #{branch}")
        commit_release_notes_for_code_freeze
        Action.sh("git push --set-upstream origin #{branch}")
      end

      def self.commit_release_notes_for_code_freeze
        Action.sh("cp RELEASE-NOTES.txt WordPress/metadata/release_notes.txt")
        Action.sh("echo > RELEASE-NOTES.txt")
        Action.sh("git add RELEASE-NOTES.txt WordPress/metadata/release_notes.txt")
        Action.sh("git commit -m \"Update release notes for code freeze\"")
      end

      def self.update_metadata()
        Action.sh("./tools/update-translations.sh")
        Action.sh("./gradlew build")
        Action.sh("git add ./WordPress/src/main/res")
        Action.sh("git commit -m \"Updates translations\"")

        Action.sh("git push")
      end
    end
  end
end
