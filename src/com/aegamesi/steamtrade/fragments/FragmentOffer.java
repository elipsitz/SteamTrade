package com.aegamesi.steamtrade.fragments;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
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
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.trade2.TradeOffer;
import com.aegamesi.steamtrade.trade2.TradeUtil;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nosoop.steamtrade.inventory.TradeInternalInventories;
import com.nosoop.steamtrade.inventory.TradeInternalInventory;
import com.nosoop.steamtrade.inventory.TradeInternalItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FragmentOffer extends FragmentBase implements OnClickListener, OnItemClickListener, AdapterView.OnItemSelectedListener {
	public Button[] tab_buttons;
	public View[] tab_views;
	public static int[] tab_notifications = new int[]{0, 0, 0};
	public int tab_selected;
	public TradeOffer offer = null;

	public View loadingView;
	public TextView loadingStatusView;

	public RadioButton tabInventoryRadioUs;
	public RadioButton tabInventoryRadioThem;
	public Spinner tabInventorySelect;
	public ArrayAdapter<AppContextPair> tabInventorySelectAdapter;
	public ListView tabInventoryList;
	public View tabInventoryLoading;
	public EditText tabInventorySearch;
	public TabInventoryListAdapter tabInventoryListAdapter;
	//
	public ListView tabOfferMeOffer;
	public ListView tabOfferOtherOffer;
	public TabOfferingsListAdapter tabOfferMeOfferAdapter;
	public TabOfferingsListAdapter tabOfferOtherOfferAdapter;
	//
	public TextView tabReviewHeading;
	public TextView tabReviewMessage;
	public EditText tabReviewMessageInput;
	public Button tabReviewAccept;
	public Button tabReviewDecline;
	public Button tabReviewCounter;
	public Button tabReviewSend;
	public Button tabReviewCancel;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		tab_views = new View[3];
		tab_buttons = new Button[3];

		fragmentName = "FragmentOffer";
	}

	private class LoadOfferTask extends AsyncTask<Void, Void, TradeOffer> {
		private Bundle bundle;
		private ProgressDialog dialog;

		protected void onPreExecute() {
			if (loadingView != null)
				loadingView.setVisibility(View.VISIBLE);

			dialog = new ProgressDialog(activity());
			dialog.setIndeterminate(true);
			dialog.setMessage(getString(R.string.offer_loading));
			dialog.show();

			bundle = getArguments();
		}

		protected TradeOffer doInBackground(Void... voids) { // the fuck
			TradeOffer offer = null;

			try {
				if (!bundle.getBoolean("from_existing", false)) {
					// generate a new offer
					long user_id = bundle.getLong("user_id"); // either SteamId#getAccountId or the public url...
					String token = bundle.getString("token"); // only if public url
					offer = TradeOffer.createNewOffer(user_id, token);
				} else {
					// load an existing offer
					long id = bundle.getLong("offer_id");
					offer = TradeOffer.loadFromExistingOffer(id);
					offer.message = bundle.getString("offer_message");
				}
			} catch (Exception e) {
				return null;
			}

			return offer;
		}

		protected void onPostExecute(TradeOffer result) {
			offer = result;

			updateUIInventory();
			updateUIOffers();
			updateUIReview();

			dialog.dismiss();
			if (offer == null) {
				if (loadingStatusView != null)
					loadingStatusView.setText(R.string.offer_error_loading);
			} else {
				if (loadingView != null)
					loadingView.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		(new LoadOfferTask()).execute();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (offer == null)
			return;
		// etc.
		activity().getSupportActionBar().setTitle(String.format(activity().getString(R.string.offer_with), offer.partnerName));
		// update UI
		updateUIInventory();
		updateUIOffers();
		updateUIReview();
	}

	@Override
	public void onPause() {
		super.onPause();

		// TODO save offer temporarily
	}

	public void updateUITabButton(int num) {
		String text = activity().getResources().getStringArray(R.array.offer_tabs)[num];
		if (tab_notifications[num] > 0)
			text += " (" + tab_notifications[num] + ")";
		if (tab_buttons[num] != null)
			tab_buttons[num].setText(text);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_offer, container, false);
		loadingView = view.findViewById(R.id.loading);
		loadingView.setVisibility(offer == null ? View.VISIBLE : View.GONE);
		loadingStatusView = (TextView) view.findViewById(R.id.loading_status);

		tab_views[0] = view.findViewById(R.id.offer_tab_inventories);
		tab_views[1] = view.findViewById(R.id.offer_tab_offerings);
		tab_views[2] = view.findViewById(R.id.offer_tab_review);
		tab_buttons[0] = (Button) view.findViewById(R.id.offer_button_inventories);
		tab_buttons[1] = (Button) view.findViewById(R.id.offer_button_offerings);
		tab_buttons[2] = (Button) view.findViewById(R.id.offer_button_review);
		for (int i = 0; i < tab_buttons.length; i++) {
			tab_buttons[i].setOnClickListener(this);
			updateUITabButton(i);
		}
		onClick(tab_buttons[0]); // just to show the tab #0
		//if (offer == null)
		//	return view;

		// TAB 0: Inventories
		tabInventoryRadioUs = (RadioButton) tab_views[0].findViewById(R.id.offer_tab_inventory_us);
		tabInventoryRadioThem = (RadioButton) tab_views[0].findViewById(R.id.offer_tab_inventory_them);
		tabInventoryRadioUs.setOnClickListener(this);
		tabInventoryRadioThem.setOnClickListener(this);
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
		tabOfferMeOffer = (ListView) tab_views[1].findViewById(R.id.trade_offer_mylist);
		tabOfferMeOffer.setAdapter(tabOfferMeOfferAdapter = new TabOfferingsListAdapter(true));
		tabOfferMeOffer.setOnItemClickListener(this);
		tabOfferOtherOffer = (ListView) tab_views[1].findViewById(R.id.trade_offer_otherlist);
		tabOfferOtherOffer.setAdapter(tabOfferOtherOfferAdapter = new TabOfferingsListAdapter(false));
		tabOfferOtherOffer.setOnItemClickListener(this);
		// TAB 2: Review
		tabReviewHeading = ((TextView) tab_views[2].findViewById(R.id.offer_tab_review_heading));
		if (offer != null)
			tabReviewHeading.setText(String.format(getString(R.string.offer_with), offer.partnerName));
		tabReviewMessage = ((TextView) tab_views[2].findViewById(R.id.offer_message));
		tabReviewMessageInput = ((EditText) tab_views[2].findViewById(R.id.offer_message_input));
		tabReviewAccept = ((Button) tab_views[2].findViewById(R.id.offer_tab_review_accept));
		tabReviewDecline = ((Button) tab_views[2].findViewById(R.id.offer_tab_review_decline));
		tabReviewCounter = ((Button) tab_views[2].findViewById(R.id.offer_tab_review_counter));
		tabReviewSend = ((Button) tab_views[2].findViewById(R.id.offer_tab_review_send));
		tabReviewCancel = ((Button) tab_views[2].findViewById(R.id.offer_tab_review_cancel));
		tabReviewAccept.setOnClickListener(this);
		tabReviewDecline.setOnClickListener(this);
		tabReviewCounter.setOnClickListener(this);
		tabReviewSend.setOnClickListener(this);
		tabReviewCancel.setOnClickListener(this);

		updateUIInventory();
		updateUIOffers();
		updateUIReview();

		return view;
	}

	public void updateUIInventory() {
		// first do inventory select
		activity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (offer == null || tabInventorySelectAdapter == null)
					return;
				boolean isUs = tabInventoryRadioUs.isChecked();
				List<AppContextPair> appContextPairs = isUs ? offer.appContextData : offer.partnerAppContextData;
				Log.d("Offer", "Inventories #: " + appContextPairs.size());
				tabInventorySelectAdapter.clear();
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
		if (offer == null)
			return;

		tabOfferMeOfferAdapter.notifyDataSetChanged();
		tabOfferOtherOfferAdapter.notifyDataSetChanged();
		updateUITabButton(1);
	}

	public void updateUIReview() {
		if (offer == null)
			return;

		tabReviewHeading.setText(String.format(getString(R.string.offer_with), offer.partnerName));
		activity().getSupportActionBar().setTitle(tabReviewHeading.getText());
		tabReviewMessage.setVisibility(offer.newOffer ? View.GONE : View.VISIBLE);
		tabReviewMessageInput.setVisibility(offer.newOffer || offer.counterOffer ? View.VISIBLE : View.GONE);
		if (offer.message != null && offer.message.trim().length() > 0)
			tabReviewMessage.setText(String.format(getString(R.string.offer_message_quote), offer.message));
		else
			tabReviewMessage.setText(R.string.offer_no_message);

		if (offer.newOffer || offer.counterOffer) {
			tabReviewAccept.setVisibility(View.GONE);
			tabReviewDecline.setVisibility(View.GONE);
			tabReviewCounter.setVisibility(View.GONE);
			tabReviewSend.setVisibility(View.VISIBLE);
			tabReviewCancel.setVisibility(View.VISIBLE);
		} else {
			tabReviewAccept.setVisibility(View.VISIBLE);
			tabReviewDecline.setVisibility(View.VISIBLE);
			tabReviewCounter.setVisibility(View.VISIBLE);
			tabReviewSend.setVisibility(View.GONE);
			tabReviewCancel.setVisibility(View.GONE);
		}
	}

	@Override
	public void onClick(View v) {
		if (v == tabInventoryRadioUs || v == tabInventoryRadioThem) {
			tabInventorySelect.setSelection(0);
			onItemSelected(tabInventorySelect, null, 0, 0);
			updateUIOffers();
			updateUIInventory();
		}
		if (v == tabReviewCancel) {
			Toast.makeText(activity(), R.string.offer_cancelled, Toast.LENGTH_SHORT).show();
			activity().getSupportFragmentManager().popBackStackImmediate();
		}
		if (v == tabReviewCounter) {
			offer.counterOffer = true;
			updateUIInventory();
			updateUIOffers();
			updateUIReview();
		}
		if (v == tabReviewDecline) {
			new GenericAsyncTask(getString(R.string.offer_declining)).execute(new Runnable() {
				@Override
				public void run() {
					offer.declineOffer();
				}
			}, new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity(), getString(R.string.offer_declined), Toast.LENGTH_SHORT).show();
					activity().getSupportFragmentManager().popBackStackImmediate();
				}
			});
		}
		if (v == tabReviewAccept) {
			new GenericAsyncTask(getString(R.string.offer_accepting)).execute(new Runnable() {
				@Override
				public void run() {
					offer.acceptOffer();
				}
			}, new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity(), getString(R.string.offer_accepted), Toast.LENGTH_SHORT).show();
					onCompleted();
				}
			});
		}
		if (v == tabReviewSend) {
			final String message = tabReviewMessageInput.getText().toString();
			new GenericAsyncTask(getString(R.string.offer_submitting)).execute(new Runnable() {
				@Override
				public void run() {
					offer.send(message);
				}
			}, new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity(), getString(R.string.offer_submitted), Toast.LENGTH_SHORT).show();
					onCompleted();
				}
			});
		}
		// tabs
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
				if (i == 2)
					updateUIReview();
				break;
			}
		}
	}

	public void onCompleted() {
		if (activity() == null || getView() == null)
			return;
		((ViewGroup) getView()).removeAllViews();
		View result = activity().getLayoutInflater().inflate(R.layout.trade_result_success, null, false);
		((ViewGroup) getView()).addView(result);
		TextView successText = (TextView) result.findViewById(R.id.trade_success_text);
		successText.setText(R.string.trade_confirm_email);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		TradeInternalAsset asset = (TradeInternalAsset) parent.getAdapter().getItem(position);
		TradeUtil.showItemInfo(getActivity(), asset, null);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (parent == tabInventorySelect) {
			updateUIInventory();
			new Thread(new Runnable() {
				@Override
				public void run() {
					boolean isUs = tabInventoryRadioUs.isChecked();
					if (isUs)
						offer.loadOwnInventory((AppContextPair) tabInventorySelect.getSelectedItem());
					else
						offer.loadPartnerInventory((AppContextPair) tabInventorySelect.getSelectedItem());
					updateUIInventory();
				}
			}).start();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
	}

	private class GenericAsyncTask extends AsyncTask<Runnable, Void, Void> {
		private Runnable after = null;
		private ProgressDialog dialog;
		private String message;

		public GenericAsyncTask(String message) {
			this.message = message;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = new ProgressDialog(activity());
			dialog.setIndeterminate(true);
			dialog.setMessage(message);
			dialog.show();
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			dialog.dismiss();
			after.run();
		}

		@Override
		protected Void doInBackground(Runnable... runnables) {
			if (runnables.length > 0) {
				runnables[0].run();
				if (runnables.length > 1)
					after = runnables[1];
			}
			return null;
		}
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
			final boolean isUs = tabInventoryRadioUs.isChecked();

			final CheckBox itemCheckbox = (CheckBox) v.findViewById(R.id.trade_item_checkbox);
			itemCheckbox.setChecked((isUs ? offer.TRADE_USER_SELF : offer.TRADE_USER_PARTNER).getOffer().contains(item));
			itemCheckbox.setEnabled(offer.counterOffer || offer.newOffer);
			itemCheckbox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					final boolean checked = ((CheckBox) view).isChecked();
					if (checked)
						(isUs ? offer.TRADE_USER_SELF : offer.TRADE_USER_PARTNER).getOffer().add(item);
					else
						(isUs ? offer.TRADE_USER_SELF : offer.TRADE_USER_PARTNER).getOffer().remove(item);
					updateUIOffers();
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
			if (offer == null)
				return null;
			AppContextPair pair = (AppContextPair) tabInventorySelect.getSelectedItem();
			boolean isUs = tabInventoryRadioUs.isChecked();
			TradeInternalInventories inventories = (isUs ? offer.TRADE_USER_SELF : offer.TRADE_USER_PARTNER).getInventories();
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
			if (offer != null)
				cachedList = new ArrayList<TradeInternalAsset>(getOffer());
			super.notifyDataSetChanged();
		}

		private Set<TradeInternalAsset> getOffer() {
			if (self)
				return offer.TRADE_USER_SELF.getOffer();
			else
				return offer.TRADE_USER_PARTNER.getOffer();
		}
	}
}