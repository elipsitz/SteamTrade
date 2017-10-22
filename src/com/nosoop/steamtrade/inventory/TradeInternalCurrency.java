/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.inventory;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class representation of a currency item in a user's inventory.
 *
 * @author nosoop
 */
public class TradeInternalCurrency extends TradeInternalAsset {
	long currencyid;
	int tradedAmount;

	public TradeInternalCurrency(AppContextPair appContext,
								 JSONObject rgCurrencyItem, JSONObject rgDescriptionItem)
			throws JSONException {
		super(appContext, rgCurrencyItem, rgDescriptionItem);

		if (rgCurrencyItem.has("id"))
			currencyid = Long.parseLong(rgCurrencyItem.getString("id"));
		else if (rgCurrencyItem.has("currencyid"))
			currencyid = Long.parseLong(rgCurrencyItem.getString("currencyid"));

		tradedAmount = 0;
	}

	public long getCurrencyId() {
		return currencyid;
	}

	/**
	 * Returns the amount of currency added to the trade.
	 */
	public int getTradedAmount() {
		return tradedAmount;
	}

	/**
	 * Sets the amount of currency added to the trade.
	 */
	public void setTradedAmount(int amount) {
		tradedAmount = amount;
	}
}
