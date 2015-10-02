package com.aegamesi.steamtrade.fragments.support;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamItemUtil;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemListView extends RecyclerView implements OnClickListener {
	public static final int MODE_LIST = 1;
	public static final int MODE_GRID = 2;

	private List<TradeInternalAsset> rawList = null;
	private List<TradeInternalAsset> filteredList = new ArrayList<TradeInternalAsset>();

	private int viewType = MODE_GRID;
	private IItemListProvider provider = null;
	private Adapter adapter;

	private LinearLayoutManager layoutManagerLinear = null;
	private GridLayoutManager layoutManagerGrid = null;
	private int gridSize = 0;
	private boolean showAllContents = false;

	public ItemListView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Process our AttributeSet
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ItemListView);
		final int N = a.getIndexCount();
		for (int i = 0; i < N; ++i) {
			int attr = a.getIndex(i);
			switch (attr) {
				case R.styleable.ItemListView_showAllContents:
					showAllContents = a.getBoolean(i, false);
					break;
			}
		}
		a.recycle();

		gridSize = context.getResources().getDimensionPixelSize(R.dimen.itemlist_gridsize);
		setHasFixedSize(!showAllContents);

		adapter = new Adapter();
		layoutManagerLinear = new LinearLayoutManager(context);
		layoutManagerGrid = new GridLayoutManager(context, 5);

		setLayoutManager(viewType == MODE_GRID ? layoutManagerGrid : layoutManagerLinear);
		setAdapter(adapter);
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
		adapter.notifyDataSetChanged();
	}


	public int getListMode() {
		return viewType;
	}

	public void setListMode(int type) {
		if (type != viewType) {
			this.viewType = type;
			setLayoutManager(type == MODE_GRID ? layoutManagerGrid : layoutManagerLinear);
			adapter.notifyDataSetChanged();
		}
	}

	public void setProvider(IItemListProvider provider) {
		this.provider = provider;
		adapter.notifyDataSetChanged();
	}

	public void setItems(List<? extends TradeInternalAsset> list) {
		this.rawList = (List<TradeInternalAsset>) list;
		filter(null);
	}

	public int getTotalItemCount() {
		return rawList == null ? 0 : rawList.size();
	}

	public int getFilteredItemCount() {
		return filteredList == null ? 0 : filteredList.size();
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.itemlist_check) {
			TradeInternalAsset item = (TradeInternalAsset) view.getTag();
			final boolean checked = ((CheckBox) view).isChecked();
			boolean allowed = true;
			if (provider != null)
				allowed = provider.onItemChecked(item, checked);

			if(!allowed) {
				((CheckBox) view).setChecked(!checked);
			}
		}
		if (view.getId() == R.id.itemlist_item) {
			// item clicked
			TradeInternalAsset item = (TradeInternalAsset) view.getTag();
			SteamItemUtil.showItemInfo(getContext(), item, null);
		}
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		super.onMeasure(widthSpec, heightSpec);

		if (gridSize > 0) {
			int spanCount = Math.max(1, getMeasuredWidth() / gridSize);
			layoutManagerGrid.setSpanCount(spanCount);
		}


	}

	public interface IItemListProvider {
		boolean onItemChecked(TradeInternalAsset item, boolean checked);

		boolean shouldItemBeChecked(TradeInternalAsset item);
	}

	private class Adapter extends RecyclerView.Adapter<ItemViewHolder> {
		@Override
		public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			int layout = viewType == MODE_LIST ? R.layout.view_itemlist_item : R.layout.view_itemlist_grid;
			View v = LayoutInflater.from(getContext()).inflate(layout, parent, false);

			if (viewType == MODE_LIST) {
				return new ListViewHolder(v);
			} else {
				return new GridViewHolder(v);
			}
		}

		@Override
		public void onBindViewHolder(ItemViewHolder holder, int position) {
			TradeInternalAsset item = filteredList.get(position);

			if (provider != null) {
				holder.checkBox.setVisibility(View.VISIBLE);
				holder.checkBox.setChecked(provider.shouldItemBeChecked(item));
				holder.checkBox.setTag(item);
			} else {
				holder.checkBox.setVisibility(View.GONE); // not gone-- we want the spacing
			}
			holder.itemView.setTag(item);

			if (holder instanceof GridViewHolder) {
				GridViewHolder h = (GridViewHolder) holder;

				String image_url = "https://steamcommunity-a.akamaihd.net/economy/image/" + item.getIconURL() + "/144x144";
				h.imageItem.setImageDrawable(getContext().getResources().getDrawable(R.drawable.default_avatar)); // so it doesn't show the old item while loading
				ImageLoader.getInstance().displayImage(image_url, h.imageItem);
				if (item.getBackgroundColor() != 0) {
					h.imageItem.setBackgroundColor(item.getBackgroundColor());
				} else {
					h.imageItem.setBackgroundResource(R.color.item_default_background);
				}
				if (item.getNameColor() != 0) {
					h.itemView.setBackgroundColor(item.getNameColor());
				} else {
					h.itemView.setBackgroundResource(R.color.item_default_border);
				}
			}

			if (holder instanceof ListViewHolder) {
				ListViewHolder h = (ListViewHolder) holder;

				h.textItem.setText(item.getDisplayName());
				if (item.getNameColor() != 0)
					h.textItem.setTextColor(item.getNameColor());
				else
					h.textItem.setTextColor(Color.rgb(198, 198, 198));
			}
		}

		@Override
		public int getItemViewType(int position) {
			return viewType;
		}

		@Override
		public int getItemCount() {
			return filteredList.size();
		}
	}

	private abstract class ItemViewHolder extends ViewHolder {
		public CheckBox checkBox;

		public ItemViewHolder(View v) {
			super(v);

			checkBox = (CheckBox) v.findViewById(R.id.itemlist_check);
			checkBox.setOnClickListener(ItemListView.this);
			v.setOnClickListener(ItemListView.this);
		}
	}

	private class ListViewHolder extends ItemViewHolder {
		public TextView textItem;

		public ListViewHolder(View v) {
			super(v);

			textItem = (TextView) v.findViewById(R.id.itemlist_name);
		}
	}

	private class GridViewHolder extends ItemViewHolder {
		public ImageView imageItem;

		public GridViewHolder(View v) {
			super(v);

			imageItem = (ImageView) v.findViewById(R.id.itemlist_image);
		}
	}
}
