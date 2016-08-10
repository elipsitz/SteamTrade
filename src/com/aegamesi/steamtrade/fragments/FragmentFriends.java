package com.aegamesi.steamtrade.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.FriendsListAdapter;
import com.aegamesi.steamtrade.steam.SteamChatManager;
import com.aegamesi.steamtrade.steam.SteamChatManager.ChatReceiver;
import com.aegamesi.steamtrade.steam.SteamService;

import java.util.List;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EFriendRelationship;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendAddedCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.PersonaStateCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;
import uk.co.thomasc.steamkit.types.steamid.SteamID;
import uk.co.thomasc.steamkit.util.cSharp.events.ActionT;

public class FragmentFriends extends FragmentBase implements OnClickListener, ChatReceiver, SearchView.OnQueryTextListener {
	public long recentChatThreshold = 2 * 24 * 60 * 60 * 1000; // 2 days
	public FriendsListAdapter adapter;
	public RecyclerView recyclerView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (abort)
			return;

		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		setTitle(getString(R.string.nav_friends));

		// update list of recent chats
		recentChatThreshold = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity()).getString("pref_recent_chats", "48"));
		recentChatThreshold *= 60 * 60 * 1000; // hours -> millis
		List<SteamID> recentChats = SteamService.singleton.chatManager.getRecentChats(recentChatThreshold);
		adapter.updateRecentChats(recentChats);

		adapter.notifyDataSetChanged(); // just to make sure
		SteamService.singleton.chatManager.receivers.add(this);
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		msg.handle(FriendAddedCallback.class, new ActionT<FriendAddedCallback>() {
			@Override
			public void call(FriendAddedCallback obj) {
				if (adapter.hasUserID(obj.getSteamID())) {
					adapter.update(obj.getSteamID());
				} else {
					adapter.add(obj.getSteamID());
				}
			}
		});
		msg.handle(PersonaStateCallback.class, new ActionT<PersonaStateCallback>() {
			@Override
			public void call(PersonaStateCallback obj) {
				SteamID id = obj.getFriendID();
				if (adapter.hasUserID(id))
					adapter.update(id);
				else
					adapter.add(id);
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_friends, container, false);

		// set up the recycler view
		recyclerView = (RecyclerView) view.findViewById(R.id.friends_list);
		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(new LinearLayoutManager(activity()));

		boolean hideBlockedUsers = PreferenceManager.getDefaultSharedPreferences(activity()).getBoolean("pref_hide_blocked_users", false);
		adapter = new FriendsListAdapter(this, null, true, hideBlockedUsers);
		recyclerView.setAdapter(adapter);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		setTitle(getString(R.string.friends));
	}

	@Override
	public void onPause() {
		super.onPause();

		if (SteamService.singleton != null && SteamService.singleton.chatManager != null)
			SteamService.singleton.chatManager.receivers.remove(this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_friends, menu);
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		searchView.setOnQueryTextListener(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_friends_add_friend:
				AlertDialog.Builder alert = new AlertDialog.Builder(activity());
				alert.setTitle(R.string.friend_add);
				alert.setMessage(R.string.friend_add_prompt);
				final EditText input = new EditText(activity());
				input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				alert.setView(input);
				alert.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						try {
							long value = Long.parseLong(input.getText().toString());
							activity().steamFriends.addFriend(new SteamID(value));
						} catch (NumberFormatException e) {
							activity().steamFriends.addFriend(input.getText().toString());
						}
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

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.friend_chat_button) {
			SteamID id = (SteamID) v.getTag();
			if (activity().steamFriends.getFriendRelationship(id) == EFriendRelationship.Friend) {
				Fragment fragment = new FragmentChat();
				Bundle bundle = new Bundle();
				bundle.putLong("steamId", id.convertToLong());
				fragment.setArguments(bundle);
				activity().browseToFragment(fragment, true);
			}
		}
		if (v.getId() == R.id.friend_request_accept) {
			SteamID id = (SteamID) v.getTag();
			activity().steamFriends.addFriend(id);
			// accepted friend request
			Toast.makeText(activity(), R.string.friend_request_accept, Toast.LENGTH_SHORT).show();
		}
		if (v.getId() == R.id.friend_request_reject) {
			SteamID id = (SteamID) v.getTag();
			activity().steamFriends.ignoreFriend(id, false);
			// ignored friend request
			Toast.makeText(activity(), R.string.friend_request_ignore, Toast.LENGTH_SHORT).show();
		}
		if (v.getId() == R.id.friends_list_item) {
			SteamID id = (SteamID) v.getTag();
			Fragment fragment = new FragmentProfile();
			Bundle bundle = new Bundle();
			bundle.putLong("steamId", id.convertToLong());
			fragment.setArguments(bundle);
			activity().browseToFragment(fragment, true);
		}
	}

	@Override
	public boolean receiveChatLine(long time, SteamID id_us, final SteamID id_them, boolean sent_by_us, int type, String message) {
		if (activity() != null) {
			if (!sent_by_us && type == SteamChatManager.CHAT_TYPE_CHAT) {
				activity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (adapter != null) {
							if (adapter.recentChats != null) {
								adapter.recentChats.remove(id_them);
								adapter.recentChats.add(0, id_them);
							}
							adapter.update(id_them);
						}
					}
				});
			}
		}
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		adapter.filter(query);
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		adapter.filter(newText);
		return true;
	}
}