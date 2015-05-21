package com.aegamesi.steamtrade;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.lib.AndroidUtil;
import com.aegamesi.steamtrade.fragments.FragmentAbout;
import com.aegamesi.steamtrade.fragments.FragmentChat;
import com.aegamesi.steamtrade.fragments.FragmentFriends;
import com.aegamesi.steamtrade.fragments.FragmentInventory;
import com.aegamesi.steamtrade.fragments.FragmentMe;
import com.aegamesi.steamtrade.fragments.FragmentOffersList;
import com.aegamesi.steamtrade.fragments.FragmentProfile;
import com.aegamesi.steamtrade.fragments.FragmentSettings;
import com.aegamesi.steamtrade.fragments.FragmentWeb;
import com.aegamesi.steamtrade.fragments.support.NavigationDrawerAdapter;
import com.aegamesi.steamtrade.steam.SteamMessageHandler;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.acra.ACRA;

import java.util.Date;
import java.util.Locale;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EChatEntryType;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendAddedCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendMsgCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.PersonaStateCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.ProfileInfoCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.SteamLevelCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamgamecoordinator.SteamGameCoordinator;
import uk.co.thomasc.steamkit.steam3.handlers.steamgamecoordinator.callbacks.CraftResponseCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.SteamTrading;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.SessionStartCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.TradeProposedCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.TradeResultCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.SteamUser;
import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;
import uk.co.thomasc.steamkit.steam3.steamclient.callbacks.DisconnectedCallback;
import uk.co.thomasc.steamkit.util.cSharp.events.ActionT;

public class MainActivity extends AppCompatActivity implements SteamMessageHandler, ListView.OnItemClickListener, BillingProcessor.IBillingHandler {
	public static MainActivity instance = null;

	public SteamFriends steamFriends;
	public SteamTrading steamTrade;
	public SteamGameCoordinator steamGC;
	public SteamUser steamUser;
	public BillingProcessor billingProcessor;

	private DrawerLayout drawerLayout;
	private View drawer;
	private ListView drawerList;
	private ActionBarDrawerToggle drawerToggle;
	private ImageView drawer_avatar;
	private TextView drawer_name;
	private TextView drawer_status;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		boolean abort = SteamService.singleton == null || SteamService.singleton.steamClient == null || SteamService.singleton.steamClient.getSteamId() == null;
		super.onCreate(abort ? null : savedInstanceState);
		setContentView(R.layout.activity_main);
		instance = this;

		if (abort) {
			// something went wrong. Go to login to be safe
			Intent intent = new Intent(this, LoginActivity.class);
			startActivity(intent);
			finish();
			return;
		}

		// get the standard steam handlers
		SteamService.singleton.messageHandler = this;
		steamTrade = SteamService.singleton.steamClient.getHandler(SteamTrading.class);
		steamUser = SteamService.singleton.steamClient.getHandler(SteamUser.class);
		steamFriends = SteamService.singleton.steamClient.getHandler(SteamFriends.class);
		steamGC = SteamService.singleton.steamClient.getHandler(SteamGameCoordinator.class);

