package com.aegamesi.steamtrade.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.co.thomasc.steamkit.types.steamid.SteamID;
import android.util.Log;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.steam.Schema.SchemaItem;
import com.aegamesi.steamtrade.steam.SteamInventory;
import com.aegamesi.steamtrade.steam.SteamInventory.SteamInventoryItem;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.aegamesi.steamtrade.steam.steamweb.SteamWeb;
import com.aegamesi.steamtrade.trade.TradeStatus.TradeEvent;
import com.aegamesi.steamtrade.trade.TradeStatus.TradeSessionAsset;

public class Trade extends Thread {
	public enum Error {
		EXCEPTION(false, "Exception"), CANCELLED(true, "Trade Cancelled"), TIMED_OUT(true, "Trade Timed Out"), FAILED(true, "Trade Failed"), VERSION_MISMATCH(true, "Trade Out of Sync");
		public boolean severe = false;
		public String text = "Error";

		private Error(boolean severe, String text) {
			this.severe = severe;
			this.text = text;
		}
	}

	// Static properties
	public static String SteamCommunityDomain = "steamcommunity.com";
	public static String SteamTradeUrl = "http://steamcommunity.com/trade/%s/";

	public SteamID myID;
	public SteamID otherID;

	// Generic Trade info
	public boolean meReady = false;
	public boolean otherReady = false;

	int lastEvent = 0;
	public String pollLock2 = "";

	// Items
	public List<Long> MyTrade = new ArrayList<Long>();
	public HashMap<Long, Integer> UsedSlots = new HashMap<Long, Integer>();
	public List<Long> OtherTrade = new ArrayList<Long>();
	//public Object[] trades;

	public SteamInventory OtherInventory;
	public SteamInventory MyInventory;
	public SteamInventory[] inventories;

	// Internal properties needed for Steam API.
	protected String baseTradeURL;
	protected String steamLogin;
	protected String sessionId;
	protected int numEvents;

	public TradeListener tradeListener;
	private TradeSession session;

	public List<Runnable> toRun;
	public boolean die = false;
	public boolean initiated = false;

	public Trade(SteamID me, SteamID other, String sessionId, String token, TradeListener listener) {
		myID = me;
		otherID = other;
		session = new TradeSession(sessionId, token, other, "440");

		this.sessionId = sessionId;
		steamLogin = token;
		listener.trade = this;
		tradeListener = listener;
		toRun = new ArrayList<Runnable>();

		baseTradeURL = String.format(Trade.SteamTradeUrl, otherID.convertToLong());
	}

