
# TODO: Move this to the release-toolkit
#
module Fastlane
  module Helpers
    class AndroidTools
      attr_reader :android_sdk_root

      def initialize(sdk_root: nil)
        @android_sdk_root = sdk_root || ENV['ANDROID_HOME'] || ENV['ANDROID_SDK_ROOT'] || ENV['ANDROID_SDK']
      end

      def tool(paths:,binary:)
        bin_path = `command -v #{binary}`.chomp
        return bin_path unless bin_path.nil? || bin_path.empty? || !File.executable?(bin_path)

        bin_path = paths
          .map { |path| File.join(android_sdk_root, path) }
          .first { |path| File.executable?(path) }
          
        UI.user_error!("Unable to find path for #{binary} in #{paths.inspect}. Verify you installed the proper Android tools.") if bin_path.nil?
        bin_path
      end

      def sdkmanager
        @sdkmanager_bin ||= tool(paths: ['cmdline-tools', 'latest', 'bin'], binary: 'sdkmanager')
      end

      def avdmanager
        @avdmanager_bin ||= tool(paths: ['cmdline-tools', 'latest', 'bin'], binary: 'avdmanager')
      end

      def emulator
        @emulator_bin ||= tool(paths: ['emulator'], binary: 'emulator')
      end

      def adb
        @adb_bin ||= tool(paths: ['platform-tools'], binary: 'adb')
      end
    end

    class AndroidEmulator
      BOOT_WAIT = 2
      BOOT_TIMEOUT = 60

      SHUTDOWN_WAIT = 2
      SHUTDOWN_TIMEOUT = 60


      def initialize
        @tools = AndroidTools.new
      end

      # Installs the system-image suitable for a given Android `api`, with `google_apis`, and for the current machine's architecture
      #
      # @param [Integer] api The Android API level to use
      #
      # @return [String] The `sdkmanager` package specifier that has been installed
      #
      def install_system_image(api:)
        package = system_image_package(api: api)
        Actions.execute_action("Installing System Image for Android #{api} (#{package})") do
          Actions.sh(@tools.sdkmanager, '--install', package)
        end
        package
      end

      # Create an emulator (AVD) for a given `api` number and `device` model
      #
      # @param [Integer] api The Android API level to use for this AVD
      # @param [String] device The Device Model to use for this AVD. Valid values can be found using `avdmanager list devices`
      # @param [String] name The name to give for the created AVD. Defaults to `<device>_API_<api>`.
      # @param [String] sdcard The size of the SD card for this device. Defaults to `512M`.
      #
      # @return [String] The device name (i.e. either `name` if provided, or the derived `<device>_API_<api>` if provided `name` was `nil``)
      #
      def create_avd(api:, device:, system_image: nil, name: nil, sdcard: '512M')
        package = system_image || system_image_package(api: api)
        device_name = name || "#{device.gsub(' ','_').capitalize}_API_#{api}"
        Actions.execute_action("Creating AVD `#{device_name}` (#{device}, API #{api})") do
          Actions.sh(
            @tools.avdmanager, 'create', 'avd',
            '--force',
            '--package', package,
            '--device', device,
            '--sdcard', sdcard,
            '--name', device_name,
          )
        end

        device_name
      end

      # Launch the emulator for the given AVD, then return the emulator serial
      #
      # @param [String] name name of the AVD to launch
      #
      # @return [String] emulator serial number corresponding to the launched AVD
      #
      def launch_avd(name:)
        port = '5554'.freeze
        serial = "emulator-#{port}"

        shut_down_emulators!(serials: [serial]) # To ensure we can launch one on the port 5554 (as it's hardcoded for simplicity)

        Actions.execute_action("Launching emulator for #{name}") do
          command = [@tools.emulator, '-avd', name, '-port', port].shelljoin + ' >/dev/null 2>/dev/null &'
          UI.command(command)
          system(command)

          UI.message('Waiting for device to finish booting...')
          Actions.sh(@tools.adb, '-s', serial, 'wait-for-device')
          retry_loop(time_between_retries: BOOT_WAIT, timeout: BOOT_TIMEOUT, description: 'waiting for device to finish booting') do
            Actions.sh(@tools.adb, '-s', serial, 'shell', 'getprop', 'sys.boot_completed').chomp == '1'
          end
        end
        serial
      end

      # @return [Array<Fastlane::Helper::AdbDevice>] List of currently booted emulators
      #
      def running_emulators
        helper = Fastlane::Helper::AdbHelper.new(adb_path: @tools.adb)
        helper.load_all_devices.select { |device| device.serial.include? "emulator" }
      end

      # Trigger a shutdown for all running emulators, and wait until there is no more emulator running.
      #
      # @param [Array<String>] serials List of emulator serials to shut down. Will shut down all of them if `nil`.
      #
      def shut_down_emulators!(serials: nil)
        Actions.execute_action("Shutting down #{serials || 'all'} emulator(s)") do
          emulators_list = running_emulators.map(&:serial)
          emulators_list &= serials unless serials.nil? # intersection of the set of running emulators with the ones we want to shut down
          emulators_list.each do |e|
            Actions.sh(@tools.adb, '-s', e, 'emu', 'kill') { |_| } # ignore error if no emulator with specified serial is running
          end

          # Wait until all emulators are killed
          retry_loop(time_between_retries: SHUTDOWN_WAIT, timeout: SHUTDOWN_TIMEOUT, description: 'waiting for devices to shutdown') do
            (emulators_list & running_emulators.map(&:serial)).empty?
          end
          UI.message("All emulators are now shut down")
        end
      end

      # Find the system-images package for the provided API, with Google APIs, and matching the current platform/architecture this lane is called from.
      #
      # @param [Integer] api The Android API level to use for this AVD
      # @return [String] The `system-images;android-<N>;google_apis;<platform>` package specifier for `sdkmanager` to use in its install command
      #
      # @note Results from this method are memoized, to avoid repeating calls to `sdkmanager` when querying for the same api level multiple times.
      #
      def system_image_package(api:)
        @system_image_packages ||= {}
        @system_image_packages[api] ||= begin
          platform = `uname -m`.chomp
          package = `#{@tools.sdkmanager} --list`.match(/^ *(system-images;android-#{api};google_apis;#{platform}(-[^ ]*)?)/)&.captures&.first
          UI.user_error!("Could not find system-image for API `#{api}` and your platform `#{platform}` in `sdkmanager --list`. Maybe Google removed it for download and it's time to update to a newer API?") if package.nil?
          package
        end
      end

      def retry_loop(time_between_retries:, timeout:, description:)
        Timeout::timeout(timeout) do
          until yield
            sleep(time_between_retries)
          end
        end
      rescue Timeout::Error
        UI.user_error!("Timed out #{description}")
      end
    end
  end
end