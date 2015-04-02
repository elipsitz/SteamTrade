package com.aegamesi.steamtrade.fragments;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.ItemListAdapter;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamWeb;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalInventories;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentInventory extends FragmentBase implements AdapterView.OnItemSelectedListener {
	public SteamID id;
	public TradeInternalInventories inventories;
	public List<AppContextPair> appContextPairs;

	public Spinner inventorySelect;
	public ArrayAdapter<AppContextPair> inventorySelectAdapter;
	public GridView inventoryGrid;
	public View loading_view;
	public ItemListAdapter adapter;
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
		error_view = view.findViewById(R.id.inventory_error_view);
		// adapter setup
		adapter = new ItemListAdapter(activity(), inventoryGrid, false, null);
		inventoryGrid.setAdapter(adapter);
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

		// select last selected inventory
		SharedPreferences prefs = activity().getPreferences(Context.MODE_PRIVATE);
		int pref_appid = prefs.getInt("inv_last_appid", -1);
		long pref_context = prefs.getLong("inv_last_context", -1);
		int pref_index = -1;
		for (int i = 0; i < appContextPairs.size(); i++)
			if (appContextPairs.get(i).getAppid() == pref_appid && appContextPairs.get(i).getContextid() == pref_context)
				pref_index = i;
		if (pref_index != -1)
			inventorySelect.setSelection(pref_index);
		// end

		return view;
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.item_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_inventory_toggle_view:
				adapter.setListMode(adapter.getListMode() == ItemListAdapter.MODE_GRID ? ItemListAdapter.MODE_LIST : ItemListAdapter.MODE_GRID);
				item.setIcon((adapter.getListMode() == ItemListAdapter.MODE_GRID) ? R.drawable.ic_collections_view_as_list : R.drawable.ic_collections_view_as_grid);
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

			response = SteamWeb.fetch(url, "GET", null, null);

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
				adapter.setItemList(inventories.getInventory(appContext).getItemList());
			}
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (parent == inventorySelect) {
			AppContextPair pair = (AppContextPair) inventorySelect.getSelectedItem();
			new FetchInventoryTask().execute(pair);

			SharedPreferences.Editor prefs = activity().getPreferences(Context.MODE_PRIVATE).edit();
			prefs.putInt("inv_last_appid", pair.getAppid());
			prefs.putLong("inv_last_context", pair.getContextid());
			prefs.apply();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {

	}
}