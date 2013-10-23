package com.aegamesi.steamtrade.fragments;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.FragmentChat.ChatAdapter;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatLine;
import com.aegamesi.steamtrade.steam.SteamInventory;
import com.aegamesi.steamtrade.steam.SteamInventory.SteamInventoryItem;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.trade.Trade;
import com.aegamesi.steamtrade.trade.Trade.Error;

public class FragmentTrade extends FragmentBase implements OnClickListener, OnItemClickListener {
	public View[] views;
	public static int[] notifications;

	public ListView tabInventoryList;
	public EditText tabInventorySearch;
	public TabInventoryListAdapter tabInventoryListAdapter;
	//
	public CheckBox tabOfferMeReady;
	public CheckBox tabOfferOtherReady;
	public ListView tabOfferMeOffer;
	public ListView tabOfferOtherOffer;
	public Button tabOfferAccept;
	public Button tabOfferCancel;
	public TabOfferingsListAdapter tabOfferMeOfferAdapter;
	public TabOfferingsListAdapter tabOfferOtherOfferAdapter;
	public ProgressBar tabOfferStatusCircle;
	//
	public ChatAdapter tabChatAdapter;
	public ListView tabChatList;
	public EditText tabChatInput;
	public Button tabChatButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		views = new View[3];
		notifications = new int[] { 0, 0, 0 };

