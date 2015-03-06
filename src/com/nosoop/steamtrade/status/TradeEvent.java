/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.status;

import com.nosoop.steamtrade.TradeListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author nosoop
 */
public class TradeEvent {
	public String steamid;
	public int action;
	public long timestamp;
	public int appid;
	public String text;
	public long contextid;
	public long assetid;
	// Currency-based.
	public int amount;
	public long currencyid;
	JSONObject jsonObject;

	public static class TradeAction {
		public static final int // Reference to trade action IDs
				// Item added (itemid = "assetid")
				ITEM_ADDED = 0,
		// Item removed (itemid = "assetid")
		ITEM_REMOVED = 1,
		// Toggle ready
		READY_TOGGLED = 2,
		// Toggle not ready
		READY_UNTOGGLED = 3,
		// Trade accepted
		TRADE_ACCEPTED = 4,
		// ? - maybe some sort of cancel
		UNKNOWN_EVENT_5 = 5,
		// Add / remove currency.
		// (SK Crowns / Energy are, other stackables are not.)
		CURRENCY_CHANGED = 6,
		// Chat (message = "text")
		MESSAGE_ADDED = 7,
		// Update stackable item count?  Initial add uses ITEM_ADDED.
		STACKABLE_CHANGED = 8;
	}

	TradeEvent(JSONObject event) throws JSONException {
		jsonObject = event;

		steamid = event.getString("steamid");
		action = event.optInt("action",
				TradeListener.TradeStatusCodes.TRADEEVENT_ACTION_MISSING);
		timestamp = event.getLong("timestamp");
		appid = event.getInt("appid");
		text = event.optString("text");

		// contextid required for private inventory only.
		contextid = event.optLong("contextid");

		// assetid required for getting item info from public inventory.
		assetid = event.optLong("assetid");

		// Amount is required when dealing in currency
		amount = event.optInt("amount", 1);

		// Currency ID is also required when dealing in currency.
		// Might not be available.
		currencyid = event.optLong("currencyid");
	}

	public JSONObject getJSONObject() {
		return jsonObject;
	}
}