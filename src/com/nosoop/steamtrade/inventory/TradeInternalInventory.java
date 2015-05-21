package com.nosoop.steamtrade.inventory;

import com.nosoop.steamtrade.TradeListener.TradeStatusCodes;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents a Steam user's inventory as displayed in trading and from viewing
 * an inventory online.
 *
 * @author nosoop (ported and adapted from ForeignInventory in the SteamBot
 *         project by Jessecar96)
 */
public class TradeInternalInventory {
	boolean inventoryValid;
	String errorMessage;
	/**
	 * List of items in the inventory, mapped to its assetid.
	 * <p/>
	 * Opted for a map for performance benefits when looking up an item by its
	 * assetid in large inventories.
	 */
	Map<Long, TradeInternalItem> inventoryItems;
	/**
	 * List of currency items in the inventory, mapped to its currencyid.
	 * <p/>
	 * Should be fine as a list, but for now.
	 */
	Map<Long, TradeInternalCurrency> currencyItems;
	/**
	 * The appid-contextid pair this inventory represents.
	 */
	final AppContextPair appContext;
	/**
	 * The AssetBuilder instance used to load this inventory.
	 */
	final AssetBuilder assetBuilder;
	/**
	 * Whether or not there is more to load in the inventory.
	 */
	boolean hasMore;
	/**
	 * The start position to load from.
	 */
	int moreStartPosition;
	/**
	 * Whether or not this inventory was cached.
	 */
	boolean wasCached;

	/**
	 * Creates a new, empty inventory.
	 *
	 * @param appContext   The appid-contextid pair this inventory represents.
	 * @param assetBuilder An AssetBuilder instance to load the inventory data.
	 */
	public TradeInternalInventory(AppContextPair appContext,
								  AssetBuilder assetBuilder) {
		this.appContext = appContext;
		this.assetBuilder = assetBuilder;

		inventoryValid = false;

		inventoryItems = new HashMap<>();
		currencyItems = new HashMap<>();

		hasMore = false;
		moreStartPosition = 0;
	}

