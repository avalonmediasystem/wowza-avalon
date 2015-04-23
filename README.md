# wowza-avalon
This project implements a Server-Side Module to bridge between the 
[Wowza Streaming Engine](http://www.wowza.com/products/streaming-engine) and
the [Avalon Media System](http://avalonmediasystem.org). It provides the logic 
and configuration necessary for Wowza to pass a token back to a running Avalon 
server to authorize stream playback.

## Instructions

### Install and configure Wowza Streaming Engine
1. Install per the [Wowza Streaming Engine User Guide](http://www.wowza.com/resources/WowzaStreamingEngine_UsersGuide.pdf)
2. `cd [wowza-root]`[1]
3. `curl -L https://github.com/avalonmediasystem/wowza-avalon/releases/download/v0.1.0-alpha/wowza-avalon-app.tar.bz2 | tar xj`
4. `chmod 0777 avalon`
5. Restart the WowzaStreamEngine service

[1] The `[wowza-root]` directory is `/Library/WowzaStreamingEngine` on OS X, or 
`/usr/local/WowzaStreamingEngine` on Linux.

### Configure Matterhorn to distribute streams to Wowza
1. In `etc/config.properties`:
    * Set `org.opencastproject.streaming.directory=[wowza-root]/avalon`
    * Comment out `org.opencastproject.hls.directory`
2. Restart matterhorn

### Configure Avalon to stream from Wowza
1. In `config/avalon.yml`:

        streaming:
          server: :wowza
          stream_token_ttl: 20
          rtmp_base: rtmp://[wowza-hostname]/avalon
          http_base: http://[wowza-hostname]:1935/avalon/_definst_

2. Restart Avalon 

## Development

This [project](https://github.com/avalonmediasystem/wowza-avalon) was created
and maintained using the [Wowza Integrated Development Environment](http://www.wowza.com/streaming/developers)
for [Eclipse](http://eclipse.org/). Please refer to Wowza's documentation to install
and configure the IDE.
