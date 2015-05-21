package com.aegamesi.steamtrade.fragments.support;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.trade2.TradeUtil;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemListAdapter extends BaseAdapter implements View.OnClickListener, AdapterView.OnItemClickListener {
	public static final int MODE_LIST = 1;
	public static final int MODE_GRID = 2;

	public IItemListProvider provider = null;
	public GridView gridView;
	public int columnNum;
	public boolean hasCheckboxes;
	private int list_mode;
	private Context context;
	private List<TradeInternalAsset> rawList = null;
	private List<TradeInternalAsset> filteredList = new ArrayList<TradeInternalAsset>();

	public ItemListAdapter(Context context, GridView gridView, boolean hasCheckboxes, IItemListProvider provider) {
		this.context = context;
		this.gridView = gridView;
		this.columnNum = 5;
		this.hasCheckboxes = hasCheckboxes;
		this.provider = provider;

		gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		gridView.setOnItemClickListener(this);
		setListMode(MODE_GRID);
	}

	public void setItemList(List<? extends TradeInternalAsset> list) {
		this.rawList = (List<TradeInternalAsset>) list;
		filter("");
		notifyDataSetChanged();
	}

	public int getListMode() {
		return list_mode;
	}

	public void setListMode(int new_mode) {
		if (new_mode != list_mode) {
			gridView.setNumColumns(new_mode == MODE_LIST ? 1 : columnNum);
			notifyDataSetChanged();
		}

		list_mode = new_mode;
	}

	@Override
	public int getCount() {
		return filteredList.size();
	}

	@Override
	public Object getItem(int position) {
		return filteredList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		TradeInternalAsset item = (TradeInternalAsset) getItem(position);

		View v = convertView;
		if (v == null || !v.getTag().equals(list_mode))
			v = inflater.inflate(list_mode == MODE_GRID ? R.layout.view_itemlist_grid : R.layout.view_itemlist_item, null);
		v.setTag(list_mode);

		CheckBox checkBox = (CheckBox) v.findViewById(R.id.itemlist_check);
		if (hasCheckboxes) {
			checkBox.setVisibility(View.VISIBLE);
			if (provider != null)
				checkBox.setChecked(provider.shouldItemBeChecked(item));
			checkBox.setTag(item);
			checkBox.setOnClickListener(this);
		} else {
			checkBox.setVisibility(View.GONE);
		}

		if (list_mode == MODE_GRID) {
			String image_url = "https://steamcommunity-a.akamaihd.net/economy/image/" + item.getIconURL() + "/144x144";
			ImageView img = (ImageView) v.findViewById(R.id.itemlist_image);
			img.setImageDrawable(context.getResources().getDrawable(R.drawable.default_avatar)); // so it doesn't show the old item while loading
			ImageLoader.getInstance().displayImage(image_url, img);
			if (item.getBackgroundColor() != 0)
				img.setBackgroundColor(item.getBackgroundColor());
			if (item.getNameColor() != 0)
				v.setBackgroundColor(item.getNameColor());
		}
		if (list_mode == MODE_LIST) {
			TextView itemName = ((TextView) v.findViewById(R.id.itemlist_name));
			itemName.setText(item.getDisplayName());
			if (item.getNameColor() != 0)
				itemName.setTextColor(item.getNameColor());
			else
				itemName.setTextColor(Color.rgb(198, 198, 198));
		}

		return v;
	}

	public void filter(String by) {
		filteredList.clear();
		if (rawList == null)
			return;
		if (by == null || by.trim().length() == 0) {
			filteredList.addAll(rawList);
		} else {
			List<TradeInternalAsset> items = rawList;
			for (TradeInternalAsset item : items)
				if (item.getDisplayName().toLowerCase(Locale.ENGLISH).contains(by.toLowerCase(Locale.ENGLISH)))
					filteredList.add(item);
		}
		notifyDataSetChanged();
	}

	@Override
	public void onClick(View view) {
		TradeInternalAsset item = (TradeInternalAsset) view.getTag();
		final boolean checked = ((CheckBox) view).isChecked();
		if (provider != null)
			provider.onItemChecked(item, checked);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		TradeInternalAsset item = (TradeInternalAsset) getItem(position);
		TradeUtil.showItemInfo(context, item, null);
	}

	public interface IItemListProvider {
		void onItemChecked(TradeInternalAsset item, boolean checked);

		boolean shouldItemBeChecked(TradeInternalAsset item);
	}
}
