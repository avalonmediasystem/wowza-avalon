# wowza-avalon
This project implements a Server-Side Module to bridge between the 
[Wowza Streaming Engine](http://www.wowza.com/products/streaming-engine) and
the [Avalon Media System](http://avalonmediasystem.org). It provides the logic 
and configuration necessary for Wowza to pass a token back to a running Avalon 
server to authorize stream playback.

## Note
This module is tested working in Wowza 4.7.5 (recommended) and 4.7.3. Currently it doesn't work with Wowza 4.7.8, fixes coming soon.

## Instructions

### Install and configure Wowza Streaming Engine
1. Install Wowza per the [Wowza Streaming Engine User Guide](http://www.wowza.com/resources/WowzaStreamingEngine_UsersGuide.pdf)
2. `cd [wowza-root]` (`/Library/WowzaStreamingEngine` on OS X, or 
`/usr/local/WowzaStreamingEngine` on Linux)
3. `curl -L https://github.com/avalonmediasystem/wowza-avalon/releases/download/v0.3.0/wowza-avalon-app.tar.bz2 | tar xj`
4. `chmod 0777 avalon`
5. In `conf/Application.xml`, edit the [avalonUrls](https://github.com/avalonmediasystem/wowza-avalon/blob/master/conf/Application.xml#L220) to point to the actual URL(s) of your Avalon app(s) 
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
          content_path: <same path as Matterhorn's streaming directory>
          stream_token_ttl: 20
          rtmp_base: rtmp://[wowza-hostname]/avalon
          http_base: http://[wowza-hostname]:1935/avalon/_definst_

4. Restart Avalon

## Development

This [project](https://github.com/avalonmediasystem/wowza-avalon) was created
and maintained using the [Wowza Integrated Development Environment](http://www.wowza.com/streaming/developers)
for [Eclipse](http://eclipse.org/). Please refer to Wowza's documentation to install
and configure the IDE (https://www.wowza.com/docs/how-to-extend-wowza-streaming-engine-using-the-wowza-ide).

To build the project, you need to make sure to define the `WOWZA-LIB` classpath variable on your local system 
(on Windows, it's typically `C:/Program Files (x86)/Wowza Media Systems/Wowza Streaming Engine ${wowza.version}/lib`;
on Mac, it could be `/Library/WowzaStreamingEngine-${wowza.version}/lib/`).

If you make changes to the source code or application settings, make sure to run the ant target `dist` in `build.xml`, 
which will generate the Wowza extension package `wowza-avalon-app.tar.bz2` under `dist/` directory. 
You can then install the extension package into the Wowza Streaming Engine.

Note: There're 2 copies of `Application.xml` in the project. To make the application settings take effect in your extension package, you need to modify `stage/conf/Application.xml`, which is the one being packaged into the bz2 file. 
