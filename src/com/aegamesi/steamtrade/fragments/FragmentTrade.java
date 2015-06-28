package com.aegamesi.steamtrade.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.OnTabSelectedListener;
import android.support.design.widget.TabLayout.Tab;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.aegamesi.lib.android.ExpandableHeightGridView;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.ChatAdapter;
import com.aegamesi.steamtrade.fragments.support.ItemListAdapter;
import com.aegamesi.steamtrade.fragments.support.ItemListView;
import com.aegamesi.steamtrade.steam.DBHelper.ChatEntry;
import com.aegamesi.steamtrade.steam.SteamChatManager;
import com.aegamesi.steamtrade.steam.SteamItemUtil;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.trade2.Trade;
import com.nosoop.steamtrade.TradeListener.TradeStatusCodes;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nosoop.steamtrade.inventory.TradeInternalInventories;
import com.nosoop.steamtrade.inventory.TradeInternalInventory;
import com.nosoop.steamtrade.inventory.TradeInternalItem;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentTrade extends FragmentBase implements OnClickListener, AdapterView.OnItemSelectedListener {
	public static int[] tab_notifications = new int[]{0, 0, 0};
	public TabLayout tab_layout;
	public View[] tab_views;

	public Spinner tabInventorySelect;
	public ArrayAdapter<AppContextPair> tabInventorySelectAdapter;
	public View tabInventoryLoading;
	public EditText tabInventorySearch;
	public ItemListView tabInventoryList;
	//
	public CheckBox tabOfferMeReady;
	public CheckBox tabOfferOtherReady;
	public ExpandableHeightGridView tabOfferMeOffer;
	public ExpandableHeightGridView tabOfferOtherOffer;
	public Button tabOfferAccept;
	public Button tabOfferCancel;
	public ItemListAdapter tabOfferMeOfferAdapter;
	public ItemListAdapter tabOfferOtherOfferAdapter;
	public ProgressBar tabOfferStatusCircle;
	//
	public ChatAdapter tabChatAdapter;
	public RecyclerView tabChatList;
	public EditText tabChatInput;
	public ImageButton tabChatButton;
	public Cursor tabChatCursor;
	public LinearLayoutManager tabChatLayoutManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		tab_views = new View[3];
	}

	@Override
	public void onResume() {
		super.onResume();
		if (trade() == null)
			return;
		// etc.
		String friendName = activity().steamFriends.getFriendPersonaName(new SteamID(SteamService.singleton.tradeManager.currentTrade.otherSteamId));
		setTitle(String.format(activity().getString(R.string.trading_with), friendName));
		SteamService.singleton.tradeManager.tradeStatus.setVisibility(View.GONE);
		// update UI
		updateUIInventory();
		updateUIOffers();
		updateUIChat();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_trade, container, false);
		tab_views[0] = view.findViewById(R.id.trade_tab_inventory);
		tab_views[1] = view.findViewById(R.id.trade_tab_offerings);
		tab_views[2] = view.findViewById(R.id.trade_tab_chat);
		tab_layout = (TabLayout) view.findViewById(R.id.tabs);
		tab_layout.setOnTabSelectedListener(new OnTabSelectedListener() {
			@Override
			public void onTabSelected(Tab tab) {
				int i = tab.getPosition();
				View tabView = tab_views[i];
				tabView.setVisibility(View.VISIBLE);
				tabView.bringToFront();
				tabView.invalidate();

				tab_notifications[i] = 0;
				updateUITabButton(i);
				if (i == 0)
					updateUIInventory();
				if (i == 1)
					updateUIOffers();
				if (i == 2) {
					if (tabChatAdapter != null)
						tabChatAdapter.time_last_read = System.currentTimeMillis();
					updateUIChat();
				}
			}

			@Override
			public void onTabUnselected(Tab tab) {
				tab_views[tab.getPosition()].setVisibility(View.GONE);
			}

			@Override
			public void onTabReselected(Tab tab) {
				onTabSelected(tab);
			}
		});
		for (int i = 0; i < tab_views.length; i++) {
			tab_layout.addTab(tab_layout.newTab(), i == 0);
			updateUITabButton(i);
		}
		if (trade() == null)
			return view;

		// TAB 0: Inventory
		tabInventorySelect = (Spinner) tab_views[0].findViewById(R.id.inventory_select);
		tabInventorySelect.setOnItemSelectedListener(this);
		tabInventorySelectAdapter = new ArrayAdapter<AppContextPair>(activity(), android.R.layout.simple_spinner_item);
		tabInventorySelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		tabInventorySelect.setAdapter(tabInventorySelectAdapter);
		tabInventoryList = (ItemListView) tab_views[0].findViewById(R.id.itemlist);
		tabInventoryList.setProvider(new ItemListView.IItemListProvider() {
			@Override
			public void onItemChecked(final TradeInternalAsset item, final boolean checked) {
				final Trade trade = trade();
				trade.run(new Runnable() {
					@Override
					public void run() {
						if (checked)
							trade.listener.tradePutItem((TradeInternalItem) item);
						else
							trade.listener.tradeRemoveItem((TradeInternalItem) item);
					}
				});

				tabOfferMeReady.setChecked(false);
				tabOfferOtherReady.setChecked(false);
				tabOfferAccept.setEnabled(false);
				tabOfferStatusCircle.setVisibility(View.GONE);
			}

			@Override
			public boolean shouldItemBeChecked(TradeInternalAsset item) {
				return trade().session.getSelf().getOffer().contains(item);
			}
		});
		tabInventoryLoading = tab_views[0].findViewById(R.id.inventory_loading);
		tabInventorySearch = (EditText) tab_views[0].findViewById(R.id.inventory_search);
		tabInventorySearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				tabInventoryList.filter(s.toString());
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		// TAB 1: Offers
		tabOfferMeReady = (CheckBox) tab_views[1].findViewById(R.id.trade_offer_myready);
		tabOfferMeReady.setOnClickListener(this);
		tabOfferOtherReady = (CheckBox) tab_views[1].findViewById(R.id.trade_offer_otherready);

		tabOfferMeOffer = (ExpandableHeightGridView) tab_views[1].findViewById(R.id.trade_offer_mylist);
		tabOfferMeOffer.setExpanded(true);
		tabOfferMeOfferAdapter = new ItemListAdapter(activity(), tabOfferMeOffer, false, null);
		tabOfferMeOffer.setAdapter(tabOfferMeOfferAdapter);

		tabOfferOtherOffer = (ExpandableHeightGridView) tab_views[1].findViewById(R.id.trade_offer_otherlist);
		tabOfferOtherOffer.setExpanded(true);
		tabOfferOtherOfferAdapter = new ItemListAdapter(activity(), tabOfferOtherOffer, false, null);
		tabOfferOtherOffer.setAdapter(tabOfferOtherOfferAdapter);

		tabOfferAccept = (Button) tab_views[1].findViewById(R.id.trade_offer_accept);
		tabOfferAccept.setOnClickListener(this);
		tabOfferCancel = (Button) tab_views[1].findViewById(R.id.trade_offer_cancel);
		tabOfferCancel.setOnClickListener(this);
		tabOfferStatusCircle = (ProgressBar) tab_views[1].findViewById(R.id.trade_status_progress);
		// TAB 2: Chat
		tabChatList = (RecyclerView) view.findViewById(R.id.chat);
		tabChatInput = (EditText) view.findViewById(R.id.chat_input);
		tabChatButton = (ImageButton) view.findViewById(R.id.chat_button);
		tabChatButton.setOnClickListener(this);

		view.findViewById(R.id.friend_info).setVisibility(View.GONE); // TODO readd this

		boolean isCompact = PreferenceManager.getDefaultSharedPreferences(activity()).getBoolean("pref_chat_compact", false);
		tabChatAdapter = new ChatAdapter(tabChatCursor, isCompact);
		tabChatAdapter.time_last_read = 0;
		tabChatList.setAdapter(tabChatAdapter);
		tabChatLayoutManager = new LinearLayoutManager(activity());
		tabChatLayoutManager.setStackFromEnd(true);
		tabChatList.setHasFixedSize(true);
		tabChatList.setLayoutManager(tabChatLayoutManager);
		tabChatList.setAdapter(tabChatAdapter);
		return view;
	}

	@Override
	public void onPause() {
		super.onPause();

		if (SteamService.singleton != null && SteamService.singleton.tradeManager != null)
			SteamService.singleton.tradeManager.updateTradeStatus();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.item_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_inventory_toggle_view:
				int new_list_mode = tabInventoryList.getListMode() == ItemListAdapter.MODE_GRID ? ItemListAdapter.MODE_LIST : ItemListAdapter.MODE_GRID;
				item.setIcon((new_list_mode == ItemListAdapter.MODE_GRID) ? R.drawable.ic_view_list : R.drawable.ic_view_module);

				tabInventoryList.setListMode(new_list_mode);
				tabOfferMeOfferAdapter.setListMode(new_list_mode);
				tabOfferOtherOfferAdapter.setListMode(new_list_mode);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public Trade trade() {
		return SteamService.singleton.tradeManager.currentTrade;
	}

	public void updateUIInventory() {
		// first do inventory select
		if (activity() == null)
			return;
		activity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (trade() == null || tabInventorySelectAdapter == null)
					return;
				Log.d("Trade", "Inventories #: " + trade().session.myAppContextData.size());
				boolean first_load = tabInventorySelectAdapter.getCount() == 0 && trade().session.myAppContextData.size() > 0;
				tabInventorySelectAdapter.clear();
				List<AppContextPair> appContextPairs = trade().session.myAppContextData;
				for (AppContextPair pair : appContextPairs)
					tabInventorySelectAdapter.add(pair);
				tabInventorySelectAdapter.notifyDataSetChanged();

				TradeInternalInventory currentInv = null;
				AppContextPair pair = (AppContextPair) tabInventorySelect.getSelectedItem();
				TradeInternalInventories inventories = trade().session.getSelf().getInventories();
				if (inventories.hasInventory(pair))
					currentInv = inventories.getInventory(pair);

				if (currentInv == null) {
					tabInventoryLoading.setVisibility(View.VISIBLE);
					tabInventoryList.setVisibility(View.GONE);
				} else {
					tabInventoryLoading.setVisibility(View.GONE);
					tabInventoryList.setVisibility(View.VISIBLE);
					tabInventoryList.setItems(currentInv.getItemList());
				}

				if (first_load) {
					// select last selected inventory
					SharedPreferences prefs = activity().getPreferences(Context.MODE_PRIVATE);
					int pref_appid = prefs.getInt("inv_last_appid", -1);
					long pref_context = prefs.getLong("inv_last_context", -1);
					int pref_index = -1;
					for (int i = 0; i < appContextPairs.size(); i++)
						if (appContextPairs.get(i).getAppid() == pref_appid && appContextPairs.get(i).getContextid() == pref_context)
							pref_index = i;
					if (pref_index != -1)
						tabInventorySelect.setSelection(pref_index);
					// end
				}

			}
		});
	}

	public void updateUIOffers() {
		if (trade() == null || trade().session == null || trade().session.getSelf() == null || trade().session.getPartner() == null)
			return;
		tabOfferMeReady.setChecked(trade().session.getSelf().isReady());
		tabOfferOtherReady.setChecked(trade().session.getPartner().isReady());
		tabOfferAccept.setEnabled(tabOfferMeReady.isChecked() && tabOfferOtherReady.isChecked());

		tabOfferMeOfferAdapter.setItemList(new ArrayList<TradeInternalAsset>(trade().session.getSelf().getOffer()));
		tabOfferOtherOfferAdapter.setItemList(new ArrayList<TradeInternalAsset>(trade().session.getPartner().getOffer()));
		updateUITabButton(1);
	}

	public void updateUITabButton(int num) {
		if (tab_layout != null) {
			Tab tab = tab_layout.getTabAt(num);
			String text = activity().getResources().getStringArray(R.array.trade_tabs)[num];
			if (tab_notifications[num] > 0)
				text += " (" + tab_notifications[num] + ")";
			tab.setText(text);
		}
	}

	@Override
	public void onClick(View v) {
		if (v == tabOfferMeReady) {
			final boolean meReady = tabOfferMeReady.isChecked(); // avoid race condition
			trade().run(new Runnable() {
				@Override
				public void run() {
					trade().session.getCmds().setReady(meReady);
				}
			});
			tabOfferAccept.setEnabled(tabOfferMeReady.isChecked() && tabOfferOtherReady.isChecked());
			tabOfferStatusCircle.setVisibility(View.GONE);
		}
		if (v == tabOfferCancel) {
			// this is on the correct thread. No need for a runnable
			SteamService.singleton.tradeManager.cancelTrade();
		}
		if (v == tabOfferAccept) {
			tabOfferStatusCircle.setVisibility(View.VISIBLE);
			trade().run(new Runnable() {
				@Override
				public void run() {
					try {
						trade().session.getCmds().acceptTrade();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			});
		}
		if (v == tabChatButton) {
			final String message;
			if ((message = tabChatInput.getText().toString().trim()).length() == 0)
				return;
			tabChatInput.setText("");
			trade().run(new Runnable() {
				@Override
				public void run() {
					trade().session.getCmds().sendMessage(message);
				}
			});

			// log line
			SteamService.singleton.chatManager.broadcastMessage(
					System.currentTimeMillis(),
					SteamService.singleton.steamClient.getSteamId(),
					new SteamID(trade().otherSteamId),
					true,
					SteamChatManager.CHAT_TYPE_TRADE,
					message
			);
			updateUIChat();
		}
	}

	public void updateUIChat() {
		// fetch a new cursor
		if (tabChatAdapter != null) {
			String friendName = activity().steamFriends.getFriendPersonaName(new SteamID(SteamService.singleton.tradeManager.currentTrade.otherSteamId));
			tabChatAdapter.setPersonaNames(activity().steamFriends.getPersonaName(), friendName);
			tabChatAdapter.color_default = getResources().getColor(R.color.steam_online);
			tabChatAdapter.changeCursor(tabChatCursor = fetchCursor());

			// now scroll to bottom (if already near the bottom)
			if (tabChatLayoutManager.findLastVisibleItemPosition() > tabChatCursor.getCount() - 3)
				tabChatList.scrollToPosition(tabChatCursor.getCount() - 1);
		}

		// TODO redo this
		if (tab_views[2].getVisibility() != View.VISIBLE) {
			tab_notifications[2]++;
			updateUITabButton(2);
		}
	}

	private Cursor fetchCursor() {
		return SteamService.singleton.db().query(
				ChatEntry.TABLE,                    // The table to query
				new String[]{ChatEntry._ID, ChatEntry.COLUMN_TIME, ChatEntry.COLUMN_MESSAGE, ChatEntry.COLUMN_SENDER},
				ChatEntry.COLUMN_OUR_ID + " = ? AND " + ChatEntry.COLUMN_OTHER_ID + " = ? AND " + ChatEntry.COLUMN_TYPE + " = ? AND " + ChatEntry.COLUMN_TIME + " > ?",
				new String[]{"" + SteamService.singleton.steamClient.getSteamId().convertToLong(), "" + trade().otherSteamId, "" + SteamChatManager.CHAT_TYPE_TRADE, "" + (trade().session.TIME_TRADE_START - 60000)},
				null, // don't group the rows
				null, // don't filter by row groups
				ChatEntry.COLUMN_TIME + " ASC"
		);
	}

	public void onError(int code, String message) {
		if (activity() == null || getView() == null)
			return;
		((ViewGroup) getView()).removeAllViews();
		View result = activity().getLayoutInflater().inflate(R.layout.trade_result_error, null, false);
		((ViewGroup) getView()).addView(result);
		TextView errorTitle = (TextView) result.findViewById(R.id.trade_error_title);
		TextView errorText = (TextView) result.findViewById(R.id.trade_error_text);
		errorText.setText(message);

		if (code == TradeStatusCodes.TRADE_REQUIRES_CONFIRMATION) {
			// this isn't really an error-- just a sort of notification that the trade hasn't been completed *yet*
			// as such, adjust the message
			errorTitle.setText(R.string.trade_completed);
		}
	}

	public void onCompleted(List<TradeInternalAsset> items) {
		if (activity() == null || getView() == null)
			return;
		((ViewGroup) getView()).removeAllViews();
		View result = activity().getLayoutInflater().inflate(R.layout.trade_result_success, null, false);
		((ViewGroup) getView()).addView(result);
		TextView successText = (TextView) result.findViewById(R.id.trade_success_text);
		successText.setText(String.format(activity().getString(R.string.trade_new_items), items.size()));
		ListView itemList = (ListView) result.findViewById(R.id.trade_result_items);
		itemList.setAdapter(new ResultsListAdapter(items));
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (parent == tabInventorySelect) {
			AppContextPair pair = (AppContextPair) tabInventorySelect.getSelectedItem();
			SharedPreferences.Editor prefs = activity().getPreferences(Context.MODE_PRIVATE).edit();
			prefs.putInt("inv_last_appid", pair.getAppid());
			prefs.putLong("inv_last_context", pair.getContextid());
			prefs.apply();

			updateUIInventory();
			trade().run(new Runnable() {
				@Override
				public void run() {
					trade().session.loadOwnInventory((AppContextPair) tabInventorySelect.getSelectedItem());
					updateUIInventory();
				}
			});
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
	}

	// RESULTS: LIST ADAPTER
	public class ResultsListAdapter extends BaseAdapter {
		public List<TradeInternalAsset> items;

		public ResultsListAdapter(List<TradeInternalAsset> items) {
			this.items = items;
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public Object getItem(int position) {
			return items.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null)
				v = activity().getLayoutInflater().inflate(R.layout.view_item_info, null);

			TradeInternalItem item = (TradeInternalItem) getItem(position);
			SteamItemUtil.populateItemInfo(v, item, null);
			return v;
		}
	}
}