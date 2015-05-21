package com.aegamesi.steamtrade.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
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

import com.aegamesi.lib.SimpleSectionedRecyclerViewAdapter;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.NewFriendsListAdapter;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatLine;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatReceiver;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;

import java.util.List;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EFriendRelationship;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentFriends extends FragmentBase implements OnClickListener, ChatReceiver, SearchView.OnQueryTextListener {
	public NewFriendsListAdapter adapter;
	public RecyclerView recyclerView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		SteamService.singleton.chat.receivers.add(this);
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
				alert.setTitle(activity().getString(R.string.friend_add));
				alert.setMessage(activity().getString(R.string.friend_add_prompt));
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_friends_new, container, false);

		// set up the recycler view
		recyclerView = (RecyclerView) view.findViewById(R.id.friends_list);
		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(new LinearLayoutManager(activity()));

		SimpleSectionedRecyclerViewAdapter.Section[] sections = new SimpleSectionedRecyclerViewAdapter.Section[NewFriendsListAdapter.FriendListCategory.values().length];
		for (int i = 0; i < sections.length; i++)
			sections[i] = new SimpleSectionedRecyclerViewAdapter.Section(0, NewFriendsListAdapter.FriendListCategory.f(i).toString());

		List<SteamID> recentChats = SteamService.singleton.chat.getRecentChats(SteamUtil.recentChatThreshold);
		adapter = new NewFriendsListAdapter(this, recentChats);
		recyclerView.setAdapter(adapter);

		return view;
	}

	public void onPersonaStateUpdate(SteamID id) {
		if (adapter.hasUserID(id))
			adapter.update(id);
		else
			adapter.add(id);
	}

	@Override
	public void onStart() {
		super.onStart();
		activity().getSupportActionBar().setTitle(R.string.friends);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.friend_profile_button) {
			Fragment fragment = new FragmentProfile();
			Bundle bundle = new Bundle();
			bundle.putLong("steamId", ((SteamID) v.getTag()).convertToLong());
			fragment.setArguments(bundle);
			activity().browseToFragment(fragment, true);
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
			if (activity().steamFriends.getFriendRelationship(id) == EFriendRelationship.Friend) {
				Fragment fragment = new FragmentChat();
				Bundle bundle = new Bundle();
				bundle.putLong("steamId", id.convertToLong());
				fragment.setArguments(bundle);
				activity().browseToFragment(fragment, true);
			}
		}
	}

	@Override
	public boolean receiveChatLine(ChatLine line, final boolean delivered) {
		if (activity() == null)
			return false;
		activity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!delivered)
					adapter.notifyDataSetChanged();
			}
		});
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