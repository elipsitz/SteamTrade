package com.nosoop.steamtrade;

import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nosoop.steamtrade.status.TradeEvent;

/**
 * Receives trade events from the TradeSession that it should be attached to.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public abstract class TradeListener {
	public TradeSession trade;

	/**
	 * Called when an error occurs during the trade such that the trade is
	 * closed.
	 *
	 * @param errorCode    The error code. Known values are defined in
	 *                     TradeListener.TradeErrorCodes.
	 * @param errorMessage An error message, if available. If not available,
	 *                     will default to TradeStatusCodes.EMPTY_MESSAGE.
	 */
	public abstract void onError(int errorCode, String errorMessage);

	/**
	 * Called when the client polls the trade. If you want to warn the other
	 * person for taking too long, implement this method and add a cancel.
	 * Otherwise, just do nothing.
	 *
	 * @param secondsSinceAction Number of seconds since the trading partner has
	 *                           done something.
	 * @param secondsSinceTrade  Number of seconds since the trade has started.
	 */
	public abstract void onTimer(int secondsSinceAction, int secondsSinceTrade);

	// TODO implement onException, create TradeException?

	/**
	 * Called when the listener is connected to a trade. Things that depend on
	 * trade session information can be assigned here. Or say a welcome message.
	 */
	public abstract void onWelcome();

	/**
	 * Called after backpacks are loaded and trading can start. If you need to
	 * store inventories in your own listener, handle that here.
	 */
	public abstract void onAfterInit();

	/**
	 * Called when the other person adds an item. If this is an item from a new
	 * inventory, that inventory is loaded before this event is called.
	 *
	 * @param inventoryItem The item added to the trade.
	 */
	public abstract void onUserAddItem(TradeInternalAsset inventoryItem);

	/**
	 * Called when the other person removes an item.
	 *
	 * @param inventoryItem The item removed from the trade.
	 */
	public abstract void onUserRemoveItem(TradeInternalAsset inventoryItem);

	/**
	 * Called when the other client send a message through Steam Trade.
	 *
	 * @param msg The received message.
	 */
	public abstract void onMessage(String msg);

	/*
	 * Called when the trading partner checks / unchecks the 'ready' box.
	 */
	public abstract void onUserSetReadyState(boolean ready);

	/**
	 * Called once the trading partner has accepted the trade and is waiting for
	 * us to accept.
	 */
	public abstract void onUserAccept();

	/**
	 * Called when something has happened in the trade.
	 */
	public abstract void onNewVersion();

	/**
	 * Called once a trade has been made.
	 */
	public abstract void onTradeSuccess();

	/**
	 * Called once the trade has been closed for the client to begin cleaning
	 * up. Called immediately after a successful trade or trade error.
	 */
	public abstract void onTradeClosed();

	/**
	 * Called when the client receives a TradeEvent that it has no idea how to
	 * handle. In this case, a subclass of TradeListener can override this
	 * method to handle the event a bit without having to recompile the library.
	 *
	 * @param event A trade event to be handled manually.
	 */
	public void onUnknownAction(TradeEvent event) {
	}

	/**
	 * Defines trade status codes to be interpreted by the onError() method.
	 */
	public static class TradeStatusCodes {
		public final static int //
				/**
				 * Non-error statuses. Everything is okay according to Steam.
				 * Something weird is going on if onError() is called with these
				 * values.
				 */
				// We are polling for updates.
				STATUS_OK = 0,
		// Both users have decided to make the trade.
		TRADE_COMPLETED = 1,
		/**
		 * Steam web errors. Something funky happened on Steam's side.
		 * The error codes are defined by Steam.
		 */
		// Why this would happen, I don't know.
		TRADE_NOT_FOUND = 2,
		// One user cancelled.
		TRADE_CANCELLED = 3,
		// The other user timed out.
		PARTNER_TIMED_OUT = 4,
		// The trade failed in general. (?????)
		TRADE_FAILED = 5,
		// The trade must be confirmed via email
		TRADE_REQUIRES_CONFIRMATION = 6,
		/**
		 * SteamTrade-Java errors. Something in this library bugged out.
		 * The following error values are defined and used within the
		 * library.
		 */
		// There was a JSONException caught when parsing the status.
		STATUS_PARSE_ERROR = 1001,
		// The trade session was unable to fetch your inventories.
		BACKPACK_SCRAPE_ERROR = 1002,
		// Unknown status -- message provided by the Status instance.
		STATUS_ERRORMESSAGE = 1003,
		// Something happened with the foreign inventory loading.
		FOREIGN_INVENTORY_LOAD_ERROR = 1004,
		// Something happened with our own inventory loading.
		OWN_INVENTORY_LOAD_ERROR = 1005,
		// The item specified could not be found in the inventory.
		USER_ITEM_NOT_FOUND = 1006,
		// The event action code is missing.
		TRADEEVENT_ACTION_MISSING = 1007;
		public final static String EMPTY_MESSAGE = "";
	}
}
