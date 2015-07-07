package com.aegamesi.steamtrade.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.ChatAdapter;
import com.aegamesi.steamtrade.steam.DBHelper.ChatEntry;
import com.aegamesi.steamtrade.steam.SteamChatManager;
import com.aegamesi.steamtrade.steam.SteamChatManager.ChatReceiver;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EChatEntryType;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendMsgCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendMsgHistoryCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.PersonaStateCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;
import uk.co.thomasc.steamkit.types.steamid.SteamID;
import uk.co.thomasc.steamkit.util.cSharp.events.ActionT;

public class FragmentChat extends FragmentBase implements ChatReceiver {
	public SteamID ourID;
	public SteamID otherID;
	public ChatAdapter adapter;
	public RecyclerView chatList;
	public LinearLayoutManager layoutManager;
	public Cursor cursor = null;

	public EditText chatInput;
	public ImageButton chatButton;
	public CircleImageView avatar;
	public TextView name;
	public TextView status;
	public TextView chat_typing;
	public View friendInfoView;
	public Handler typingHandler;
	public Runnable typingRunnable = null;
	public boolean fromProfile = false;
	private long time_last_read;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(abort)
			return;

		ourID = SteamService.singleton.steamClient.getSteamId();
		otherID = new SteamID(getArguments().getLong("steamId"));
		fromProfile = getArguments().getBoolean("fromProfile", false);
		//time_last_read = System.currentTimeMillis();
		time_last_read = activity().getPreferences(Context.MODE_PRIVATE).getLong("chat_read_" + ourID.convertToLong() + "_" + otherID.convertToLong(), 0);

		if (SteamService.singleton.chatManager.unreadMessages.contains(otherID)) {
			SteamService.singleton.chatManager.unreadMessages.remove(otherID);
			SteamService.singleton.chatManager.updateNotification();

			if (activity().getFragmentByClass(FragmentFriends.class) != null)
				activity().getFragmentByClass(FragmentFriends.class).adapter.notifyDataSetChanged();
		}

		// typing timer
		typingHandler = new Handler();
		typingRunnable = new Runnable() {
			@Override
			public void run() {
				if (chat_typing != null)
					chat_typing.setVisibility(View.GONE);
			}
		};

