package com.aegamesi.steamtrade.steam;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class SteamUtil {
	public static String apikey = ""; // kept in secret.xml
	public final static int colorGame = Color.parseColor("#AED04E");
	public final static int colorOnline = Color.parseColor("#9CC6FF");
	public final static int colorOffline = Color.parseColor("#CFD2D3");
	public final static long recentChatThreshold = 7 * 24 * 60 * 60 * 1000; // 7 days

	public static byte[] calculateSHA1(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			return md.digest(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	final protected static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

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

	public static void disconnectWithDialog(final Context context, final String message) {
		class SteamDisconnectTask extends AsyncTask<Void, Void, Void> {
			private ProgressDialog dialog;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				dialog = new ProgressDialog(context);
				dialog.setCancelable(false);
				dialog.setMessage(message);
				dialog.show();
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				try {
					dialog.dismiss();
				} catch (IllegalArgumentException e) {
				}
			}

			@Override
			protected Void doInBackground(Void... params) {
				SteamService.singleton.steamClient.disconnect();
				return null;
			}
		}
		new SteamDisconnectTask().execute();
	}

	public static void copyToClipboard(Context context, String str) {
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(str);
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData.newPlainText("Chat Line", str);
			clipboard.setPrimaryClip(clip);
		}
	}

	public static long convertSteamIdToCommunityId(String steamId) {
		if (!steamId.matches("^STEAM_[0-1]:[0-1]:[0-9]+$"))
			return -1;
		String[] tmpId = steamId.substring(6).split(":");
		return Long.valueOf(tmpId[1]) + Long.valueOf(tmpId[2]) * 2 + 76561197960265728L;
	}

	public static String generateCommunityURL(SteamID id) {
		String url = "http://steamcommunity.com/profiles/"; //76561198000739785
		url += SteamUtil.convertSteamIdToCommunityId(id.render());
		return url;
	}

	public static String decodeJSString(String str) {
		try {
			return new JSONObject("{str:" + str + "}").getString("str");
		} catch (JSONException e) {
			return "error";
		}
	}
}
