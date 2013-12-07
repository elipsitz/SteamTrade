package com.aegamesi.steamtrade;

import org.acra.ACRA;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendAddedCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.PersonaStateCallback;
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
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.aegamesi.steamtrade.fragments.FragmentChat;
import com.aegamesi.steamtrade.fragments.FragmentCrafting;
import com.aegamesi.steamtrade.fragments.FragmentFriends;
import com.aegamesi.steamtrade.fragments.FragmentHome;
import com.aegamesi.steamtrade.fragments.FragmentInventory;
import com.aegamesi.steamtrade.fragments.FragmentMe;
import com.aegamesi.steamtrade.fragments.FragmentProfile;
import com.aegamesi.steamtrade.steam.SteamMessageHandler;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class MainActivity extends SherlockFragmentActivity implements SteamMessageHandler, ListView.OnItemClickListener {
	public static MainActivity instance = null;

	public SteamFriends steamFriends;
	public SteamTrading steamTrade;
	public SteamGameCoordinator steamGC;
	public SteamUser steamUser;

	private DrawerLayout drawerLayout;
	private ListView drawerList;
	private ActionBarDrawerToggle drawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		boolean abort = SteamService.singleton == null || SteamService.singleton.steamClient == null;
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

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerList = (ListView) findViewById(R.id.left_drawer);
		drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, getResources().getStringArray(R.array.app_sections)));
		drawerList.setOnItemClickListener(this);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);

		drawerLayout.setDrawerListener(drawerToggle);
		getSupportActionBar().show();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		SteamService.singleton.messageHandler = this;
		steamTrade = SteamService.singleton.steamClient.getHandler(SteamTrading.class);
		steamUser = SteamService.singleton.steamClient.getHandler(SteamUser.class);
		steamFriends = SteamService.singleton.steamClient.getHandler(SteamFriends.class);
		steamGC = SteamService.singleton.steamClient.getHandler(SteamGameCoordinator.class);

		if (savedInstanceState == null)
			browseToFragment(new FragmentHome(), false);
		SteamService.singleton.tradeManager.setupTradeStatus();
		SteamService.singleton.tradeManager.updateTradeStatus();

		if (getIntent().getBooleanExtra("isLoggingIn", false)) {
			tracker().send(MapBuilder.createEvent("steam", "login", "", null).build());
		}
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
		EasyTracker.getInstance(this).activityStart(this); // Google Analytics
	}

	@Override
	protected void onStop() {
		super.onStop();

		EasyTracker.getInstance(this).activityStop(this); // Google Analytics
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		Log.i("Steam Message", "Got " + msg.getClass().getName());
		msg.handle(DisconnectedCallback.class, new ActionT<DisconnectedCallback>() {
			@Override
			public void call(DisconnectedCallback obj) {
				// go back to the login screen
				Intent intent = new Intent(MainActivity.this, LoginActivity.class);
				MainActivity.this.startActivity(intent);
				Toast.makeText(MainActivity.this, R.string.error_disconnected, Toast.LENGTH_LONG).show();
				finish();
			}
		});
		msg.handle(PersonaStateCallback.class, new ActionT<PersonaStateCallback>() {
			@Override
			public void call(PersonaStateCallback obj) {
				// TODO investigate friend requests, etc.
				// update various user UI interfaces

				// update current user avatar
				if (obj.getFriendID().equals(steamUser.getSteamId()))
					steamFriends.cache.getLocalUser().avatarHash = obj.getAvatarHash();
				FragmentChat chatFragment = getFragmentByClass(FragmentChat.class);
				if (chatFragment != null && chatFragment.id.equals(obj.getFriendID()))
					chatFragment.updateView();
				FragmentProfile profileFragment = getFragmentByClass(FragmentProfile.class);
				if (profileFragment != null && profileFragment.id.equals(obj.getFriendID()))
					profileFragment.updateView();
				FragmentFriends friendsFragment = getFragmentByClass(FragmentFriends.class);
				if (friendsFragment != null)
					friendsFragment.updateFriends();
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
		// our crafting was completed (or failed)
		msg.handle(CraftResponseCallback.class, new ActionT<CraftResponseCallback>() {
			@Override
			public void call(CraftResponseCallback obj) {
				FragmentCrafting craftFragment = getFragmentByClass(FragmentCrafting.class);
				craftFragment.craftResponse(obj);
				//tabMainCraft.setEnabled(false);
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
		drawerLayout.closeDrawer(drawerList);
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
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			toggleDrawer();
			return true;
		} else {
			return super.onKeyUp(keyCode, event);
		}
	}

	public void toggleDrawer() {
		if (drawerLayout.isDrawerOpen(drawerList)) {
			drawerLayout.closeDrawer(drawerList);
		} else {
			// hide IME
			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (inputManager != null && this.getCurrentFocus() != null)
				inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

			drawerLayout.openDrawer(drawerList);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch (position) {
		case 0: // home
			browseToFragment(new FragmentHome(), false);
			break;
		case 1: // me
			browseToFragment(new FragmentMe(), false);
			break;
		case 2: // friends
			browseToFragment(new FragmentFriends(), false);
			break;
		case 3: // inventory
			browseToFragment(new FragmentInventory(), false);
			break;
		case 4: // crafting
			browseToFragment(new FragmentCrafting(), false);
			//Toast.makeText(this, R.string.feature_not_implemented, Toast.LENGTH_LONG).show();
			break;
		case 5: // sign out
			SteamUtil.disconnectWithDialog(this, getString(R.string.signingout));
			return; // ******
		}
		getSupportActionBar().setTitle(((ArrayAdapter<?>) drawerList.getAdapter()).getItem(position).toString());
	}

	public EasyTracker tracker() {
		return EasyTracker.getInstance(this);
	}
}
