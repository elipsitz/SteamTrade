package com.aegamesi.steamtrade.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.types.steamid.SteamID;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.loopj.android.image.SmartImageView;

public class FriendListAdapter extends BaseExpandableListAdapter {
	public FragmentFriends fragment;
	public Map<FriendListCategory, List<FriendListItem>> items = new HashMap<FriendListCategory, List<FriendListItem>>();
	public List<FriendListCategory> categories = new ArrayList<FriendListCategory>();
	private SteamFriends steamFriends;

	public FriendListAdapter(FragmentFriends fragment) {
		this.fragment = fragment;
		steamFriends = SteamService.singleton.steamClient.getHandler(SteamFriends.class);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return items.get(categories.get(groupPosition)).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return items.get(categories.get(groupPosition)).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return categories.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return categories.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	public void updateList(List<SteamID> recentChats, List<SteamID> list) {
		items.clear();
		categories.clear();

		ArrayList<FriendListItem> tempitems = new ArrayList<FriendListItem>();
		for (SteamID id : list)
			if (!recentChats.contains(id))
				tempitems.add(new FriendListItem(id));
		Collections.sort(tempitems, new Comparator<FriendListItem>() {
			@Override
			public int compare(FriendListItem lhs, FriendListItem rhs) {
				if (lhs.category == rhs.category)
					return lhs.name.toLowerCase(Locale.US).compareTo(rhs.name.toLowerCase(Locale.US));
				return lhs.category.compareTo(rhs.category);
			}
		});

		// do recent chats
		FriendListCategory lastCategory = FriendListCategory.RECENTCHAT;
		ArrayList<FriendListItem> categoryChildren = new ArrayList<FriendListItem>();
		if (recentChats.size() > 0)
			for (SteamID recent : recentChats)
				categoryChildren.add(new FriendListItem(recent));

		for (FriendListItem item : tempitems) {
			if (lastCategory != item.category) {
				if (categoryChildren != null && categoryChildren.size() > 0) {
					categories.add(lastCategory);
					items.put(lastCategory, categoryChildren);
				}
				categoryChildren = new ArrayList<FriendListItem>();
			}
			lastCategory = item.category;
			categoryChildren.add(item);
		}
		// now do the last category
		if (categoryChildren != null && categoryChildren.size() > 0) {
			categories.add(lastCategory);
			items.put(lastCategory, categoryChildren);
		}

		this.notifyDataSetChanged();
	}

	@Override
	public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null)
			v = fragment.activity().getLayoutInflater().inflate(R.layout.fragment_friends_listitem, null);

		FriendListItem p = (FriendListItem) getChild(groupPosition, childPosition);
		if (p != null) {
			SmartImageView avatar = (SmartImageView) v.findViewById(R.id.friend_avatar_left);
			TextView name = (TextView) v.findViewById(R.id.friend_name);
			TextView status = (TextView) v.findViewById(R.id.friend_status);
			ImageButton chatButton = (ImageButton) v.findViewById(R.id.friend_chat_button);

			name.setText(p.name);
			if (p.game != null && !p.game.isEmpty())
				status.setText("Playing " + p.game);
			else
				status.setText(p.state.toString());

			if (p.game != null && !p.game.isEmpty()) {
				// 8BC53F (AED04E ?) game
				name.setTextColor(SteamUtil.colorGame);
				status.setTextColor(SteamUtil.colorGame);
				avatar.setBackgroundColor(SteamUtil.colorGame);
			} else if (p.state == EPersonaState.Offline || p.state == null) {
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

			if (SteamService.singleton.chat.unreadMessages.containsKey(p.steamid))
				v.setBackgroundColor(fragment.activity().getResources().getColor(R.color.color_friend_unread));
			else
				v.setBackgroundColor(0x00000000);

			chatButton.setTag(p.steamid);
			chatButton.setOnClickListener(fragment);
			chatButton.setFocusable(false);

			avatar.setImageResource(R.drawable.default_avatar);
			if (p.avatar_url != null)
				avatar.setImageUrl(p.avatar_url);
		}
		return v;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		if (convertView == null)
			convertView = fragment.activity().getLayoutInflater().inflate(R.layout.fragment_friends_section, null);
		TextView item = (TextView) convertView.findViewById(R.id.friend_section_header);
		item.setText(getGroup(groupPosition).toString());
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	public class FriendListItem {
		public FriendListCategory category;
		public EPersonaState state = null;
		public SteamID steamid = null;
		public String name = null;
		public String game = null;
		public String avatar_url = null;

		public FriendListItem(SteamID steamid) {
			this.steamid = steamid;
			this.state = steamFriends.getFriendPersonaState(steamid);
			this.name = steamFriends.getFriendPersonaName(steamid);
			this.game = steamFriends.getFriendGamePlayedName(steamid);
			category = FriendListCategory.get(SteamUtil.getQuantizedPersonaState(state, game));

			String imgHash = SteamUtil.bytesToHex(steamFriends.getFriendAvatar(steamid)).toLowerCase(Locale.US);
			if (!imgHash.equals("0000000000000000000000000000000000000000"))
				avatar_url = "http://media.steampowered.com/steamcommunity/public/images/avatars/" + imgHash.substring(0, 2) + "/" + imgHash + "_medium.jpg";
		}
	}

	public enum FriendListCategory {
		FRIENDREQUEST("Friend Requests"), RECENTCHAT("Recent Chats"), INGAME("In-Game"), ONLINE("Online"), OFFLINE("Offline");
		public static FriendListCategory get(int quantized) {
			if (quantized == 0)
				return ONLINE;
			if (quantized == 1)
				return INGAME;
			return OFFLINE;
		}

		private FriendListCategory(final String text) {
			this.text = text;
		}

		private final String text;

		@Override
		public String toString() {
			return text;
		}
	}
}