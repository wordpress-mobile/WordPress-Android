module Fastlane
  module Actions
    module SharedValues
      TAKE_ANDROID_EMULATOR_SCREENSHOTS_CUSTOM_VALUE = :TAKE_ANDROID_EMULATOR_SCREENSHOTS_CUSTOM_VALUE
    end

    class TakeAndroidEmulatorScreenshotsAction < Action
      def self.run(params)
        UI.message "Taking Android emulator screenhots for #{params[:devices].count} devices"

        require_relative '../helpers/android_virtual_device_helper.rb'

        params[:devices].each do |device|
          device_serial = device[:device_serial]
          if device_serial.nil?
            launcher = Fastlane::Helpers::AndroidVirtualDeviceLauncher.new
            device_serial = launcher.trigger(device_name: device[:device_name])
          end

          enter_demo_mode(device_serial)
          take_device_screenshots(device_serial, device[:screenshot_type], params[:screenshot_options])
          exit_demo_mode(device_serial)
        end
      end

      def self.enter_demo_mode(device_serial)
        # Setup device for demo
        other_action.adb(command: "shell settings put global sysui_demo_allowed 1",
                        serial: device_serial)
        # Disable system notifications
        other_action.adb(command: "shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false",
                        serial: device_serial)
        # Enjoy lunch while browsing the Play Store
        other_action.adb(command: "shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1337",
                        serial: device_serial)
      end

      def self.exit_demo_mode(device_serial)
        # Exit demo mode
        other_action.adb(command: "shell am broadcast -a com.android.systemui.demo -e command exit", serial: device_serial)
      end

      def self.take_device_screenshots(device_serial, screenshot_type, screenshot_options)
        merged_options = screenshot_options.merge({
          device_type: screenshot_type,
          specific_device: device_serial,
        })

        # Running an action from here using Dir.chdir unpredictably so relative paths are unreliable
        # Use absolute paths so that Screengrab can find the paths
        if merged_options.key?(:app_apk_path)
          merged_options[:app_apk_path] = absolute_path(merged_options[:app_apk_path])
        end
        if merged_options.key?(:tests_apk_path)
          merged_options[:tests_apk_path] = absolute_path(merged_options[:tests_apk_path])
        end
        output_directory = merged_options.fetch(:output_directory, File.join("fastlane", "metadata", "android"))
        merged_options[:output_directory] = absolute_path(output_directory)

        other_action.capture_android_screenshots(merged_options)
      end

      def self.absolute_path(relative_path)
        Pathname.new(relative_path).expand_path.to_s
      end

      #####################################################
      # @!group Validation
      #####################################################

      def self.verify_devices(devices)
        UI.user_error! "Devices must not be empty" if devices.empty?
        devices.each { |device| verify_device(device) }
      end

      def self.verify_device(device)
        UI.user_error! "Each device must be a valid Hash" unless device.is_a?(Hash)

        required_keys = [:screenshot_type]
        required_keys.each do |key|
          UI.user_error! "Device: '#{key}' is a required key" unless device.key?(key)
        end

        unless device.key?(:device_name) || device.key?(:device_serial)
          UI.user_error! "Device: must have ':device_name' or ':device_serial'"
        end
      end

      #####################################################
      # @!group Documentation
      #####################################################
      def self.description
        "Take screenhots in Android Emulators"
      end

      def self.details
        "Take screenhots for all the specified Android Emulator configurations"
      end

      def self.available_options
        [
          FastlaneCore::ConfigItem.new(key: :devices,
                                      env_name: "FL_TAKE_ANDROID_EMULATOR_SCREENSHOTS_DEVICES",
                                      description: "Android emulator configurations to capture screenshots in",
                                      type: Array,
                                      verify_block: proc do |value|
                                        verify_devices(value)
                                      end),
          FastlaneCore::ConfigItem.new(key: :screenshot_options,
                                      env_name: "FL_TAKE_ANDROID_EMULATOR_SCREENSHOT_OPTIONS",
                                      description: "The screenshot options to be provided to Fastlane Screengrab",
                                      type: Hash)
        ]
      end

      def self.output

      end

      def self.return_value
        # If your method provides a return value, you can describe here what it does
      end

      def self.authors
        ["jtreanor"]
      end

      def self.is_supported?(platform)
        platform == :android
      end
    end
  end
end
