/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.status;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author nosoop
 */
public class TradeAssetsObj {

	int appid;
	long contextid;
	long assetid;

	long amount;

	TradeAssetsObj(JSONObject obj) throws JSONException {
		this.appid = Integer.parseInt(obj.getString("appid"));
		this.contextid = Long.parseLong(obj.getString("contextid"));
		this.assetid = Long.parseLong(obj.getString("assetid"));

		if (obj.has("amount")) {
			this.amount = Long.parseLong(obj.getString("amount"));
		}
	}

}
