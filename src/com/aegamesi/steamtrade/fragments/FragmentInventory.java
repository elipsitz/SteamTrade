package com.aegamesi.steamtrade.fragments;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.trade2.TradeUtil;
import com.loopj.android.image.SmartImageView;
import com.nosoop.steamtrade.SteamWeb;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nosoop.steamtrade.inventory.TradeInternalInventories;
import com.nosoop.steamtrade.inventory.TradeInternalInventory;
import com.nosoop.steamtrade.inventory.TradeInternalItem;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentInventory extends FragmentBase implements OnItemClickListener, AdapterView.OnItemSelectedListener {
	public SteamID id;
	public TradeInternalInventories inventories;
	public List<AppContextPair> appContextPairs;
	public boolean isGrid = true;

	public Spinner inventorySelect;
	public ArrayAdapter<AppContextPair> inventorySelectAdapter;
	public GridView inventoryGrid;
	public ListView inventoryList;
	public View loading_view;
	public InventoryAdapter adapter;
	public View error_view;
	public EditText inventorySearch;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		appContextPairs = new ArrayList<AppContextPair>();
		appContextPairs.add(new AppContextPair(440, 2, "Team Fortress 2"));
		appContextPairs.add(new AppContextPair(570, 2, "Dota 2"));
		appContextPairs.add(new AppContextPair(730, 2, "Counter-Strike: Global Offensive"));
		appContextPairs.add(new AppContextPair(753, 1, "Steam Gifts"));
		appContextPairs.add(new AppContextPair(753, 3, "Steam Coupons"));
		appContextPairs.add(new AppContextPair(753, 6, "Steam Community"));
		inventories = new TradeInternalInventories();

		long myID = SteamService.singleton.steamClient.getSteamId().convertToLong();
		if (getArguments() != null)
			id = new SteamID(getArguments().getLong("steamId", myID));
		else
			id = new SteamID(myID);

		adapter = new InventoryAdapter();
		fragmentName = "FragmentInventory";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_inventory, container, false);

		// create inventory select spinner
		inventorySelect = (Spinner) view.findViewById(R.id.inventory_select);
		inventorySelect.setOnItemSelectedListener(this);
		inventorySelectAdapter = new ArrayAdapter<AppContextPair>(activity(), android.R.layout.simple_spinner_item);
		inventorySelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		for (AppContextPair pair : appContextPairs)
			inventorySelectAdapter.add(pair);
		inventorySelectAdapter.notifyDataSetChanged();
		inventorySelect.setAdapter(inventorySelectAdapter);
		//end
		loading_view = view.findViewById(R.id.inventory_loading);
		inventoryGrid = (GridView) view.findViewById(R.id.inventory_grid);
		inventoryList = (ListView) view.findViewById(R.id.inventory_list);
		error_view = view.findViewById(R.id.inventory_error_view);
		// adapter setup
		inventoryGrid.setAdapter(adapter);
		inventoryList.setAdapter(adapter);
		inventoryGrid.setOnItemClickListener(this);
		inventoryList.setOnItemClickListener(this);
		// inventory search
		inventorySearch = (EditText) view.findViewById(R.id.inventory_search);
		inventorySearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				adapter.filter(s.toString());
			}
		});
		return view;
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_inventory, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_inventory_toggle_view:
				isGrid = !isGrid;
				item.setIcon(isGrid ? R.drawable.ic_collections_view_as_list : R.drawable.ic_collections_view_as_grid);
				inventoryGrid.setVisibility(isGrid ? View.VISIBLE : View.GONE);
				inventoryList.setVisibility(!isGrid ? View.VISIBLE : View.GONE);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private class FetchInventoryTask extends AsyncTask<AppContextPair, Integer, JSONObject> {
		public AppContextPair appContext;

		@Override
		protected void onPreExecute() {
			loading_view.setVisibility(View.VISIBLE);
		}

		@Override
		protected JSONObject doInBackground(AppContextPair... args) {
			// arg 0: steamid
			// arg 1: appid
			// arg 2: contextid
			//http://steamcommunity.com/profiles/76561197960422183/inventory/json/440/2/

			final String url, response;
			appContext = args[0];

			if (inventories.hasInventory(appContext))
				return null; // no reason to load it again

			// TODO Add support for large inventories ourselves.
			url = String.format("http://steamcommunity.com/profiles/%d/inventory/json/%d/%d/",
					id.convertToLong(), appContext.getAppid(), appContext.getContextid());

			response = SteamWeb.fetch(url, "GET", null, SteamService.singleton.sessionID, SteamService.singleton.token, null);

			try {
				return new JSONObject(response);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			if (result != null) {
				try {
					inventories.addInventory(appContext, result);
				} catch (Exception e) {
					error_view.setVisibility(View.VISIBLE);
					TextView error_text = (TextView) error_view.findViewById(R.id.inventory_error_text);
					error_text.setText(e.getMessage() == null ? getString(R.string.inv_error_loading) : e.getMessage());
					return;
				}
			}

			// get rid of UI stuff,
			if (activity() != null) {
				loading_view.setVisibility(View.GONE);
				adapter.filter("");
			}
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (parent == inventorySelect) {
			new FetchInventoryTask().execute((AppContextPair) inventorySelect.getSelectedItem());
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {

	}

	public class InventoryAdapter extends BaseAdapter {
		List<TradeInternalAsset> filteredList = new ArrayList<TradeInternalAsset>();

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
			View v = convertView;
			TradeInternalItem item = (TradeInternalItem) getItem(position);
			if (isGrid) {
				if (v == null || !v.getTag().equals(isGrid))
					v = activity().getLayoutInflater().inflate(R.layout.inventory_grid_item, null);

				String image_url = "https://steamcommunity-a.akamaihd.net/economy/image/" + item.getIconURL() + "/144x144";
				SmartImageView img = (SmartImageView) v.findViewById(R.id.item_image);
				img.setImageDrawable(getResources().getDrawable(R.drawable.default_avatar)); // so it doesn't show the old item while loading
				img.setImageUrl(image_url);
				if (item.getBackgroundColor() != 0)
					img.setBackgroundColor(item.getBackgroundColor());
				if (item.getNameColor() != 0)
					v.setBackgroundColor(item.getNameColor());
			} else {
				if (v == null || !v.getTag().equals(isGrid))
					v = activity().getLayoutInflater().inflate(R.layout.inventory_item_item, null);

				TextView itemName = ((TextView) v.findViewById(R.id.inventory_item_name));
				itemName.setText(item.getDisplayName());
				if (item.getNameColor() != 0)
					itemName.setTextColor(item.getNameColor());
				else
					itemName.setTextColor(Color.rgb(198, 198, 198));
			}
			v.setTag(isGrid);
			return v;
		}

		public void filter(String by) {
			filteredList.clear();
			if (getInventory() == null)
				return;
			if (by.trim().length() == 0) {
				filteredList.addAll(getInventory().getItemList());
			} else {
				List<TradeInternalItem> items = getInventory().getItemList();
				for (TradeInternalItem item : items)
					if (item.getDisplayName().toLowerCase(Locale.ENGLISH).contains(by.toLowerCase(Locale.ENGLISH)))
						filteredList.add(item);
			}
			notifyDataSetChanged();
		}

		public TradeInternalInventory getInventory() {
			AppContextPair pair = (AppContextPair) inventorySelect.getSelectedItem();
			if (inventories.hasInventory(pair))
				return inventories.getInventory(pair);

			return null;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		TradeInternalItem item = (TradeInternalItem) parent.getAdapter().getItem(position);
		TradeUtil.showItemInfo(activity(), item, appContextPairs);
	}
}