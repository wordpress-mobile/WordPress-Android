module Fastlane
  module Actions
    module SharedValues
      
    end

    class UpdateMetadataSourceAction < Action
      def self.run(params)
        # fastlane will take care of reading in the parameter and fetching the environment variable:
        UI.message "Parameter .po file path: #{params[:po_file_path]}"
        UI.message "Release version: #{params[:release_version]}"

        # Init
        require_relative '../helpers/metadata_helper.rb'
        create_block_parsers(params[:release_version], params[:source_files])

        # Do
        check_source_files(params[:source_files])
        temp_po_name = create_temp_po(params)
        swap_po(params[:po_file_path], temp_po_name)

        UI.message "File #{params[:po_file_path]} updated!"
      end

      # Verifies that all the source files are available  
      # to this action
      def self.check_source_files(source_files)
        source_files.values.each do | file_path |
          UI.user_error!("Couldn't find file at path '#{file_path}'") unless File.exist?(file_path)
        end
      end

      # Creates a temp po file merging
      # new data for known tags
      # and the data already in the original
      # .po fo the others.
      def self.create_temp_po(params)
        orig = params[:po_file_path]
        target = self.create_target_file_path(orig)

        # Clear if older exists
        File.delete(target) if File.exists? target 

        # Create the new one
        begin
          File.open(target, "a") do |fw|
            File.open(orig, "r").each do |fr|
              write_target_block(fw, fr)
            end 
          end
        rescue 
          File.delete(target) if File.exists? target 
          raise 
        end 

        target 
      end

      # Deletes the old po and moves the temp one
      # to the final location
      def self.swap_po(orig_file_path, temp_file_path)
        File.delete(orig_file_path) if File.exists? orig_file_path
        File.rename(temp_file_path, orig_file_path)
      end

      # Generates the temp file path 
      def self.create_target_file_path(orig_file_path)
        "#{File.dirname(orig_file_path)}/#{File.basename(orig_file_path, ".*")}.tmp"
      end

      # Creates the block instances
      def self.create_block_parsers(release_version, block_files)
        @blocks = Array.new

        # Inits default handler
        @blocks.push (Fastlane::Helpers::UnknownMetadataBlock.new)

        # Init special handlers
        block_files.each do | key, file_path |
          if (key == :release_note) 
            @blocks.push (Fastlane::Helpers::ReleaseNoteMetadataBlock.new(key, file_path, release_version))
          else
            @blocks.push (Fastlane::Helpers::StandardMetadataBlock.new(key, file_path))
          end
        end

        # Sets the default 
        @current_block = @blocks[0]
      end

      # Manages tags depending on the type
      def self.write_target_block(fw, line)
        if (is_block_id(line))
          key = line.split(' ')[1].tr('\"', '')
          @blocks.each do | block |
            @current_block = block if block.is_handler_for(key)
          end
        end

        if (is_comment(line))
          @current_block = @blocks.first
        end

        @current_block.handle_line(fw, line)
      end

      def self.is_block_id(line)
        line.start_with?('msgctxt')
      end

      def self.is_comment(line)
        line.start_with?('#')
      end



      #####################################################
      # @!group Documentation
      #####################################################

      def self.description
        "Updates a .po file with new data from .txt files"
      end

      def self.details
        "You can use this action to update the .po file that contains the string to load to GlotPress for localization."
      end

      def self.available_options
        # Define all options your action supports. 
        
        # Below a few examples
        [
          FastlaneCore::ConfigItem.new(key: :po_file_path,
                                       env_name: "FL_UPDATE_METADATA_SOURCE_PO_FILE_PATH", 
                                       description: "The path of the .po file to update", 
                                       is_string: true,
                                       verify_block: proc do |value|
                                          UI.user_error!("No .po file path for UpdateMetadataSourceAction given, pass using `po_file_path: 'file path'`") unless (value and not value.empty?)
                                          UI.user_error!("Couldn't find file at path '#{value}'") unless File.exist?(value)
                                       end),
          FastlaneCore::ConfigItem.new(key: :release_version,
                                       env_name: "FL_UPDATE_METADATA_SOURCE_RELEASE_VERSION",
                                       description: "The release version of the app (to use to mark the release notes)",
                                       verify_block: proc do |value|
                                        UI.user_error!("No relase version for UpdateMetadataSourceAction given, pass using `release_version: 'version'`") unless (value and not value.empty?) 
                                      end),
          FastlaneCore::ConfigItem.new(key: :source_files,
                                        env_name: "FL_UPDATE_METADATA_SOURCE_SOURCE_FILES",
                                        description: "The hash with the path to the source files and the key to use to include their content",
                                        is_string: false,
                                        verify_block: proc do |value|
                                          UI.user_error!("No source file hash for UpdateMetadataSourceAction given, pass using `source_files: 'source file hash'`") unless (value and not value.empty?)
                                       end)
        ]
      end

      def self.output
          
      end

      def self.return_value
        # If your method provides a return value, you can describe here what it does
      end

      def self.authors
        ["loremattei"]
      end

      def self.is_supported?(platform)
        # you can do things like
        # 
        #  true
        # 
        #  platform == :ios
        # 
        #  [:ios, :mac].include?(platform)
        # 

        [:ios, :android].include?(platform)
      end
    end
  end
end
