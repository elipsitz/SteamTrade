package com.aegamesi.steamtrade.steam.steamweb;

import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import uk.co.thomasc.steamkit.util.crypto.RSACrypto;

public class SteamWeb {

	public static String request(String url, String method, Map<String, String> data, String cookies) {
		return SteamWeb.request(url, method, data, cookies, true);
	}

	public static String request(String url, String method, Map<String, String> data, String cookies, boolean ajax) {
		String out = "";
		try {
			String dataString = "";
			if (data != null) {
				for (final String key : data.keySet()) {
					dataString += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(data.get(key), "UTF-8") + "&";
				}
			}
			if (!method.equals("POST")) {
				url += "?" + dataString;
			}
			final URL url2 = new URL(url);
			final HttpURLConnection conn = (HttpURLConnection) url2.openConnection();
			conn.setRequestProperty("Cookie", cookies);
			conn.setRequestMethod(method);
			System.setProperty("http.agent", "");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.17 Safari/537.36");
			conn.setRequestProperty("Host", "steamcommunity.com");
			conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
			conn.setRequestProperty("Accept", "text/javascript, text/hml, application/xml, text/xml, */*");
			conn.setRequestProperty("Referer", "http://steamcommunity.com/trade/1");

			if (ajax) {
				conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
				conn.setRequestProperty("X-Prototype-Version", "1.7");
			}

			if (method.equals("POST")) {
				conn.setDoOutput(true);
				final OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
				os.write(dataString);
				os.flush();
			}

			// Log.d("SteamWeb", "Data: " + dataString);
			// Log.d("SteamWeb", "Cookie: " + cookies);
			// Log.d("SteamWeb", "Response code: " + conn.getResponseCode());

			// cookies = conn.getHeaderField("Set-Cookie");
			final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				if (out.length() > 0) {
					out += "\n";
				}
				out += line;
			}

		} catch (final MalformedURLException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return out;
	}

	/**
	 * Executes the login by using the Steam Website.
	 */
	public static String doLogin(String username, String password) {
		Gson gson = new Gson();
		Map<String, String> data = new HashMap<String, String>();
		data.put("username", username);
		final String response = SteamWeb.request("https://steamcommunity.com/login/getrsakey", "POST", data, null, false);
		final GetRsaKey rsaJSON = gson.fromJson(response, GetRsaKey.class);

		// Validate
		if (!rsaJSON.success)
			return null;

		// RSA Encryption
		final RSACrypto rsa = new RSACrypto(new BigInteger(1, SteamWeb.hexToByte(rsaJSON.publickey_exp)), new BigInteger(1, SteamWeb.hexToByte(rsaJSON.publickey_mod)), false);

		final byte[] encodedPassword = rsa.encrypt(password.getBytes());
		final String encryptedBase64Password = Base64.encodeToString(encodedPassword, Base64.DEFAULT);

		SteamResult loginJson = null;
		// String cookies;
		do {
			Log.d("SteamWeb", "Logging In...");

			final boolean captcha = loginJson != null && loginJson.captcha_needed == true;

			String time = "";
			String capGID = "";
			try {
				time = URLEncoder.encode(rsaJSON.timestamp, "UTF-8");
				capGID = loginJson == null ? null : URLEncoder.encode(loginJson.captcha_gid, "UTF-8");
			} catch (final UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

			data = new HashMap<String, String>();
			data.put("password", encryptedBase64Password);
			data.put("username", username);
			data.put("emailauth", "");

			// Captcha
			String capText = "";
			if (captcha) {
				Log.e("SteamWeb", "CAPTCHA");
				return null;
			}

			data.put("captcha_gid", captcha ? capGID : "");
			data.put("captcha_text", captcha ? capText : "");
			// Captcha end

			data.put("emailsteamid", "");
			data.put("rsatimestamp", time);

			final String webResponse = SteamWeb.request("https://steamcommunity.com/login/dologin/", "POST", data, null, false);
			loginJson = gson.fromJson(webResponse, SteamResult.class);

			// cookies = webResponse.cookies;
		} while (loginJson.captcha_needed == true);

		if (loginJson.success == true) {
			// submitCookies(cookies);
			// return cookies;
		} else {
			Log.e("SteamWeb", loginJson.message);
		}
		return null;
	}

	static void submitCookies(String cookies) {
		try {
			final URL url2 = new URL("https://steamcommunity.com/");
			final HttpURLConnection conn = (HttpURLConnection) url2.openConnection();
			conn.connect();
			conn.setRequestProperty("Cookie", cookies);
			conn.setRequestMethod("POST");
			System.setProperty("http.agent", "");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.100 Safari/534.30");
			conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");

			final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			reader.readLine();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	static byte[] hexToByte(String hex) {
		if (hex.length() % 2 == 1) {
			Log.e("SteamWeb", "The binary key cannot have an odd number of digits");
			return new byte[0];
		}

		final byte[] arr = new byte[hex.length() >> 1];
		final int l = hex.length();

		for (int i = 0; i < l >> 1; ++i) {
			arr[i] = (byte) ((SteamWeb.getHexVal(hex.charAt(i << 1)) << 4) + SteamWeb.getHexVal(hex.charAt((i << 1) + 1)));
		}

		return arr;
	}

	static int getHexVal(char hex) {
		final int val = hex;
		return val - (val < 58 ? 48 : 55);
	}

	// public static bool ValidateRemoteCertificate(object sender,
	// X509Certificate certificate, X509Chain chain, SslPolicyErrors
	// policyErrors) {
	// allow all certificates
	// return true;
	// }
}