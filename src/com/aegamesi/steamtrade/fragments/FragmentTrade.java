package com.aegamesi.steamtrade.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.FragmentChat.ChatAdapter;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatLine;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.trade2.Trade;
import com.aegamesi.steamtrade.trade2.TradeUtil;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nosoop.steamtrade.inventory.TradeInternalInventories;
import com.nosoop.steamtrade.inventory.TradeInternalInventory;
import com.nosoop.steamtrade.inventory.TradeInternalItem;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentTrade extends FragmentBase implements OnClickListener, OnItemClickListener, AdapterView.OnItemSelectedListener {
	public Button[] tab_buttons;
	public View[] tab_views;
	public static int[] tab_notifications = new int[]{0, 0, 0};
	public int tab_selected;

	public Spinner tabInventorySelect;
	public ArrayAdapter<AppContextPair> tabInventorySelectAdapter;
	public ListView tabInventoryList;
	public View tabInventoryLoading;
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

		tab_views = new View[3];
		tab_buttons = new Button[3];

		fragmentName = "FragmentTrade";
	}

	@Override
	public void onResume() {
		super.onResume();
		if (trade() == null)
			return;
		// etc.
		String friendName = activity().steamFriends.getFriendPersonaName(new SteamID(SteamService.singleton.tradeManager.currentTrade.otherSteamId));
		activity().getSupportActionBar().setTitle(String.format(activity().getString(R.string.trading_with), friendName));
		SteamService.singleton.tradeManager.tradeStatus.setVisibility(View.GONE);
		// update UI
		updateUIInventory();
		updateUIOffers();
	}

	@Override
	public void onPause() {
		super.onPause();

		SteamService.singleton.tradeManager.updateTradeStatus();
	}

	public void updateUITabButton(int num) {
		String text = activity().getResources().getStringArray(R.array.trade_tabs)[num];
		if (tab_notifications[num] > 0)
			text += " (" + tab_notifications[num] + ")";
		if (tab_buttons[num] != null)
			tab_buttons[num].setText(text);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_trade, container, false);
		tab_views[0] = view.findViewById(R.id.trade_tab_inventory);
		tab_views[1] = view.findViewById(R.id.trade_tab_offerings);
		tab_views[2] = view.findViewById(R.id.trade_tab_chat);
		tab_buttons[0] = (Button) view.findViewById(R.id.trade_button_inventory);
		tab_buttons[1] = (Button) view.findViewById(R.id.trade_button_offerings);
		tab_buttons[2] = (Button) view.findViewById(R.id.trade_button_chat);
		for (int i = 0; i < tab_buttons.length; i++) {
			tab_buttons[i].setOnClickListener(this);
			updateUITabButton(i);
		}
		onClick(tab_buttons[0]); // just to show the tab #0
		if (trade() == null)
			return view;

		// TAB 0: Inventory
		tabInventorySelect = (Spinner) tab_views[0].findViewById(R.id.trade_tab_inventory_select);
		tabInventorySelect.setOnItemSelectedListener(this);
		tabInventorySelectAdapter = new ArrayAdapter<AppContextPair>(activity(), android.R.layout.simple_spinner_item);
		tabInventorySelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		tabInventorySelect.setAdapter(tabInventorySelectAdapter);
		tabInventoryList = (ListView) tab_views[0].findViewById(R.id.trade_tab_inventory_list);
		tabInventoryList.setAdapter(tabInventoryListAdapter = new TabInventoryListAdapter());
		tabInventoryList.setOnItemClickListener(this);
		tabInventoryListAdapter.filter("");
		tabInventoryLoading = tab_views[0].findViewById(R.id.trade_tab_inventory_loading);
		tabInventorySearch = (EditText) tab_views[0].findViewById(R.id.inventory_search);
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
		tabOfferMeReady = (CheckBox) tab_views[1].findViewById(R.id.trade_offer_myready);
		tabOfferMeReady.setOnClickListener(this);
		tabOfferOtherReady = (CheckBox) tab_views[1].findViewById(R.id.trade_offer_otherready);
		tabOfferMeOffer = (ListView) tab_views[1].findViewById(R.id.trade_offer_mylist);
		tabOfferMeOffer.setAdapter(tabOfferMeOfferAdapter = new TabOfferingsListAdapter(true));
		tabOfferMeOffer.setOnItemClickListener(this);
		tabOfferOtherOffer = (ListView) tab_views[1].findViewById(R.id.trade_offer_otherlist);
		tabOfferOtherOffer.setAdapter(tabOfferOtherOfferAdapter = new TabOfferingsListAdapter(false));
		tabOfferOtherOffer.setOnItemClickListener(this);
		tabOfferAccept = (Button) tab_views[1].findViewById(R.id.trade_offer_accept);
		tabOfferAccept.setOnClickListener(this);
		tabOfferCancel = (Button) tab_views[1].findViewById(R.id.trade_offer_cancel);
		tabOfferCancel.setOnClickListener(this);
		tabOfferStatusCircle = (ProgressBar) tab_views[1].findViewById(R.id.trade_status_progress);
		// TAB 2: Chat
		tabChatList = (ListView) view.findViewById(R.id.chat);
		tabChatInput = (EditText) view.findViewById(R.id.chat_input);
		tabChatButton = (Button) view.findViewById(R.id.chat_button);
		tabChatButton.setOnClickListener(this);
		tabChatList.setAdapter(tabChatAdapter = new ChatAdapter());
		ArrayList<ChatLine> tabChatBacklog = SteamService.singleton.chat.getChatHistory(new SteamID(trade().otherSteamId), "t", "Trade Started");
		if (tabChatBacklog != null)
			for (ChatLine line : tabChatBacklog)
				tabChatAdapter.addChatLine(line);
		tabChatList.setSelection(tabChatList.getCount() - 1);
		return view;
	}

	public void updateUIInventory() {
		// first do inventory select
		activity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (trade() == null || tabInventorySelectAdapter == null)
					return;
				Log.d("Trade", "Inventories #: " + trade().session.myAppContextData.size());
				tabInventorySelectAdapter.clear();
				List<AppContextPair> appContextPairs = trade().session.myAppContextData;
				for (AppContextPair pair : appContextPairs)
					tabInventorySelectAdapter.add(pair);
				tabInventorySelectAdapter.notifyDataSetChanged();

				if (tabInventoryListAdapter.getInventory() == null) {
					tabInventoryLoading.setVisibility(View.VISIBLE);
					tabInventoryList.setVisibility(View.GONE);
				} else {
					tabInventoryLoading.setVisibility(View.GONE);
					tabInventoryList.setVisibility(View.VISIBLE);
					tabInventoryListAdapter.filter("");
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

		tabOfferMeOfferAdapter.notifyDataSetChanged();
		tabOfferOtherOfferAdapter.notifyDataSetChanged();
		updateUITabButton(1);
	}

	public void updateUIChat(SteamID sender, String message) {
		if (tabChatAdapter != null) {
			ChatLine line = new ChatLine();
			line.message = message;
			line.steamId = sender;
			line.time = (new Date()).getTime();
			tabChatAdapter.addChatLine(line);
			SteamService.singleton.chat.logLine(line, line.steamId, "t");
		}
		if (tab_selected != 2) {
			tab_notifications[2]++;
			updateUITabButton(2);
		}
	}

	public void onError(String error) {
		if (activity() == null || getView() == null)
			return;
		((ViewGroup) getView()).removeAllViews();
		View result = activity().getLayoutInflater().inflate(R.layout.trade_result_error, null, false);
		((ViewGroup) getView()).addView(result);
		TextView errorText = (TextView) result.findViewById(R.id.trade_error_text);
		errorText.setText(error);
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

	public Trade trade() {
		return SteamService.singleton.tradeManager.currentTrade;
	}

	@Override
	public void onClick(View v) {
		if (v == tabOfferMeReady) {
			trade().run(new Runnable() {
				@Override
				public void run() {
					trade().session.getCmds().setReady(tabOfferMeReady.isChecked());
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

			ChatLine chatline = new ChatLine();
			chatline.steamId = null;
			chatline.message = message;
			chatline.time = (new Date()).getTime();
			SteamService.singleton.chat.logLine(chatline, new SteamID(trade().otherSteamId), "t");
			tabChatAdapter.addChatLine(chatline);
		}
		for (int i = 0; i < tab_buttons.length; i++) {
			if (v == tab_buttons[i]) {
				for (int j = 0; j < tab_buttons.length; j++) { // first switch to that tab
					tab_views[j].setVisibility((i == j) ? View.VISIBLE : View.GONE);
					if (i == j) {
						tab_views[j].getParent().bringChildToFront(tab_views[i]);
					}
				}
				tab_views[i].getParent().requestLayout();
				tab_notifications[i] = 0;
				updateUITabButton(i);
				if (i == 0)
					updateUIInventory();
				if (i == 1)
					updateUIOffers();
				break;
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		TradeInternalAsset asset = (TradeInternalAsset) parent.getAdapter().getItem(position);
		TradeUtil.showItemInfo(getActivity(), asset, trade().session.myAppContextData);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (parent == tabInventorySelect) {
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

	// TAB INVENTORY: LIST ADAPTER
	private class TabInventoryListAdapter extends BaseAdapter {
		List<TradeInternalItem> filteredList = new ArrayList<TradeInternalItem>();

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
				v = activity().getLayoutInflater().inflate(R.layout.inventory_trade_item, null);
			final TradeInternalItem item = (TradeInternalItem) getItem(position);

			final CheckBox itemCheckbox = (CheckBox) v.findViewById(R.id.trade_item_checkbox);
			itemCheckbox.setChecked(trade().session.getSelf().getOffer().contains(item));
			itemCheckbox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					final Trade trade = trade();
					final boolean checked = ((CheckBox) view).isChecked();
					trade.run(new Runnable() {
						@Override
						public void run() {
							if (checked)
								trade.listener.tradePutItem(item);
							else
								trade.listener.tradeRemoveItem(item);
						}
					});

					tabOfferMeReady.setChecked(false);
					tabOfferOtherReady.setChecked(false);
					tabOfferAccept.setEnabled(false);
					tabOfferStatusCircle.setVisibility(View.GONE);
				}
			});

			// return 'https://steamcommunity-a.akamaihd.net/economy/image/' + v_trim(imageName) + strSize;
			TextView itemName = ((TextView) v.findViewById(R.id.inventory_item_name));
			itemName.setText(item.getDisplayName());
			if (item.getNameColor() != 0)
				itemName.setTextColor(item.getNameColor());
			else
				itemName.setTextColor(Color.rgb(198, 198, 198));

			return v;
		}

		public void filter(String by) {
			filteredList.clear();
			if (getInventory() == null)
				return;
			if (by.trim().length() == 0) {
				filteredList.addAll(getInventory().getItemList());
			} else {
				List<TradeInternalItem> items = getInventory().getItemList();
				for (TradeInternalItem item : items)
					if (item.getDisplayName().toLowerCase(Locale.ENGLISH).contains(by.toLowerCase(Locale.ENGLISH)))
						filteredList.add(item);
			}
			notifyDataSetChanged();
		}

		public TradeInternalInventory getInventory() {
			if (trade() == null)
				return null;
			AppContextPair pair = (AppContextPair) tabInventorySelect.getSelectedItem();
			TradeInternalInventories inventories = trade().session.getSelf().getInventories();
			if (inventories.hasInventory(pair))
				return inventories.getInventory(pair);

			return null;
		}
	}

	// TAB OFFERINGS: LIST ADAPTER
	public class TabOfferingsListAdapter extends BaseAdapter {
		private List<TradeInternalAsset> cachedList;
		public boolean self = false;

		public TabOfferingsListAdapter(boolean self) {
			this.self = self;
		}

		@Override
		public int getCount() {
			return cachedList == null ? 0 : cachedList.size();
		}

		@Override
		public Object getItem(int position) {
			return (position >= getCount()) ? null : cachedList.get(position);
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
			TradeInternalAsset item = (TradeInternalAsset) getItem(position);

			if (item != null) {
				TextView itemName = ((TextView) v.findViewById(R.id.inventory_item_name));
				itemName.setText(item.getDisplayName());
				if (item.getNameColor() != 0)
					itemName.setTextColor(item.getNameColor());
				else
					itemName.setTextColor(Color.rgb(198, 198, 198));
			}
			return v;
		}

		@Override
		public void notifyDataSetChanged() {
			cachedList = new ArrayList<TradeInternalAsset>(getOffer());
			super.notifyDataSetChanged();
		}

		private Set<TradeInternalAsset> getOffer() {
			if (self)
				return trade().session.getSelf().getOffer();
			else
				return trade().session.getPartner().getOffer();
		}
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
			TradeUtil.populateItemInfo(v, item, trade().session.myAppContextData);
			return v;
		}
	}
}