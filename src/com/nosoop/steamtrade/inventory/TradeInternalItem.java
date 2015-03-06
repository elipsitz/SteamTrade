package com.nosoop.steamtrade.inventory;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class representation of an item in a user's inventory.
 *
 * @author nosoop
 */
public class TradeInternalItem extends TradeInternalAsset {
	public static final TradeInternalItem UNAVAILABLE = null;
	long instanceid;
	boolean stackable;

	public TradeInternalItem(AppContextPair appContext,
							 JSONObject rgInventoryItem, JSONObject rgDescriptionItem)
			throws JSONException {
		super(appContext, rgInventoryItem, rgDescriptionItem);

		this.instanceid =
				Long.parseLong(rgDescriptionItem.optString("instanceid", "0"));

		this.stackable = (amount > 1);
	}

	public long getInstanceid() {
		return instanceid;
	}

	public boolean isStackable() {
		return stackable;
	}

	@Override
	public String getDisplayName() {
		if (getType().isEmpty()) {
			return super.getDisplayName();
		}
		return String.format("%s (%s)", super.getDisplayName(), this.getType());
	}
}