	/**
	 * Takes a JSON data feed received from loading our inventory and creates a
	 * representation of.
	 *
	 * @param json         A JSONObject representation of the inventory to load.
	 * @param appContext   The appid-contextid pair this inventory represents.
	 * @param assetBuilder An AssetBuilder instance to load the inventory data.
	 */
	public TradeInternalInventory(JSONObject json, AppContextPair appContext,
								  AssetBuilder assetBuilder) {
		this.appContext = appContext;
		this.assetBuilder = assetBuilder;

		inventoryValid = false;

		inventoryItems = new HashMap<>();
		currencyItems = new HashMap<>();

		hasMore = false;
		moreStartPosition = 0;

		try {
			if (json.getBoolean("success")) {
				parseInventory(json);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * For large inventories, load additional inventory data.
	 *
	 * @param json
	 */
	public void loadMore(JSONObject json) {
		try {
			if (json.getBoolean("success")) {
				parseInventory(json);
			} else {
				inventoryValid = false;
				errorMessage = json.optString("error",
						TradeStatusCodes.EMPTY_MESSAGE);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the AppContextPair associated with the inventory. Example: A Team
	 * Fortress 2 inventory would return an AppContextPair equal to
	 * <code>new AppContextPair(440, 2);</code>
	 *
	 * @return An AppContextPair "key" representing this instance.
	 */
	public AppContextPair getAppContextPair() {
		return appContext;
	}

	/**
	 * Gets the user's available trading inventory, sorted with respect to inventory position.
	 *
	 * @return A List containing all the available TradeInternalItem instances.
	 */
	public List<TradeInternalItem> getItemList() {
		List<TradeInternalItem> list = new ArrayList<TradeInternalItem>(inventoryItems.values());

		// now sort with respect to position
		// TODO offer this somewhere in the UI with more flexibility
		Collections.sort(list, new Comparator<TradeInternalItem>() {
			@Override
			public int compare(TradeInternalItem a, TradeInternalItem b) {
				return (a.pos < b.pos) ? -1 : (a.pos > b.pos ? 1 : 0);
			}
		});

		return list;
	}

	/**
	 * Gets the user's available currency items for the game.
	 *
	 * @return A List containing all available TradeInternalCurrency instances.
	 */
	public List<TradeInternalCurrency> getCurrencyList() {
		return new ArrayList<TradeInternalCurrency>(currencyItems.values());
	}

	/**
	 * Returns whether or not the inventory loading was successful.
	 *
	 * @return
	 */
	public boolean isValid() {
		return inventoryValid;
	}

	/**
	 * Returns the error message associated with the error response.
	 *
	 * @return The JSON error message from the response if the inventory loading
	 * was unsuccessful, or an empty string if it was.
	 */
	public String getErrorMessage() {
		if (!inventoryValid) {
			return errorMessage;
		} else {
			return "";
		}
	}

	/**
	 * Retrieves an item by its assetid.
	 *
	 * @param assetid The assetid of the TradeInternalItem to get.
	 * @return A TradeInternalItem instance if available, or an instance of null
	 * if not.
	 */
	public TradeInternalItem getItem(long assetid) {
		if (inventoryItems.containsKey(assetid)) {
			return inventoryItems.get(assetid);
		}
		return TradeInternalItem.UNAVAILABLE;
	}

	/**
	 * Retrieves a currency item by its currencyid
	 *
	 * @param currencyid The currencyid of the TradeInternalCurrency to get.
	 * @return A TradeInternalCurrency instance if available, or an instance of
	 * null if not.
	 */
	public TradeInternalCurrency getCurrency(long currencyid) {
		if (currencyItems.containsKey(currencyid)) {
			return currencyItems.get(currencyid);
		}
		return null;
	}

	/**
	 * Helper method to parse out the JSON inventory format.
	 *
	 * @param json JSONObject representing inventory to be parsed.
	 * @throws JSONException
	 */
	private void parseInventory(final JSONObject json) throws JSONException {
		inventoryValid = true;

		// Convenience map to associate class/instance to description.
		Map<ClassInstancePair, JSONObject> descriptions = new HashMap<>();
		JSONObject rgDescriptions = json.optJSONObject("rgDescriptions");
		if (rgDescriptions != null) {
			Iterator<String> i = rgDescriptions.keys();
			while (i.hasNext()) {
				final String rgDescriptionKey = i.next();
				JSONObject rgDescriptionItem =
						rgDescriptions.getJSONObject(rgDescriptionKey);

				int classid = rgDescriptionItem.getInt("classid");
				long instanceid = rgDescriptionItem.getLong("instanceid");

				descriptions.put(new ClassInstancePair(classid, instanceid),
						rgDescriptionItem);
			}
		}

		// Add non-currency items.
		JSONObject rgInventory = json.optJSONObject("rgInventory");
		if (rgInventory != null) {
			Iterator<String> i = rgInventory.keys();
			while (i.hasNext()) {
				final String rgInventoryItem = i.next();
				JSONObject invInstance =
						rgInventory.getJSONObject(rgInventoryItem);

				ClassInstancePair itemCI = new ClassInstancePair(
						Integer.parseInt(invInstance.getString("classid")),
						Long.parseLong(
								invInstance.optString("instanceid", "0")));

				try {
					TradeInternalItem generatedItem =
							assetBuilder.generateItem(appContext, invInstance,
									descriptions.get(itemCI));

					inventoryItems.put(generatedItem.assetid, generatedItem);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}

		// Add currency items
		JSONObject rgCurrency = json.optJSONObject("rgCurrency");
		if (rgCurrency != null) {
			Iterator<String> i = rgCurrency.keys();
			while (i.hasNext()) {
				String rgCurrencyItem = i.next();
				JSONObject invInstance =
						rgCurrency.getJSONObject(rgCurrencyItem);

				ClassInstancePair itemCI = new ClassInstancePair(
						Integer.parseInt(invInstance.getString("classid")),
						Long.parseLong(invInstance.optString("instanceid", "0")));

				try {
					TradeInternalCurrency generatedItem =
							assetBuilder.generateCurrency(appContext,
									invInstance, descriptions.get(itemCI));

					currencyItems.put(generatedItem.assetid, generatedItem);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}

		// Check if there is more to the inventory.
		hasMore = json.optBoolean("more");

		if (hasMore) {
			// Shift the start position.
			moreStartPosition = json.getInt("more_start");
		}
	}

	public boolean hasMore() {
		return this.hasMore;
	}

	public int getMoreStartPosition() {
		return this.moreStartPosition;
	}

	// TODO Write toJSONObject() method to support inventory saving?

	/**
	 * Utility class to identify class-instance pairs.
	 *
	 * @author nosoop < nosoop at users.noreply.github.com >
	 */
	public static class ClassInstancePair {
		int classid;
		long instanceid;

		/**
		 * Creates a class-instance pair.
		 *
		 * @param classid
		 * @param instanceid
		 */
		public ClassInstancePair(int classid, long instanceid) {
			this.classid = classid;
			this.instanceid = instanceid;
		}

		@Override
		public int hashCode() {
			return 497 * classid + (int) instanceid;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final ClassInstancePair other = (ClassInstancePair) obj;
			if (this.classid != other.classid) {
				return false;
			}
			return this.instanceid == other.instanceid;
		}
	}

}