		// get message history from steam
		activity().steamFriends.requestFriendMessageHistory(otherID);
	}

	@Override
	public void onResume() {
		super.onResume();

		// set up the cursor
		adapter.changeCursor(cursor = fetchCursor());

		activity().getPreferences(Context.MODE_PRIVATE).edit().putLong("chat_read_" + ourID.convertToLong() + "_" + otherID.convertToLong(), System.currentTimeMillis()).apply();

		if (SteamService.singleton != null && SteamService.singleton.chatManager != null && SteamService.singleton.chatManager.receivers != null)
			SteamService.singleton.chatManager.receivers.add(0, this);
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		msg.handle(FriendMsgCallback.class, new ActionT<FriendMsgCallback>() {
			@Override
			public void call(FriendMsgCallback callback) {
				final EChatEntryType type = callback.getEntryType();

				if (type == EChatEntryType.Typing && callback.getSender().equals(otherID)) {
					// set a timer for the thing
					Log.d("SteamKit", "User is typing a message...");
					if (chat_typing != null)
						chat_typing.setVisibility(View.VISIBLE);
					if (typingHandler != null) {
						typingHandler.removeCallbacks(typingRunnable);
						typingHandler.postDelayed(typingRunnable, 15 * 1000L);
					}
				}
			}
		});
		msg.handle(PersonaStateCallback.class, new ActionT<PersonaStateCallback>() {
			@Override
			public void call(PersonaStateCallback obj) {
				if (otherID != null && otherID.equals(obj.getFriendID()))
					updateView();
			}
		});
		msg.handle(FriendMsgHistoryCallback.class, new ActionT<FriendMsgHistoryCallback>() {
			@Override
			public void call(FriendMsgHistoryCallback obj) {
				if (obj.getSteamId().equals(otherID)) {
					// updated list (already received...)
					// scroll to bottom
					if (cursor != null)
						chatList.scrollToPosition(cursor.getCount() - 1);
				}
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_chat, container, false);

		chatList = (RecyclerView) view.findViewById(R.id.chat);
		chatInput = (EditText) view.findViewById(R.id.chat_input);
		chatButton = (ImageButton) view.findViewById(R.id.chat_button);
		avatar = (CircleImageView) view.findViewById(R.id.friend_avatar_left);
		name = (TextView) view.findViewById(R.id.friend_name);
		status = (TextView) view.findViewById(R.id.friend_status);
		chat_typing = (TextView) view.findViewById(R.id.chat_typing);
		friendInfoView = view.findViewById(R.id.friend_info);
		view.findViewById(R.id.friend_chat_button).setVisibility(View.GONE);

		chatInput.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (event == null)
					return false;
				if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
					String message;
					if ((message = chatInput.getText().toString().trim()).length() == 0)
						return false;
					chatInput.setText("");
					SteamService.singleton.chatManager.sendMessage(otherID, message);
					return true;
				}
				return false;
			}
		});
		chatButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String message;
				if ((message = chatInput.getText().toString().trim()).length() == 0)
					return;
				chatInput.setText("");
				SteamService.singleton.chatManager.sendMessage(otherID, message);
			}
		});
		friendInfoView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!fromProfile) {
					Fragment fragment = new FragmentProfile();
					Bundle bundle = new Bundle();
					bundle.putLong("steamId", otherID.convertToLong());
					fragment.setArguments(bundle);
					activity().browseToFragment(fragment, true);
				} else {
					activity().getSupportFragmentManager().popBackStack();
				}
			}
		});

		boolean isCompact = PreferenceManager.getDefaultSharedPreferences(activity()).getBoolean("pref_chat_compact", false);
		adapter = new ChatAdapter(cursor, isCompact);
		adapter.time_last_read = time_last_read;
		layoutManager = new LinearLayoutManager(activity());
		layoutManager.setStackFromEnd(true);
		chatList.setHasFixedSize(true);
		chatList.setLayoutManager(layoutManager);
		chatList.setAdapter(adapter);

		setTitle(getString(R.string.chat));
		updateView();
		return view;
	}

	@Override
	public void onPause() {
		super.onPause();

		adapter.changeCursor(null);

		if (SteamService.singleton != null && SteamService.singleton.chatManager != null && SteamService.singleton.chatManager.receivers != null)
			SteamService.singleton.chatManager.receivers.remove(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private Cursor fetchCursor() {
		if (SteamService.singleton != null) {
			return SteamService.singleton.db().query(
					ChatEntry.TABLE,                    // The table to query
					new String[]{ChatEntry._ID, ChatEntry.COLUMN_TIME, ChatEntry.COLUMN_MESSAGE, ChatEntry.COLUMN_SENDER},
					ChatEntry.COLUMN_OUR_ID + " = ? AND " + ChatEntry.COLUMN_OTHER_ID + " = ? AND " + ChatEntry.COLUMN_TYPE + " = ?",
					new String[]{"" + ourID.convertToLong(), "" + otherID.convertToLong(), "" + SteamChatManager.CHAT_TYPE_CHAT},
					null, // don't group the rows
					null, // don't filter by row groups
					ChatEntry.COLUMN_TIME + " ASC"
			);
		}
		return null;
	}

	public void updateView() {
		if (activity() == null || activity().steamFriends == null)
			return;
		String friendGamePlayedName = activity().steamFriends.getFriendGamePlayedName(otherID);
		EPersonaState friendPersonaState = activity().steamFriends.getFriendPersonaState(otherID);
		String friendNickname = activity().steamFriends.getFriendNickname(otherID);
		String friendPersonaName = activity().steamFriends.getFriendPersonaName(otherID);
		if (friendNickname == null)
			name.setText(friendPersonaName);
		else
			name.setText(friendPersonaName + " (" + friendNickname + ")");

		if (friendGamePlayedName != null && friendGamePlayedName.length() != 0)
			status.setText(getString(R.string.playing) + " " + friendGamePlayedName);
		else
			status.setText(friendPersonaState.toString());


		adapter.setPersonaNames(activity().steamFriends.getPersonaName(), friendPersonaName);

		// do colors for profile view
		Resources resources = getResources();
		int color = resources.getColor(R.color.steam_online);
		if (friendGamePlayedName != null && friendGamePlayedName.length() > 0)
			color = resources.getColor(R.color.steam_game);
		else if (friendPersonaState == EPersonaState.Offline || friendPersonaState == null)
			color = resources.getColor(R.color.steam_offline);

		name.setTextColor(color);
		status.setTextColor(color);
		avatar.setBorderColor(color);
		adapter.color_default = color;

		String imgHash = SteamUtil.bytesToHex(activity().steamFriends.getFriendAvatar(otherID)).toLowerCase(Locale.US);
		avatar.setImageResource(R.drawable.default_avatar);
		if (!imgHash.equals("0000000000000000000000000000000000000000") && imgHash.length() == 40)
			ImageLoader.getInstance().displayImage("http://media.steampowered.com/steamcommunity/public/images/avatars/" + imgHash.substring(0, 2) + "/" + imgHash + "_medium.jpg", avatar);
	}

	@Override
	public boolean receiveChatLine(long time, SteamID id_us, SteamID id_them, final boolean sent_by_us, int type, String message) {
		if (id_them.equals(otherID)) {
			activity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					adapter.changeCursor(cursor = fetchCursor());
					// now scroll to bottom (if already near the bottom)
					if (layoutManager.findLastVisibleItemPosition() > cursor.getCount() - 3)
						chatList.scrollToPosition(cursor.getCount() - 1);

					if (!sent_by_us && chat_typing != null)
						chat_typing.setVisibility(View.GONE);
				}
			});
			return true;
		}
		return false;
	}
}