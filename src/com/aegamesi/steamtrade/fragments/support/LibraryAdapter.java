package com.aegamesi.steamtrade.fragments.support;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.aegamesi.lib.android.AndroidUtil;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.FragmentLibrary;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolderGame> {
	public static final int SORT_ALPHABETICAL = 0;
	public static final int SORT_PLAYTIME = 1;
	public int currentSort = SORT_ALPHABETICAL;
	private List<FragmentLibrary.LibraryEntry> games = null;
	private List<FragmentLibrary.LibraryEntry> filteredList;
	private FragmentLibrary fragment;

	public LibraryAdapter(FragmentLibrary fragmentLibrary) {
		this.fragment = fragmentLibrary;
		filteredList = new ArrayList<FragmentLibrary.LibraryEntry>();
	}

	@Override
	public ViewHolderGame onCreateViewHolder(ViewGroup parent, int viewType) {
		return new ViewHolderGame(parent);
	}

	@Override
	public void onBindViewHolder(ViewHolderGame h, int position) {
		FragmentLibrary.LibraryEntry entry = filteredList.get(position);

		h.itemView.setTag(entry);

		h.imageHeader.setImageResource(R.drawable.default_game);
		if (entry.img_logo_url.trim().length() > 0) {
			String image_url = "http://media.steampowered.com/steamcommunity/public/images/apps/%d/%s.jpg";
			ImageLoader.getInstance().displayImage(String.format(image_url, entry.appid, entry.img_logo_url), h.imageHeader);
		}

		h.textName.setText(entry.name);

		String playtimeText = "";
		if (entry.playtime_forever > 0.0 && entry.playtime_2weeks == 0.0) {
			String baseString = h.itemView.getResources().getString(R.string.library_playtime);
			playtimeText = String.format(baseString, entry.playtime_forever);
		} else if (entry.playtime_forever > 0.0 && entry.playtime_2weeks > 0.0) {
			String baseString = h.itemView.getResources().getString(R.string.library_playtime_full);
			playtimeText = String.format(baseString, entry.playtime_2weeks, entry.playtime_forever);
		}
		h.textPlaytime.setText(playtimeText);
		h.textPlaytime.setVisibility(playtimeText.length() > 0 ? View.VISIBLE : View.GONE);
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public int getItemCount() {
		return filteredList == null ? 0 : filteredList.size();
	}

	public void filter(String by) {
		filteredList.clear();
		if (games == null)
			return;
		if (by == null || by.trim().length() == 0) {
			filteredList.addAll(games);
		} else {
			for (FragmentLibrary.LibraryEntry game : games)
				if (game.name.toLowerCase(Locale.ENGLISH).contains(by.toLowerCase(Locale.ENGLISH)))
					filteredList.add(game);
		}
		notifyDataSetChanged();
	}

	public void setGames(List<FragmentLibrary.LibraryEntry> list, final int sort) {
		if (list != null) {
			Collections.sort(list, new Comparator<FragmentLibrary.LibraryEntry>() {
				@Override
				public int compare(FragmentLibrary.LibraryEntry lhs, FragmentLibrary.LibraryEntry rhs) {
					if (sort == SORT_ALPHABETICAL)
						return lhs.name.compareTo(rhs.name);
					if (sort == SORT_PLAYTIME)
						return -AndroidUtil.numCompare(lhs.playtime_forever, rhs.playtime_forever);
					return 0;
				}
			});
		}
		currentSort = sort;
		games = list;
		filter("");
	}

	public class ViewHolderGame extends ViewHolder {
		public ImageView imageHeader;
		public TextView textName;
		public TextView textPlaytime;

		public ViewHolderGame(ViewGroup parent) {
			super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_library_item, parent, false));

			imageHeader = (ImageView) itemView.findViewById(R.id.game_banner);
			textName = (TextView) itemView.findViewById(R.id.game_name);
			textPlaytime = (TextView) itemView.findViewById(R.id.game_playtime);

			itemView.setOnClickListener(fragment);
		}
	}
}
