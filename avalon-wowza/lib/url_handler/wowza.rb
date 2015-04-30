module UrlHandler
  class Wowza

    def self.patterns
      {
        'rtmp' => {
          'video' => "<%=prefix%>:<%=media_id%>/<%=stream_id%>/<%=filename%>",
          'audio' => "<%=prefix%>:<%=media_id%>/<%=stream_id%>/<%=filename%>",
        },
        'http' => {
          'video' => "<%=prefix%>:<%=media_id%>/<%=stream_id%>/<%=filename%>.<%=extension%>/playlist.m3u8",
          'audio' => "<%=prefix%>:<%=media_id%>/<%=stream_id%>/<%=filename%>.<%=extension%>/playlist.m3u8",
        }
      }
    end

  end
end
