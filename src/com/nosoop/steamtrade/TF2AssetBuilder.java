/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade;

import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.AssetBuilder;
import com.nosoop.steamtrade.inventory.TradeInternalItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An asset builder that generates TF2Item instances. Add this to a list and
 * pass it to the TradeSession instance to use.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public class TF2AssetBuilder extends AssetBuilder {
	/**
	 * Parses the inventory data and returns a TF2Item instance.
	 *
	 * @param appContext    The appid/contextid pair for the inventory this item
	 *                      resides in.
	 * @param rgInventory   The item's rgInventory object.
	 * @param rgDescription The item's rgDescription object.
	 * @return A TF2Item instance.
	 * @throws JSONException
	 */
	@Override
	public TradeInternalItem generateItem(AppContextPair appContext, JSONObject rgInventory, JSONObject rgDescription) throws JSONException {
		return new TF2Item(appContext, rgInventory, rgDescription);
	}

	@Override
	public boolean isSupported(AppContextPair appContext) {
		return appContext.getAppid() == 440 && appContext.getContextid() == 2;
	}

	/**
	 * Enumeration of known TF2 item qualities. And undefined.
	 */
	public enum Quality {
		NORMAL, RARITY1, RARITY2, VINTAGE, RARITY3, RARITY4, UNIQUE,
		COMMUNITY, DEVELOPER, SELFMADE, CUSTOMIZED, STRANGE, COMPLETED,
		HAUNTED, COLLECTORS, UNDEFINED;

		static Quality getQuality(int quality) {
			switch (quality) {
				case 0:
					return NORMAL;
				case 1:
					return RARITY1;
				case 2:
					return RARITY2;
				case 3:
					return VINTAGE;
				case 4:
					return RARITY3;
				case 5:
					return RARITY4;
				case 6:
					return UNIQUE;
				case 7:
					return COMMUNITY;
				case 8:
					return DEVELOPER;
				case 9:
					return SELFMADE;
				case 10:
					return CUSTOMIZED;
				case 11:
					return STRANGE;
				case 12:
					return COMPLETED;
				case 13:
					return HAUNTED;
				case 14:
					return COLLECTORS;
				default:
					return UNDEFINED;
			}
		}
	}

	/**
	 * A TradeInternalItem instance that holds data specific to Team Fortress 2
	 * items.
	 *
	 * @author nosoop < nosoop at users.noreply.github.com >
	 */
	public class TF2Item extends TradeInternalItem {
		/**
		 * Whether or not the item was an item put into gift wrap.
		 */
		boolean wasGifted;
		/**
		 * The defindex of the item. Good to have if you'd like to refer to the
		 * schema for some reason.
		 */
		long defIndex;
		/**
		 * The quality indicator of the item. Also only good with the schema
		 * really, though the name should have it.
		 */
		Quality quality;

		public TF2Item(AppContextPair appContext, JSONObject rgInventoryItem,
					   JSONObject rgDescriptionItem) throws JSONException {
			super(appContext, rgInventoryItem, rgDescriptionItem);

			this.wasGifted = false;

			JSONObject appData = rgDescriptionItem.optJSONObject("app_data");
			if (appData != null) {
				if (appData.has("def_index")) {
					defIndex = Long.parseLong(appData.getString("def_index"));
				}

				if (appData.has("quality")) {
					quality = Quality.getQuality(
							Integer.parseInt(appData.getString("quality")));
				}
			}

			// Iterate through descriptions.
			JSONArray descs = rgDescriptionItem.optJSONArray("descriptions");
			if (descs != null) {
				for (int i = 0; i < descs.length(); i++) {
					JSONObject descriptionItem = descs.getJSONObject(i);
					String descriptionValue = descriptionItem.getString("value");

					/**
					 * Check if the description contains text that states if the
					 * item is gifted.
					 *
					 * TODO Make this language dependent, as it assumes the
					 * trade interface is in English.
					 */
					if (descriptionValue.contains("Gift from")) {
						wasGifted = true;
					}
				}
			}

			/**
			 * If you have access to IEconItems_440/GetPlayerItems, and assuming
			 * the trading partner does not have a private inventory, you can
			 * also load the player data from the API yourself and identify the
			 * item by its "id" key matching the assetid of this instance.
			 *
			 * Assuming JSON:
			 *
			 * In .attributes[], the existence of defindex 186 means the item
			 * was a gift.
			 *
			 * .custom_name and .custom_desc or the existence of defindices 500
			 * and 501 means that the item has a custom name and custom
			 * description.
			 */
		}

		/**
		 * Some function to check if this item is renamed based off of the
		 * difference in market name and visible name.
		 *
		 * @return
		 */
		public boolean isRenamed() {
			return !getMarketName().equals(getName())
					&& getName().matches("''.*''");
		}

		@Override
		public String getDisplayName() {
			String invName;

			invName = this.getName();

			// Format item name for renamed items.
			if (this.isRenamed()) {
				invName = String.format("%s (%s)", invName, getMarketName());
			}

			// Format item name for gifted items.
			if (this.wasGifted) {
				invName = String.format("%s (gifted)", invName);
			}

			// TODO Format item for unusual effect, etc?

			return invName;
		}
	}

}