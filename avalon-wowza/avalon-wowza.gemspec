# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'avalon/wowza/version'

Gem::Specification.new do |spec|
  spec.name          = "avalon-wowza"
  spec.version       = Avalon::Wowza::VERSION
  spec.authors       = ["Michael Klein"]
  spec.email         = ["mbklein@gmail.com"]
  spec.summary       = "Wowza Streaming Engine support for the Avalon Media System"
  spec.description   = "Wowza Streaming Engine support for the Avalon Media System"
  spec.homepage      = "https://github.com/avalonmediasystem/wowza-avalon"
  spec.license       = "Apache 2.0"

  spec.files         = Dir['lib/**/*'] + ['Gemfile','Rakefile','LICENSE','NOTICE','avalon-wowza.gemspec']
  spec.require_paths = ["lib"]

  spec.add_development_dependency "bundler", "~> 1.7"
  spec.add_development_dependency "rake", "~> 10.0"
end
