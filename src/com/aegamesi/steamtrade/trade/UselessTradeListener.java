package com.aegamesi.steamtrade.trade;

import com.aegamesi.steamtrade.steam.Schema.SchemaItem;
import com.aegamesi.steamtrade.steam.SteamInventory.SteamInventoryItem;
import com.aegamesi.steamtrade.trade.Trade.Error;

public class UselessTradeListener extends TradeListener {
	@Override
	public void onError(Error error) {
	}

	@Override
	public void onAfterInit() {
	}

	@Override
	public void onUserAddItem(SchemaItem schemaItem, SteamInventoryItem inventoryItem) {
	}

	@Override
	public void onUserRemoveItem(SchemaItem schemaItem, SteamInventoryItem inventoryItem) {
	}

	@Override
	public void onMessage(String msg) {
	}

	@Override
	public void onUserSetReadyState(boolean ready) {
	}

	@Override
	public void onUserAccept() {
	}

	@Override
	public void onNewVersion() {
	}

	@Override
	public void onComplete() {
	}

	@Override
	public void onOfferUpdated() {
	}
}
