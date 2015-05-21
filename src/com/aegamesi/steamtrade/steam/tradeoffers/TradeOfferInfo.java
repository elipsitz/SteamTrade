package com.aegamesi.steamtrade.steam.tradeoffers;

import com.aegamesi.steamtrade.steam.SteamUtil;
import com.aegamesi.steamtrade.steam.SteamWeb;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.AssetBuilder;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nosoop.steamtrade.inventory.TradeInternalInventories;
import com.nosoop.steamtrade.inventory.TradeInternalInventory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

public class TradeOfferInfo {

	/**
	 * The AssetBuilder to use when building the assets for this offer.
	 */
	final AssetBuilder assetBuilder;
	/**
	 * A unique identifier for the trade offer.
	 */
	@Getter
	long tradeofferid;
	/**
	 * Your partner in the trade offer.
	 */
	@Getter
	long accountid_other;
	/**
	 * A message included by the creator of the trade offer
	 */
	@Getter
	String message;
	/**
	 * Unix time when the offer will expire (or expired, if it is in the past)
	 */
	@Getter
	long expiration_time;
	/**
	 * The state of the trade offer. These are the possible values:
	 * <p/>
	 * ETradeOfferState:
	 * Invalid			1	Invalid
	 * Active			2	This trade offer has been sent, neither party has acted on it yet.
	 * Accepted			3	The trade offer was accepted by the recipient and items were exchanged.
	 * Countered		4	The recipient made a counter offer
	 * Expired			5	The trade offer was not accepted before the expiration date
	 * Canceled			6	The sender cancelled the offer
	 * Declined			7	The recipient declined the offer
	 * InvalidItems		8	Some of the items in the offer are no longer available (indicated by the missing flag in the output)
	 * EmailCanceled	10	The receiver cancelled the offer via email
	 */
	@Getter
	ETradeOfferState trade_offer_state;
	/**
	 * Array of CEcon_Asset, items you will give up in the trade (regardless of who created the offer)
	 */
	@Getter
	List<TradeInternalAsset> items_to_give;
	/**
	 * Array of CEcon_Asset, items you will receive in the trade (regardless of who created the offer)
	 */
	@Getter
	List<TradeInternalAsset> items_to_receive;
	/**
	 * Boolean to indicate this is an offer you created.
	 */
	@Getter
	boolean is_our_offer;
	/**
	 * Unix timestamp of the time the offer was sent
	 */
	@Getter
	long time_created;
	/**
	 * Unix timestamp of the tiem the trade_offer_state last changed.
	 */
	@Getter
	long time_updated;
	/**
	 * Boolean to indicate this is an offer automatically created from a real-time trade.
	 */
	@Getter
	boolean from_real_time_trade;

