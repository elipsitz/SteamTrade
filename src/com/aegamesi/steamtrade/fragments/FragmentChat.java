package com.aegamesi.steamtrade.fragments;

import java.util.ArrayList;
import java.util.Locale;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.types.steamid.SteamID;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatLine;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatReceiver;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.loopj.android.image.SmartImageView;

public class FragmentChat extends FragmentBase implements ChatReceiver {
	public SteamID id;
	public ChatAdapter adapter;

	public ListView chatList;
	public EditText chatInput;
	public Button chatButton;
	public SmartImageView avatar;
	public TextView name;
	public TextView status;
	public View friendInfoView;

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

		adapter = new ChatAdapter();
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

		if (game != null && game.length() != 0) {
			// 8BC53F (AED04E ?) game
			name.setTextColor(SteamUtil.colorGame);
			status.setTextColor(SteamUtil.colorGame);
			avatar.setBackgroundColor(SteamUtil.colorGame);
		} else if (state == EPersonaState.Offline || state == null) {
			// 898989 (CFD2D3 ?) offline
			name.setTextColor(SteamUtil.colorOffline);
			status.setTextColor(SteamUtil.colorOffline);
			avatar.setBackgroundColor(SteamUtil.colorOffline);
		} else {
			// 86B5D9 (9CC6FF ?) online
			name.setTextColor(SteamUtil.colorOnline);
			status.setTextColor(SteamUtil.colorOnline);
			avatar.setBackgroundColor(SteamUtil.colorOnline);
		}

		String imgHash = SteamUtil.bytesToHex(activity().steamFriends.getFriendAvatar(id)).toLowerCase(Locale.US);
		avatar.setImageResource(R.drawable.default_avatar);
		if (imgHash != null && !imgHash.equals("0000000000000000000000000000000000000000") && imgHash.length() == 40)
			avatar.setImageUrl("http://media.steampowered.com/steamcommunity/public/images/avatars/" + imgHash.substring(0, 2) + "/" + imgHash + "_medium.jpg");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		SteamService.singleton.chat.receivers.remove(this);
	}

	public static class ChatAdapter extends BaseAdapter {
		public ArrayList<ChatLine> chatLines = new ArrayList<ChatLine>();

		public void addChatLine(ChatLine line) {
			chatLines.add(line);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return chatLines.size();
		}

		@Override
		public Object getItem(int position) {
			return chatLines.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null)
				v = MainActivity.instance.getLayoutInflater().inflate(R.layout.chat_list_item, null);
			ChatLine line = chatLines.get(position);

			SmartImageView left_avatar = (SmartImageView) v.findViewById(R.id.friend_avatar_left);
			SmartImageView right_avatar = (SmartImageView) v.findViewById(R.id.friend_avatar_right);
			TextView message = (TextView) v.findViewById(R.id.chat_message);
			left_avatar.setVisibility(line.steamId == null ? View.GONE : View.VISIBLE);
			right_avatar.setVisibility(line.steamId == null ? View.VISIBLE : View.GONE);
			message.setGravity(line.steamId == null ? Gravity.RIGHT : Gravity.LEFT);
			SmartImageView avatarView = line.steamId == null ? right_avatar : left_avatar;

			message.setText(line.message);
			avatarView.setImageResource(R.drawable.default_avatar);
			String avatar = SteamUtil.bytesToHex(MainActivity.instance.steamFriends.getFriendAvatar(line.steamId == null ? SteamService.singleton.steamClient.getSteamId() : line.steamId)).toLowerCase(Locale.US);
			if (!avatar.equals("0000000000000000000000000000000000000000"))
				avatarView.setImageUrl("http://media.steampowered.com/steamcommunity/public/images/avatars/" + avatar.substring(0, 2) + "/" + avatar + ".jpg");

			return v;
		}
	}

	@Override
	public boolean receiveChatLine(final ChatLine line, boolean delivered) {
		if (line.steamId == null || line.steamId.equals(id) && activity() != null) {
			activity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					adapter.addChatLine(line);
				}
			});
			if (isVisible() && visible)
				return true;
		}
		return false;
	}
}