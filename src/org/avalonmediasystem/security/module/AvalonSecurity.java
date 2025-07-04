package org.avalonmediasystem.security.module;

import com.wowza.wms.application.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.httpstreamer.model.*;

public class AvalonSecurity extends ModuleBase {

	public String defaultUrl = "http://localhost:3000/";
	private ArrayList<URL> avalonUrls = new ArrayList<URL>();
	private String pathPrefix;

	public void onAppStart(IApplicationInstance appInstance) {
		String urlprop = appInstance.getProperties().getPropertyStr("avalonUrls", defaultUrl);
		getLogger().debug("Read property avalonUrls from application configuration: " + urlprop);
		String[] urlarr = null;
		if (!StringUtils.isEmpty(urlprop)) {
			urlarr = urlprop.split(","); // the list of Avalon URLs separated by ","
		}
		if (urlarr == null || urlarr.length == 0) {
			getLogger().error("Unable to initialize Avalon security module without avalonUrls defined.");
			return;
		}

		for (String urlstr : urlarr) {
			urlstr = StringUtils.trim(urlstr);
			if (StringUtils.endsWith(urlstr, "/")) {
				urlstr = StringUtils.chop(urlstr);
			}
			try {
				avalonUrls.add(new URI(urlstr).toURL());
			} catch (URISyntaxException | MalformedURLException err) {
				getLogger().error("Skipping invalid URL " + urlstr + " from the allowed Avalon URLs list.", err);
			}
		}

		pathPrefix = appInstance.getProperties().getPropertyStr("pathPrefix", "");
		getLogger().info("Initialized Avalon security module with pathPrefix: " + pathPrefix + " and allowed URLs: " + avalonUrls);
	}

	private List<String> authStream(URL baseAuthUrl, String authToken) {
		List<String> authorized = new ArrayList<String>();
		URL authUrl;

		if (baseAuthUrl == null || authToken == null) {
			return authorized;
		}

		// check if the referrer URL is on the avalonUrls white-list
		if (!avalonUrls.contains(baseAuthUrl)) {
			getLogger().info("The referer's URL " + baseAuthUrl + " is not on the Avalon URLs white-list.");
			return authorized;
		}

		try {
			authUrl = new URI(baseAuthUrl + "/authorize?token=" + authToken).toURL();
		} catch (URISyntaxException | MalformedURLException err) {
			getLogger().error(
					"Error generating authUrl from baseAuthUrl " + baseAuthUrl + " and authToken " + authToken, err);
			return authorized;
		}

		getLogger().info("Authorizing against " + authUrl.toString());
		try {
			HttpURLConnection http = (HttpURLConnection) authUrl.openConnection();
			http.addRequestProperty("Accept", "text/plain");
			http.setRequestMethod("GET");
			http.connect();
			if (http.getResponseCode() == 202) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
				String authorizedStream = reader.readLine();
				while (authorizedStream != null) {
					authorized.add(authorizedStream);
					authorizedStream = reader.readLine();
				}
				getLogger().info("Authorized to streams " + authorized.toString());
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
			String query = parts[parts.length - 1];
			List<NameValuePair> httpParams = URLEncodedUtils.parse(query, Charset.defaultCharset());
			for (NameValuePair param : httpParams) {
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
			getLogger().debug("getBaseAuthUrl: " + referer);
			URL refUrl = new URI(referer).toURL();
			baseAuthUrl = new URI(refUrl.getProtocol(), null, refUrl.getHost(), refUrl.getPort(), null, null, null).toURL();
			getLogger().debug("Retrieved baseAuthUrl " + baseAuthUrl + " from referer " + referer);
		} catch (NullPointerException | URISyntaxException | MalformedURLException err) {
			getLogger().error("Invalid referer's URL passed in: " + referer);
		}
		return baseAuthUrl;
	}

	public void onConnect(IClient client, RequestFunction function, AMFDataList params) {
		getLogger().info("onConnect: Client: " + client.getClientId() + ", function: " + function.getType()
				+ ", params: " + params);
		AMFDataObj connectObj = (AMFDataObj) params.get(2);
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

		for (String authorizedStream : authorized) {
			getLogger().info("Testing " + authorizedStream + " against " + streamName + " with possible prefix: " + pathPrefix);
			if ((authorized != null) && (streamName.startsWith(authorizedStream) || streamName.startsWith(pathPrefix + authorizedStream))) {
				httpSession.acceptSession();
				getLogger().info("The requested is accepted. ");
				return;
			}
		}
		getLogger().info("The request is rejected. ");
		httpSession.rejectSession();
	}
}
