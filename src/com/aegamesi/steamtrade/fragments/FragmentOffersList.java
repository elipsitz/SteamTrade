package com.aegamesi.steamtrade.fragments;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.lib.ExpandableHeightGridView;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.ItemListAdapter;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.aegamesi.steamtrade.steam.SteamWeb;
import com.aegamesi.steamtrade.steam.tradeoffers.TradeOfferInfo;
import com.nosoop.steamtrade.inventory.AppContextPair;

import java.net.URL;
import java.util.List;
import java.util.Map;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EAccountType;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EUniverse;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentOffersList extends FragmentBase implements View.OnClickListener {
	public TradeOffersAdapter adapter;
	public ListView list;
	public View loading_view;
	public List<TradeOfferInfo> offers = null;
	public TextView offers_status;
	public RadioButton radio_incoming;
	public RadioButton radio_sent;

	public long queued_cancel = 0;
	public long queued_decline = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		if (getArguments() != null && getArguments().containsKey("new_offer_url"))
			createOfferFromUrl(getArguments().getString("new_offer_url"));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_offers_list, menu);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_view_tradeoffers, container, false);

		loading_view = view.findViewById(R.id.offers_loading);
		adapter = new TradeOffersAdapter();
		list = (ListView) view.findViewById(R.id.offers_list);
		list.setAdapter(adapter);
		offers_status = (TextView) view.findViewById(R.id.offers_status);
		radio_incoming = (RadioButton) view.findViewById(R.id.offers_radio_incoming);
		radio_incoming.setOnClickListener(this);
		radio_sent = (RadioButton) view.findViewById(R.id.offers_radio_sent);
		radio_sent.setOnClickListener(this);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		if (offers == null)
			new FetchOffersTask().execute();
		//activity().getSupportActionBar().setTitle(R.string.trade_offer);
	}

	@Override
	public void onClick(View view) {
		if (view == radio_incoming || view == radio_sent) {
			new FetchOffersTask().execute();
		}

		if (view.getId() == R.id.offer_button_respond || view.getId() == R.id.offer_button_cancel || view.getId() == R.id.offer_button_decline) {
			final TradeOfferInfo offerInfo = offers.get((int) view.getTag());
			switch (view.getId()) {
				case R.id.offer_button_respond:
					Fragment fragment = new FragmentOffer();
					Bundle bundle = new Bundle();
					bundle.putBoolean("from_existing", true);
					bundle.putLong("offer_id", offerInfo.getTradeofferid());
					bundle.putString("offer_message", offerInfo.getMessage());
					fragment.setArguments(bundle);
					activity().browseToFragment(fragment, true);
					break;
				case R.id.offer_button_cancel: {
					AlertDialog.Builder builder = new AlertDialog.Builder(activity());
					builder.setNegativeButton(R.string.no, null);
					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							queued_cancel = offerInfo.getTradeofferid();
							new FetchOffersTask().execute();
						}
					});
					builder.setMessage(R.string.offer_confirm_cancel);
					builder.show();
				}
				break;
				case R.id.offer_button_decline: {
					AlertDialog.Builder builder = new AlertDialog.Builder(activity());
					builder.setNegativeButton(R.string.no, null);
					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							queued_decline = offerInfo.getTradeofferid();
							new FetchOffersTask().execute();
						}
					});
					builder.setMessage(R.string.offer_confirm_decline);
					builder.show();
				}
				break;
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_offerslist_create:
				AlertDialog.Builder alert = new AlertDialog.Builder(activity());
				alert.setTitle(activity().getString(R.string.offer_create));
				alert.setMessage(activity().getString(R.string.offer_create_prompt));
				final EditText input = new EditText(activity());
				input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				alert.setView(input);
				alert.setPositiveButton(R.string.offer_create, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String str = input.getText().toString();
						boolean result = createOfferFromUrl(str);
						if (!result)
							Toast.makeText(activity(), R.string.offer_invalid_url, Toast.LENGTH_LONG).show();
					}
				});
				alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});
				AlertDialog dialog = alert.show();
				TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
				if (messageView != null)
					messageView.setGravity(Gravity.CENTER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private boolean createOfferFromUrl(String str) {
		try {
			if (str == null || str.trim().length() == 0)
				return false;
			URL url = new URL(str);
			if (!url.getHost().equalsIgnoreCase("steamcommunity.com"))
				return false;
			if (!(url.getPath().equalsIgnoreCase("/tradeoffer/new/") || url.getPath().equalsIgnoreCase("/tradeoffer/new")))
				return false;
			Map<String, String> query = SteamUtil.splitQuery(url);
			if (!query.containsKey("partner"))
				return false;

			long partnerID = Long.parseLong(query.get("partner"));
			String partnerToken = query.containsKey("token") ? query.get("token").trim() : null;

			Fragment fragment = new FragmentOffer();
			Bundle bundle = new Bundle();
			bundle.putBoolean("from_existing", false);
			bundle.putLong("user_id", partnerID); // getAccountID *NOT* convertToLong
			bundle.putString("token", partnerToken);
			fragment.setArguments(bundle);
			activity().browseToFragment(fragment, true);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private List<TradeOfferInfo>[] fetchTradeOffers(boolean get_sent_offers, boolean get_received_offers, boolean active_offers) {
		String webapi_url = "https://api.steampowered.com/IEconService/GetTradeOffers/v1/?key=%s&format=json&get_sent_offers=%d&get_received_offers=%d&get_descriptions=1&language=english&active_only=%d&historical_only=%d&time_historical_cutoff=%d";
		webapi_url = String.format(webapi_url, SteamUtil.apikey, get_sent_offers ? 1 : 0, get_received_offers ? 1 : 0, active_offers ? 1 : 0, active_offers ? 0 : 1, Long.MAX_VALUE);
		String response = SteamWeb.fetch(webapi_url, "GET", null, "");

		return TradeOfferInfo.parseGetTradeOffers(response);
	}

	private class FetchOffersTask extends AsyncTask<Void, Void, List<TradeOfferInfo>> {
		public AppContextPair appContext;

		@Override
		protected void onPreExecute() {
			loading_view.setVisibility(View.VISIBLE);
			offers_status.setVisibility(View.GONE);
		}

		@Override
		protected List<TradeOfferInfo> doInBackground(Void... args) {
			if (queued_decline != 0) {
				TradeOfferInfo.attemptDeclineOffer(queued_decline);
				queued_decline = 0;
			}
			if (queued_cancel != 0) {
				TradeOfferInfo.attemptCancelOffer(queued_cancel);
				queued_cancel = 0;
			}

			List<TradeOfferInfo>[] offers = fetchTradeOffers(radio_sent.isChecked(), radio_incoming.isChecked(), true);
			return offers[radio_sent.isChecked() ? 0 : 1];
		}

		@Override
		protected void onPostExecute(List<TradeOfferInfo> result) {
			if (activity() == null)
				return;

			offers = result;
			adapter.notifyDataSetChanged();
			if (result == null) {
				// an error has occurred!
				offers_status.setText(R.string.offer_error_loading);
			} else {
				offers_status.setText(String.format(getString(R.string.offer_count), offers.size()));
			}

			// get rid of UI stuff,
			loading_view.setVisibility(View.GONE);
			offers_status.setVisibility(View.VISIBLE);
		}
	}

	public class TradeOffersAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return (offers == null) ? 0 : offers.size();
		}

		@Override
		public Object getItem(int i) {
			return offers.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View v, ViewGroup viewGroup) {
			if (v == null)
				v = activity().getLayoutInflater().inflate(R.layout.card_tradeoffer, null);


			TradeOfferInfo info = (TradeOfferInfo) getItem(i);
			v.setTag(info.getTradeofferid());
			String offer_partner = activity().steamFriends.getFriendPersonaName(new SteamID((int) info.getAccountid_other(), EUniverse.Public, EAccountType.Individual));

			((TextView) v.findViewById(R.id.offer_heading)).setText(String.format(getString(info.is_our_offer() ? R.string.offer_sent_heading : R.string.offer_received_heading), offer_partner));
			((TextView) v.findViewById(R.id.offer_subtitle)).setText(getResources().getStringArray(R.array.offer_status)[info.getTrade_offer_state().v()]);
			if (info.getMessage() != null) {
				TextView messageView = (TextView) v.findViewById(R.id.offer_message);
				messageView.setVisibility(View.VISIBLE);
				messageView.setText("\"" + info.getMessage() + "\"");
			} else {
				v.findViewById(R.id.offer_message).setVisibility(View.GONE);
			}

			// buttons
			Button button_respond = (Button) v.findViewById(R.id.offer_button_respond);
			Button button_cancel = (Button) v.findViewById(R.id.offer_button_cancel);
			Button button_decline = (Button) v.findViewById(R.id.offer_button_decline);
			button_respond.setTag(i);
			button_cancel.setTag(i);
			button_decline.setTag(i);
			button_respond.setOnClickListener(FragmentOffersList.this);
			button_cancel.setOnClickListener(FragmentOffersList.this);
			button_decline.setOnClickListener(FragmentOffersList.this);
			button_respond.setVisibility(info.is_our_offer() ? View.GONE : View.VISIBLE);
			button_decline.setVisibility(info.is_our_offer() ? View.GONE : View.VISIBLE);
			button_cancel.setVisibility(info.is_our_offer() ? View.VISIBLE : View.GONE);


			ExpandableHeightGridView grid_will_give = (ExpandableHeightGridView) v.findViewById(R.id.offer_items_give);
			ItemListAdapter adapter_will_give = new ItemListAdapter(activity(), grid_will_give, false, null);
			adapter_will_give.setItemList(info.getItems_to_give());
			adapter_will_give.setListMode(ItemListAdapter.MODE_GRID);
			grid_will_give.setAdapter(adapter_will_give);
			grid_will_give.setExpanded(true);
			ExpandableHeightGridView grid_will_receive = (ExpandableHeightGridView) v.findViewById(R.id.offer_items_receive);
			ItemListAdapter adapter_will_receive = new ItemListAdapter(activity(), grid_will_receive, false, null);
			adapter_will_receive.setItemList(info.getItems_to_receive());
			adapter_will_receive.setListMode(ItemListAdapter.MODE_GRID);
			grid_will_receive.setAdapter(adapter_will_receive);
			grid_will_receive.setExpanded(true);

			return v;
		}
	}
}