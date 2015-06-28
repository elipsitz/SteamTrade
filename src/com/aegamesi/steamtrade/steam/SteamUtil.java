package com.aegamesi.steamtrade.steam;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SteamUtil {
	final protected static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	private final static HashMap<String, String> bbCodeMap = new HashMap<String, String>();
	public static String webApiKey = null; // kept in secret.xml

	static {
		//bbCodeMap.put("(\r\n|\r|\n|\n\r)", "<br/>");
		bbCodeMap.put("(?i)\\[b\\](.+?)\\[/b\\]", "<b>$1</b>");
		bbCodeMap.put("(?i)\\[i\\](.+?)\\[/i\\]", "<i>$1</i>");
		bbCodeMap.put("(?i)\\[u\\](.+?)\\[/u\\]", "<u>$1</u>");
		bbCodeMap.put("(?i)\\[h1\\](.+?)\\[/h1\\]", "<h5>$1</h5>");
		bbCodeMap.put("(?i)\\[spoiler\\](.+?)\\[/spoiler\\]", "[SPOILER: $1]");
		bbCodeMap.put("(?i)\\[strike\\](.+?)\\[/strike\\]", "<strike>$1</strike>");
		bbCodeMap.put("(?i)\\[url\\](.+?)\\[/url\\]", "<a href=\"$1\">$1</a>");
		bbCodeMap.put("(?i)\\[url=(.+?)\\](.+?)\\[/url\\]", "<a href=\"$1\">$2</a>");

		// this wonderful piece of regex matches all links that *aren't* already parsed... and parses them.
		bbCodeMap.put("(?i)(?<![\\\"\\'])(?<![\\\"\\']>)((http|https|ftp):\\/\\/[\\w?=&.\\/-;#~%-]+)", "<a href=\"$1\">$1</a>");
		//bbCodeMap.put("(?i)(?<!=\")((?:\\b[a-zA-Z]+:\\/\\/)?[a-zA-Z0-9\\-?&;#~=\\.\\/\\@]+\\.[a-zA-Z]{3,6})", "<a href=\"$1\">$1</a>");
	}

	public static byte[] calculateSHA1(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			return md.digest(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String bytesToHex(byte[] bytes) {
		if (bytes == null)
			return "0000000000000000000000000000000000000000";
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
		Map<String, String> query_pairs = new LinkedHashMap<String, String>();
		String query = url.getQuery();
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		}
		return query_pairs;
	}

	public static String decodeJSString(String str) {
		try {
			return new JSONObject("{str:" + str + "}").getString("str");
		} catch (JSONException e) {
			return "error";
		}
	}

	public static String parseBBCode(String source) {
		source = parseEmoticons(source);

		for (Map.Entry entry : bbCodeMap.entrySet())
			source = source.replaceAll(entry.getKey().toString(), entry.getValue().toString());

		Log.d("BB", source);
		return source;
	}

	public static String parseEmoticons(String source) {
		return source.replaceAll("\u02D0([a-zA-Z_]+)\u02D0", "<img src=\"http://steamcommunity-a.akamaihd.net/economy/emoticon/$1\">").replaceAll("(\r\n|\r|\n|\n\r)", "<br/>");
	}
}
