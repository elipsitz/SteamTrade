package com.aegamesi.steamtrade.fragments.support;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aegamesi.lib.AndroidUtil;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.FragmentFriends;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EFriendRelationship;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class NewFriendsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private List<FriendListItem> filteredDataset;
	private List<FriendListItem> dataset;
	private FragmentFriends fragment;
	private String filter = null;

	public NewFriendsListAdapter(FragmentFriends fragment, List<SteamID> recentChats) {
		this.fragment = fragment;
		createList(recentChats);
	}

	public void createList(List<SteamID> recentChats) {
		SteamFriends steamFriends = SteamService.singleton.steamClient.getHandler(SteamFriends.class);
		dataset = new ArrayList<FriendListItem>();
		filteredDataset = new ArrayList<FriendListItem>();

		if (steamFriends != null) {
			List<SteamID> listFriends = steamFriends.getFriendList();
			for (SteamID id : listFriends)
				dataset.add(new FriendListItem(id));

			// add recent chats (as duplicates)
			for (SteamID id : recentChats) {
				if (steamFriends.getFriendRelationship(id) != EFriendRelationship.None) {
					FriendListItem item = new FriendListItem(id);
					item.category = FriendListCategory.RECENTCHAT;
					dataset.add(item);
				}
			}
		}
		for (FriendListCategory category : FriendListCategory.values()) {
			dataset.add(new FriendListItem(category));
		}

		Collections.sort(dataset);
	}

	public boolean hasUserID(SteamID id) {
		for (FriendListItem item : dataset)
			if (id.equals(item.steamid))
				return true;
		return false;
	}

	private int determineItemPosition(FriendListItem item) {
		for (int i = 0; i < dataset.size(); i++) {
			if (item.compareTo(dataset.get(i)) < 1)
				return i;
		}
		return dataset.size() - 1;
	}

	public void add(SteamID id) {
		FriendListItem newItem = new FriendListItem(id);
		int pos = determineItemPosition(newItem);
		dataset.add(pos, newItem);
		notifyItemInserted(pos);
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
			dataset.remove(position);
			notifyItemRemoved(position);
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
			item.update();
			dataset.remove(item);
			int new_position = determineItemPosition(item);
			dataset.add(new_position, item);
			if (new_position != position)
				notifyItemMoved(position, new_position);
			notifyItemChanged(new_position);
			Log.i("FriendsListAdapter", "Item (" + item.steamid + ") updated from " + position + " to " + new_position);
		}
	}

	@Override
	public int getItemViewType(int position) {
		FriendListItem item = (filter == null ? dataset : filteredDataset).get(position);
		return item.steamid == null ? 0 /* section */ : 1 /* friend */;
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

			int color = SteamUtil.colorOnline;
			if (p.category == FriendListCategory.BLOCKED)
				color = SteamUtil.colorBlocked;
			else if (p.game != null && p.game.length() > 0)
				color = SteamUtil.colorGame;
			else if (p.state == EPersonaState.Offline || p.state == null)
				color = SteamUtil.colorOffline;

			holder.textName.setTextColor(color);
			holder.textStatus.setTextColor(color);
			holder.imageAvatar.setBorderColor(color);

		if (SteamService.singleton.chat.unreadMessages.containsKey(p.steamid))
			holder.viewBg.setBackgroundColor(fragment.activity().getResources().getColor(R.color.color_friend_unread));
		else
			holder.viewBg.setBackgroundColor(0x00000000);

			holder.buttonProfile.setTag(p.steamid);
			holder.buttonProfile.setOnClickListener(fragment);
			holder.buttonProfile.setFocusable(false);
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
				if (item.name == null || item.name.toLowerCase().contains(filter))
					filteredDataset.add(item);
			}
		}

		if ((filter == null && this.filter != null) || (filter != null && this.filter == null) || (filter != null && !filter.equalsIgnoreCase(this.filter))) {
			this.filter = filter;
			notifyDataSetChanged();
		}
	}

	public enum FriendListCategory {
		FRIENDREQUEST(0, "Friend Requests"), RECENTCHAT(1, "Recent Chats"), INGAME(2, "In-Game"), ONLINE(3, "Online"), OFFLINE(4, "Offline"), BLOCKED(5, "Blocked");

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
			if (relationship == EFriendRelationship.Friend && state != EPersonaState.Offline) {
				if (game != null && game.length() > 0)
					return FriendListCategory.INGAME;
				return FriendListCategory.ONLINE;
			}
			return FriendListCategory.OFFLINE;
		}

		@Override
		public int compareTo(FriendListItem friendListItem) {
			int compare = AndroidUtil.intCompare(this.category.order, friendListItem.category.order);
			if (compare != 0)
				return compare;

			// these two statements are for separators
			if (this.steamid == null && friendListItem.steamid != null)
				return -1;
			if (this.steamid != null && friendListItem.steamid == null)
				return 1;

			compare = this.name.toLowerCase(Locale.getDefault()).compareTo(friendListItem.name.toLowerCase(Locale.getDefault()));
			return compare;
		}
	}

	public class ViewHolderFriend extends RecyclerView.ViewHolder {
		public CircleImageView imageAvatar;
		public View viewBg;
		public TextView textName;
		public TextView textStatus;
		public ImageButton buttonProfile;
		public ImageButton buttonAccept;
		public ImageButton buttonReject;

		public ViewHolderFriend(ViewGroup parent) {
			super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_friends_list_item, parent, false));
			itemView.setOnClickListener(fragment);

			viewBg = itemView.findViewById(R.id.friend_bg);
			imageAvatar = (CircleImageView) itemView.findViewById(R.id.friend_avatar_left);
			textName = (TextView) itemView.findViewById(R.id.friend_name);
			textStatus = (TextView) itemView.findViewById(R.id.friend_status);
			buttonProfile = (ImageButton) itemView.findViewById(R.id.friend_profile_button);
			buttonAccept = (ImageButton) itemView.findViewById(R.id.friend_request_accept);
			buttonReject = (ImageButton) itemView.findViewById(R.id.friend_request_reject);
		}
	}

	public class ViewHolderSection extends RecyclerView.ViewHolder {
		public TextView textTitle;

		public ViewHolderSection(ViewGroup parent) {
			super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_friends_list_section, parent, false));

			textTitle = (TextView) itemView.findViewById(R.id.section_text);
		}
	}
}