		// set up the nav drawer
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer = findViewById(R.id.left_drawer);
		drawerList = (ListView) findViewById(R.id.drawer_list);
		drawerList.setAdapter(new NavigationDrawerAdapter(this, getResources().getStringArray(R.array.app_sections)));
		drawerList.setOnItemClickListener(this);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
		drawerLayout.setDrawerListener(drawerToggle);
		if (getSupportActionBar() != null) {
			getSupportActionBar().show();
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setHomeButtonEnabled(true);
		}
		drawer_avatar = (ImageView) findViewById(R.id.drawer_avatar);
		drawer_name = (TextView) findViewById(R.id.drawer_name);
		drawer_status = (TextView) findViewById(R.id.drawer_status);
		drawer.findViewById(R.id.drawer_profile).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				browseToFragment(new FragmentMe(), false);
			}
		});
		updateDrawerProfile();

		if (savedInstanceState == null)
			browseToFragment(new FragmentMe(), false);
		SteamService.singleton.tradeManager.setupTradeStatus();
		SteamService.singleton.tradeManager.updateTradeStatus();

		if (getIntent().getBooleanExtra("isLoggingIn", false)) {
			tracker().send(new HitBuilders.EventBuilder()
					.setCategory("Steam")
					.setAction("Login")
					.build());
		}

		// handle our URL stuff
		if (getIntent() != null && ((getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) || getIntent().getStringExtra("url") != null)) {
			String url;
			url = getIntent().getStringExtra("url");
			if (url == null) {
				url = getIntent().getData().toString();
			}/* else {
				url = url.substring(url.indexOf("steamcommunity.com") + ("steamcommunity.com".length()));
			}*/
			Log.d("Ice", "Received url: " + url);
			// XXX TODO https://steamcommunity.com/linkfilter/?url=http://tf2outpost.com/user/325887
			// ignore link filtered things

			if (url.contains("steamcommunity.com/linkfilter/?url=")) {
				// don't filter these...
				String new_url = url.substring(url.indexOf("/linkfilter/?url=") + "/linkfilter/?url=".length());
				Log.d("Ice", "Passing through linkfilter url: '" + new_url + "'");
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(new_url));
				startActivity(browserIntent);
			} else if (url.contains("steamcommunity.com/tradeoffer/new")) {
				Fragment fragment = new FragmentOffersList();
				Bundle bundle = new Bundle();
				bundle.putString("new_offer_url", url);
				fragment.setArguments(bundle);
				browseToFragment(fragment, false);
			} else if (url.contains("steamcommunity.com/id/") || url.contains("steamcommunity.com/profiles/")) {
				Fragment fragment = new FragmentProfile();
				Bundle bundle = new Bundle();
				bundle.putString("url", url);
				fragment.setArguments(bundle);
				browseToFragment(fragment, false);
			} else {
				// default to steam browser
				Fragment fragment = new FragmentWeb();
				Bundle bundle = new Bundle();
				bundle.putString("url", url);
				fragment.setArguments(bundle);
				browseToFragment(fragment, false);
			}
		}

		// set up billing processor
		billingProcessor = new BillingProcessor(this, getString(R.string.license_key), this);
		billingProcessor.loadOwnedPurchasesFromGoogle();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onStart() {
		super.onStart();

		// chat notification
		if (getIntent().getBooleanExtra("fromNotification", false)) {
			long steamID = getIntent().getLongExtra("notificationSteamID", 0);
			if (steamID == 0) {
				browseToFragment(new FragmentFriends(), false);
			} else {
				Fragment fragment = new FragmentChat();
				Bundle bundle = new Bundle();
				bundle.putLong("steamId", steamID);
				fragment.setArguments(bundle);
				browseToFragment(fragment, true);
			}
		}

		if (SteamService.singleton.steamClient.getSteamId() != null)
			ACRA.getErrorReporter().putCustomData("steamid64", SteamService.singleton.steamClient.getSteamId().render());
	}

	private void updateDrawerProfile() {
		EPersonaState state = steamFriends.getPersonaState();
		String name = steamFriends.getPersonaName();
		String avatar = SteamUtil.bytesToHex(steamFriends.getFriendAvatar(SteamService.singleton.steamClient.getSteamId())).toLowerCase(Locale.US);

		drawer_name.setText(name);
		drawer_status.setText(getResources().getStringArray(R.array.persona_states)[state.v()]);
		drawer_name.setTextColor(SteamUtil.colorOnline);
		drawer_status.setTextColor(SteamUtil.colorOnline);

		drawer_avatar.setImageResource(R.drawable.default_avatar);
		if (!avatar.equals("0000000000000000000000000000000000000000"))
			ImageLoader.getInstance().displayImage("http://media.steampowered.com/steamcommunity/public/images/avatars/" + avatar.substring(0, 2) + "/" + avatar + "_full.jpg", drawer_avatar);
	}

	private void disconnectWithDialog(final Context context, final String message) {
		class SteamDisconnectTask extends AsyncTask<Void, Void, Void> {
			private ProgressDialog dialog;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				dialog = new ProgressDialog(context);
				dialog.setCancelable(false);
				dialog.setMessage(message);
				dialog.show();
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				try {
					dialog.dismiss();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}

				// go back to login screen
				Intent intent = new Intent(MainActivity.this, LoginActivity.class);
				intent.putExtra("attemptReconnect", false); // only reconnect if not manually signing out
				MainActivity.this.startActivity(intent);
				Toast.makeText(MainActivity.this, R.string.signed_out, Toast.LENGTH_LONG).show();
				finish();
			}

			@Override
			protected Void doInBackground(Void... params) {
				// this is really goddamn slow
				//SteamService.singleton.steamClient.disconnect();
				if (SteamService.singleton != null)
					SteamService.singleton.disconnect();

				return null;
			}
		}
		new SteamDisconnectTask().execute();
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		Log.i("Steam Message", "Got " + msg.getClass().getName());
		msg.handle(DisconnectedCallback.class, new ActionT<DisconnectedCallback>() {
			@Override
			public void call(DisconnectedCallback obj) {
				// go back to the login screen
				boolean pref_reconnect = PreferenceManager.getDefaultSharedPreferences(SteamService.singleton).getBoolean("pref_reconnect", true);
				Intent intent = new Intent(MainActivity.this, LoginActivity.class);
				intent.putExtra("attemptReconnect", pref_reconnect); // only reconnect if not manually signing out
				MainActivity.this.startActivity(intent);
				Toast.makeText(MainActivity.this, R.string.error_disconnected, Toast.LENGTH_LONG).show();
				finish();
			}
		});
		msg.handle(PersonaStateCallback.class, new ActionT<PersonaStateCallback>() {
			@Override
			public void call(PersonaStateCallback obj) {
				// update various user UI interfaces

				if (obj.getFriendID().equals(steamUser.getSteamId())) {
					// update current user avatar / drawer
					steamFriends.cache.getLocalUser().avatarHash = obj.getAvatarHash();
					updateDrawerProfile();
				}
				FragmentChat chatFragment = getFragmentByClass(FragmentChat.class);
				if (chatFragment != null && chatFragment.id != null && chatFragment.id.equals(obj.getFriendID()))
					chatFragment.updateView();
				FragmentProfile profileFragment = getFragmentByClass(FragmentProfile.class);
				if (profileFragment != null && profileFragment.id != null && profileFragment.id.equals(obj.getFriendID()))
					profileFragment.updatePersona(obj);
				FragmentFriends friendsFragment = getFragmentByClass(FragmentFriends.class);
				if (friendsFragment != null)
					friendsFragment.onPersonaStateUpdate(obj.getFriendID());
			}
		});
		msg.handle(SteamLevelCallback.class, new ActionT<SteamLevelCallback>() {
			@Override
			public void call(SteamLevelCallback obj) {
				FragmentProfile profileFragment = getFragmentByClass(FragmentProfile.class);
				if (profileFragment != null && profileFragment.id != null && obj.getLevelMap().containsKey(profileFragment.id))
					profileFragment.updateLevel(obj.getLevelMap().get(profileFragment.id));
			}
		});
		msg.handle(ProfileInfoCallback.class, new ActionT<ProfileInfoCallback>() {
			@Override
			public void call(ProfileInfoCallback obj) {
				FragmentProfile profileFragment = getFragmentByClass(FragmentProfile.class);
				if (profileFragment != null)
					profileFragment.updateProfile(obj);
			}
		});
		// this means that a trade has just begun
		msg.handle(SessionStartCallback.class, new ActionT<SessionStartCallback>() {
			@Override
			public void call(SessionStartCallback obj) {
				SteamService.singleton.tradeManager.callbackSessionStart(obj);
			}
		});
		// someone wants to trade with us
		msg.handle(TradeProposedCallback.class, new ActionT<TradeProposedCallback>() {
			@Override
			public void call(TradeProposedCallback obj) {
				SteamService.singleton.tradeManager.callbackTradeProposed(obj);
			}
		});
		// response to a trade request (by us?)
		msg.handle(TradeResultCallback.class, new ActionT<TradeResultCallback>() {
			@Override
			public void call(TradeResultCallback obj) {
				SteamService.singleton.tradeManager.callbackTradeResult(obj);
			}
		});
		msg.handle(FriendAddedCallback.class, new ActionT<FriendAddedCallback>() {
			@Override
			public void call(FriendAddedCallback obj) {
				if (obj.getResult() != EResult.OK) {
					Toast.makeText(MainActivity.this, String.format(getString(R.string.friend_add_fail), obj.getResult().toString()), Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(MainActivity.this, getString(R.string.friend_add_success), Toast.LENGTH_LONG).show();
				}

				FragmentFriends friendsFragment = getFragmentByClass(FragmentFriends.class);
				if (friendsFragment != null)
					friendsFragment.adapter.add(obj.getSteamID());
			}
		});
		// our crafting was completed (or failed)
		msg.handle(CraftResponseCallback.class, new ActionT<CraftResponseCallback>() {
			@Override
			public void call(CraftResponseCallback obj) {
				FragmentInventory inv = getFragmentByClass(FragmentInventory.class);
				if (inv != null)
					inv.onCraftingCompleted(obj);
				//craftFragment.craftResponse(obj); // TODO reenable
				//tabMainCraft.setEnabled(false);
			}
		});
		msg.handle(FriendMsgCallback.class, new ActionT<FriendMsgCallback>() {
			@Override
			public void call(FriendMsgCallback callback) {
				final EChatEntryType type = callback.getEntryType();

				if (type == EChatEntryType.Typing) {
					FragmentChat chatFragment = getFragmentByClass(FragmentChat.class);
					if (chatFragment != null)
						chatFragment.onUserTyping(callback.getSender());
				}
			}
		});
		// GC messages
		/*msg.handle(MessageCallback.class, new ActionT<MessageCallback>() {
			@Override
			public void call(MessageCallback obj) {
				String infoString = "EMSG: " + obj.getEMsg() + "\n";
				infoString += "type: " + obj.getMessage().getMsgType() + " source job: " + obj.getMessage().getSourceJobID() + " target job: " + obj.getMessage().getTargetJobID();
				infoString += "\n  " + new String(obj.getMessage().getData());
				infoString += "\n is proto a: " + obj.isProto() + "   is proto b: " + obj.getMessage().isProto();
				Log.i("messagecallback!!!!", infoString);
				FragmentCrafting craftFragment = getFragmentByClass(FragmentCrafting.class);
			}
		});*/
	}

	public void browseToFragment(Fragment fragment, boolean isSubFragment) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		if (isSubFragment)
			transaction.addToBackStack(null);
		else
			fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		transaction.replace(R.id.content_frame, fragment, fragment.getClass().getName()).commit();
		drawerLayout.closeDrawer(drawer);
	}

	@SuppressWarnings("unchecked")
	public <T extends Fragment> T getFragmentByClass(Class<T> clazz) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(clazz.getName());
		return fragment == null ? null : (T) fragment;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				toggleDrawer();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			toggleDrawer();
			return true;
		} else {
			return super.onKeyUp(keyCode, event);
		}
	}

	public void toggleDrawer() {
		if (drawerLayout.isDrawerOpen(drawer)) {
			drawerLayout.closeDrawer(drawer);
		} else {
			// hide IME
			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (inputManager != null && this.getCurrentFocus() != null)
				inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

			drawerLayout.openDrawer(drawer);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch (position) {
			case 0: // friends
				browseToFragment(new FragmentFriends(), false);
				break;
			case 1: // inventory
				browseToFragment(new FragmentInventory(), false);
				break;
			case 2: // trade offers
				browseToFragment(new FragmentOffersList(), false);
				//browseToFragment(new FragmentCrafting(), false);
				//Toast.makeText(this, R.string.feature_not_implemented, Toast.LENGTH_LONG).show();
				break;
			case 3: // steam browser
				browseToFragment(new FragmentWeb(), false);
				break;
			case 4:
				return;// 4: spacer
			case 5: // preferences
				browseToFragment(new FragmentSettings(), false);
				break;
			case 6: // about
				browseToFragment(new FragmentAbout(), false);
				break;
			case 7:
				return;// 7: spacer
			case 8: // sign out
				disconnectWithDialog(this, getString(R.string.signingout));
				return; // ******
		}
		if (getSupportActionBar() != null)
			getSupportActionBar().setTitle((drawerList.getAdapter()).getItem(position).toString());
	}

	public Tracker tracker() {
		return ((SteamTrade) getApplication()).getTracker();
	}

	@Override
	protected void onDestroy() {
		if (billingProcessor != null)
			billingProcessor.release();

		super.onDestroy();
	}

	@Override
	public void onProductPurchased(String productId, TransactionDetails transactionDetails) {
		Toast.makeText(this, R.string.purchase_complete, Toast.LENGTH_LONG).show();
		if (transactionDetails.productId.equals(FragmentSettings.IAP_REMOVEADS)) {
			// by default, remove ads
			PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("pref_remove_ads", true).apply();
		}
	}

	@Override
	public void onPurchaseHistoryRestored() {
		if (!billingProcessor.listOwnedProducts().contains(FragmentSettings.IAP_REMOVEADS)) {
			// the user did not purchase remove ads-- just set the preference to false.
			PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("pref_remove_ads", true).apply();
		}
	}

	@Override
	public void onBillingError(int i, Throwable throwable) {

	}

	@Override
	public void onBillingInitialized() {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		boolean handled = false;
		handled |= billingProcessor.handleActivityResult(requestCode, resultCode, data);

		FragmentSettings fragmentSettings = getFragmentByClass(FragmentSettings.class);
		if(fragmentSettings != null)
			handled |= fragmentSettings.handleActivityResult(requestCode, resultCode, data);

		if(!handled)
			super.onActivityResult(requestCode, resultCode, data);
	}

	public static class AdFragment extends Fragment {
		public AdView mAdView = null;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
								 Bundle savedInstanceState) {
			return inflater.inflate(R.layout.ad_fragment, container, false);
		}

		@Override
		public void onActivityCreated(Bundle bundle) {
			super.onActivityCreated(bundle);

			if (getView() == null)
				return;
			mAdView = (AdView) getView().findViewById(R.id.adView);

			boolean removed_ads = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_remove_ads", false);
			Date installTime = AndroidUtil.getInstallTime(getActivity().getPackageManager(), "com.aegamesi.steamtrade");
			long time = (new Date()).getTime() - installTime.getTime();
			if (time < 1000 * 60 * 60 * 24 || removed_ads) {
				// 1 day of no ads
				mAdView.setVisibility(View.GONE);
			} else {
				AdRequest adRequest = new AdRequest.Builder().build();
				mAdView.loadAd(adRequest);
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			if (mAdView != null)
				mAdView.resume();
		}

		@Override
		public void onPause() {
			super.onPause();
			if (mAdView != null)
				mAdView.pause();
		}
	}
}
