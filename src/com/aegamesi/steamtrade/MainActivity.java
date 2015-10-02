package com.aegamesi.steamtrade;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.fragments.FragmentAbout;
import com.aegamesi.steamtrade.fragments.FragmentFriends;
import com.aegamesi.steamtrade.fragments.FragmentInventory;
import com.aegamesi.steamtrade.fragments.FragmentLibrary;
import com.aegamesi.steamtrade.fragments.FragmentMe;
import com.aegamesi.steamtrade.fragments.FragmentOffersList;
import com.aegamesi.steamtrade.fragments.FragmentProfile;
import com.aegamesi.steamtrade.fragments.FragmentSettings;
import com.aegamesi.steamtrade.fragments.FragmentWeb;
import com.aegamesi.steamtrade.steam.SteamMessageHandler;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.acra.ACRA;

import java.util.Locale;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendAddedCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.PersonaStateCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamgamecoordinator.SteamGameCoordinator;
import uk.co.thomasc.steamkit.steam3.handlers.steamnotifications.SteamNotifications;
import uk.co.thomasc.steamkit.steam3.handlers.steamnotifications.callbacks.NotificationUpdateCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.SteamTrading;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.SessionStartCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.TradeProposedCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.TradeResultCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.SteamUser;
import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;
import uk.co.thomasc.steamkit.steam3.steamclient.callbacks.DisconnectedCallback;
import uk.co.thomasc.steamkit.util.cSharp.events.ActionT;

public class MainActivity extends AppCompatActivity implements SteamMessageHandler, BillingProcessor.IBillingHandler, OnNavigationItemSelectedListener {
	public static MainActivity instance = null;
	public boolean isActive = false;

	public SteamFriends steamFriends;
	public SteamTrading steamTrade;
	public SteamGameCoordinator steamGC;
	public SteamUser steamUser;
	public SteamNotifications steamNotifications;
	public BillingProcessor billingProcessor;

