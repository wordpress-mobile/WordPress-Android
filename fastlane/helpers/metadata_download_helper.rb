require 'net/http'
require 'json'

module Fastlane
  module Helpers
    
    class MetadataDownloader
      attr_reader :target_folder, :target_files

      def initialize(target_folder, target_files)
        @target_folder = target_folder
        @target_files = target_files
      end

      # Downloads data from GlotPress, 
      # in JSON format
      def download(target_locale, glotpress_url)
        uri = URI(glotpress_url)
        response = Net::HTTP.get_response(uri)
        if response.code == "301"
          response = Net::HTTP.get_response(URI.parse(response.header['location']))
        end

        loc_data = JSON.parse(response.body) rescue loc_data = nil
        parse_data(target_locale, loc_data)       
      end

      # Parse JSON data and update the local files
      def parse_data(target_locale, loc_data)
        delete_existing_metadata(target_locale)

        if (loc_data == nil)
          UI.message "No translation available for #{target_locale}"
          return
        end

        loc_data.each do | d |
          key = d[0].split(/\u0004/).first

          target_files.each do | file |
            if (file[0].to_s == key)
              data=file[1]
              if (data.key?(:max_size)) && (data[:max_size] != 0) && ((d[1].to_s.length - 3) > data[:max_size]) then
                UI.message("Rejecting #{target_locale} traslation for #{key}: translation length: #{d[1].to_s.length} - max allowed length: #{data[:max_size]}")
              else
                save_metadata(target_locale, file[1][:desc], d[1])
              end 
            end
          end
        end
      end

      # Writes the downloaded content
      # to the target file
      def save_metadata(locale, file_name, content)
        file_path = get_target_file_path(locale, file_name)
        
        dir_path = File.dirname(file_path)
        Dir.mkdir(dir_path) unless File.exists?(dir_path)

        File.open(file_path, "w") { |file| file.puts(content) }
      end

      # Some small helpers
      def delete_existing_metadata(target_locale)
        @target_files.each do | file |
          file_path = get_target_file_path(target_locale, file[1][:desc])
          File.delete(file_path) if File.exists? file_path
        end
      end

      
      def get_target_file_path(locale, file_name)
        "#{@target_folder}/#{locale}/#{file_name}"
      end
    end

    
  end
end
