package com.aegamesi.steamtrade.trade;

import com.aegamesi.steamtrade.steam.Schema.SchemaItem;
import com.aegamesi.steamtrade.steam.SteamInventory.SteamInventoryItem;
import com.aegamesi.steamtrade.trade.Trade.Error;

public abstract class TradeListener {
	protected int slot;
	public Trade trade;

	public abstract void onError(Error error);

	public abstract void onAfterInit();

	public abstract void onUserAddItem(SchemaItem schemaItem, SteamInventoryItem inventoryItem);

	public abstract void onUserRemoveItem(SchemaItem schemaItem, SteamInventoryItem inventoryItem);

	public abstract void onMessage(String msg);

	public abstract void onUserSetReadyState(boolean ready);

	public abstract void onUserAccept();

	public abstract void onNewVersion();

	public abstract void onComplete();

	public abstract void onOfferUpdated();
}