	public Toolbar toolbar;
	public ProgressBar progressBar;
	public TabLayout tabs;
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private ImageView drawerAvatar;
	private TextView drawerName;
	private TextView drawerStatus;
	private CardView drawerNotifyCard;
	private TextView drawerNotifyText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!assertSteamConnection())
			return;

		setContentView(R.layout.activity_main);
		instance = this;

		// inform the user about SteamGuard restrictions
		if (SteamService.extras != null && SteamService.extras.getBoolean("alertSteamGuard", false)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (SteamService.extras != null)
						SteamService.extras.putBoolean("alertSteamGuard", false);
				}
			});
			builder.setMessage(R.string.steamguard_new);
			builder.show();
		}

		// get the standard steam handlers
		SteamService.singleton.messageHandler = this;
		steamTrade = SteamService.singleton.steamClient.getHandler(SteamTrading.class);
		steamUser = SteamService.singleton.steamClient.getHandler(SteamUser.class);
		steamFriends = SteamService.singleton.steamClient.getHandler(SteamFriends.class);
		steamGC = SteamService.singleton.steamClient.getHandler(SteamGameCoordinator.class);
		steamNotifications = SteamService.singleton.steamClient.getHandler(SteamNotifications.class);

		// set up the toolbar
		progressBar = (ProgressBar) findViewById(R.id.progress_bar);
		tabs = (TabLayout) findViewById(R.id.tabs);
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);

			drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
			NavigationView navigationView = (NavigationView) findViewById(R.id.navigation);
			navigationView.setNavigationItemSelectedListener(this);
			drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
				@Override
				public void onDrawerSlide(View drawerView, float slideOffset) {
					super.onDrawerSlide(drawerView, 0);
				}
			};
			// drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
			drawerLayout.setDrawerListener(drawerToggle);

			// set up
			drawerAvatar = (ImageView) findViewById(R.id.drawer_avatar);
			drawerName = (TextView) findViewById(R.id.drawer_name);
			drawerStatus = (TextView) findViewById(R.id.drawer_status);
			drawerNotifyCard = (CardView) findViewById(R.id.notify_card);
			drawerNotifyText = (TextView) findViewById(R.id.notify_text);
			findViewById(R.id.drawer_profile).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					browseToFragment(new FragmentMe(), true);
				}
			});
		}

		// set up the nav drawer
		updateDrawerProfile();

		if (savedInstanceState == null)
			browseToFragment(new FragmentMe(), true);
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
				browseToFragment(fragment, true);
			} else if (url.contains("steamcommunity.com/id/") || url.contains("steamcommunity.com/profiles/")) {
				Fragment fragment = new FragmentProfile();
				Bundle bundle = new Bundle();
				bundle.putString("url", url);
				fragment.setArguments(bundle);
				browseToFragment(fragment, true);
			} else {
				// default to steam browser
				Fragment fragment = new FragmentWeb();
				Bundle bundle = new Bundle();
				bundle.putString("url", url);
				fragment.setArguments(bundle);
				browseToFragment(fragment, true);
			}
		}

		// set up billing processor
		billingProcessor = new BillingProcessor(this, getString(R.string.iab_license_key), this);
		billingProcessor.loadOwnedPurchasesFromGoogle();

		// "rate this app!"
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final int num_launches = prefs.getInt("num_launches", 0) + 1;
		prefs.edit().putInt("num_launches", num_launches).apply();
		boolean rated = prefs.getBoolean("rated", false);
		if(num_launches > 0 && (num_launches % 10 == 0) && !rated && num_launches <= (10 * 5)) {
			// show the snackbar
			Snackbar.make(findViewById(android.R.id.content), R.string.rate_snackbar_text, Snackbar.LENGTH_LONG)
					.setAction(R.string.rate_snackbar_action, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							prefs.edit().putBoolean("rated", true).apply();
							Uri marketUri = Uri.parse("market://details?id=" + getPackageName());
							startActivity(new Intent(Intent.ACTION_VIEW).setData(marketUri));
						}
					}).show();
		}


		// setup amazon ads
		//AdRegistration.enableLogging(debug_amazon_ads);
		//AdRegistration.enableTesting(debug_amazon_ads);
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
	protected void onDestroy() {
		if (billingProcessor != null)
			billingProcessor.release();

		super.onDestroy();
	}


	public boolean assertSteamConnection() {
		boolean abort = SteamService.singleton == null || SteamService.singleton.steamClient == null || SteamService.singleton.steamClient.getSteamId() == null;
		if (abort) {
			// something went wrong. Go to login to be safe
			Intent intent = new Intent(this, LoginActivity.class);
			startActivity(intent);
			finish();
		}
		return !abort;
	}

	public void browseToFragment(Fragment fragment, boolean addToBackStack) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		if (addToBackStack)
			transaction.addToBackStack(null);
		//else
		//	fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);*/
		transaction.replace(R.id.content_frame, fragment, fragment.getClass().getName()).commit();
		drawerLayout.closeDrawer(GravityCompat.START);
	}

	private void updateDrawerProfile() {
		EPersonaState state = steamFriends.getPersonaState();
		String name = steamFriends.getPersonaName();
		String avatar = SteamUtil.bytesToHex(steamFriends.getFriendAvatar(SteamService.singleton.steamClient.getSteamId())).toLowerCase(Locale.US);

		drawerName.setText(name);
		drawerStatus.setText(getResources().getStringArray(R.array.persona_states)[state.v()]);
		drawerName.setTextColor(getResources().getColor(R.color.steam_online));
		drawerStatus.setTextColor(getResources().getColor(R.color.steam_online));

		int notifications = steamNotifications.getTotalNotificationCount();
		drawerNotifyText.setText("" + notifications);
		drawerNotifyCard.setCardBackgroundColor(getResources().getColor(notifications == 0 ? R.color.notification_off : R.color.notification_on));

		drawerAvatar.setImageResource(R.drawable.default_avatar);
		if (!avatar.equals("0000000000000000000000000000000000000000")) {
			String avatarURL = "http://media.steampowered.com/steamcommunity/public/images/avatars/" + avatar.substring(0, 2) + "/" + avatar + "_full.jpg";
			ImageLoader.getInstance().displayImage(avatarURL, drawerAvatar);

			if (SteamService.extras != null && SteamService.extras.containsKey("username")) {
				String key = "avatar_" + SteamService.extras.getString("username");
				PreferenceManager.getDefaultSharedPreferences(this).edit().putString(key, avatarURL).apply();
			}
		}
	}

	public Tracker tracker() {
		return ((SteamTrade) getApplication()).getTracker();
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		msg.handle(DisconnectedCallback.class, new ActionT<DisconnectedCallback>() {
			@Override
			public void call(DisconnectedCallback obj) {
				// go back to the login screen
				// only if currently active
				if (isActive) {
					Intent intent = new Intent(MainActivity.this, LoginActivity.class);
					MainActivity.this.startActivity(intent);
					Toast.makeText(MainActivity.this, R.string.error_disconnected, Toast.LENGTH_LONG).show();
					finish();
				}
			}
		});
		msg.handle(PersonaStateCallback.class, new ActionT<PersonaStateCallback>() {
			@Override
			public void call(PersonaStateCallback obj) {
				if (obj.getFriendID().equals(steamUser.getSteamId())) {
					// update current user avatar / drawer
					steamFriends.cache.getLocalUser().avatarHash = obj.getAvatarHash();
					updateDrawerProfile();
				}
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
			}
		});
		msg.handle(NotificationUpdateCallback.class, new ActionT<NotificationUpdateCallback>() {
			@Override
			public void call(NotificationUpdateCallback obj) {
				updateDrawerProfile();
			}
		});


		// Now, we find the fragments and pass the message on that way
		FragmentManager fragmentManager = getSupportFragmentManager();
		for (Fragment fragment : fragmentManager.getFragments()) {
			if (fragment instanceof SteamMessageHandler) {
				((SteamMessageHandler) fragment).handleSteamMessage(msg);
			}
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			toggleDrawer();
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Fragment activeFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
			if (activeFragment instanceof FragmentWeb) {
				// go *back* if possible
				if (((FragmentWeb) activeFragment).onBackPressed())
					return true;
			}
		}

		return super.onKeyUp(keyCode, event);
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

	public void toggleDrawer() {
		if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
			drawerLayout.closeDrawer(GravityCompat.START);
		} else {
			// hide IME
			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (inputManager != null && this.getCurrentFocus() != null)
				inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

			drawerLayout.openDrawer(GravityCompat.START);
		}
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			case R.id.nav_friends:
				browseToFragment(new FragmentFriends(), true);
				break;
			case R.id.nav_inventory:
				browseToFragment(new FragmentInventory(), true);
				break;
			case R.id.nav_offers:
				browseToFragment(new FragmentOffersList(), true);
				break;
			case R.id.nav_games:
				browseToFragment(new FragmentLibrary(), true);
				break;
			case R.id.nav_market: {
				Bundle args = new Bundle();
				args.putBoolean("headless", true);
				args.putString("url", "https://steamcommunity.com/market/");
				FragmentWeb fragment = new FragmentWeb();
				fragment.setArguments(args);
				browseToFragment(fragment, true);
				break;
			}
			case R.id.nav_store: {
				Bundle args = new Bundle();
				args.putBoolean("headless", true);
				args.putStringArray("tabs", getResources().getStringArray(R.array.store_tabs));
				args.putStringArray("tabUrls", getResources().getStringArray(R.array.store_urls));
				FragmentWeb fragment = new FragmentWeb();
				fragment.setArguments(args);
				browseToFragment(fragment, true);
				break;
			}
			case R.id.nav_browser:
				browseToFragment(new FragmentWeb(), true);
				break;
			case R.id.nav_settings:
				browseToFragment(new FragmentSettings(), true);
				break;
			case R.id.nav_about:
				browseToFragment(new FragmentAbout(), true);
				break;
			case R.id.nav_signout:
				disconnectWithDialog(this, getString(R.string.signingout));
				return true;
			default:
				return true;
		}

		//menuItem.setChecked(true);
		toolbar.setTitle(menuItem.getTitle());
		return true;
	}

	private void disconnectWithDialog(final Context context, final String message) {
		class SteamDisconnectTask extends AsyncTask<Void, Void, Void> {
			private ProgressDialog dialog;

			@Override
			protected Void doInBackground(Void... params) {
				// this is really goddamn slow
				steamUser.logOff();
				SteamService.singleton.steamClient.disconnect();
				SteamService.attemptReconnect = false;
				if (SteamService.singleton != null)
					SteamService.singleton.disconnect();


				return null;
			}

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
				MainActivity.this.startActivity(intent);
				Toast.makeText(MainActivity.this, R.string.signed_out, Toast.LENGTH_LONG).show();
				finish();
			}
		}
		new SteamDisconnectTask().execute();
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
			boolean override = SteamService.singleton != null && SteamService.singleton.steamClient != null && SteamService.singleton.steamClient.getSteamId().convertToLong() == 76561198000739785L;
			if (!override)
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("pref_remove_ads", false).apply();
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
		if (fragmentSettings != null)
			handled |= fragmentSettings.handleActivityResult(requestCode, resultCode, data);

		if (!handled)
			super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		isActive = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		isActive = true;

		if (!assertSteamConnection())
			return;

		if (SteamService.singleton.steamClient.getSteamId() != null)
			ACRA.getErrorReporter().putCustomData("steamid64", SteamService.singleton.steamClient.getSteamId().render());
	}

	@Override
	protected void onStart() {
		super.onStart();

		// fragments from intent
		String fragmentName = getIntent().getStringExtra("fragment");
		if (fragmentName != null) {
			Class<? extends Fragment> fragmentClass = null;
			try {
				fragmentClass = (Class<? extends Fragment>) Class.forName(fragmentName);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			if (fragmentClass != null) {
				Fragment fragment = null;
				try {
					fragment = fragmentClass.newInstance();
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (fragment != null) {
					Bundle arguments = getIntent().getBundleExtra("arguments");
					if (arguments != null)
						fragment.setArguments(arguments);
					browseToFragment(fragment, getIntent().getBooleanExtra("fragment_subfragment", true));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends Fragment> T getFragmentByClass(Class<T> clazz) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(clazz.getName());
		return fragment == null ? null : (T) fragment;
	}
}
