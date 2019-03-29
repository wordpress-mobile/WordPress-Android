module Fastlane
  module Helpers
    module AndroidVirtualDevicePathHelper
      def self.android_home

        android_sdk_root = ENV['ANDROID_HOME'] || ENV['ANDROID_SDK_ROOT'] || ENV['ANDROID_SDK']

        if android_sdk_root
            return android_sdk_root
        end

        begin
            require 'java-properties'
        rescue LoadError
            UI.user_error! "Unable to load dependencies. Did you run `bundle` and `bundle exec fastlane`?"
        end

        propertiesFile = "#{project_root_path}/local.properties"

        local = nil

        if File.exist? propertiesFile
            properties = JavaProperties.load(propertiesFile)
            local = properties[:"sdk.dir"]
        end

        if android_sdk_root.nil? && local.nil?
            UI.user_error! "There is no Android SDK root set. Unable to continue."
        elsif android_sdk_root.nil? && local
            UI.user_error! "There is no Android SDK root set. Unable to continue.\nTo solve this, add:\n\nexport ANDROID_HOME=#{local}\nexport ANDROID_SDK_ROOT=$ANDROID_HOME\n\nto ~/.profile"
        end

        return android_sdk_root
      end

      def self.emulator_path
        Pathname.new(android_home).join("emulator/emulator").to_s
      end

      def self.adb_path
        Pathname.new(android_home).join("platform-tools/adb").to_s
      end

      def self.project_root_path
        Pathname.new(File.dirname(__FILE__)).parent().parent()
      end
    end

    module AndroidVirtualDevicesAdbHelper
      def self.adb(command: nil, serial: "")
        Fastlane::Actions::AdbAction.run(
          adb_path: AndroidVirtualDevicePathHelper.adb_path,
          command: command,
          serial: serial)
      end

      def self.adb_devices
        Fastlane::Actions::AdbDevicesAction.run(adb_path: AndroidVirtualDevicePathHelper.adb_path)
      end

      def self.adb_emulators
        adb_devices.select do |device|
            device.serial.include? "emulator"
        end
      end

      def self.boot_completed?
        adb(command: "shell getprop sys.boot_completed").to_i == 1
      end

      def self.wait_for_device
        adb(command: "wait-for-device")
      end

      def self.shutdown_device(device_serial)
        adb(command: "emu kill", serial: device_serial)
      end
    end

    class AndroidVirtualDeviceLauncher
      LAUNCH_TIMEOUT = 60
      LAUNCH_WAIT = 2

      def trigger(device_name: nil, wipe_data: true)
        unless installed_avd?(device_name)
          UI.user_error!("Android Virtual Device '#{device_name}' not found")
        end

        killer = AndroidVirtualDeviceKiller.new
        killer.shutdown_all_devices

        launch(device_name, wipe_data)
        wait_for_device_boot

        device_serial = AndroidVirtualDevicesAdbHelper.adb_devices.first.serial
        device_serial
      end

      private

      def installed_avd?(device_name)
        avds = Action.sh("#{AndroidVirtualDevicePathHelper.emulator_path} -list-avds").split("\n")
        avds.include? device_name
      end

      def launch(device_name, wipe_data)
        UI.message("Launching emulator '#{device_name}'")

        command = [
          AndroidVirtualDevicePathHelper.emulator_path,
          "-avd \"#{device_name}\"",
          "-no-snapshot",
          "-gpu auto",
        ]
        command << "-wipe-data" if wipe_data
        command << "&" # Run in background
        joined_command = command.join(" ")

        UI.command(joined_command)
        system(joined_command)
      end

      def wait_for_device_boot
        AndroidVirtualDevicesAdbHelper.wait_for_device
        begin
          # Wait for complete boot
          Timeout::timeout(LAUNCH_TIMEOUT) do
            next if AndroidVirtualDevicesAdbHelper.boot_completed?
            until AndroidVirtualDevicesAdbHelper.boot_completed?
              sleep(LAUNCH_WAIT)
            end
          end
        rescue Timeout::Error
          UI.user_error!("Timed out waiting for the device to boot")
        end
      end
    end

    class AndroidVirtualDeviceKiller
      SHUTDOWN_TIMEOUT = 60
      SHUTDOWN_WAIT = 2

      def shutdown_all_devices
        UI.message("Shutting down all emulators")
        AndroidVirtualDevicesAdbHelper.adb_emulators.each do |device|
          AndroidVirtualDevicesAdbHelper.shutdown_device(device.serial)
        end
        wait_for_no_devices
      end

      private

      def wait_for_no_devices
        begin
          Timeout::timeout(SHUTDOWN_TIMEOUT) do
            next if AndroidVirtualDevicesAdbHelper.adb_emulators.empty?
            until AndroidVirtualDevicesAdbHelper.adb_emulators.empty?
              sleep(SHUTDOWN_WAIT)
            end
          end
        rescue Timeout::Error
          UI.user_error!("Timed out waiting for devices to shutdown")
        end
      end
    end
  end
end
