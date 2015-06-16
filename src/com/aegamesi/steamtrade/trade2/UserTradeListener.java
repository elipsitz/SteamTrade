/*
 * The MIT License
 *
 * Copyright 2014 nosoop < nosoop at users.noreply.github.com >.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.aegamesi.steamtrade.trade2;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.fragments.FragmentTrade;
import com.aegamesi.steamtrade.steam.SteamChatManager;
import com.aegamesi.steamtrade.steam.SteamService;
import com.google.android.gms.analytics.HitBuilders;
import com.nosoop.steamtrade.TradeListener;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nosoop.steamtrade.inventory.TradeInternalItem;

import java.util.ArrayList;

import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class UserTradeListener extends TradeListener {
	private final short MAX_ITEMS_IN_TRADE = 256;
	public boolean loaded = false;
	public boolean active = true;
	int actionCount = 0;
	private TradeInternalItem ourTradeSlotsFilled[];

	public UserTradeListener() {
		ourTradeSlotsFilled = new TradeInternalItem[MAX_ITEMS_IN_TRADE];
		for (int i = 0; i < ourTradeSlotsFilled.length; i++) {
			ourTradeSlotsFilled[i] = null;
		}
	}

	private void updateFragmentUIInventory() {
		MainActivity.instance.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (fragment() == null)
					return;
				fragment().updateUIInventory();
			}
		});
	}

	public FragmentTrade fragment() {
		return MainActivity.instance.getFragmentByClass(FragmentTrade.class);
	}

	public boolean tradePutItem(TradeInternalItem item) {
		// Make sure the item isn't in the trade already.
		if (getSlotByItemID(item) == -1) {
			int slotToFill = getFirstFreeSlot();
			trade.getCmds().addItem(item, slotToFill);
			ourTradeSlotsFilled[slotToFill] = item;
			updateFragmentUIOffers();
			return true;
		}
		return false;
	}

	/**
	 * Finds an item currently in the trade based on the item's id.
	 *
	 * @param item Item to search for.
	 * @return The item's position in the trade if it is in the trade, -1 if
	 * not.
	 */
	private int getSlotByItemID(TradeInternalItem item) {
		for (int i = 0; i < ourTradeSlotsFilled.length; i++) {
			if (ourTradeSlotsFilled[i] == item) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Finds the first open slot in a trade.
	 *
	 * @return The position of the first "empty" slot in the trade, -1 if
	 * there are no empty slots.
	 */
	private int getFirstFreeSlot() {
		for (int i = 0; i < ourTradeSlotsFilled.length; i++) {
			if (ourTradeSlotsFilled[i] == null) {
				return i;
			}
		}
		return -1;
	}

	private void updateFragmentUIOffers() {
		MainActivity.instance.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (fragment() == null)
					return;
				fragment().updateUIOffers();
			}
		});
	}

	public boolean tradeRemoveItem(TradeInternalItem item) {
		int slotToRemove;
		if ((slotToRemove = getSlotByItemID(item)) != -1) {
			trade.getCmds().removeItem(item);
			ourTradeSlotsFilled[slotToRemove] = null;
			updateFragmentUIOffers();
			return true;
		}
		return false;
	}

	/**
	 * Event fired when the trade has encountered an error.
	 *
	 * @param errorCode The error code for the given error. Known values are
	 *                  available as constants under TradeListener.TradeErrorCodes.
	 */
	@Override
	public void onError(final int errorCode, String msg) {
		String errorMessage;
		switch (errorCode) {
			case TradeStatusCodes.STATUS_ERRORMESSAGE:
				errorMessage = msg;
				break;
			case TradeStatusCodes.TRADE_CANCELLED:
				errorMessage = "The trade has been canceled.";
				break;
			case TradeStatusCodes.STATUS_PARSE_ERROR:
				errorMessage = "We have timed out.";
				break;
			case TradeStatusCodes.PARTNER_TIMED_OUT:
				errorMessage = "Other user timed out.";
				break;
			case TradeStatusCodes.TRADE_FAILED:
				errorMessage = "Trade failed.";
				break;
			case TradeStatusCodes.TRADE_REQUIRES_CONFIRMATION:
				errorMessage = "Trade successful; however, both parties may need to confirm the trade via email due to Steam security restrictions.";
				break;
			default:
				errorMessage = "Unhandled error code " + errorCode + ".";
		}

		if (!msg.equals(TradeStatusCodes.EMPTY_MESSAGE)) {
			errorMessage += " (" + msg + ")";
		}

		active = false;
		final String error = errorMessage;
		MainActivity.instance.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (fragment() != null)
					fragment().onError(errorCode, error);
			}
		});
	}

	/**
	 * Event fired every time the trade session is polled for updates to notify
	 * the listener of how long we have been in the trade and how long it has
	 * been since the trade partner's last input.
	 * <p/>
	 * Taking this approach over letting the software writer make their own
	 * polling thread to keep things simple.
	 *
	 * @param secondsSinceAction
	 * @param secondsSinceTrade
	 */
	@Override
	public void onTimer(int secondsSinceAction, int secondsSinceTrade) {
		updateFragmentUIOffers(); // TODO remove this
	}

	/**
	 * Called once everything but inventories have been initialized. (Originally
	 * had to wait until all inventories were loaded, might as well keep it in
	 * case.)
	 */
	@Override
	public void onWelcome() {
		//trade.getCmds().sendMessage("Hello!  Please wait while I figure out what items I have.");
	}

	/**
	 * Called once everything is set. Show our inventory at our frontend, etc.
	 */
	@Override
	public void onAfterInit() {
		loaded = true;

		MainActivity.instance.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (SteamService.singleton != null && SteamService.singleton.tradeManager != null)
					SteamService.singleton.tradeManager.updateTradeStatus();
			}
		});

		//trade.getCmds().sendMessage("Ready to trade!");
		//trade.loadOwnInventory(new AppContextPair(440, 2));
	}

	/**
	 * Called when our trading partner has added an item.
	 *
	 * @param asset
	 */
	@Override
	public void onUserAddItem(TradeInternalAsset asset) {
		FragmentTrade.tab_notifications[1]++;
		updateFragmentUIOffers();
	}

	/**
	 * Called when our trading partner has removed an item.
	 *
	 * @param inventoryItem
	 */
	@Override
	public void onUserRemoveItem(TradeInternalAsset inventoryItem) {
		FragmentTrade.tab_notifications[1]++;
		updateFragmentUIOffers();
	}

	/**
	 * Called when our trading partner sent a message. In this example we will
	 * add a random item whenever the other person says something.
	 *
	 * @param msg The message text.
	 */
	@Override
	public void onMessage(final String msg) {
		MainActivity.instance.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				SteamService.singleton.chatManager.broadcastMessage(
						System.currentTimeMillis(),
						SteamService.singleton.steamClient.getSteamId(),
						new SteamID(fragment().trade().otherSteamId),
						false,
						SteamChatManager.CHAT_TYPE_TRADE,
						msg
				);
				if (fragment() != null)
					fragment().updateUIChat();
			}
		});
	}

	/**
	 * Called when our trading partner ticked or unticked the "ready" checkbox.
	 * In response, we will do the opposite of what they did so the trade never
	 * happens, 50% of the time.
	 *
	 * @param ready Whether or not the checkbox is set.
	 */
	@Override
	public void onUserSetReadyState(boolean ready) {
		updateFragmentUIOffers();
	}

	/**
	 * Called when the other user accepts the trade, in case you want to do
	 * something about it.
	 */
	@Override
	public void onUserAccept() {
		/*trade.getCmds().sendMessage("Hah. Nope. Cancelled.");

		// TODO Handle JSONException in the library.
		try {
			trade.getCmds().cancelTrade();
		} catch (JSONException ex) {
		}*/
	}

	/**
	 * An event occurred. Normally wouldn't have to do anything here, but go
	 * ahead and do something if you want.
	 */
	@Override
	public void onNewVersion() {
		actionCount++;
	}

	/**
	 * Called when the trade has been completed successfully.
	 */
	@Override
	public void onTradeSuccess() {
		System.out.println("Items traded.");
		active = false;

		// goal!
		MainActivity.instance.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (fragment() == null)
					if (fragment().activity() != null)
						fragment().activity().tracker().send(new HitBuilders.EventBuilder().setCategory("Steam").setAction("Trade_Complete").setValue(trade.getPartner().getOffer().size()).build());

				fragment().onCompleted(new ArrayList<TradeInternalAsset>(trade.getPartner().getOffer()));
			}
		});
	}

	/**
	 * Called when the trade is done and we should stop polling for updates.
	 * Remember, you can only be in one trade at a time (?), so you should be
	 * telling the client that we are ready for another trade.
	 */
	@Override
	public void onTradeClosed() {
		// Cleanup and whatnot.
		active = false;

		MainActivity.instance.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (SteamService.singleton != null && SteamService.singleton.tradeManager != null)
					SteamService.singleton.tradeManager.notifyTradeHasEnded();
			}
		});
	}
}