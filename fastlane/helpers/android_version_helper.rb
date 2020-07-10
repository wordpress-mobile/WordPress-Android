module Fastlane
  module Helpers
    module AndroidVersionHelper
      MAJOR_NUMBER = 0
      MINOR_NUMBER = 1
      HOTFIX_NUMBER = 2
      ALPHA_PREFIX = "alpha-"
      RC_SUFFIX = "-rc"

      def self.get_version_name
        get_version_name_from_file("./WordPress/build.gradle")
      end

      def self.get_build_version_code
        get_version_build_from_file("./WordPress/build.gradle")
      end

      def self.is_alpha_version(version)
        version.start_with?(ALPHA_PREFIX)
      end

      def self.is_beta_version(version)
        version.include?(RC_SUFFIX)
      end

      def self.calc_next_alpha_version_name(version)
        alpha_number = version.sub(ALPHA_PREFIX, '')
        "#{ALPHA_PREFIX}#{alpha_number.to_i() + 1}"
      end

      def self.calc_prev_release_version(version)
        vp = get_version_parts(version)
        if (vp[MINOR_NUMBER] == 0)
          vp[MAJOR_NUMBER] -= 1
          vp[MINOR_NUMBER] = 9
        else
          vp[MINOR_NUMBER] -= 1
        end

        "#{vp[MAJOR_NUMBER]}.#{vp[MINOR_NUMBER]}"
      end

      def self.is_hotfix(version)
        return false if is_alpha_version(version)
        vp = get_version_parts(version)
        return (vp.length > 2) && (vp[HOTFIX_NUMBER] != 0)
      end

      # private

      def self.get_version_parts(version)
        version.split(".").fill("0", version.length...2).map{|chr| chr.to_i}
      end

      def self.get_version_name_from_file(file_path)
        Action.sh("cat #{file_path} | grep versionName").split(' ')[1].tr('\"', '')
      end

      def self.get_version_build_from_file(file_path)
        Action.sh("cat #{file_path} | grep versionCode").split(' ')[1]
      end
    end
  end
end
