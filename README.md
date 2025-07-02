# wowza-avalon
This project implements a Server-Side Module to bridge between the 
[Wowza Streaming Engine](http://www.wowza.com/products/streaming-engine) and
the [Avalon Media System](http://avalonmediasystem.org). It provides the logic 
and configuration necessary for Wowza to pass a token back to a running Avalon 
server to authorize stream playback.

## Note
This module is tested working in Wowza 4.9.4 (recommended) with Java 21.

## Instructions

### <a href="#wowza-docker-install"></a>Install and configure Wowza Streaming Engine (Docker with [avalon-docker](https://github.com/avalonmediasystem/avalon-docker))
1. Add wowza and wowza-manager services to docker-compose.yml (based on [wowza trial docker-compose documentation](https://www.wowza.com/docs/trial-wowza-streaming-engine-using-a-docker-compose-deployment#3-download-the-docker-compose-yaml-file)).  Be sure to fill in your license key and comment out the hls service.
```
  wowza:
    image: wowza/wowza-streaming-engine:latest
    environment:
      - WSE_LICENSE_KEY=<trial-license-key>
      - ADMIN_USER=admin
      - ADMIN_PASSWORD=password
      - IPWHITELIST=*
      - LOG_LEVEL=INFO #DEBUG INFO WARN ERROR
    #to persist the configurations between starts of Wowza Streaming Engine, uncomment the lines below:
    volumes:
      - ./wse/applications:/usr/local/WowzaStreamingEngine/applications
      - ./wse/conf:/usr/local/WowzaStreamingEngine/conf
      - ./wse/content:/usr/local/WowzaStreamingEngine/content
      - ./wse/lib.addon:/usr/local/WowzaStreamingEngine/lib.addon
      - ./wse/logs:/usr/local/WowzaStreamingEngine/logs
    ports:
      - 8087:8087
      - 80:80
      - 443:443
      - 1935:1935

  wowza-manager:
    image: wowza/wowza-streaming-engine-manager:latest
    environment:
      - LOG_LEVEL=INFO #DEBUG INFO WARN ERROR
    ports:
      - 8088:8080
```
2. Make directory for mounted volumes: `mkdir wse wse/lib.addon`
3. Copy `AvalonSecurity.jar` from this repository into the `wse/lib.addon` directory.
4. Start wowza and wowza-manager: `docker-compose up wowza wowza-manager`

### Install and configure Wowza Streaming Engine (Standalone)
1. [Download and install Wowza Streaming Engine](https://www.wowza.com/docs/how-to-install-and-configure-wowza-streaming-engine)
2. Copy AvalonSecurity.jar into the Wowza lib directory (`/Library/WowzaStreamingEngine/lib` on OS X, or 
`/usr/local/WowzaStreamingEngine/lib` on Linux)
3. Start Wowza and navigate to the Wowza Manager

### <a href="#setup-wowza-app"></a>Setting up a Streaming Application
1. Log into wowza-manger http://localhost:8088
2. Navigate to the Applications top navigation tab and click VOD to start creating the streaming application.
3. Name the new application avalon in the modal and click Add.
4. Fill out the form:
```
Playback types: Apple HLS
Content directory: <path-to-derivatives-directory>
```
5. Click Save
6. Go to the Modules tab, click Edit, then Add Module
7. Fill out the form:
```
Name: AvalonSecurity
Description: Avalon Media System Security Shim 
Fully Qualified Class Name: org.avalonmediasystem.security.module.AvalonSecurity
```
8. Click Add, click Save, then Restart Now
9. Go to Properties tab, click the Custom quick link, then the Edit button
10. Click Add Custom Property and fill in the form:
```
Name: avalonUrls
Value: http://avalon:3000  (<avalon-base-url>)
``` 
11. Click Add, click Save, then Restart Now

### <a href="#setup-wowza-media-cache-app"></a>Setting up a Streaming Application for S3
1. Log into wowza-manger http://localhost:8088
2. Navigate to Media Cache settings (Server tab then click on Media Source in left navigation bar).
3. Click on the Sources tab then Add Media Cache Source
4. Fill in the form:
```
Source name: minio  (Later referenced as <wowza-media-source-name>) 
Source type: amazons3
Prefix: minio/  (Later referenced as <wowza-media-source-prefix>)
Base path: http://minio:9000/derivatives/
```
5. Click Add then the Restart Now button
6. Navigate to the Applications top navigation tab and click VOD Edge to start creating the streaming application.
7. Name the new application avalon in the modal and click Add.
8. Fill out the form:
```
Playback types: Apple HLS
Media Cache Sources: Some and select minio  (<wowza-media-source-name>)
```
9. Click Save
10. Go to the Modules tab, click Edit, then Add Module
11. Fill out the form:
```
Name: AvalonSecurity
Description: Avalon Media System Security Shim 
Fully Qualified Class Name: org.avalonmediasystem.security.module.AvalonSecurity
```
12. Click Add, click Save, then Restart Now
13. Go to Properties tab, click the Custom quick link, then the Edit button
14. Click Add Custom Property and fill in the form:
```
Name: avalonUrls
Value: http://avalon:3000  (<avalon-base-url>)
``` 
15. Click Add Custom Property and fill in the form:
```
Name: pathPrefix
Value: minio/  (<wowza-media-source-prefix>)
``` 
16. Click Add, click Save, then Restart Now


### <a href="#configure-avalon-for-wowza"></a>Configure Avalon to stream from Wowza
1. Update the `streaming` configuration in `config/settings.yml`:
```
        streaming:
          server: :wowza
          content_path: <path to derivatives directory>
          stream_token_ttl: 20
          http_base: http://<wowza-hostname>:1935/avalon/_definst_
```
2. If using S3 Media Cache, edit `wowza` configuration in `config/url_helpers.yml` and add the wowza media source prefix:
```
wowza:
  http:
    video: <%=http_base%>/<%=extension%>:<wowza-media-source-prefix>/<%=path%>/<%=filename%>.<%=extension%>/playlist.m3u8
    audio: <%=http_base%>/<%=extension%>:<wowza-media-source-prefix>/<%=path%>/<%=filename%>.<%=extension%>/playlist.m3u8
```
3. Restart Avalon


## Development

### <a hre="wowza-in-avalon-dev-environment"></a>Setting up Wowza in Avalon docker compose devevelopment environment
1. Get a [Wowza trial license key](https://www.wowza.com/pricing/trial)
2. Add wowza and wowza-manager services to docker-compose.yml using [instructions above](#wowza-docker-install) and adding the `internal` and `external` networks to each.
3. Log into wowza-manger http://localhost:8088 (admin/password) and create streaming application using [instructions above](#setup-wowza-media-cache-app)
4. Configure avalon for wowza using [instructions above](#configure-avalon-for-wowza)
5. Instead of changing settings.yml add environment variables to the `avalon` service.  Note that the HTTP base uses the docker bridge gateway ip of 172.17.0.1.  This allows the browser and avalon to reach wowza.
```
      - SETTINGS__STREAMING__HTTP_BASE=http://172.17.0.1:1935/avalon/_definst_
      - SETTINGS__STREAMING__AUTH_REFERER=http://avalon:3000
      - SETTINGS__STREAMING__SERVER=wowza
```
6. Make sure you have the patch for `lib/avalon/m3u8_reader.rb` in `self.read`:
```
elsif io.is_a?(String)
        if io =~ /^https?:/
          URI.open(io, "Referer" => Settings.streaming.auth_referer || Rails.application.routes.url_helpers.root_url) { |resp| new(resp, Addressable::URI.parse(io), recursive: recursive) }
```
7. Bring up whole stack:
```
docker-compose down
docker-compose up avalon worker wowza wowza-manager
```
8. Go to http://localhost:3000 , login, and create a new collection and work and verifies it streams

### Setting up Wowza IDE and building new jar
1. Setup a directory with Wowza's libraries:
```
mkdir wowza-libraries
docker create --name wowza-temp wowza/wowza-streaming-engine
docker cp wowza-temp:/usr/local/WowzaStreamingEngine/lib.default wowza-libraries/lib/
docker rm wowza-temp
```
2. Follow [Wowza's instructions](https://www.wowza.com/docs/how-to-extend-wowza-streaming-engine-using-the-wowza-ide) to install [Eclipse](http://eclipse.org) and the [Wowza Integrated Development Environment](http://www.wowza.com/streaming/developers).  Check that the version of Java installed works with the version expected by the Wowza Streaming Engine (Currently 21).
3. Trust the Wowza IDE in the modals that popup and restart Eclipse
4. Create a New Wowza Streaming Engine Project: File -> New -> Other... -> Wowza Streaming Engine Project
```
Project Name: AvalonSecurity
Wowza Streaming Engine Location: <path to wowza-libraries directory (not the lib directory itself)>
```
5. Click Next and fill in the next form:
```
Package: org.avalonmediasystem.security.module
Name: AvalonSecurity
```
6. Click Finish
7. Overwrite contents of opened editor with `src/org/avalonmediasystem/security/module/AvalonSecurity.java`
8. Save and should automatically build jar file in wowza-libraries directory
9. Copy `wowza-libraries/lib/AvalonSecurity.jar` to `wse/lib.addon/` in [avalon development environment setup above](#wowza-in-avalon-dev-environment)
8. Restart wowza container to load the new jar
