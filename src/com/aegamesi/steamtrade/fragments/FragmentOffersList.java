package com.aegamesi.steamtrade.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.aegamesi.steamtrade.trade2.TradeOfferInfo;
import com.nosoop.steamtrade.inventory.AppContextPair;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class FragmentOffersList extends FragmentBase implements AdapterView.OnItemClickListener {
	public TradeOffersAdapter adapter;
	public ListView list;
	public View loading_view;
	public List<TradeOfferInfo> offers;
	public TextView offers_status;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		fragmentName = "FragmentOffersList";

		if(getArguments().containsKey("new_offer_url"))
			createOfferFromUrl(getArguments().getString("new_offer_url"));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_offers_list, menu);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_view_tradeoffers, container, false);

		loading_view = view.findViewById(R.id.offers_loading);
		adapter = new TradeOffersAdapter();
		list = (ListView) view.findViewById(R.id.offers_list);
		list.setOnItemClickListener(this);
		list.setAdapter(adapter);
		offers_status = (TextView) view.findViewById(R.id.offers_status);
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		new FetchOffersTask().execute();
		//activity().getSupportActionBar().setTitle(R.string.trade_offer);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (parent == list && offers != null && position < offers.size()) {
			TradeOfferInfo offerInfo = offers.get(position);

			Fragment fragment = new FragmentOffer();
			Bundle bundle = new Bundle();
			bundle.putBoolean("from_existing", true);
			bundle.putLong("offer_id", offerInfo.id);
			bundle.putString("offer_message", offerInfo.message);
			fragment.setArguments(bundle);
			activity().browseToFragment(fragment, true);
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

	private class FetchOffersTask extends AsyncTask<Void, Void, List<TradeOfferInfo>> {
		public AppContextPair appContext;

		@Override
		protected void onPreExecute() {
			loading_view.setVisibility(View.VISIBLE);
			offers_status.setVisibility(View.GONE);
		}

		@Override
		protected List<TradeOfferInfo> doInBackground(Void... args) {
			return TradeOfferInfo.getTradeOffers();
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
				v = activity().getLayoutInflater().inflate(R.layout.listitem_tradeoffer, null);

			TradeOfferInfo info = (TradeOfferInfo) getItem(i);
			v.setVisibility(info.active ? View.VISIBLE : View.GONE);
			v.setTag(info.id);
			((TextView) v.findViewById(R.id.offer_heading)).setText(String.format(getString(R.string.offer_from), info.senderName));
			((TextView) v.findViewById(R.id.offer_summary)).setText(String.format(getString(R.string.offer_contents), info.numItemsThem, info.numItemsUs));
			if (info.message != null && info.message.trim().length() > 0)
				((TextView) v.findViewById(R.id.offer_message)).setText("\"" + info.message + "\"");
			else
				((TextView) v.findViewById(R.id.offer_message)).setText(R.string.offer_no_message);

			return v;
		}
	}
}