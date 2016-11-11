# wowza-avalon
This project implements a Server-Side Module to bridge between the 
[Wowza Streaming Engine](http://www.wowza.com/products/streaming-engine) and
the [Avalon Media System](http://avalonmediasystem.org). It provides the logic 
and configuration necessary for Wowza to pass a token back to a running Avalon 
server to authorize stream playback.

## Instructions

### Install and configure Wowza Streaming Engine
1. Install Wowza per the [Wowza Streaming Engine User Guide](http://www.wowza.com/resources/WowzaStreamingEngine_UsersGuide.pdf)
2. `cd [wowza-root]` (`/Library/WowzaStreamingEngine` on OS X, or 
`/usr/local/WowzaStreamingEngine` on Linux)
3. `curl -L https://github.com/avalonmediasystem/wowza-avalon/releases/download/v0.2.0/wowza-avalon-app.tar.bz2 | tar xj`
4. `chmod 0777 avalon`
5. Restart the WowzaStreamEngine service

### Configure Matterhorn to distribute streams to Wowza
1. In `etc/config.properties`:
    * Set `org.opencastproject.streaming.directory=[wowza-root]/avalon`
    * Comment out `org.opencastproject.hls.directory`
2. Restart matterhorn

### Configure Avalon to stream from Wowza
1. Add this line to your Avalon server's Gemfile:

        gem 'avalon-wowza'
       
2. Execute:

        $ bundle install
       
3. Update the `streaming` configuration in `config/avalon.yml`:

        streaming:
          server: :wowza
          stream_token_ttl: 20
          rtmp_base: rtmp://[wowza-hostname]/avalon
          http_base: http://[wowza-hostname]:1935/avalon/_definst_

4. Restart Avalon 

## Development

This [project](https://github.com/avalonmediasystem/wowza-avalon) was created
and maintained using the [Wowza Integrated Development Environment](http://www.wowza.com/streaming/developers)
for [Eclipse](http://eclipse.org/). Please refer to Wowza's documentation to install
and configure the IDE.
