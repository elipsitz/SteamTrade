package com.nosoop.steamtrade.inventory;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a user's collection of game inventories, retrievable by the
 * inventory-specific appid and contextid. Also holds a varied list of
 * AssetBuilders to be used to load certain inventories.
 *
 * @author nosoop
 */
public class TradeInternalInventories {
	Map<AppContextPair, TradeInternalInventory> gameInventories;
	/**
	 * A list of AssetBuilder instances that can load inventories. First
	 * AssetBuilder that accepts an inventory takes precedence over other
	 * AssetBuilders that accept the inventory that are lower in the list.
	 */
	final List<AssetBuilder> inventoryLoaders;
	/**
	 * A default AssetBuilder instance. Accepts all inventories.
	 */
	public final static AssetBuilder DEFAULT_ASSET_BUILDER = new AssetBuilder() {
		@Override
		public boolean isSupported(AppContextPair appContext) {
			return true;
		}
	};

	/**
	 * Creates a new TradeInternalInventories instance with an empty
	 * AssetBuilder list.
	 */
	public TradeInternalInventories() {
		this(new ArrayList<AssetBuilder>());
	}

	/**
	 * Creates a new TradeInternalInventories instance with a given list of
	 * AssetBuilders to use.
	 *
	 * @param assetBuild A list of AssetBuilders to test inventory loading.
	 */
	public TradeInternalInventories(List<AssetBuilder> assetBuild) {
		this.inventoryLoaders = assetBuild;
		this.gameInventories = new HashMap<>();
	}

	public void addCachedInventory(AppContextPair appContext, JSONObject feed) {
		AssetBuilder asset = DEFAULT_ASSET_BUILDER;

		// Load the first supported AssetBuilder if any, else use default.
		for (AssetBuilder build : inventoryLoaders) {
			if (build.isSupported(appContext)) {
				asset = build;
				break;
			}
		}

		TradeInternalInventory inv =
				new TradeInternalInventory(feed, appContext, asset);
		inv.wasCached = true;

		gameInventories.put(appContext, inv);
	}

	/**
	 * Adds an inventory to the collection using a given AppContextPair and
	 * loads it using the supplied inventory data JSONObject.
	 *
	 * @param appContext
	 * @param feed
	 */
	public void addInventory(AppContextPair appContext, JSONObject feed) {
		AssetBuilder asset = DEFAULT_ASSET_BUILDER;

		// Load the first supported AssetBuilder if any, else use default.
		for (AssetBuilder build : inventoryLoaders) {
			if (build.isSupported(appContext)) {
				asset = build;
				break;
			}
		}

		gameInventories.put(appContext,
				new TradeInternalInventory(feed, appContext, asset));
	}

	/**
	 * Adds a new, empty inventory to the collection with just an
	 * AppContextPair.
	 *
	 * @param appContext
	 */
	public void addInventory(AppContextPair appContext) {
		AssetBuilder asset = DEFAULT_ASSET_BUILDER;

		// Load the first supported AssetBuilder if any, else use default.
		for (AssetBuilder build : inventoryLoaders) {
			if (build.isSupported(appContext)) {
				asset = build;
				break;
			}
		}

		gameInventories.put(appContext,
				new TradeInternalInventory(appContext, asset));
	}

	/**
	 * Returns a boolean value stating if the inventory collection contains a
	 * specific inventory.
	 *
	 * @param appid     A game's appid.
	 * @param contextid An game's inventory contextid.
	 * @return Whether or not the inventory map contains a key value
	 * AppContextPair represented by the given appid and contextid.
	 */
	public boolean hasInventory(int appid, long contextid) {
		return hasInventory(getInventoryKey(appid, contextid));
	}

	/**
	 * Returns a boolean value stating if the inventory collection contains a
	 * specific inventory.
	 *
	 * @param contextdata An AppContextPair representing a game inventory to
	 *                    check for.
	 * @return Whether or not the inventory map contains a key value of the
	 * given AppContextPair.
	 */
	public boolean hasInventory(AppContextPair contextdata) {
		return gameInventories.containsKey(contextdata);
	}

	/**
	 * @param appid
	 * @param contextid
	 * @return TradeInternalInventory for the given appid and contextid.
	 */
	public TradeInternalInventory getInventory(int appid, long contextid) {
		return gameInventories.get(getInventoryKey(appid, contextid));
	}

	/**
	 * @param contextdata
	 * @return TradeInternalInventory for the given AppContextPair.
	 */
	public TradeInternalInventory getInventory(AppContextPair contextdata) {
		return gameInventories.get(contextdata);
	}

	/**
	 * @param appid
	 * @param contextid
	 * @return An unnamed AppContextPair.
	 */
	private AppContextPair getInventoryKey(int appid, long contextid) {
		return new AppContextPair(appid, contextid);
	}

	/**
	 * @return A list of all available / known inventories held by this object.
	 */
	public List<TradeInternalInventory> getInventories() {
		return new ArrayList<>(gameInventories.values());
	}
}
