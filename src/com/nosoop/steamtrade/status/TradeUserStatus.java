package com.nosoop.steamtrade.status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents user status in trade.
 *
 * @author nosoop
 */
public class TradeUserStatus {

	public boolean ready;
	public boolean confirmed;
	public int sec_since_touch;
	public List<TradeAssetsObj> assets;

	TradeUserStatus(JSONObject obj) throws JSONException {
		ready = obj.getLong("ready") == 1;
		confirmed = obj.getLong("confirmed") == 1;
		sec_since_touch = obj.getInt("sec_since_touch");

		// TODO Add asset support to update variable-item quantities.
		JSONArray assetsRef = obj.optJSONArray("assets");

		if (assetsRef != null) {
			assets = new ArrayList<>();

			for (int i = 0; i < assetsRef.length(); i++) {
				JSONObject asset = assetsRef.optJSONObject(i);
				assets.add(new TradeAssetsObj((JSONObject) asset));
			}
		}
	}
}