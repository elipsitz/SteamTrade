/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.inventory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a generic, tradable asset and its basic characteristics.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public abstract class TradeInternalAsset {
	/**
	 * The inventory this item resides in, as defined by its appid and contextid
	 * pair.
	 */
	AppContextPair appContext;
	/**
	 * The display name of the item. If the item was renamed (as it could be in
	 * TF2, it will be that name.
	 */
	String name;
	/**
	 * The name it would be grouped under in the Steam Community Market. Is an
	 * empty string if not in the Market.
	 */
	String marketName;
	/**
	 * The name it would be grouped under in the Steam Community Market. Is an
	 * empty string if not in the Market.
	 */
	String marketHashName;
	/**
	 * The item's type.
	 */
	String type;
	/**
	 * The position of the item in the inventory. defaults to (-1)
	 */
	int pos;
	/**
	 * The class number of this object. Two similar items (e.g., a pair of Loose
	 * Cannons) will have the same class number.
	 */
	int classid;
	/**
	 * The number of items of this object has.
	 */
	int amount;
	/**
	 * The item id number.
	 */
	long assetid;
	/**
	 * List of description instances.
	 */
	List<Description> descriptions;
	/**
	 * Icon url
	 */
	String icon_url;
	/**
	 * Name color
	 */
	String name_color;
	/**
	 * Background color
	 */
	String background_color;
	/**
	 * Whether or not it's marketable
	 */
	boolean marketable;


	/**
	 * Creates a new TradeInternalAsset instance.
	 *
	 * @param rgInventoryItem   This asset's member JSONObject of "rgInventory" or
	 *                          "rgCurrency".
	 * @param rgDescriptionItem This asset's member JSONObject of
	 *                          "rgDescription".
	 * @throws JSONException when the values with names "name", "market_name",
	 *                       "type", "classid", or "appid" are not found in the rgDescriptionItem
	 *                       member object and/or when "classid", "amount", or "id" are not found in
	 *                       the rgInventory or rgCurrency member object.
	 */
	TradeInternalAsset(AppContextPair appContext, JSONObject rgInventoryItem,
					   JSONObject rgDescriptionItem) throws JSONException {
		String classidString;
		this.appContext = appContext;

		this.name = rgDescriptionItem.getString("name");
		this.marketName = rgDescriptionItem.getString("market_name");
		this.marketHashName = rgDescriptionItem.optString("market_hash_name", null);
		this.type = rgDescriptionItem.getString("type");
		this.icon_url = rgDescriptionItem.getString("icon_url");

		this.name_color = rgDescriptionItem.optString("name_color", null);
		this.background_color = rgDescriptionItem.optString("background_color", null);

		classidString = rgInventoryItem.getString("classid");
		this.classid = Integer.parseInt(classidString);

		this.amount = Integer.parseInt(rgInventoryItem.optString("amount", "1"));
		this.pos = rgInventoryItem.optInt("pos", -1);

		this.marketable = rgDescriptionItem.optInt("marketable", 0) != 0;

		if(rgInventoryItem.has("id"))
			this.assetid = Long.parseLong(rgInventoryItem.getString("id"));
		else if(rgInventoryItem.has("assetid"))
			this.assetid = Long.parseLong(rgInventoryItem.getString("assetid"));
		else if(rgInventoryItem.has("currencyid"))
			this.assetid = Long.parseLong(rgInventoryItem.getString("currencyid"));

		this.descriptions = new ArrayList<>();

		JSONArray dsArr = rgDescriptionItem.optJSONArray("descriptions");
		if (dsArr != null) {
			for (int i = 0; i < dsArr.length(); i++) {
				this.descriptions.add(new Description(dsArr.getJSONObject(i)));
			}
		}

		/**
		 * Verify that the input appid is the same appid passed in the
		 * constructor.
		 */
		int descriptionAppid = Integer.parseInt(
				rgDescriptionItem.getString("appid"));
		assert (descriptionAppid == this.appContext.appid);

		/**
		 * Assert that the classid matches rgDescription and rgCurrency or
		 * rgInventory by hash.
		 */
		String descriptionClassidString =
				rgDescriptionItem.getString("classid");
		assert (descriptionClassidString.equals(classidString));
	}

	/**
	 * Returns the display name of this asset, defaulting to its name. Allowed
	 * to be overridden by subclasses.
	 *
	 * @return String representing the name of this asset.
	 */
	public String getDisplayName() {
		return getName();
	}

	/**
	 * Returns the name of this asset.
	 *
	 * @return The name of this asset, as defined by the "name"-named name-value
	 * pair in the item's "rgDescriptions" JSONObject member entry.
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Returns the market name of this asset.
	 *
	 * @return The market name of this asset. If it does not have a market name,
	 * it returns an empty string.
	 */
	public final String getMarketName() {
		return marketName;
	}

	/**
	 * Returns the market hash name of this asset.
	 *
	 * @return The market hash name of this asset. If it does not have a market hash name,
	 * it returns an empty string.
	 */
	public final String getMarketHashName() {
		return marketHashName;
	}

	/**
	 * Returns the marketability of this asset.
	 *
	 * @return The marketability of this asset
	 */
	public final boolean isMarketable() {
		return marketable;
	}

	/**
	 * Returns the appid of this asset.
	 *
	 * @return The appid of this asset. Defined by the AppContextPair instance
	 * passed to it, this is asserted to be the same value as the one defined in
	 * the "appid"-named name-value pair in the item's "rgDescriptions"
	 * JSONObject member entry.
	 */
	public final int getAppid() {
		return appContext.appid;
	}

	/**
	 * Returns the contextid of this asset.
	 *
	 * @return The contextid of this asset. Defined by the AppContextPair
	 * instance passed to it, unlike the appid, there is no way to verify that
	 * the contextid is correctly defined.
	 */
	public final long getContextid() {
		return appContext.contextid;
	}

	/**
	 * Returns the assetid of this asset.
	 *
	 * @return The assetid of this asset, defined by the "id"-named name-value
	 * pair of this asset's "rgInventory" JSONObject member entry.
	 */
	public final long getAssetid() {
		return assetid;
	}

	/**
	 * Returns the classid of this asset.
	 *
	 * @return The classid of this asset, defined by the "classid"-named
	 * name-value pair of the asset's "rgDescription" JSONObject member entry
	 * and asserted to be equal to the similar value in the asset's
	 * "rgInventory" entry.
	 */
	public final int getClassid() {
		return classid;
	}

	/**
	 * Returns the amount of this asset.
	 *
	 * @return The amount of this asset, defined by the "amount"-named
	 * name-value pair of the asset's "rgInventory" JSONObject member entry
	 */
	public final int getAmount() {
		return amount;
	}
	/**
	 * Returns the position of this asset.
	 *
	 * @return The position of this asset, defined by the "pos"-named
	 * name-value pair of the asset's "rgInventory" JSONObject member entry
	 */
	public final int getPosition() {
		return pos;
	}

	/**
	 * Returns the type of this asset.
	 *
	 * @return The amount of this asset, defined by the "type"-named name-value
	 * pair of the asset's "rgDescription" JSONObject member entry
	 */
	public final String getType() {
		return type;
	}

	/**
	 * Returns the name_color of the asset.
	 *
	 * @return The url of the icon of this asset, defined by the "name_color"-named name-value
	 * pair of the asset's "rgDescription" JSONObject member entry
	 */
	public final int getNameColor() {
		if (name_color == null || name_color.length() == 0)
			return 0;
		long color = Integer.parseInt(name_color, 16);
		color |= 0x00000000ff000000; // add 0xff alpha
		return (int) color;
	}

	/**
	 * Returns the background_color of the asset.
	 *
	 * @return The url of the icon of this asset, defined by the "background_color"-named name-value
	 * pair of the asset's "rgDescription" JSONObject member entry
	 */
	public final int getBackgroundColor() {
		if (background_color == null || background_color.length() == 0)
			return 0;
		long color = Integer.parseInt(background_color, 16);
		color |= 0x00000000ff000000; // add 0xff alpha
		return (int) color;
	}

	/**
	 * Returns the icon_url of the asset.
	 *
	 * @return The url of the icon of this asset, defined by the "icon_url"-named name-value
	 * pair of the asset's "rgDescription" JSONObject member entry
	 */
	public final String getIconURL() {
		return icon_url;
	}

	public final List<Description> getDescriptions() {
		return descriptions;
	}

	public static class Description {
		final String type;
		final String value;
		final int color;

		private Description(JSONObject descriptions) throws JSONException {
			type = descriptions.optString("type", "text");
			value = descriptions.getString("value");

			String hexColor = descriptions.optString("color", "000000");

			color = Integer.parseInt(hexColor, 16);
		}

		public int getColor() {
			return color;
		}

		public String getValue() {
			return value;
		}

		public String getType() {
			return type;
		}
	}

}
