package com.aegamesi.steamtrade.steam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

// created by aegamesi -- decouple SteamWeb from TradeSession for maximum code reuse
public class SteamWeb {
	/**
	 * The user-agent string to use when making requests.
	 */
	final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36";/*"SteamTrade-Java/1.0 "
				+ "(Windows; U; Windows NT 6.1; en-US; "
                + "Valve Steam Client/1392853084; SteamTrade-Java Client; ) "
                + "AppleWebKit/535.19 (KHTML, like Gecko) "
                + "Chrome/18.0.1025.166 Safari/535.19";*/

	/**
	 * Requests a String representation of an online file (for Steam).
	 *
	 * @param url    Location to fetch.
	 * @param method "GET" or "POST"
	 * @param data   The data to be added to the data stream or request
	 *               params.
	 * @return The server's String response to the request.
	 */
	public static String fetch(String url, String method, Map<String, String> data, String referrer) {

		String cookies = SteamService.generateSteamWebCookies();
		return SteamWeb.request(url, method, data, cookies, referrer == null ? "http://steamcommunity.com/trade/1" : referrer);
	}

	/**
	 * Fetches an api key from /dev/registerkey.
	 *
	 * @param domain "Domain" to send to Steam.
	 * @return The api key, or null if there was an error.
	 */
	public static String requestWebAPIKey(String domain) {
		String page = fetch("https://steamcommunity.com/dev/apikey", "GET", null, "http://steamcommunity.com/dev/");
		String key = parseWebAPIKey(page);
		if (key != null)
			return key;
		// otherwise, we need to register for a key

		Map<String, String> data = new HashMap<String, String>();
		data.put("domain", domain);
		data.put("agreeToTerms", "agreed");
		data.put("Submit", "Register");
		data.put("sessionid", SteamService.singleton.sessionID); // new as of 5/12/2015
		page = fetch("https://steamcommunity.com/dev/registerkey", "POST", data, "https://steamcommunity.com/dev/apikey");
		return parseWebAPIKey(page);
	}

	private static String parseWebAPIKey(String page) {
		Matcher matcher = Pattern.compile("<p>Key: ([0-9A-F]+)</p>").matcher(page);

		String apikey = null;
		if (matcher.find())
			apikey = matcher.group(1);

		return (apikey != null && apikey.length() == 32) ? apikey : null;
	}

	/**
	 * Requests a String representation of an online file (for Steam).
	 *
	 * @param url     Location to fetch.
	 * @param method  "GET" or "POST"
	 * @param data    The data to be added to the data stream or request
	 *                params.
	 * @param cookies A string of cookie data to be added to the request
	 *                headers.
	 * @return The server's String response to the request.
	 */
	public static String request(String url, String method, Map<String, String> data,
								 String cookies, String referrer) {
		boolean ajax = true;
		StringBuilder out = new StringBuilder();
		try {
			String dataString;
			StringBuilder dataStringBuffer = new StringBuilder();
			if (data != null) {
				for (Map.Entry<String, String> entry : data.entrySet()) {
					dataStringBuffer.append(
							URLEncoder.encode(entry.getKey(), "UTF-8"))
							.append("=").append(
							URLEncoder.encode(entry.getValue(), "UTF-8"))
							.append("&");
				}
			}
			dataString = dataStringBuffer.toString();
			if (!method.equals("POST") && dataString.length() > 0) {
				url += "?" + dataString;
			}
			final URL url2 = new URL(url);
			final HttpURLConnection conn =
					(HttpURLConnection) url2.openConnection();
			conn.setRequestProperty("Cookie", cookies);
			conn.setRequestMethod(method);
			System.setProperty("http.agent", "");

			conn.setRequestProperty("User-Agent", USER_AGENT);

			conn.setRequestProperty("Host", "steamcommunity.com");
			conn.setRequestProperty("Content-type",
					"application/x-www-form-urlencoded; charset=UTF-8");
			conn.setRequestProperty("Accept",
					"text/javascript, text/hml, "
							+ "application/xml, text/xml, */*");

			/**
			 * Turns out we need a referer, otherwise we get an error from
			 * the server. Just use the trade URL as one since we have it on
			 * hand, and it's been known to work.
			 *
			 * http://steamcommunity.com/trade/1 was used for other
			 * libraries, but having a hardcoded thing like that is gross.
			 */
			conn.setRequestProperty("Referer", referrer);

			// Accept compressed responses.  (We can decompress it.)
			conn.setRequestProperty("Accept-Encoding", "gzip,deflate");

			if (ajax) {
				conn.setRequestProperty("X-Requested-With",
						"XMLHttpRequest");
				conn.setRequestProperty("X-Prototype-Version", "1.7");
			}

			if (method.equals("POST")) {
				conn.setDoOutput(true);
				try {
					OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
					os.write(dataString.substring(0,
							dataString.length() - 1));
					os.flush();
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			java.io.InputStream netStream = conn.getInputStream();

			// If GZIPped response, then use the gzip decoder.
			if (conn.getContentEncoding() != null) {
				if (conn.getContentEncoding().contains("gzip")) {
					netStream = new GZIPInputStream(netStream);
				} else if (conn.getContentEncoding().contains("deflate")) {
					netStream = new InflaterInputStream(netStream, new Inflater(true));
				}
			}

			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(netStream));
				String line; // Stores the currently read line.
				while ((line = reader.readLine()) != null) {
					if (out.length() > 0) {
						out.append('\n');
					}
					out.append(line);
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return out.toString();
	}
}
