package com.aegamesi.steamtrade.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.ChatAdapter;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatLine;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatReceiver;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.loopj.android.image.SmartImageView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentChat extends FragmentBase implements ChatReceiver {
	public SteamID id;
	public ChatAdapter adapter;

	public ListView chatList;
	public EditText chatInput;
	public Button chatButton;
	public SmartImageView avatar;
	public TextView name;
	public TextView status;
	public TextView chat_typing;
	public View friendInfoView;

	public Handler typingHandler;
	public Runnable typingRunnable = null;
	public boolean visible = false;
	public boolean fromProfile = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		id = new SteamID(getArguments().getLong("steamId"));
		fromProfile = getArguments().getBoolean("fromProfile", false);
		SteamService.singleton.chat.receivers.add(0, this);

		if (SteamService.singleton.chat.unreadMessages.containsKey(id)) {
			SteamService.singleton.chat.unreadMessages.remove(id);
			SteamService.singleton.chat.updateNotification();

			if (activity().getFragmentByClass(FragmentFriends.class) != null)
				activity().getFragmentByClass(FragmentFriends.class).adapter.notifyDataSetChanged();
		}

		// typing timer
		typingHandler = new Handler();
		typingRunnable = new Runnable() {
			@Override
			public void run() {
				if (chat_typing != null)
					chat_typing.setVisibility(View.INVISIBLE);
			}
		};

		fragmentName = "FragmentChat";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_chat, container, false);

		chatList = (ListView) view.findViewById(R.id.chat);
		chatInput = (EditText) view.findViewById(R.id.chat_input);
		chatButton = (Button) view.findViewById(R.id.chat_button);
		avatar = (SmartImageView) view.findViewById(R.id.friend_avatar_left);
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
					SteamService.singleton.chat.sendMessage(id, message);
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
				SteamService.singleton.chat.sendMessage(id, message);
			}
		});
		friendInfoView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!fromProfile) {
					Fragment fragment = new FragmentProfile();
					Bundle bundle = new Bundle();
					bundle.putLong("steamId", id.convertToLong());
					fragment.setArguments(bundle);
					activity().browseToFragment(fragment, true);
				} else {
					activity().getSupportFragmentManager().popBackStack();
				}
			}
		});
		chatList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long id) {
				// copy to clipboard
				String message = ((ChatLine) view.getTag()).message;
				SteamUtil.copyToClipboard(activity(), message);
				Toast.makeText(activity(), R.string.copied_to_clipboard, Toast.LENGTH_LONG).show();
				return true;
			}
		});

		adapter = new ChatAdapter();
		adapter.last_read = activity().getPreferences(Context.MODE_PRIVATE).getLong("chat_read_" + SteamService.singleton.steamClient.getSteamId().convertToLong() + "_" + id.convertToLong(), -1);
		chatList.setAdapter(adapter);
		ArrayList<ChatLine> backlog = SteamService.singleton.chat.getChatHistory(id, "c", null);
		if (backlog != null)
			for (ChatLine line : backlog)
				adapter.addChatLine(line);
		chatList.setSelection(chatList.getCount() - 1);

		activity().getSupportActionBar().setTitle(R.string.chat);
		updateView();
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		visible = true;
	}

	@Override
	public void onPause() {
		super.onPause();
		visible = false;

		// commit last read date
		SharedPreferences.Editor prefs = activity().getPreferences(Context.MODE_PRIVATE).edit();
		prefs.putLong("chat_read_" + SteamService.singleton.steamClient.getSteamId().convertToLong() + "_" + id.convertToLong(), ((new Date()).getTime()));
		prefs.apply();
	}

	public void updateView() {
		if (activity() == null || activity().steamFriends == null)
			return;
		String game = activity().steamFriends.getFriendGamePlayedName(id);
		EPersonaState state = activity().steamFriends.getFriendPersonaState(id);
		name.setText(activity().steamFriends.getFriendPersonaName(id));
		if (game != null && game.length() != 0)
			status.setText(getString(R.string.playing) + " " + game);
		else
			status.setText(state.toString());

		// do colors for profile view
		// TODO refactor this into a separate method
		int color = SteamUtil.colorOnline;
		if (game != null && game.length() != 0) {
			// 8BC53F (AED04E ?) game
			color = SteamUtil.colorGame;
		} else if (state == EPersonaState.Offline || state == null) {
			// 898989 (CFD2D3 ?) offline
			color = SteamUtil.colorOffline;
		}
		name.setTextColor(color);
		status.setTextColor(color);
		avatar.setBackgroundColor(color);
		adapter.color_default = color;

		String imgHash = SteamUtil.bytesToHex(activity().steamFriends.getFriendAvatar(id)).toLowerCase(Locale.US);
		avatar.setImageResource(R.drawable.default_avatar);
		if (!imgHash.equals("0000000000000000000000000000000000000000") && imgHash.length() == 40)
			avatar.setImageUrl("http://media.steampowered.com/steamcommunity/public/images/avatars/" + imgHash.substring(0, 2) + "/" + imgHash + "_medium.jpg");
	}

	public void onUserTyping(SteamID user) {
		if (user.equals(id)) {
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



	@Override
	public void onDestroy() {
		super.onDestroy();
		SteamService.singleton.chat.receivers.remove(this);
	}

	@Override
	public boolean receiveChatLine(final ChatLine line, boolean delivered) {
		if (line.steamId == null || line.steamId.equals(id) && activity() != null) {
			activity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					adapter.addChatLine(line);
					if (line.steamId != null && chat_typing != null)
						chat_typing.setVisibility(View.INVISIBLE);
				}
			});
			if (isVisible() && visible)
				return true;
		}
		return false;
	}
}