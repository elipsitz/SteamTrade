package com.aegamesi.steamtrade.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.FriendListAdapter;
import com.aegamesi.steamtrade.fragments.support.FriendListAdapter.FriendListCategory;
import com.aegamesi.steamtrade.fragments.support.FriendListAdapter.FriendListItem;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatLine;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatReceiver;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;

import java.util.List;

import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentFriends extends FragmentBase implements OnChildClickListener, OnClickListener, ChatReceiver {
	public FriendListAdapter adapter;
	public ExpandableListView list;
	public EditText friendSearch;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		SteamService.singleton.chat.receivers.add(this);
		fragmentName = "FragmentFriends";
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_friends, menu);
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
		View view = inflater.inflate(R.layout.fragment_friends, container, false);

		adapter = new FriendListAdapter(this);
		list = (ExpandableListView) view.findViewById(R.id.friends_list);
		list.setOnChildClickListener(this);
		list.setAdapter(adapter);
		updateFriends();

		for (int i = 0; i < adapter.getGroupCount(); i++) {
			if ((adapter.getGroup(i) == FriendListCategory.OFFLINE) || (adapter.getGroup(i) == FriendListCategory.BLOCKED))
				list.collapseGroup(i);
			else
				list.expandGroup(i);
		}

		friendSearch = (EditText) view.findViewById(R.id.friends_search);
		friendSearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				adapter.filter(s.toString());
			}
		});
		return view;
	}

	public void updateFriends() {
		List<SteamID> recentChats = SteamService.singleton.chat.getRecentChats(SteamUtil.recentChatThreshold);
		List<SteamID> friends = SteamService.singleton.steamClient.getHandler(SteamFriends.class).getFriendList();
		adapter.updateList(recentChats, friends);
	}

	@Override
	public void onStart() {
		super.onStart();
		activity().getSupportActionBar().setTitle(R.string.friends);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		FriendListItem item = (FriendListItem) adapter.getChild(groupPosition, childPosition);

		Fragment fragment = new FragmentProfile();
		Bundle bundle = new Bundle();
		bundle.putLong("steamId", item.steamid.convertToLong());
		fragment.setArguments(bundle);
		activity().browseToFragment(fragment, true);
		return true;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.friend_chat_button) {
			Fragment fragment = new FragmentChat();
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
}