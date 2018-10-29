module Fastlane
  module Helpers
    module AndroidGitHelper
     
      def self.git_checkout_and_pull(branch)
        Action.sh("git checkout #{branch}")
        Action.sh("git pull")
      end
      
    end
  end
end