	/**
	 * Takes a JSONObject from IEconService/GetTradeOffer[s]
	 *
	 * @param json         A JSONObject representation of the trade offer to load
	 * @param descriptions A JSONArray containing descriptions of the items in the trade offer.
	 */
	public TradeOfferInfo(JSONObject json, JSONArray descriptions, AssetBuilder builder) {
		items_to_give = new ArrayList<>();
		items_to_receive = new ArrayList<>();

		assetBuilder = (builder == null) ? TradeInternalInventories.DEFAULT_ASSET_BUILDER : builder;

		try {
			parseOffer(json, descriptions);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Attempts to cancel a trade offer via WebAPI/IEconService
	 *
	 * @param tradeofferid The trade offer identifier
	 * @return String result of the webapi call
	 */
	public static String attemptCancelOffer(long tradeofferid) {
		String webapi_url = "https://api.steampowered.com/IEconService/CancelTradeOffer/v1/";
		Map<String, String> data = new HashMap<String, String>();
		data.put("key", SteamUtil.apikey);
		data.put("format", "json");
		data.put("tradeofferid", tradeofferid + "");

		return SteamWeb.fetch(webapi_url, "POST", data, "");
	}

	/**
	 * Attempts to decline a trade offer via WebAPI/IEconService
	 *
	 * @param tradeofferid The trade offer identifier
	 * @return String result of the webapi call
	 */
	public static String attemptDeclineOffer(long tradeofferid) {
		String webapi_url = "https://api.steampowered.com/IEconService/DeclineTradeOffer/v1/";
		Map<String, String> data = new HashMap<String, String>();
		data.put("key", SteamUtil.apikey);
		data.put("format", "json");
		data.put("tradeofferid", tradeofferid + "");

		return SteamWeb.fetch(webapi_url, "POST", data, "");
	}

	/**
	 * Parses a response from IEconService/GetTradeOffers
	 *
	 * @param response The response from the WebAPI
	 * @return A List[TradeOffer>][] with the trade offers (0 -- trade offers sent, 1 -- trade offers received)
	 */
	public static List<TradeOfferInfo>[] parseGetTradeOffers(String response) {
		List<TradeOfferInfo>[] offers = new List[]{new ArrayList<TradeOfferInfo>(), new ArrayList<TradeOfferInfo>()};
		try {
			JSONObject jsonResponse = new JSONObject(response);
			if (jsonResponse.has("response")) {
				jsonResponse = jsonResponse.getJSONObject("response");
				JSONArray descriptions = jsonResponse.optJSONArray("descriptions");
				if (jsonResponse.has("trade_offers_sent")) {
					JSONArray trade_offers_sent = jsonResponse.getJSONArray("trade_offers_sent");
					for (int i = 0; i < trade_offers_sent.length(); i++)
						offers[0].add(new TradeOfferInfo(trade_offers_sent.getJSONObject(i), descriptions, null));
				}
				if (jsonResponse.has("trade_offers_received")) {
					JSONArray trade_offers_received = jsonResponse.getJSONArray("trade_offers_received");
					for (int i = 0; i < trade_offers_received.length(); i++)
						offers[1].add(new TradeOfferInfo(trade_offers_received.getJSONObject(i), descriptions, null));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return offers;
	}

	/**
	 * Helper method to parse out the JSON trade offer format.
	 *
	 * @param json           JSONObject representing offer to be parsed.
	 * @param rgDescriptions JSONObject representing descriptions of items in the offer.
	 * @throws JSONException
	 */
	private void parseOffer(JSONObject json, JSONArray rgDescriptions) throws JSONException {
		tradeofferid = json.getLong("tradeofferid");
		accountid_other = json.getLong("accountid_other");
		message = json.optString("message");
		if (message != null && message.trim().length() == 0)
			message = null;
		expiration_time = json.getLong("expiration_time");
		trade_offer_state = ETradeOfferState.f(json.getInt("trade_offer_state"));
		is_our_offer = json.getBoolean("is_our_offer");
		time_created = json.getLong("time_created");
		time_updated = json.getLong("time_updated");
		from_real_time_trade = json.getBoolean("from_real_time_trade");

		// Convenience map to associate class/instance to description.
		Map<TradeInternalInventory.ClassInstancePair, JSONObject> descriptions = new HashMap<>();
		if (rgDescriptions != null) {
			for (int i = 0; i < rgDescriptions.length(); i++) {
				JSONObject rgDescriptionItem = rgDescriptions.getJSONObject(i);

				int classid = rgDescriptionItem.getInt("classid");
				long instanceid = rgDescriptionItem.getLong("instanceid");

				descriptions.put(new TradeInternalInventory.ClassInstancePair(classid, instanceid), rgDescriptionItem);
			}
		}

		// Add assets to items_to_give and items_to_receive
		parseAssetList(json.optJSONArray("items_to_give"), descriptions, items_to_give);
		parseAssetList(json.optJSONArray("items_to_receive"), descriptions, items_to_receive);
	}

	/**
	 * Helper method to parse asset lists (both normal items and currency)
	 *
	 * @param assetList    A JSONObject of assets to add to the list
	 * @param descriptions A Map of descriptions to use when building assets
	 * @param resultList   The list to insert assets into
	 * @throws JSONException
	 */
	private void parseAssetList(JSONArray assetList, Map<TradeInternalInventory.ClassInstancePair, JSONObject> descriptions, List<TradeInternalAsset> resultList) throws JSONException {
		if (assetList != null) {
			for (int i = 0; i < assetList.length(); i++) {
				JSONObject invInstance = assetList.getJSONObject(i);

				TradeInternalInventory.ClassInstancePair itemCI = new TradeInternalInventory.ClassInstancePair(
						Integer.parseInt(invInstance.getString("classid")),
						Long.parseLong(invInstance.optString("instanceid", "0")));
				AppContextPair itemAC = new AppContextPair(
						Integer.parseInt(invInstance.getString("appid")),
						Integer.parseInt(invInstance.getString("contextid")));

				try {
					TradeInternalAsset generatedAsset = null;
					if (invInstance.has("assetid"))
						generatedAsset = assetBuilder.generateItem(itemAC, invInstance, descriptions.get(itemCI));
					else if (invInstance.has("currencyid"))
						generatedAsset = assetBuilder.generateCurrency(itemAC, invInstance, descriptions.get(itemCI));

					if (generatedAsset != null)
						resultList.add(generatedAsset);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}


	/**
	 * Enum representing possible trade offer states
	 */
	public enum ETradeOfferState {
		Invalid(1), Active(2), Accepted(3), Countered(4), Expired(5), Canceled(6), Declined(7), InvalidItems(8), EmailPending(9), EmailCanceled(10);

		private static HashMap<Integer, ETradeOfferState> values = new HashMap<Integer, ETradeOfferState>();

		static {
			for (final ETradeOfferState type : ETradeOfferState.values()) {
				ETradeOfferState.values.put(type.v(), type);
			}
		}

		private int code;

		ETradeOfferState(int code) {
			this.code = code;
		}

		public static ETradeOfferState f(int code) {
			return ETradeOfferState.values.get(code);
		}

		public int v() {
			return code;
		}
	}
}
