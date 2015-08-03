package com.aegamesi.steamtrade.fragments;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.OffersListAdapter;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.aegamesi.steamtrade.steam.SteamWeb;
import com.aegamesi.steamtrade.steam.tradeoffers.TradeOfferInfo;
import com.nosoop.steamtrade.inventory.AppContextPair;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EAccountType;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EUniverse;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.PersonaStateCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;
import uk.co.thomasc.steamkit.types.steamid.SteamID;
import uk.co.thomasc.steamkit.util.cSharp.events.ActionT;

public class FragmentOffersList extends FragmentBase implements View.OnClickListener {
	public OffersListAdapter adapterOffers;
	public RecyclerView listOffers;
	public LinearLayoutManager layoutManager;

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
		if (abort)
			return;

		setHasOptionsMenu(true);

		if (getArguments() != null && getArguments().containsKey("new_offer_url"))
			createOfferFromUrl(getArguments().getString("new_offer_url"));
	}

	@Override
	public void onResume() {
		super.onResume();
		setTitle(getString(R.string.nav_offers));
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		msg.handle(PersonaStateCallback.class, new ActionT<PersonaStateCallback>() {
			@Override
			public void call(PersonaStateCallback obj) {
				if (offers == null)
					return;
				for (int i = 0; i < offers.size(); i++) {
					TradeOfferInfo offer = adapterOffers.offers.get(i);
					SteamID steamID = new SteamID((int) offer.getAccountid_other(), EUniverse.Public, EAccountType.Individual);
					if (steamID.equals(obj.getFriendID())) {
						adapterOffers.notifyItemChanged(i);
					}
				}
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_offerslist, container, false);

		loading_view = view.findViewById(R.id.offers_loading);
		offers_status = (TextView) view.findViewById(R.id.offers_status);
		radio_incoming = (RadioButton) view.findViewById(R.id.offers_radio_incoming);
		radio_incoming.setOnClickListener(this);
		radio_sent = (RadioButton) view.findViewById(R.id.offers_radio_sent);
		radio_sent.setOnClickListener(this);

		listOffers = (RecyclerView) view.findViewById(R.id.offers_list);
		adapterOffers = new OffersListAdapter(this);
		adapterOffers.offers = offers;
		adapterOffers.notifyDataSetChanged();
		layoutManager = new LinearLayoutManager(activity());
		listOffers.setHasFixedSize(true);
		listOffers.setLayoutManager(layoutManager);
		listOffers.setAdapter(adapterOffers);

		if (offers == null) {
			offers_status.setVisibility(View.GONE);
		} else {
			offers_status.setVisibility(View.VISIBLE);
			offers_status.setText(String.format(getString(R.string.offer_count), offers.size()));
		}

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		if (offers == null) {
			if (SteamUtil.webApiKey != null && SteamUtil.webApiKey.length() > 0)
				new FetchOffersTask().execute();
			else
				Toast.makeText(activity(), R.string.api_key_not_loaded, Toast.LENGTH_LONG).show();
		}
		//activity().getSupportActionBar().setTitle(R.string.trade_offer);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_offers_list, menu);
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

	@Override
	public void onClick(View view) {
		if (view == radio_incoming || view == radio_sent) {
			new FetchOffersTask().execute();
		}

		if (view.getId() == R.id.offer_button_respond || view.getId() == R.id.offer_button_cancel || view.getId() == R.id.offer_button_decline || view.getId() == R.id.offer_button_profile) {
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
				case R.id.offer_button_profile: {
					SteamID steamID = new SteamID((int) offerInfo.getAccountid_other(), EUniverse.Public, EAccountType.Individual);
					Fragment fragmentProfile = new FragmentProfile();
					Bundle arguments = new Bundle();
					arguments.putLong("steamId", steamID.convertToLong());
					fragmentProfile.setArguments(arguments);
					activity().browseToFragment(fragmentProfile, true);
				}
				break;
			}
		}
	}

	private List<TradeOfferInfo>[] fetchTradeOffers(boolean get_sent_offers, boolean get_received_offers, boolean active_offers) {
		String webapi_url = "https://api.steampowered.com/IEconService/GetTradeOffers/v1/?key=%s&format=json&get_sent_offers=%d&get_received_offers=%d&get_descriptions=1&language=english&active_only=%d&historical_only=%d&time_historical_cutoff=%d";
		webapi_url = String.format(webapi_url, SteamUtil.webApiKey, get_sent_offers ? 1 : 0, get_received_offers ? 1 : 0, active_offers ? 1 : 0, active_offers ? 0 : 1, Long.MAX_VALUE);
		String response = SteamWeb.fetch(webapi_url, "GET", null, "");

		return TradeOfferInfo.parseGetTradeOffers(response);
	}

	private class FetchOffersTask extends AsyncTask<Void, Void, List<TradeOfferInfo>> {
		public AppContextPair appContext;
		public boolean fetchIncoming = false;
		public boolean fetchSent = false;

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

			List<TradeOfferInfo>[] offers = fetchTradeOffers(fetchSent, fetchIncoming, true);
			return offers[fetchSent ? 0 : 1];
		}

		@Override
		protected void onPreExecute() {
			loading_view.setVisibility(View.VISIBLE);
			offers_status.setVisibility(View.GONE);

			fetchIncoming = radio_incoming.isChecked();
			fetchSent = radio_sent.isChecked();
		}

		@Override
		protected void onPostExecute(List<TradeOfferInfo> result) {
			if (activity() == null)
				return;

			offers = result;
			adapterOffers.offers = offers;
			adapterOffers.notifyDataSetChanged();
			if (result == null) {
				// an error has occurred!
				offers_status.setText(R.string.offers_error_loading);
			} else {
				offers_status.setText(String.format(getString(R.string.offer_count), offers.size()));

				// now let's get all the names of people we *don't* know
				SteamFriends steamFriends = activity().steamFriends;
				if (steamFriends != null) {
					Set<SteamID> steamIdList = new HashSet<SteamID>();
					for (TradeOfferInfo offer : offers) {
						SteamID otherID = new SteamID((int) offer.getAccountid_other(), EUniverse.Public, EAccountType.Individual);
						if (steamFriends.getFriendPersonaName(otherID).equals("[unknown]"))
							steamIdList.add(otherID);
					}
					if (steamIdList.size() > 0)
						steamFriends.requestFriendInfo(steamIdList);
				}
			}

			// get rid of UI stuff,
			loading_view.setVisibility(View.GONE);
			offers_status.setVisibility(View.VISIBLE);
		}
	}
}