		fragmentName = "FragmentTrade";
	}

	@Override
	public void onResume() {
		super.onResume();
		if (trade() == null)
			return;
		// tabs
		ActionBar actionBar = activity().getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ActionBar.TabListener tabListener = new ActionBar.TabListener() {
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				views[tab.getPosition()].setVisibility(View.VISIBLE);
				notifications[tab.getPosition()] = 0;
				updateTab(tab.getPosition());
			}

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				views[tab.getPosition()].setVisibility(View.GONE);
			}

			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				// ignore
			}
		};

		for (int n = 0; n < 3; n++) {
			actionBar.addTab(actionBar.newTab().setTabListener(tabListener));
			updateTab(n);
		}
		// etc.
		String friendName = SteamService.singleton.steamClient.getHandler(SteamFriends.class).getFriendPersonaName(SteamService.singleton.tradeManager.currentTrade.otherID);
		activity().getSupportActionBar().setTitle(String.format(activity().getString(R.string.trading_with), friendName));
		SteamService.singleton.tradeManager.tradeStatus.setVisibility(View.GONE);
	}

	@Override
	public void onPause() {
		super.onPause();

		ActionBar actionBar = activity().getSupportActionBar();
		actionBar.removeAllTabs();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		SteamService.singleton.tradeManager.updateTradeStatus();
	}

	public void updateTab(int num) {
		String text = activity().getResources().getStringArray(R.array.trade_tabs)[num];
		if (notifications[num] > 0)
			text += " (" + notifications[num] + ")";
		activity().getSupportActionBar().getTabAt(num).setText(text);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_trade, container, false);
		views[0] = view.findViewById(R.id.trade_tab_inventory);
		views[1] = view.findViewById(R.id.trade_tab_offerings);
		views[2] = view.findViewById(R.id.trade_tab_chat);
		if (trade() == null)
			return view;

		// TAB 0: Inventory
		tabInventoryList = (ListView) views[0].findViewById(R.id.trade_tab_inventory_list);
		tabInventoryList.setAdapter(tabInventoryListAdapter = new TabInventoryListAdapter());
		tabInventoryList.setOnItemClickListener(this);
		tabInventoryListAdapter.filter("");
		tabInventorySearch = (EditText) views[0].findViewById(R.id.inventory_search);
		tabInventorySearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				tabInventoryListAdapter.filter(s.toString());
			}
		});
		// TAB 1: Offers
		tabOfferMeReady = (CheckBox) views[1].findViewById(R.id.trade_offer_myready);
		tabOfferMeReady.setOnClickListener(this);
		tabOfferOtherReady = (CheckBox) views[1].findViewById(R.id.trade_offer_otherready);
		tabOfferMeOffer = (ListView) views[1].findViewById(R.id.trade_offer_mylist);
		tabOfferMeOffer.setAdapter(tabOfferMeOfferAdapter = new TabOfferingsListAdapter(trade().MyTrade, trade().MyInventory));
		tabOfferMeOffer.setOnItemClickListener(this);
		tabOfferOtherOffer = (ListView) views[1].findViewById(R.id.trade_offer_otherlist);
		tabOfferOtherOffer.setAdapter(tabOfferOtherOfferAdapter = new TabOfferingsListAdapter(trade().OtherTrade, trade().OtherInventory));
		tabOfferOtherOffer.setOnItemClickListener(this);
		tabOfferAccept = (Button) views[1].findViewById(R.id.trade_offer_accept);
		tabOfferAccept.setOnClickListener(this);
		tabOfferCancel = (Button) views[1].findViewById(R.id.trade_offer_cancel);
		tabOfferCancel.setOnClickListener(this);
		tabOfferStatusCircle = (ProgressBar) views[1].findViewById(R.id.trade_status_progress);
		// TAB 2: Chat
		tabChatList = (ListView) view.findViewById(R.id.chat);
		tabChatInput = (EditText) view.findViewById(R.id.chat_input);
		tabChatButton = (Button) view.findViewById(R.id.chat_button);
		tabChatButton.setOnClickListener(this);
		tabChatList.setAdapter(tabChatAdapter = new ChatAdapter());
		ArrayList<ChatLine> tabChatBacklog = SteamService.singleton.chat.getChatHistory(trade().otherID, "t", "Trade Started");
		if (tabChatBacklog != null)
			for (ChatLine line : tabChatBacklog)
				tabChatAdapter.addChatLine(line);
		tabChatList.setSelection(tabChatList.getCount() - 1);
		return view;
	}

	public void onOfferUpdated() {
		tabOfferMeOfferAdapter.notifyDataSetChanged();
		tabOfferOtherOfferAdapter.notifyDataSetChanged();
		if (activity() != null && activity().getSupportActionBar() != null && activity().getSupportActionBar().getSelectedTab() != null && activity().getSupportActionBar().getSelectedTab().getPosition() != 1) {
			notifications[1]++;
			updateTab(1);
		}
	}

	public void onError(Error error) {
		ActionBar actionBar = activity().getSupportActionBar();
		actionBar.removeAllTabs();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		((ViewGroup) getView()).removeAllViews();
		View result = activity().getLayoutInflater().inflate(R.layout.trade_result_error, null, false);
		((ViewGroup) getView()).addView(result);
		TextView errorText = (TextView) result.findViewById(R.id.trade_error_text);
		errorText.setText(error.text);
	}

	public void onCompleted(List<SteamInventoryItem> items) {
		ActionBar actionBar = activity().getSupportActionBar();
		actionBar.removeAllTabs();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		((ViewGroup) getView()).removeAllViews();
		View result = activity().getLayoutInflater().inflate(R.layout.trade_result_success, null, false);
		((ViewGroup) getView()).addView(result);
		TextView successText = (TextView) result.findViewById(R.id.trade_success_text);
		successText.setText(String.format(activity().getString(R.string.trade_new_items), items.size()));
		ListView itemList = (ListView) result.findViewById(R.id.trade_result_items);
		itemList.setAdapter(new ResultsListAdapter(items));
	}

	public void onReceiveMessage(ChatLine line) {
		tabChatAdapter.addChatLine(line);
		if (activity().getSupportActionBar().getSelectedTab().getPosition() != 2) {
			notifications[2]++;
			updateTab(2);
		}
	}

	public Trade trade() {
		return SteamService.singleton.tradeManager.currentTrade;
	}

	@Override
	public void onClick(View v) {
		if (v == tabOfferMeReady) {
			trade().toRun.add(new Runnable() {
				@Override
				public void run() {
					trade().setReady(tabOfferMeReady.isChecked());
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
			trade().toRun.add(new Runnable() {
				@Override
				public void run() {
					trade().acceptTrade();
				}
			});
		}
		if (v == tabChatButton) {
			final String message;
			if ((message = tabChatInput.getText().toString().trim()).length() == 0)
				return;
			tabChatInput.setText("");
			trade().toRun.add(new Runnable() {
				@Override
				public void run() {
					trade().sendMessage(message);
				}
			});

			ChatLine chatline = new ChatLine();
			chatline.steamId = null;
			chatline.message = message;
			chatline.time = (new Date()).getTime();
			SteamService.singleton.chat.logLine(chatline, trade().otherID, "t");
			tabChatAdapter.addChatLine(chatline);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		SteamInventoryItem item = (SteamInventoryItem) parent.getAdapter().getItem(position);
		FragmentInventory.showItemInfo(activity(), item);
	}

	// TAB INVENTORY: LIST ADAPTER
	private class TabInventoryListAdapter extends BaseAdapter {
		List<SteamInventoryItem> filteredList = new ArrayList<SteamInventoryItem>();

		@Override
		public int getCount() {
			return filteredList.size();
		}

		@Override
		public Object getItem(int position) {
			return filteredList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null)
				v = activity().getLayoutInflater().inflate(R.layout.trade_tab_inventory_item, null);
			SteamInventoryItem item = (SteamInventoryItem) getItem(position);

			CheckBox itemCheckbox = (CheckBox) v.findViewById(R.id.trade_item_checkbox);
			itemCheckbox.setChecked(trade().MyTrade.contains(item.id));
			itemCheckbox.setTag(item.id);
			itemCheckbox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					final long id = (Long) view.getTag();
					final Trade trade = trade();
					final boolean checked = ((CheckBox) view).isChecked();
					trade.toRun.add(new Runnable() {
						@Override
						public void run() {
							if (checked)
								trade.addItem(id);
							else
								trade.removeItem(id);
						}
					});
					tabOfferMeReady.setChecked(false);
					tabOfferOtherReady.setChecked(false);
					tabOfferAccept.setEnabled(false);
					tabOfferStatusCircle.setVisibility(View.GONE);
				}
			});

			item.populateListView(v);
			return v;
		}

		public void filter(String by) {
			filteredList.clear();
			if (by.trim().length() == 0) {
				if (trade() != null && trade().MyInventory != null)
					filteredList.addAll(trade().MyInventory.items);
			} else {
				List<SteamInventoryItem> items = trade().MyInventory.items;
				for (SteamInventoryItem item : items)
					if (item.fullname().toLowerCase(Locale.ENGLISH).contains(by.toLowerCase(Locale.ENGLISH)))
						filteredList.add(item);
			}
			notifyDataSetChanged();
		}
	}

	// TAB OFFERINGS: LIST ADAPTER
	public class TabOfferingsListAdapter extends BaseAdapter {
		public List<Long> trade;
		public SteamInventory inventory;

		public TabOfferingsListAdapter(List<Long> trade, SteamInventory inventory) {
			this.trade = trade;
			this.inventory = inventory;
		}

		@Override
		public int getCount() {
			return trade.size();
		}

		@Override
		public Object getItem(int position) {
			return (position >= trade.size()) ? null : inventory.getItem(trade.get(position));
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null)
				v = activity().getLayoutInflater().inflate(R.layout.inventory_item_item, null);
			SteamInventoryItem item = (SteamInventoryItem) getItem(position);

			item.populateListView(v);
			return v;
		}
	}

	// RESULTS: LIST ADAPTER
	public class ResultsListAdapter extends BaseAdapter {
		public List<SteamInventoryItem> items;

		public ResultsListAdapter(List<SteamInventoryItem> items) {
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
				v = activity().getLayoutInflater().inflate(R.layout.view_item_details, null);

			SteamInventoryItem item = (SteamInventoryItem) getItem(position);
			item.populateDetailView(v);
			return v;
		}
	}
}