	@Override
	public void run() {
		// <------ INITIATE ----->
		try {
			// fetch other player's inventory from the Steam API.
			OtherInventory = SteamInventory.fetchInventory(otherID, SteamUtil.apikey, false, null); // no cache
			if (OtherInventory == null)
				throw new Exception("Could not fetch other player's inventory via Steam API!");

			// fetch our inventory from the Steam API.
			MyInventory = SteamInventory.fetchInventory(myID, SteamUtil.apikey, false, null); // no cache
			MyInventory.filterTradable();
			if (MyInventory == null)
				throw new Exception("Could not fetch own inventory via Steam API!");

			inventories = new SteamInventory[] { MyInventory, OtherInventory };
			initiated = true;
			tradeListener.onAfterInit();
		} catch (final Exception e) {
			tradeListener.onError(Error.EXCEPTION);
			e.printStackTrace();
		}
		// <------- START POLLING, MAIN LOOP ------>
		while (true) {
			if (die)
				break;
			Poll();
			while (toRun.size() > 0)
				toRun.remove(0).run();
			try {
				Thread.sleep(800);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public TradeStatus status = null;

	public void Poll() {
		synchronized (pollLock2) {
			status = session.getStatus();
			if (status == null)
				return;
			boolean isUs = true;
			boolean abort = false;

			// Update version
			if (status.newversion) {
				session.version = status.version;
				// copy assets
				MyTrade.clear();
				OtherTrade.clear();
				if (status.me.assets != null)
					for (TradeSessionAsset asset : status.me.assets)
						if (asset != null)
							MyTrade.add(asset.assetid);
				if (status.them.assets != null)
					for (TradeSessionAsset asset : status.them.assets)
						if (asset != null)
							OtherTrade.add(asset.assetid);

				tradeListener.onOfferUpdated();
			} else {
				if (session.version > session.version) {
					// uh oh...we missed a version! abort.
					abort = true;
					tradeListener.onError(Error.VERSION_MISMATCH);
				}
			}

			if (lastEvent < status.events.size()) {
				for (; lastEvent < status.events.size(); lastEvent++) {
					final TradeEvent evt = status.events.get(lastEvent);
					Log.d("SteamTrade", "Got new Event: " + evt.action + " -: " + evt.toString());
					isUs = !evt.steamid.equals(String.valueOf(otherID.convertToLong()));

					switch (evt.action) {
					case 0: // add item
						if (!isUs) {
							if (OtherInventory == null)
								loadPrivateBP(evt);
							SteamInventoryItem item = OtherInventory.getItem(evt.assetid);
							SchemaItem schemaItem = SteamService.singleton.schema.items.get(item.defindex);
							tradeListener.onUserAddItem(schemaItem, item);
						}
						meReady = false;
						otherReady = false;
						break;
					case 1: // remove item
						if (!isUs) {
							if (OtherInventory == null)
								loadPrivateBP(evt);
							SteamInventoryItem item = OtherInventory.getItem(evt.assetid);
							SchemaItem schemaItem = SteamService.singleton.schema.items.get(item.defindex);
							tradeListener.onUserRemoveItem(schemaItem, item);
						}
						meReady = false;
						otherReady = false;
						break;
					case 2: // toggle ready
						if (!isUs) {
							otherReady = true;
							tradeListener.onUserSetReadyState(true);
						} else {
							meReady = true;
						}
						break;
					case 3: // toggle not ready
						if (!isUs) {
							otherReady = false;
							tradeListener.onUserSetReadyState(false);
						} else {
							meReady = false;
						}
						break;
					case 4: // user accept?
						if (!isUs)
							tradeListener.onUserAccept();
						break;
					case 7: // chat
						if (!isUs)
							tradeListener.onMessage(evt.text);
						break;
					default:
						Log.e("Trade", "Unknown Event ID: " + evt.action);
						break;
					}
				}

			}

			if (status.trade_status == 3) {
				tradeListener.onError(Error.CANCELLED);
				abort = true;
			} else if (status.trade_status == 4) {
				tradeListener.onError(Error.TIMED_OUT);
				abort = true;
			} else if (status.trade_status == 5) {
				tradeListener.onError(Error.FAILED);
				abort = true;
			} else if (status.trade_status == 1) {
				tradeListener.onComplete();
				abort = true;
			} else if (status.trade_status == 0) {
				// nothing happened
			} else {
				Log.d("SteamTrade", "Unknown trade status: " + status.trade_status);
			}

			// abort trade if an error occurs...or trade completed
			if (abort) {
				//error
				die = true;
				MainActivity.instance.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						SteamService.singleton.tradeManager.notifyTradeHasEnded();
					}
				});
			}

			// Update Local Variables
			if (status.them != null) {
				otherReady = status.them.ready == 1 ? true : false;
				meReady = status.me.ready == 1 ? true : false;
			}

			// Update version (or logpos!)
			if (status.newversion)
				tradeListener.onNewVersion();
			if (status.logpos != 0)
				session.logpos = status.logpos;
		}
	}

	public void loadPrivateBP(TradeEvent evt) {
		OtherInventory = session.getForeignInventory(otherID, evt.contextid).asInventory();
	}

	public boolean sendMessage(String message) {
		return session.sendMessageWebCommand(message);
	}

	public boolean addItem(long itemid) {
		int slot = nextTradeSlot();
		boolean result = session.addItemWebCommand(itemid, slot);
		if (result) {
			MyTrade.add(itemid);
			UsedSlots.put(itemid, slot);
		}
		return result;
	}

	private int nextTradeSlot() {
		int slot = 0;
		while (UsedSlots.containsValue(slot))
			slot++;
		return slot;
	}

	public boolean removeItem(long itemid) {
		boolean result = session.removeItemWebCommand(itemid, UsedSlots.get(itemid));
		if (result) {
			MyTrade.remove(itemid);
			UsedSlots.remove(itemid);
		}
		return result;
	}

	public boolean setReady(boolean ready) {
		// TODO verify local items...
		return session.setReadyWebCommand(ready);
	}

	public boolean cancelTrade() {
		return session.cancelTradeWebCommand();
	}

	public boolean acceptTrade() {
		return session.acceptTradeWebCommand();
	}

	protected String fetch(String url, String method, Map<String, String> data) {
		return fetch(url, method, data, true);
	}

	protected String fetch(String url, String method, Map<String, String> data, boolean sendLoginData) {
		String cookies = "";
		if (sendLoginData)
			cookies = "sessionid=" + sessionId + ";steamLogin=" + steamLogin;
		final String response = SteamWeb.request(url, method, data, cookies);
		return response;
	}
}
