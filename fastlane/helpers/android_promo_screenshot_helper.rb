require 'tmpdir'

module Fastlane
  module Helpers

    class AndroidPromoScreenshots
      attr_reader :device, :locales, :orig_folder, :target_folder, :default_locale, :metadata_folder

      TEXT_OFFSET_X = 0
      TEXT_OFFSET_Y = 58
      DEFAULT_TEXT_SIZE = 80
      private_constant :TEXT_OFFSET_X, :TEXT_OFFSET_Y, :DEFAULT_TEXT_SIZE

      def initialize(locales, default_locale, orig_folder, target_folder, metadata_folder)
        @locales = locales
        @default_locale = default_locale
        @orig_folder = orig_folder
        @target_folder = target_folder
        @metadata_folder = metadata_folder

        load_default_locale()
      end

      # Generate all the required screenshots for
      # the provided device
      def generate_device(device)
        @device = device
        UI.message("Generate promo screenshot for device: #{@device[:device]}")

        locales.each do | locale |
          generate_locale(locale[:google_play], locale[:promo_config])
        end
      end

      # Download the used font to the tmp folder
      # if it's not there
      def self.require_font()
        font_file = self.get_font_path()
        if (File.exist?(font_file))
          return
        end

        font_folder = File.dirname(font_file)
        Dir.mkdir(font_folder) unless File.exist?(font_folder)
        Fastlane::Actions::sh("wget \"https://fonts.google.com/download?family=Noto%20Serif\" -O \"#{font_folder}/noto.zip\"")
        Fastlane::Actions::sh("unzip \"#{font_folder}/noto.zip\" -d \"#{font_folder}\"")
      end

    private
      # Generate the screenshots for
      # the provided locale
      def generate_locale(locale, locale_options)
        UI.message("Generating #{locale}...")

        target_folder = verify_target_folder(locale)
        strings = get_promo_strings_for(locale)
        files = Dir["#{get_screenshots_orig_path(locale)}screenshot*"].sort
        text_size = get_text_size_for(locale_options)
        text_font = get_text_font_for(locale_options)
        text_offset = get_text_offset_y()

        idx = 1
        files.each do | file |
          generate_screenshot(file, get_local_at(idx.to_s, strings), target_folder, text_size, text_offset, text_font)
          idx = idx + 1
        end
      end

      # Generate a promo screenshot
      def generate_screenshot(file, string, target_folder, text_size, text_offset, text_font)
        target_file = "#{target_folder}#{File.basename(file)}"
        puts "Generate screenshots for #{file} to #{target_file}"

        # Temp file paths
        cut_file = "#{target_file}_cut"
        resized_file = "#{target_file}_resize"
        comp_file = "#{target_file}_comp"

        # 1. Cut toolbar
        #Fastlane::Actions::sh("magick \"#{file}\" -crop 0x#{device[:comp_size].split("x")[2].to_i - 95}+0+0  \"#{cut_file}\"")

        # 2. Resize original screenshot
        Fastlane::Actions::sh("magick \"#{file}\" -resize #{device[:comp_size]}\\! \"#{resized_file}\"")
        File.delete(cut_file) if File.exist?(cut_file)

        # 3. Put it on the background
        Fastlane::Actions::sh("magick #{@device[:template]} \"#{resized_file}\" -geometry #{device[:comp_offset]} -composite \"#{comp_file}\"")
        File.delete(resized_file) if File.exist?(resized_file)

        # 4. Put the promo string on top of it
        Fastlane::Actions::sh("magick \"#{comp_file}\" -gravity north -pointsize #{text_size} -font \"#{text_font}\" -draw \"fill white text #{TEXT_OFFSET_X},#{text_offset} \\\"#{string}\\\"\" \"#{target_file}\"")
        File.delete(comp_file) if File.exist?(comp_file)
      end

      # Loads the promo strings in the default locale
      # -> to be used when a localisation is missing
      def load_default_locale()
        @default_strings = get_promo_strings_for(@default_locale)
      end

      # Gets the promo string, picking the default one
      # if the localised version is missing
      def get_local_at(index, strings)
        if (strings.key?(index))
          return strings[index]
        end

        if (@default_strings.key?(index))
          return @default_strings[index]
        end

        return "Unknown"
      end

      # Loads the localised promo string set for
      # the provided locale
      def get_promo_strings_for(locale)
        strings = { }

        path = get_locale_path(locale)
        files = Dir["#{path}*"]

        files.each do | promo_file |
          # Extract the string ID code
          promo_file_name = File.basename(promo_file, ".txt")
          promo_id = promo_file_name.split('_').last

          # Read the file into a string
          promo_string = File.read(promo_file)

          # Add to hash
          strings[promo_id] = promo_string
        end

        return strings
      end

      # Helpers
      def get_text_size_for(locale_options)
        text_adj = (@device.key?(:text_adj) ? @device[:text_adj] : 100) / 100.0
        return (DEFAULT_TEXT_SIZE * text_adj) unless locale_options.key?(:text_size)
        locale_options[:text_size] * text_adj
      end

      def get_text_font_for(locale_options)
        return AndroidPromoScreenshots.get_font_path() unless locale_options.key?(:font)
        locale_options[:font]
      end

      def get_text_offset_y()
        return TEXT_OFFSET_Y unless @device.key?(:text_offset)
        @device[:text_offset]
      end

      def verify_target_folder(locale)
        folder = get_screenshots_target_path(locale)
        Dir.mkdir(folder) unless File.exists?(folder)

        return folder
      end

      def get_locale_path(locale)
        "#{@metadata_folder}/#{locale}/"
      end

      def get_screenshots_orig_path(locale)
        "#{@orig_folder}/#{locale}/images/#{@device[:device]}/"
      end

      def get_screenshots_target_path(locale)
        "#{@target_folder}/#{locale}/images/#{@device[:device]}/"
      end

      def self.get_font_path()
        "#{Dir.tmpdir()}/font/NotoSerif-Bold.ttf"
      end
    end
  end
end
