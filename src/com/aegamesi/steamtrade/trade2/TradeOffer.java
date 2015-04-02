package com.aegamesi.steamtrade.trade2;

import android.util.Log;

import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.aegamesi.steamtrade.steam.SteamWeb;
import com.nosoop.steamtrade.ContextScraper;
import com.nosoop.steamtrade.TradeSession;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.AssetBuilder;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class TradeOffer {
	private String sessionID;
	public String partnerName;
	public SteamID partnerID;
	private long tradeID = 1;
	private String inventoryLoadUrl;
	private String partnerInventoryLoadUrl;
	public List<AppContextPair> appContextData;
	public List<AppContextPair> partnerAppContextData;
	public String message = null;
	public boolean newOffer = false;
	public boolean counterOffer = false;

	public int trade_version;
	public String accessToken = null;

	public TradeSession.TradeUser TRADE_USER_SELF;
	public TradeSession.TradeUser TRADE_USER_PARTNER;

	protected TradeOffer() {
		TRADE_USER_SELF = new TradeSession.TradeUser(SteamService.singleton.steamClient.getSteamId().convertToLong(), new ArrayList<AssetBuilder>());
		TRADE_USER_PARTNER = new TradeSession.TradeUser(0L, new ArrayList<AssetBuilder>());
	}

	// create a new offer, given an account ID and token (for non friends)
	public static TradeOffer createNewOffer(long user, String token) {
		TradeOffer offer = new TradeOffer();
		offer.newOffer = true;
		offer.tradeID = 0;
		offer.accessToken = token;
		String url = "https://steamcommunity.com/tradeoffer/new/?partner=" + user;
		if (token != null && token.trim().length() > 0)
			url += "&token=" + token;
		offer.loadInformation(url);

		return offer;
	}

	// load from an existing offer (like, to view or counteroffer)
	public static TradeOffer loadFromExistingOffer(long id) {
		TradeOffer offer = new TradeOffer();
		offer.newOffer = false;
		offer.tradeID = id;
		String url = "https://steamcommunity.com/tradeoffer/" + id;
		offer.loadInformation(url);

		return offer;
	}

	private void loadInformation(String url) {
		// lots of scraping here
		String html = SteamWeb.fetch(url, "GET", null, "http://steamcommunity.com/my/tradeoffers");
		Pattern pattern = Pattern.compile("^\\s*var\\s+(g_.+?)\\s+=\\s+(.+?);\\r?$", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(html);
		Map<String, String> javascriptGlobals = new HashMap<String, String>();
		while (matcher.find())
			javascriptGlobals.put(matcher.group(1), matcher.group(2));

		// Now, use the javascript globals to populate our offer
		try {
			appContextData = ContextScraper.parseContextData(javascriptGlobals.get("g_rgAppContextData"));
			partnerAppContextData = ContextScraper.parseContextData(javascriptGlobals.get("g_rgPartnerAppContextData"));
		} catch (Exception e) {
			e.printStackTrace(); // XXX maybe throw error?
		}
		partnerID = new SteamID(Long.parseLong(SteamUtil.decodeJSString(javascriptGlobals.get("g_ulTradePartnerSteamID"))));
		partnerName = SteamUtil.decodeJSString(javascriptGlobals.get("g_strTradePartnerPersonaName"));
		sessionID = SteamUtil.decodeJSString(javascriptGlobals.get("g_sessionID"));
		inventoryLoadUrl = SteamUtil.decodeJSString(javascriptGlobals.get("g_strInventoryLoadURL"));
		partnerInventoryLoadUrl = SteamUtil.decodeJSString(javascriptGlobals.get("g_strTradePartnerInventoryLoadURL"));
		//https://steamcommunity.com/tradeoffer/341993026/partnerinventory/

		try {
			JSONObject tradeStatus = new JSONObject(javascriptGlobals.get("g_rgCurrentTradeStatus"));
			trade_version = tradeStatus.getInt("version");
			loadUserAssets(TRADE_USER_SELF, tradeStatus.getJSONObject("me").getJSONArray("assets"));
			loadUserAssets(TRADE_USER_PARTNER, tradeStatus.getJSONObject("them").getJSONArray("assets"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadUserAssets(TradeSession.TradeUser user, JSONArray assets) throws JSONException {
		for (int i = 0; i < assets.length(); i++) {
			JSONObject obj = assets.getJSONObject(i);
			int appid = Integer.parseInt(obj.getString("appid"));
			long contextid = Long.parseLong(obj.getString("contextid"));
			long assetid = Long.parseLong(obj.getString("assetid"));
			long amount = obj.has("amount") ? Long.parseLong(obj.getString("amount")) : 1;
			AppContextPair acp = new AppContextPair(appid, contextid);

			if (!user.getInventories().hasInventory(acp)) {
				if (user == TRADE_USER_SELF)
					loadOwnInventory(acp);
				if (user == TRADE_USER_PARTNER)
					loadPartnerInventory(acp);
			}

			TradeInternalAsset asset = null;
			if (user.getInventories().getInventory(acp) != null)
				asset = user.getInventories().getInventory(acp).getItem(assetid);
			if (asset != null)
				user.getOffer().add(asset);
			else
				Log.e("TradeOffer", "Error loading item: (" + appid + "," + contextid + ")|" + assetid);
		}
	}

	public void loadOwnInventory(AppContextPair appContext) {
		final String url, response;

		if (TRADE_USER_SELF.getInventories().hasInventory(appContext))
			return;

		url = inventoryLoadUrl + appContext.getAppid() + "/" + appContext.getContextid() + "/?trading=1";
		response = SteamWeb.fetch(url, "GET", null, "https://steamcommunity.com/tradeoffer/" + tradeID);

		try {
			JSONObject responseObject = new JSONObject(response);
			TRADE_USER_SELF.getInventories().addInventory(appContext, responseObject);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadPartnerInventory(AppContextPair appContext) {
		final String url, response;

		if (TRADE_USER_PARTNER.getInventories().hasInventory(appContext))
			return;

		Map<String, String> data = new HashMap<String, String>();
		data.put("sessionid", SteamService.singleton.sessionID.trim());
		data.put("partner", Long.toString(partnerID.convertToLong()));
		data.put("appid", Long.toString(appContext.getAppid()));
		data.put("contextid", Long.toString(appContext.getContextid()));
		response = SteamWeb.fetch(partnerInventoryLoadUrl, "GET", data, "https://steamcommunity.com/tradeoffer/" + tradeID);

		try {
			JSONObject responseObject = new JSONObject(response);
			TRADE_USER_PARTNER.getInventories().addInventory(appContext, responseObject);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public JSONObject send(String message) {
		try {
			trade_version++;

			// create json object
			JSONObject tradeStatus = new JSONObject();
			tradeStatus.put("newversion", true);
			tradeStatus.put("version", trade_version);
			JSONObject userMe = new JSONObject();
			{
				userMe.put("ready", false);
				userMe.put("currency", new JSONArray());
				JSONArray assets = new JSONArray();
				for (TradeInternalAsset asset : TRADE_USER_SELF.getOffer()) {
					JSONObject jsonAsset = new JSONObject();
					jsonAsset.put("appid", asset.getAppid());
					jsonAsset.put("contextid", asset.getContextid() + "");
					jsonAsset.put("assetid", asset.getAssetid() + "");
					jsonAsset.put("amount", asset.getAmount());
					assets.put(jsonAsset);
				}
				userMe.put("assets", assets);
			}
			tradeStatus.put("me", userMe);
			JSONObject userThem = new JSONObject();
			{
				userThem.put("ready", false);
				userThem.put("currency", new JSONArray());
				JSONArray assets = new JSONArray();
				for (TradeInternalAsset asset : TRADE_USER_PARTNER.getOffer()) {
					JSONObject jsonAsset = new JSONObject();
					jsonAsset.put("appid", asset.getAppid());
					jsonAsset.put("contextid", asset.getContextid() + "");
					jsonAsset.put("assetid", asset.getAssetid() + "");
					jsonAsset.put("amount", asset.getAmount());
					assets.put(jsonAsset);
				}
				userThem.put("assets", assets);
			}
			tradeStatus.put("them", userThem);
			Log.d("JSON", tradeStatus.toString());

			Map<String, String> data = new HashMap<String, String>();
			data.put("sessionid", SteamService.singleton.sessionID.trim());
			data.put("partner", Long.toString(partnerID.convertToLong()));
			data.put("serverid", "1");
			data.put("tradeoffermessage", message);
			data.put("json_tradeoffer", tradeStatus.toString());
			if (counterOffer)
				data.put("tradeofferid_countered", "" + tradeID);
			if (accessToken == null)
				data.put("trade_offer_create_params", "{}");
			else
				data.put("trade_offer_create_params", "{\"trade_offer_access_token\": \"" + accessToken + "\"}");

			String response = SteamWeb.fetch("https://steamcommunity.com/tradeoffer/new/send", "POST", data, "https://steamcommunity.com/tradeoffer/" + tradeID);
			Log.d("TradeOffer", response);
			return (response != null && response.length() > 0) ? new JSONObject(response) : null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public JSONObject acceptOffer() {
		try {
			String url = "https://steamcommunity.com/tradeoffer/" + tradeID + "/accept";
			Map<String, String> data = new HashMap<String, String>();
			data.put("sessionid", SteamService.singleton.sessionID.trim());
			data.put("serverid", "1");
			data.put("tradeofferid", tradeID + "");
			String response = SteamWeb.fetch(url, "POST", data, "https://steamcommunity.com/tradeoffer/" + tradeID);
			Log.d("TradeOffer", response);
			return (response != null && response.length() > 0) ? new JSONObject(response) : null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public JSONObject declineOffer() {
		try {
			String url = "https://steamcommunity.com/tradeoffer/" + tradeID + "/decline";
			Map<String, String> data = new HashMap<String, String>();
			data.put("sessionid", sessionID);
			data.put("serverid", "1");
			data.put("tradeofferid", tradeID + "");
			String response = SteamWeb.fetch(url, "POST", data, "https://steamcommunity.com/tradeoffer/" + tradeID);
			Log.d("TradeOffer", response);
			return (response != null && response.length() > 0) ? new JSONObject(response) : null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
