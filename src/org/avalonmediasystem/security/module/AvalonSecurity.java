package org.avalonmediasystem.security.module;

import com.wowza.wms.application.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.httpstreamer.model.*;

import java.io.IOException;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.charset.Charset;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.NameValuePair;

public class AvalonSecurity extends ModuleBase {

  public String defaultUrl = "http://localhost:3000/";
  private URL baseAuthUrl;

  public void onAppStart(IApplicationInstance appInstance) {
    String avalonUrl = appInstance.getProperties().getPropertyStr("avalonUrl",defaultUrl);
    try {
      baseAuthUrl = new URL(avalonUrl);
      getLogger().info("Initialized Avalon security module at " + baseAuthUrl);
    } catch(MalformedURLException err) {
      getLogger().error("Unable to initialize Avalon security module", err);
    }
  }

  private String authStream(String authToken) {
    URL authUrl;

    try {
      authUrl = new URL(baseAuthUrl, "authorize?token="+authToken);
    } catch(MalformedURLException err) {
      getLogger().error("Error parsing URL", err);
      return null;
    }

    getLogger().debug("Authorizing against " + authUrl.toString());
    try {
      HttpURLConnection http = (HttpURLConnection)authUrl.openConnection();
      http.addRequestProperty("Accept", "text/plain");
      http.setRequestMethod("GET");
      http.connect();
      if (http.getResponseCode() != 202 ) {
        return null;
      } else {
        BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
        String authorized = reader.readLine().trim();
        getLogger().debug("Authorized to stream " + authorized);
        return authorized;
      }
    } catch (IOException err) {
      getLogger().error("Error connecting to " + authUrl.toString(), err);
      return null;
    }
  }

  private String getAuthToken(String source) {
    if (source != null) {
      String[] parts = source.split("\\?");
      String query = parts[parts.length-1];
      List<NameValuePair> httpParams = URLEncodedUtils.parse(query,Charset.defaultCharset());
      for (NameValuePair param:httpParams) {
        if (param.getName().equals("token")) {
          return param.getValue();
        }
      }
    }
    return null;
  }

  public void onConnect(IClient client, RequestFunction function,
      AMFDataList params) {
    AMFDataObj connectObj = (AMFDataObj)params.get(2);
    String appName = connectObj.get("app").toString();
    String authToken = getAuthToken(appName);
    String authorized = authStream(authToken).replace(" ", ";");
    getLogger().info("StreamReadAccess: " + authorized);
    client.setStreamReadAccess(authorized);
  }

  public void onHTTPSessionCreate(IHTTPStreamerSession httpSession) {
    getLogger().info("onHTTPSessionCreate: " + httpSession.getSessionId());
    String query = httpSession.getQueryStr();
    String authToken = getAuthToken(query);

    String authResponse = authStream(authToken);
    if (authResponse == null) {
      httpSession.rejectSession();
      return;
    }
    
    String[] authorized = authResponse.split(" ");
    String streamName = httpSession.getStreamName();

    for (String authorizedStream:authorized) {
      getLogger().info("Testing " + authorizedStream + " against " + streamName);
      if ((authorized != null) && streamName.startsWith(authorizedStream)) {
        httpSession.acceptSession();
        return;
      }
    }
    httpSession.rejectSession();
  }
}
