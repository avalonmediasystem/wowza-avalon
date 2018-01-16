package org.avalonmediasystem.security.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.amf.AMFDataObj;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.client.IClient;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.request.RequestFunction;

public class AvalonSecurity extends ModuleBase {

  public String defaultUrl = "http://localhost:3000/";
  private ArrayList<URL> avalonUrls = new ArrayList<URL>();

  // TODO check with team whether the current logging contains sensitive info that shall be removed
  
  public void onAppStart(IApplicationInstance appInstance) {
    String urlprop = appInstance.getProperties().getPropertyStr("avalonUrls", defaultUrl);
    String[] urlarr = null;
    if (!StringUtils.isEmpty(urlprop)) {
    	urlarr = urlprop.split(","); // the list of Avalon URLs separated by ","
    }
    if (urlarr == null || urlarr.length == 0) {
        getLogger().error("Unable to initialize Avalon security module without avalonUrls defined.");
    	return;
    }

    try {
    	for (String urlstr : urlarr) {
    		avalonUrls.add(new URL(urlstr));
    	}
    	getLogger().info("Initialized Avalon security module with allowed URLs: " + avalonUrls);
    } catch(MalformedURLException err) {
      getLogger().error("Unable to initialize Avalon security module with invalid avalonUrls: " + urlarr, err);
    }
  }

  private List<String> authStream(URL baseAuthUrl, String authToken) {
    List<String> authorized = new ArrayList<String>();
    URL authUrl;

    if (baseAuthUrl == null || authToken == null) {
    	return authorized;
    }
    
    // check if the referrer URL is on the avalonUrls white-list
    if (!avalonUrls.contains(baseAuthUrl)) {
    	getLogger().debug("The referer's URL " + baseAuthUrl + " is not on the Avalon URLs white-list.");
    	return authorized;
    }
    
    try {
      authUrl = new URL(baseAuthUrl, "authorize?token="+authToken);
    } catch(MalformedURLException err) {
      getLogger().error("Error generating authUrl from baseAuthUrl " + baseAuthUrl + " and authToken " + authToken, err);
      return authorized;
    }

    getLogger().debug("Authorizing against " + authUrl.toString());
    try {
      HttpURLConnection http = (HttpURLConnection)authUrl.openConnection();
      http.addRequestProperty("Accept", "text/plain");
      http.setRequestMethod("GET");
      http.connect();
      if (http.getResponseCode() == 202 ) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
        String authorizedStream = reader.readLine();
        while (authorizedStream != null) {
          authorized.add(authorizedStream);
          authorizedStream = reader.readLine();
        }
        getLogger().debug("Authorized to streams " + authorized.toString());
      }
      return authorized;
    } catch (IOException err) {
      getLogger().error("Error connecting to " + authUrl.toString(), err);
      return authorized;
    }
  }

  private String getAuthToken(String source) {
    if (source != null) {
      String[] parts = source.split("\\?");
      String query = parts[parts.length-1];
      List<NameValuePair> httpParams = URLEncodedUtils.parse(query,Charset.defaultCharset());
      for (NameValuePair param:httpParams) {
        if (param.getName().equals("token")) {
          getLogger().debug("Retrieved token " + param.getValue() + " from source " + source);
          return param.getValue();
        }
      }
    }
    return null;
  }
  
  private URL getBaseAuthUrl(String referer) {
	  URL baseAuthUrl = null;
	  try {
		  URL refUrl = new URL(referer);
		  baseAuthUrl = new URL(refUrl.getProtocol(), refUrl.getHost(), refUrl.getPort(), "");
          getLogger().debug("Retrieved baseAuthUrl " + baseAuthUrl + " from referer " + referer);
	  }
	  catch(MalformedURLException err) {
		  getLogger().error("Invalid referer's URL passed in: " + referer);
	  }	  
	  return baseAuthUrl;
  }

  public void onConnect(IClient client, RequestFunction function, AMFDataList params) {
    AMFDataObj connectObj = (AMFDataObj)params.get(2);
    String appName = connectObj.get("app").toString();
    String authToken = getAuthToken(appName);
    URL baseAuthUrl = getBaseAuthUrl(client.getReferrer());
    String authorized = StringUtils.join(authStream(baseAuthUrl, authToken), ";");
    getLogger().info("StreamReadAccess: " + authorized);
    client.setStreamReadAccess(authorized);
  }

  public void onHTTPSessionCreate(IHTTPStreamerSession httpSession) {
    getLogger().info("onHTTPSessionCreate: " + httpSession.getSessionId());
    String query = httpSession.getQueryStr();
    String authToken = getAuthToken(query);
    URL baseAuthUrl = getBaseAuthUrl(httpSession.getReferrer());
    
    List<String> authorized = authStream(baseAuthUrl, authToken);
    if (authorized.isEmpty()) {
      httpSession.rejectSession();
      getLogger().info("No stream is accessible. ");
      return;
    }
    String streamName = httpSession.getStreamName();

    for (String authorizedStream:authorized) {
      getLogger().info("Testing " + authorizedStream + " against " + streamName);
      if ((authorized != null) && streamName.startsWith(authorizedStream)) {
        httpSession.acceptSession();
        getLogger().info("The requested is accepted. ");
        return;
      }
    }
    getLogger().info("The request is rejected. ");
    httpSession.rejectSession();
  }
}
