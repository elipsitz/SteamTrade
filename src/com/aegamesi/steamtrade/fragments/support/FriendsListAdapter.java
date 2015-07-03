package com.aegamesi.steamtrade.fragments.support;

import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aegamesi.lib.android.AndroidUtil;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.FragmentFriends;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EFriendRelationship;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FriendsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	public List<SteamID> recentChats = null;
	private List<FriendListItem> filteredDataset;
	private List<FriendListItem> dataset;
	private Map<FriendListCategory, Integer> categoryCounts;
	private FragmentFriends fragment;
	private String filter = null;

	public FriendsListAdapter(FragmentFriends fragment) {
		this.fragment = fragment;

		createList();
	}

	public void createList() {
		SteamFriends steamFriends = SteamService.singleton.steamClient.getHandler(SteamFriends.class);
		dataset = new ArrayList<FriendListItem>();
		filteredDataset = new ArrayList<FriendListItem>();

		// initialize category count list
		categoryCounts = new HashMap<FriendListCategory, Integer>();
		for (FriendListCategory category : FriendListCategory.values())
			categoryCounts.put(category, 0);

		// populate friends list
		if (steamFriends != null) {
			List<SteamID> listFriends = steamFriends.getFriendList();
			for (SteamID id : listFriends) {
				FriendListItem item = new FriendListItem(id);
				dataset.add(item);
				categoryCounts.put(item.category, categoryCounts.get(item.category) + 1);
			}
		}

		// add section headers
		for (Entry<FriendListCategory, Integer> categoryEntry : categoryCounts.entrySet()) {
			if (categoryEntry.getValue() > 0)
				dataset.add(new FriendListItem(categoryEntry.getKey()));
		}

		Collections.sort(dataset);
	}

	public boolean hasUserID(SteamID id) {
		for (FriendListItem item : dataset)
			if (id.equals(item.steamid))
				return true;
		return false;
	}

	public void add(SteamID id) {
		FriendListItem newItem = new FriendListItem(id);
		int pos = determineItemPosition(newItem);
		dataset.add(pos, newItem);
		notifyItemInserted(pos);

		incrementCategoryCount(newItem.category);
	}

	private void incrementCategoryCount(FriendListCategory category) {
		int categoryCount = categoryCounts.get(category);
		categoryCounts.put(category, ++categoryCount);

		if (categoryCount == 1) {
			FriendListItem item = new FriendListItem(category);
			int pos = determineItemPosition(item);
			dataset.add(pos, item);
			notifyItemInserted(pos);
		}
	}

	private void deincrementCategoryCount(FriendListCategory category) {
		int categoryCount = categoryCounts.get(category);
		categoryCounts.put(category, --categoryCount);

		if (categoryCount == 0) {
			int position = -1;
			for (int i = 0; i < dataset.size(); i++) {
				if (dataset.get(i).steamid == null && dataset.get(i).category == category) {
					position = i;
					break;
				}
			}

			if (position != -1) {
				dataset.remove(position);
				notifyItemRemoved(position);
			}
		}
	}

	private int determineItemPosition(FriendListItem item) {
		for (int i = 0; i < dataset.size(); i++) {
			if (item.compareTo(dataset.get(i)) < 1)
				return i;
		}
		return dataset.size();
	}

	public void remove(SteamID id) {
		int position = -1;
		for (int i = 0; i < dataset.size(); i++) {
			if (dataset.get(i).steamid != null && dataset.get(i).steamid.equals(id)) {
				position = i;
				break;
			}
		}

		if (position != -1) {
			FriendListCategory category = dataset.get(position).category;

			dataset.remove(position);
			notifyItemRemoved(position);

			deincrementCategoryCount(category);
		}
	}

	public void updateRecentChats(List<SteamID> newList) {
		List<SteamID> oldList = recentChats;
		recentChats = newList;

		if (oldList != null) {
			for (SteamID id : oldList)
				if (newList.contains(id) != oldList.contains(id))
					update(id);
			for (SteamID id : newList)
				if (newList.contains(id) != oldList.contains(id))
					update(id);
		} else {
			for (SteamID id : newList)
				update(id);
		}
	}

	public void update(SteamID id) {
		int position = -1;
		for (int i = 0; i < dataset.size(); i++) {
			if (dataset.get(i).steamid != null && dataset.get(i).steamid.equals(id)) {
				position = i;
				break;
			}
		}

		if (position != -1) {
			FriendListItem item = dataset.get(position);
			FriendListCategory oldCategory = item.category;
			item.update();
			dataset.remove(item);
			int new_position = determineItemPosition(item);
			dataset.add(new_position, item);
			if (new_position != position)
				notifyItemMoved(position, new_position);
			notifyItemChanged(new_position);
			Log.i("FriendsListAdapter", "Item (" + item.steamid + ") updated from " + position + " to " + new_position);

			if (oldCategory != item.category) {
				deincrementCategoryCount(oldCategory);
				incrementCategoryCount(item.category);
			}
		}
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (viewType == 0) {
			// section
			return new ViewHolderSection(parent);
		}
		if (viewType == 1) {
			// friend
			return new ViewHolderFriend(parent);
		}
		return null;
	}

	@Override
	public void onBindViewHolder(ViewHolder h, int position) {
		// - get element from your dataset at this position
		// - replace the contents of the view with that element
		List<FriendListItem> list = (filter == null ? dataset : filteredDataset);
		final FriendListItem p = list.get(position);
		final int type = getItemViewType(position);

		if (type == 0) { // section
			ViewHolderSection holder = (ViewHolderSection) h;

			// if the view below this one isn't the same category (or doesn't exist), this category is empty.
			holder.textTitle.setText(p.category.text);
		}
		if (type == 1) { // friend
			ViewHolderFriend holder = (ViewHolderFriend) h;
			holder.itemView.setTag(p.steamid);

		/* DO Populate View */
			if (p.nickname == null)
				holder.textName.setText(p.name);
			else
				holder.textName.setText(p.name + " (" + p.nickname + ")");

			if (p.game != null && p.game.length() > 0)
				holder.textStatus.setText("Playing " + p.game);
			else if (p.category == FriendListCategory.BLOCKED)
				holder.textStatus.setText("Blocked");
			else if (p.category == FriendListCategory.FRIENDREQUEST)
				holder.textStatus.setText("Friend Request");
			else if (p.state == EPersonaState.Offline && p.lastOnline != 0L)
				holder.textStatus.setText("Last online: " + DateUtils.getRelativeTimeSpanString(p.lastOnline, System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_RELATIVE));
			else
				holder.textStatus.setText(p.state.toString());

			// friend request buttons
			if (p.category == FriendListCategory.FRIENDREQUEST) {
				holder.buttonAccept.setVisibility(View.VISIBLE);
				holder.buttonReject.setVisibility(View.VISIBLE);
			} else {
				holder.buttonAccept.setVisibility(View.GONE);
				holder.buttonReject.setVisibility(View.GONE);
			}

			Resources resources = fragment.getResources();
			int color = resources.getColor(R.color.steam_online);
			if (p.category == FriendListCategory.BLOCKED)
				color = resources.getColor(R.color.steam_blocked);
			else if (p.game != null && p.game.length() > 0)
				color = resources.getColor(R.color.steam_game);
			else if (p.state == EPersonaState.Offline || p.state == null)
				color = resources.getColor(R.color.steam_offline);

			holder.textName.setTextColor(color);
			holder.textStatus.setTextColor(color);
			holder.imageAvatar.setBorderColor(color);

			holder.buttonChat.setVisibility((p.relationship == EFriendRelationship.Friend) ? View.VISIBLE : View.GONE);
			if (SteamService.singleton.chatManager.unreadMessages.contains(p.steamid)) {
				holder.buttonChat.setColorFilter(resources.getColor(R.color.steam_online), Mode.MULTIPLY);
				holder.buttonChat.setImageResource(R.drawable.ic_comment_processing);
			} else {
				holder.buttonChat.setColorFilter(resources.getColor(R.color.steam_offline), Mode.MULTIPLY);
				holder.buttonChat.setImageResource(R.drawable.ic_comment);
			}

			holder.buttonChat.setTag(p.steamid);
			holder.buttonChat.setOnClickListener(fragment);
			holder.buttonChat.setFocusable(false);
			holder.buttonAccept.setTag(p.steamid);
			holder.buttonAccept.setOnClickListener(fragment);
			holder.buttonAccept.setFocusable(false);
			holder.buttonReject.setTag(p.steamid);
			holder.buttonReject.setOnClickListener(fragment);
			holder.buttonReject.setFocusable(false);

			holder.imageAvatar.setImageResource(R.drawable.default_avatar);
			if (p.avatar_url != null)
				ImageLoader.getInstance().displayImage(p.avatar_url, holder.imageAvatar);
		}
	}

	@Override
	public int getItemViewType(int position) {
		FriendListItem item = (filter == null ? dataset : filteredDataset).get(position);
		return item.steamid == null ? 0 /* section */ : 1 /* friend */;
	}

	@Override
	public int getItemCount() {
		if (filter == null)
			return dataset.size();
		return filteredDataset.size();
	}

	public void filter(String filter) {
		if (filter == null || filter.trim().length() == 0)
			filter = null;

		if (filter != null) {
			filter = filter.toLowerCase();
			filteredDataset.clear();
			for (FriendListItem item : dataset) {
				if (item.name == null || item.name.toLowerCase().contains(filter) || (item.nickname != null && item.nickname.toLowerCase().contains(filter)))
					filteredDataset.add(item);
			}
		}

		if ((filter == null && this.filter != null) || (filter != null && this.filter == null) || (filter != null && !filter.equalsIgnoreCase(this.filter))) {
			this.filter = filter;
			notifyDataSetChanged();
		}
	}

	public enum FriendListCategory {
		FRIENDREQUEST(0, "Friend Requests"), RECENTCHAT(1, "Recent Chats"), INGAME(2, "In-Game"), ONLINE(3, "Online"), OFFLINE(4, "Offline"), REQUESTPENDING(5, "Pending Friend Requests"), BLOCKED(6, "Blocked");

		public final int order;
		private final String text;

		FriendListCategory(final int order, final String text) {
			this.order = order;
			this.text = text;
		}

		public static FriendListCategory f(int order) {
			for (int i = 0; i < values().length; i++)
				if (values()[i].order == order)
					return values()[i];
			return null;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	public class FriendListItem implements Comparable<FriendListItem> {
		public FriendListCategory category;
		public EPersonaState state = null;
		public EFriendRelationship relationship = null;
		public SteamID steamid = null;
		public String name = null;
		public String game = null;
		public String avatar_url = null;
		public String nickname = null;
		public long lastOnline = 0L;

		public FriendListItem(SteamID steamid) {
			this.steamid = steamid;

			update();
		}

		public FriendListItem(FriendListCategory category) {
			this.category = category;
		}

		public void update() {
			SteamFriends steamFriends = SteamService.singleton.steamClient.getHandler(SteamFriends.class);
			if (steamFriends == null)
				return;

			this.relationship = steamFriends.getFriendRelationship(steamid);
			this.state = steamFriends.getFriendPersonaState(steamid);
			this.name = steamFriends.getFriendPersonaName(steamid);
			this.game = steamFriends.getFriendGamePlayedName(steamid);
			this.nickname = steamFriends.getFriendNickname(steamid);
			this.lastOnline = steamFriends.getFriendLastLogoff(steamid) * 1000L; // convert to millis
			category = findCategory();

			if (steamid != null && steamFriends.getFriendAvatar(steamid) != null) {
				String imgHash = SteamUtil.bytesToHex(steamFriends.getFriendAvatar(steamid)).toLowerCase(Locale.US);
				if (!imgHash.equals("0000000000000000000000000000000000000000") && imgHash.length() == 40)
					avatar_url = "http://media.steampowered.com/steamcommunity/public/images/avatars/" + imgHash.substring(0, 2) + "/" + imgHash + "_medium.jpg";
			}
		}

		private FriendListCategory findCategory() {
			if ((recentChats != null && recentChats.contains(steamid)) || SteamService.singleton.chatManager.unreadMessages.contains(steamid))
				return FriendListCategory.RECENTCHAT;
			if (relationship == EFriendRelationship.Blocked || relationship == EFriendRelationship.Ignored || relationship == EFriendRelationship.IgnoredFriend)
				return FriendListCategory.BLOCKED;
			if (relationship == EFriendRelationship.RequestRecipient)
				return FriendListCategory.FRIENDREQUEST;
			if (relationship == EFriendRelationship.RequestInitiator)
				return FriendListCategory.REQUESTPENDING;
			if (relationship == EFriendRelationship.Friend && state != EPersonaState.Offline) {
				if (game != null && game.length() > 0)
					return FriendListCategory.INGAME;
				return FriendListCategory.ONLINE;
			}
			return FriendListCategory.OFFLINE;
		}

		@Override
		public int compareTo(FriendListItem other) {
			int compare = AndroidUtil.numCompare(this.category.order, other.category.order);
			if (compare != 0)
				return compare;

			// these two statements are for separators
			if (this.steamid == null && other.steamid != null)
				return -1;
			if (this.steamid != null && other.steamid == null)
				return 1;

			// next, sort recent chats by time, not alphabetically
			if (category == FriendListCategory.RECENTCHAT && recentChats != null) {
				int aPosition = recentChats.indexOf(steamid);
				int bPosition = recentChats.indexOf(other.steamid);
				return AndroidUtil.numCompare(aPosition, bPosition);
			}
			// sort offline friends by last time online
			if (category == FriendListCategory.OFFLINE) {
				return -AndroidUtil.numCompare(lastOnline, other.lastOnline);
			}

			compare = this.name.toLowerCase(Locale.getDefault()).compareTo(other.name.toLowerCase(Locale.getDefault()));
			return compare;
		}
	}

	class ViewHolderFriend extends RecyclerView.ViewHolder {
		public CircleImageView imageAvatar;
		public TextView textName;
		public TextView textStatus;
		public ImageButton buttonChat;
		public ImageButton buttonAccept;
		public ImageButton buttonReject;

		public ViewHolderFriend(ViewGroup parent) {
			super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_friends_list_item, parent, false));
			itemView.setOnClickListener(fragment);

			imageAvatar = (CircleImageView) itemView.findViewById(R.id.friend_avatar_left);
			textName = (TextView) itemView.findViewById(R.id.friend_name);
			textStatus = (TextView) itemView.findViewById(R.id.friend_status);
			buttonChat = (ImageButton) itemView.findViewById(R.id.friend_chat_button);
			buttonAccept = (ImageButton) itemView.findViewById(R.id.friend_request_accept);
			buttonReject = (ImageButton) itemView.findViewById(R.id.friend_request_reject);
		}
	}

	class ViewHolderSection extends RecyclerView.ViewHolder {
		public TextView textTitle;

		public ViewHolderSection(ViewGroup parent) {
			super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_friends_list_section, parent, false));

			textTitle = (TextView) itemView.findViewById(R.id.section_text);
		}
	}
}
