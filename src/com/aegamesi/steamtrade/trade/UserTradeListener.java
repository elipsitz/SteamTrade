package com.aegamesi.steamtrade.trade;

import android.util.Log;
import android.view.View;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.fragments.FragmentTrade;
import com.aegamesi.steamtrade.steam.Schema.SchemaItem;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatLine;
import com.aegamesi.steamtrade.steam.SteamInventory.SteamInventoryItem;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.trade.Trade.Error;

import java.util.ArrayList;
import java.util.Date;

public class UserTradeListener extends TradeListener {
	public FragmentTrade fragment() {
		return MainActivity.instance.getFragmentByClass(FragmentTrade.class);
	}

	@Override
	public void onError(final Error error) {
		Log.e("SteamTrade", "Error Trading: " + error);
		if (error.severe && fragment() != null) {
			MainActivity.instance.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					fragment().onError(error);
				}
			});
		}
		SteamService.singleton.chat.appendToLog(trade.otherID.convertToLong() + "", "<-- Trade Ended -->");
	}

	@Override
	public void onAfterInit() {
		MainActivity.instance.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				SteamService.singleton.tradeManager.updateTradeStatus();
			}
		});
	}

	@Override
	public void onUserAddItem(SchemaItem schemaItem, SteamInventoryItem inventoryItem) {
		if (fragment() != null) {
			updateReadyUI(false, false);
		}
	}

	@Override
	public void onUserRemoveItem(SchemaItem schemaItem, SteamInventoryItem inventoryItem) {
		if (fragment() != null) {
			updateReadyUI(false, false);
		}
	}

	@Override
	public void onOfferUpdated() {
		if (fragment() != null) {
			MainActivity.instance.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					fragment().onOfferUpdated();
				}
			});
		}
	}

	@Override
	public void onMessage(String msg) {
		final ChatLine chatline = new ChatLine();
		chatline.steamId = trade.otherID;
		chatline.message = msg;
		chatline.time = (new Date()).getTime();
		SteamService.singleton.chat.logLine(chatline, trade.otherID, "t");

		if (fragment() != null) {
			MainActivity.instance.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					fragment().onReceiveMessage(chatline);
				}
			});
		}
	}

	@Override
	public void onUserSetReadyState(final boolean ready) {
		if (fragment() != null) {
			updateReadyUI(fragment().tabOfferMeReady.isChecked(), ready);
		}
	}

	private void updateReadyUI(final boolean meReady, final boolean otherReady) {
		if (fragment() != null) {
			MainActivity.instance.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					fragment().tabOfferMeReady.setChecked(meReady);
					fragment().tabOfferOtherReady.setChecked(otherReady);
					fragment().tabOfferAccept.setEnabled(meReady && otherReady);
					if (!meReady || !otherReady)
						fragment().tabOfferStatusCircle.setVisibility(View.GONE);
				}
			});
		}
	}

	@Override
	public void onUserAccept() {
	}

	@Override
	public void onNewVersion() {

	}

	@Override
	public void onComplete() {
		if (fragment() != null) {
			MainActivity.instance.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ArrayList<SteamInventoryItem> items = new ArrayList<SteamInventoryItem>();
					for (long id : trade.OtherTrade)
						items.add(trade.OtherInventory.getItem(id));
					fragment().onCompleted(items);
				}
			});
		}
		SteamService.singleton.chat.appendToLog(trade.otherID.convertToLong() + "", "<-- Trade Ended -->");
	}
}
