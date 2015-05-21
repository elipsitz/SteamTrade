package com.aegamesi.steamtrade.fragments.support;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;


public class NavigationDrawerAdapter extends ArrayAdapter<String> {
	public final String[] items;

	public NavigationDrawerAdapter(Context context, String[] items) {
		super(context, R.layout.drawer_list_item, items);

		this.items = items;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		String item = items[position];

		boolean divider = item.equals("---");
		int layout = divider ? R.layout.drawer_list_divider : R.layout.drawer_list_item;
		if (view == null || view.getTag() != divider)
			view = LayoutInflater.from(getContext()).inflate(layout, parent, false);
		view.setTag(divider);

		if (!divider) {
			TextView text = (TextView) view.findViewById(android.R.id.text1);
			text.setText(item);
		}

		return view;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return !items[position].equals("---");
	}
}
