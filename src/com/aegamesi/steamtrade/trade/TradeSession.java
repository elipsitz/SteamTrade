package com.aegamesi.steamtrade.trade;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import uk.co.thomasc.steamkit.types.steamid.SteamID;

import com.aegamesi.steamtrade.steam.steamweb.SteamWeb;
import com.google.gson.Gson;

public class TradeSession {
	public static String steamCommunityDomain = "steamcommunity.com";
	public static String steamTradeUrl = "http://steamcommunity.com/trade/%d/";

	public String sessionIDEsc;
	public String baseTradeURL;
	public int version = 1; // you must increment this every time an item is added or removed
	public int logpos;
	public String cookies;

	public String steamLogin;
	public String sessionID;
	public SteamID otherID;
	public String appID;

	public TradeSession(String sessionID, String steamLogin, SteamID otherID, String appID) {
		this.steamLogin = steamLogin.trim(); // TODO make sure logged on to steam
		this.sessionID = sessionID;
		this.otherID = otherID;
		this.appID = appID;

		try {
			sessionIDEsc = URLDecoder.decode(sessionID.trim(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		cookies = "sessionid=" + sessionID.trim() + ";steamLogin=" + steamLogin;
		baseTradeURL = String.format(steamTradeUrl, otherID.convertToLong());
	}

	public String fetch(String url, String method, Map<String, String> data) {
		return SteamWeb.request(url + "/", method, data, cookies);
	}

	// web commands
	public TradeStatus getStatus() {
		Map<String, String> data = new HashMap<String, String>();
		data.put("sessionid", sessionIDEsc);
		data.put("logpos", "" + logpos);
		data.put("version", "" + version);

		String result = fetch(baseTradeURL + "tradestatus", "POST", data);
		return new Gson().fromJson(result, TradeStatus.class);
	}

	public ForeignInventory getForeignInventory(SteamID otherID, int contextID) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("sessionid", sessionIDEsc);
		data.put("steamid", "" + otherID.convertToLong());
		data.put("appid", "" + appID);
		data.put("contextid", "" + contextID);

		String result = fetch(baseTradeURL + "foreigninventory", "POST", data);
		return ForeignInventory.fromJSON(result);
	}

	public boolean sendMessageWebCommand(String msg) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("sessionid", sessionIDEsc);
		data.put("message", msg);
		data.put("logpos", "" + logpos);
		data.put("version", "" + version);

		String result = fetch(baseTradeURL + "chat", "POST", data);
		SteamWebResponse response = new Gson().fromJson(result, SteamWebResponse.class);
		return response != null && response.success;
	}

	public boolean addItemWebCommand(long itemid, int slot) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("sessionid", sessionIDEsc);
		data.put("appid", appID);
		data.put("contextid", "2"); // ???
		data.put("itemid", "" + itemid);
		data.put("slot", "" + slot);

		String result = fetch(baseTradeURL + "additem", "POST", data);
		SteamWebResponse response = new Gson().fromJson(result, SteamWebResponse.class);
		return response != null && response.success;
	}

	public boolean removeItemWebCommand(long itemid, int slot) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("sessionid", sessionIDEsc);
		data.put("appid", appID);
		data.put("contextid", "2"); // ???
		data.put("itemid", "" + itemid);
		data.put("slot", "" + slot);

		String result = fetch(baseTradeURL + "removeitem", "POST", data);
		SteamWebResponse response = new Gson().fromJson(result, SteamWebResponse.class);
		return response != null && response.success;
	}

	public boolean setReadyWebCommand(boolean ready) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("sessionid", sessionIDEsc);
		data.put("ready", ready ? "true" : "false");
		data.put("version", "" + version);

		String result = fetch(baseTradeURL + "toggleready", "POST", data);
		SteamWebResponse response = new Gson().fromJson(result, SteamWebResponse.class);
		return response != null && response.success;
	}

	public boolean acceptTradeWebCommand() {
		Map<String, String> data = new HashMap<String, String>();
		data.put("sessionid", sessionIDEsc);
		data.put("version", "" + version);

		String result = fetch(baseTradeURL + "confirm", "POST", data);
		SteamWebResponse response = new Gson().fromJson(result, SteamWebResponse.class);
		return response != null && response.success;
	}

	public boolean cancelTradeWebCommand() {
		Map<String, String> data = new HashMap<String, String>();
		data.put("sessionid", sessionIDEsc);

		String result = fetch(baseTradeURL + "cancel", "POST", data);
		SteamWebResponse response = new Gson().fromJson(result, SteamWebResponse.class);
		return response != null && response.success;
	}

	private class SteamWebResponse {
		public boolean success = false;
	}
}