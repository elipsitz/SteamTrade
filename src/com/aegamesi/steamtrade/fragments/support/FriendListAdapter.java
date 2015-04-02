package com.aegamesi.steamtrade.fragments.support;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.FragmentFriends;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.loopj.android.image.SmartImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EFriendRelationship;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FriendListAdapter extends BaseExpandableListAdapter {
	public FragmentFriends fragment;
	public Map<FriendListCategory, List<FriendListItem>> friendsList = new LinkedHashMap<FriendListCategory, List<FriendListItem>>();
	public List<FriendListCategory> categories = new ArrayList<FriendListCategory>();
	private SteamFriends steamFriends;

	public String filterString = "";
	private List<SteamID> recentChats = null;
	private List<SteamID> list = null;

	public FriendListAdapter(FragmentFriends fragment) {
		this.fragment = fragment;
		steamFriends = SteamService.singleton.steamClient.getHandler(SteamFriends.class);

		// sort the categories in order
		categories.add(FriendListCategory.FRIENDREQUEST);
		categories.add(FriendListCategory.RECENTCHAT);
		categories.add(FriendListCategory.INGAME);
		categories.add(FriendListCategory.ONLINE);
		categories.add(FriendListCategory.OFFLINE);
		categories.add(FriendListCategory.BLOCKED);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return friendsList.get(categories.get(groupPosition)).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return friendsList.get(categories.get(groupPosition)).size();
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
		this.recentChats = recentChats;
		this.list = list;
		redoList();
	}

	private void redoList() {
		friendsList.clear();

		// first off, recent chats
		if (recentChats.size() > 0) {
			List<FriendListItem> recentChatList = new ArrayList<FriendListItem>();
			for (SteamID id : recentChats)
				if (steamFriends.getFriendRelationship(id) != EFriendRelationship.None)
					recentChatList.add(new FriendListItem(id));
			friendsList.put(FriendListCategory.RECENTCHAT, recentChatList);
		}

		boolean doFilter = filterString != null && filterString.trim().length() != 0;
		for (SteamID id : list) {
			FriendListItem item = new FriendListItem(id);
			if (item.category == null || recentChats.contains(id))
				continue;
			if (doFilter && !item.name.toLowerCase(Locale.getDefault()).contains(filterString))
				continue;

			if (!friendsList.containsKey(item.category))
				friendsList.put(item.category, new ArrayList<FriendListItem>());
			friendsList.get(item.category).add(item);
		}
		// now sort
		{
			Iterator<FriendListCategory> i = categories.iterator();
			while (i.hasNext()) {
				FriendListCategory category = i.next();
				if (friendsList.containsKey(category))
					Collections.sort(friendsList.get(category));
				else
					friendsList.put(category, new ArrayList<FriendListItem>());
			}
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
			ImageButton acceptButton = (ImageButton) v.findViewById(R.id.friend_request_accept);
			ImageButton rejectButton = (ImageButton) v.findViewById(R.id.friend_request_reject);

			name.setText(p.name);
			if (p.game != null && p.game.length() > 0)
				status.setText("Playing " + p.game);
			else if (p.category == FriendListCategory.BLOCKED)
				status.setText("Blocked");

			else if (p.category == FriendListCategory.FRIENDREQUEST)
				status.setText("Friend Request");
			else
				status.setText(p.state.toString());

			// friend request buttons
			if (p.category == FriendListCategory.FRIENDREQUEST) {
				acceptButton.setVisibility(View.VISIBLE);
				rejectButton.setVisibility(View.VISIBLE);
				chatButton.setVisibility(View.GONE);
			} else {
				chatButton.setVisibility(View.VISIBLE);
				acceptButton.setVisibility(View.GONE);
				rejectButton.setVisibility(View.GONE);
			}

			if (p.game != null && p.game.length() > 0) {
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
			acceptButton.setTag(p.steamid);
			acceptButton.setOnClickListener(fragment);
			acceptButton.setFocusable(false);
			rejectButton.setTag(p.steamid);
			rejectButton.setOnClickListener(fragment);
			rejectButton.setFocusable(false);

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

	public class FriendListItem implements Comparable<FriendListItem> {
		public FriendListCategory category;
		public EPersonaState state = null;
		public EFriendRelationship relationship = null;
		public SteamID steamid = null;
		public String name = null;
		public String game = null;
		public String avatar_url = null;

		public FriendListItem(SteamID steamid) {
			this.steamid = steamid;
			this.relationship = steamFriends.getFriendRelationship(steamid);
			this.state = steamFriends.getFriendPersonaState(steamid);
			this.name = steamFriends.getFriendPersonaName(steamid);
			this.game = steamFriends.getFriendGamePlayedName(steamid);
			category = findCategory();

			if (steamid != null && steamFriends.getFriendAvatar(steamid) != null) {
				String imgHash = SteamUtil.bytesToHex(steamFriends.getFriendAvatar(steamid)).toLowerCase(Locale.US);
				if (!imgHash.equals("0000000000000000000000000000000000000000") && imgHash.length() == 40)
					avatar_url = "http://media.steampowered.com/steamcommunity/public/images/avatars/" + imgHash.substring(0, 2) + "/" + imgHash + "_medium.jpg";
			}
		}

		private FriendListCategory findCategory() {
			if (relationship == EFriendRelationship.Blocked || relationship == EFriendRelationship.Ignored || relationship == EFriendRelationship.IgnoredFriend)
				return FriendListCategory.BLOCKED;
			if (relationship == EFriendRelationship.RequestRecipient)
				return FriendListCategory.FRIENDREQUEST;
			if (relationship == EFriendRelationship.Friend) {
				if (state == EPersonaState.Offline) {
					return FriendListCategory.OFFLINE;
				} else {
					if (game != null && game.length() > 0)
						return FriendListCategory.INGAME;
					return FriendListCategory.ONLINE;
				}
			}
			return null;
		}

		@Override
		public int compareTo(FriendListItem friendListItem) {
			return this.name.toLowerCase(Locale.getDefault()).compareTo(friendListItem.name.toLowerCase(Locale.getDefault()));
		}
	}

	public enum FriendListCategory {
		FRIENDREQUEST("Friend Requests"), RECENTCHAT("Recent Chats"), INGAME("In-Game"), ONLINE("Online"), OFFLINE("Offline"), BLOCKED("Blocked");

		private FriendListCategory(final String text) {
			this.text = text;
		}

		private final String text;

		@Override
		public String toString() {
			return text;
		}
	}

	public void filter(String string) {
		filterString = string.toLowerCase(Locale.ENGLISH);
		redoList();
	}
}