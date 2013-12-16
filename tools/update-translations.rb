#!/usr/bin/env ruby -wKU

# supported languages:

LANGS={
  'ar' => 'ar', # Arabic
  'bg' => 'bg', # Bulgarian
  'bs' => 'bs', # Bosnian
  'da' => 'da', # Danish
  'de' => 'de', # German
  'el' => 'el', # Greek
  'es' => 'es', # Spanish
  'eu' => 'eu', # Basque
  'fi' => 'fi', # Finnish
  'fr' => 'fr', # French
  'hi' => 'hi', # Hindi
  'he' => 'he', # Hebrew
  'hr' => 'hr', # Croatian
  'hu' => 'hu', # Hungarian
  'id' => 'id', # Indonesian
  'it' => 'it', # Italian
  'ja' => 'ja', # Japanese
  'ka' => 'ka', # Georgian
  'ko' => 'ko', # Korean
  'lv' => 'lv', # Latvian
  'lt' => 'lt', # Lithuanian
  'nb' => 'nb', # Norwegian
  'nl' => 'nl', # Dutch
  'pl' => 'pl', # Polish
  'pt' => 'pt', # Portuguese
  'pt-br' => 'pt-br', # Portuguese (Brazil)
  'ro' => 'ro', # Romanian
  'sk' => 'sk', # Slovak
  'sl' => 'sl', # Slovenian
  'sr' => 'sr', # Serbian
  'sv' => 'sv', # Swedish
  'th' => 'th', # Thai
  'tl' => 'tl', # Tagalog
  'tr' => 'tr', # Turkish
  'uk' => 'uk', # Ukranian
  'vi' => 'vi', # Vietnamese
  'zh' => 'zh', # Chinese
  'zh-cn' => 'zh-cn', # Chinese (China)
  'zh-tw' => 'zh-tw' # Chinese (Taiwan)
}

LANGS.each do |code,local|
  puts "Updating #{code}"
  system "cp res/values-#{local}/strings.xml res/values-#{local}/strings.xml.bak"
  system "curl --globoff -so res/values-#{local}/strings.xml \"http://translate.wordpress.org/projects/android/dev/#{code}/default/export-translations?filters[status]=current&format=android\"" or begin
    puts "Error downloading #{code}"
  end
  system "rm res/values-#{local}/strings.xml.bak"
  system "grep -a '\\x00\\x22\\x00\\x22' res/values-#{local}/strings.xml"
end