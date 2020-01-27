Pod::Spec.new do |s|
  s.name           = 'WordPressMocks'
  s.version        = '0.0.7'
  s.platform       = :ios
  s.summary        = 'Network mocking for testing the WordPress mobile apps.'
  s.homepage       = 'https://github.com/wordpress-mobile/WordPressMocks'
  s.license        = { type: 'GPLv2', file: 'LICENSE.md' }
  s.author         = { 'James Treanor' => 'jtreanor3@gmail.com' }
  s.source         = { git: "https://github.com/wordpress-mobile/WordPressMocks.git", :tag => s.version.to_s }
  s.preserve_paths = 'WordPressMocks/src', 'scripts'
end
