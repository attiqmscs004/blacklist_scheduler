package com.blacklist.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class HttpUtil {
	private static final Logger logger = Logger.getLogger(HttpUtil.class);

	public static String[] executePostJsonRequest(String url,
			Map<String, Object> params, String jsonString) {
		Map<String, Object> bodyContents = new HashMap<String, Object>();
		if (jsonString == null) {
			jsonString = "";
		}
		bodyContents.put("json", jsonString);
		String contentType = "application/json";
		return executePostRequest(url, params, bodyContents, contentType);
	}

	public static String[] executePostTextRequest(String url,
			Map<String, Object> params, String text) {
		logger.info("executePostTextRequest Post");

		Map<String, Object> bodyContents = new HashMap<String, Object>();
		if (text == null) {
			text = "";
		}
		bodyContents.put("text", text);
		String contentType = "";
		return executePostRequest(url, params, bodyContents, contentType);
	}

	public static String[] executePostFormDataRequest(String url,
			Map<String, Object> params, Map<String, Object> formFields) {
		String contentType = "application/x-www-form-urlencoded";
		return executePostRequest(url, params, formFields, contentType);
	}

	private static String[] executePostRequest(String url,
			Map<String, Object> params, Map<String, Object> bodyContents,
			String contentType) {
		String[] responseArray = new String[2];
		try {
			logger.info("executePostRequest Post");
			HttpClient httpClient = HttpClientBuilder.create().build(); // Use
																		// this
																		// instead
																		// :
																		// DefaultHttpClient
																		// is
																		// deprecated
			logger.info("after http client creation");

			if (params != null && params.size() > 0) {
				url += "?";
				Set<Entry<String, Object>> entrySet = params.entrySet();
				if (entrySet != null) {
					for (Entry<String, Object> entry : entrySet) {
						try {
							if (entry.getValue() != null) {
								url += entry.getKey()
										+ "="
										+ URLEncoder.encode(entry.getValue()
												.toString(), "UTF-8") + "&";
							}
						} catch (UnsupportedEncodingException e) {
							logger.info("Exception while adding params to URL. Ex="
									+ e.getMessage());
						} catch (NullPointerException e) {
							logger.info("Exception while adding params to URL. Ex="
									+ e.getMessage());
						}
					}
				}
			}

			logger.info("Before Post");
			HttpPost request = new HttpPost(url);

			logger.info("After Post");

			if (contentType != null && !contentType.isEmpty()) {
				request.addHeader("content-type", contentType);
			}

			if (bodyContents != null && bodyContents.size() > 0) {
				if (contentType.equals("application/json")) { // "application/json"
					StringEntity body = new StringEntity(bodyContents.get(
							"json").toString());
					request.setEntity(body);
				} else if (contentType
						.equals("application/x-www-form-urlencoded")) { // "application/x-www-form-urlencoded"
					Set<String> keys = bodyContents.keySet();
					if (keys != null) {
						List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
						for (String key : keys) {
							// StringEntity body = new StringEntity(key + "=" +
							// params.get(key).toString());
							// request.setEntity(body);
							urlParameters.add(new BasicNameValuePair(key,
									params.get(key).toString()));

						}
						request.setEntity(new UrlEncodedFormEntity(
								urlParameters));
					}
				} else {
					if (bodyContents.containsKey("text")) {
						logger.info("Getting body text");
						StringEntity text = new StringEntity(bodyContents.get(
								"text").toString());

						request.setEntity(text);
					}

				}
			}
			logger.info("Executing URL " + url);
			HttpResponse response = httpClient.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			String responseString = EntityUtils.toString(entity, "UTF-8");
			logger.info(responseString);
			responseArray[0] = "" + statusCode;
			responseArray[1] = responseString;
		} catch (Exception ex) {
			logger.info(ex.getMessage());
			responseArray[0] = "-100";
			responseArray[1] = ex.getMessage();
		} finally {
			// httpClient.getConnectionManager().shutdown(); //Deprecated
		}

		return responseArray;
	}

}