package com.aegamesi.steamtrade.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import uk.co.thomasc.steamkit.base.ClientMsgProtobuf;
import uk.co.thomasc.steamkit.base.gc.tf2.ECraftingRecipe;
import uk.co.thomasc.steamkit.base.generated.SteammessagesClientserver.CMsgClientGamesPlayed;
import uk.co.thomasc.steamkit.base.generated.SteammessagesClientserver.CMsgClientGamesPlayed.GamePlayed;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EMsg;
import uk.co.thomasc.steamkit.steam3.handlers.steamgamecoordinator.callbacks.CraftResponseCallback;
import uk.co.thomasc.steamkit.types.steamid.SteamID;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamInventory;
import com.aegamesi.steamtrade.steam.SteamInventory.SteamInventoryItem;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;

public class FragmentCrafting extends FragmentBase implements OnClickListener, OnItemClickListener {
	public View[] views;
	public SteamInventory inventory = null;
	public List<Long> selectedItems = new ArrayList<Long>();
	public ActionBar.TabListener tabListener;

	public View loading_view;
	public View error_view;
	public View success_view;
	public ListView tabInventoryList;
	public EditText tabInventorySearch;
	public TabInventoryListAdapter tabInventoryListAdapter;
	public List<Long> craftingResults = null;
	//
	public ListView tabMainList;
	public Button tabMainClear;
	public Button tabMainCraft;
	public TabMainListAdapter tabMainListAdapter;
	public ProgressBar tabMainStatusCircle;
	public Button tabResultContinueCrafting;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		views = new View[2];

		fragmentName = "FragmentCrafting";
		tabInventoryListAdapter = new TabInventoryListAdapter();
	}

	@Override
	public void onResume() {
		super.onResume();
		// tabs
		ActionBar actionBar = activity().getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		tabListener = new ActionBar.TabListener() {
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				views[tab.getPosition()].setVisibility(View.VISIBLE);
				updateTab(tab.getPosition());
			}

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				views[tab.getPosition()].setVisibility(View.GONE);
			}

			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				// ignore
			}
		};

		for (int n = 0; n < 2; n++) {
			actionBar.addTab(actionBar.newTab().setTabListener(tabListener));
			updateTab(n);
		}
		// etc.
		//activity().getSupportActionBar().setTitle();
		// start "playing" tf2
		new Thread(new Runnable() {
			@Override
			public void run() {
				final ClientMsgProtobuf<CMsgClientGamesPlayed.Builder> playGame = new ClientMsgProtobuf<CMsgClientGamesPlayed.Builder>(CMsgClientGamesPlayed.class, EMsg.ClientGamesPlayed);
				playGame.getBody().addGamesPlayed(GamePlayed.newBuilder().setGameId(440).build());
				SteamService.singleton.steamClient.send(playGame);
			}
		}).start();
	}

	@Override
	public void onStart() {
		super.onStart();

		if (inventory == null)
			new FetchInventoryTask().execute(SteamService.singleton.steamClient.getSteamId());
	}

	@Override
	public void onPause() {
		super.onPause();

		ActionBar actionBar = activity().getSupportActionBar();
		actionBar.removeAllTabs();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	public void updateTab(int num) {
		String text = activity().getResources().getStringArray(R.array.craft_tabs)[num];
		activity().getSupportActionBar().getTabAt(num).setText(text);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_craft, container, false);
		error_view = view.findViewById(R.id.craft_result_error);
		success_view = view.findViewById(R.id.craft_result_success);
		loading_view = view.findViewById(R.id.inventory_loading);
		views[0] = view.findViewById(R.id.craft_tab_inventory);
		views[1] = view.findViewById(R.id.craft_tab_main);
		
		error_view.setVisibility(View.GONE);
		success_view.setVisibility(View.GONE);

		// TAB 0: Inventory
		tabInventoryList = (ListView) views[0].findViewById(R.id.craft_tab_inventory_list);
		tabInventoryList.setOnItemClickListener(this);
		if (inventory != null) {
			tabInventoryList.setAdapter(tabInventoryListAdapter);
			tabInventoryListAdapter.filter("");
		}
		tabInventorySearch = (EditText) views[0].findViewById(R.id.inventory_search);
		tabInventorySearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				tabInventoryListAdapter.filter(s.toString());
			}
		});
		// TAB 1: Offers
		tabMainList = (ListView) views[1].findViewById(R.id.craft_main_list_ingredients);
		tabMainList.setAdapter(tabMainListAdapter = new TabMainListAdapter());
		tabMainList.setOnItemClickListener(this);
		tabMainCraft = (Button) views[1].findViewById(R.id.craft_main_craft);
		tabMainCraft.setOnClickListener(this);
		tabMainClear = (Button) views[1].findViewById(R.id.craft_main_clear);
		tabMainClear.setOnClickListener(this);
		tabMainStatusCircle = (ProgressBar) views[1].findViewById(R.id.craft_main_status_progress);
		// TAB RESULTS
		tabResultContinueCrafting = (Button) success_view.findViewById(R.id.craft_result_continue);
		tabResultContinueCrafting.setOnClickListener(this);
		return view;
	}

	public void onCompleted(List<SteamInventoryItem> items) {
		ActionBar actionBar = activity().getSupportActionBar();
		actionBar.removeAllTabs();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		success_view.setVisibility(View.VISIBLE);
		TextView successText = (TextView) success_view.findViewById(R.id.craft_success_text);
		successText.setText(String.format(activity().getString(R.string.craft_new_items), items.size()));
		ListView itemList = (ListView) success_view.findViewById(R.id.craft_result_items);
		itemList.setAdapter(new ResultsListAdapter(items));
	}

	public void craftResponse(CraftResponseCallback obj) {
		tabMainStatusCircle.setVisibility(View.GONE);
		if (obj.getItems().size() == 0) {
			// unsuccessful
			Toast.makeText(activity(), R.string.craft_unsuccessful, Toast.LENGTH_LONG).show();
			tabMainCraft.setEnabled(true);
		} else {
			for (int i = 0; i < selectedItems.size(); i++)
				inventory.items.remove(inventory.getItem(selectedItems.get(i)));
			
			craftingResults = new ArrayList<Long>();
			craftingResults.addAll(obj.getItems());
			new FetchInventoryTask().execute(SteamService.singleton.steamClient.getSteamId());

			selectedItems.clear();
			tabMainListAdapter.notifyDataSetChanged();
			tabInventoryListAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onClick(View v) {
		if (v == tabMainClear) {
			selectedItems.clear();
			Toast.makeText(activity(), R.string.craft_cleared, Toast.LENGTH_LONG).show();

			tabMainListAdapter.notifyDataSetChanged();
			tabInventoryListAdapter.notifyDataSetChanged();
		}
		if (v == tabMainCraft) {
			Toast.makeText(activity(), R.string.craft_crafting, Toast.LENGTH_LONG).show();
			tabMainStatusCircle.setVisibility(View.VISIBLE);
			tabMainCraft.setEnabled(false);

			long[] craftItems = new long[selectedItems.size()];
			for (int i = 0; i < selectedItems.size(); i++)
				craftItems[i] = selectedItems.get(i);
			craftingResults = null;
			activity().steamGC.craft(440, ECraftingRecipe.BestFit, craftItems);
		}
		if(v == tabResultContinueCrafting) {
			error_view.setVisibility(View.GONE);
			success_view.setVisibility(View.GONE);
			
			tabMainListAdapter.notifyDataSetChanged();
			tabInventoryListAdapter.notifyDataSetChanged();
			tabInventoryListAdapter.filter(tabInventoryListAdapter.filter);
			
			ActionBar actionBar = activity().getSupportActionBar();
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			for (int n = 0; n < 2; n++) {
				actionBar.addTab(actionBar.newTab().setTabListener(tabListener));
				updateTab(n);
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		SteamInventoryItem item = (SteamInventoryItem) parent.getAdapter().getItem(position);
		FragmentInventory.showItemInfo(activity(), item);
	}

	// TAB INVENTORY: LIST ADAPTER
	private class TabInventoryListAdapter extends BaseAdapter {
		public String filter = "";
		public List<SteamInventoryItem> filteredList = new ArrayList<SteamInventoryItem>();

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
			if (v == null)
				v = activity().getLayoutInflater().inflate(R.layout.trade_tab_inventory_item, null);
			SteamInventoryItem item = (SteamInventoryItem) getItem(position);

			CheckBox itemCheckbox = (CheckBox) v.findViewById(R.id.trade_item_checkbox);
			itemCheckbox.setChecked(selectedItems.contains(item.id));
			itemCheckbox.setTag(item.id);
			itemCheckbox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					long id = (Long) view.getTag();
					boolean checked = ((CheckBox) view).isChecked();
					if (checked)
						selectedItems.add(id);
					else
						selectedItems.remove(id);
					tabMainListAdapter.notifyDataSetChanged();

					tabMainCraft.setEnabled(selectedItems.size() > 0);
				}
			});

			item.populateListView(v);
			return v;
		}

		public void filter(String by) {
			if (inventory == null || inventory.items == null)
				return;
			filter = by;
			filteredList.clear();
			List<SteamInventoryItem> items = inventory.items;
			for (SteamInventoryItem item : items) {
				boolean nameMatch = by.trim().length() == 0 || item.fullname().toLowerCase(Locale.ENGLISH).contains(by.toLowerCase(Locale.ENGLISH));
				if (nameMatch && !item.flag_cannot_craft)
					filteredList.add(item);
			}
			notifyDataSetChanged();
		}
	}

	// TAB MAIN: LIST ADAPTER
	public class TabMainListAdapter extends BaseAdapter {

		public TabMainListAdapter() {
		}

		@Override
		public int getCount() {
			return selectedItems.size();
		}

		@Override
		public Object getItem(int position) {
			return (position >= selectedItems.size()) ? null : inventory.getItem(selectedItems.get(position));
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null)
				v = activity().getLayoutInflater().inflate(R.layout.inventory_item_item, null);
			SteamInventoryItem item = (SteamInventoryItem) getItem(position);

			item.populateListView(v);
			return v;
		}
	}

	// RESULTS: LIST ADAPTER
	public class ResultsListAdapter extends BaseAdapter {
		public List<SteamInventoryItem> items;

		public ResultsListAdapter(List<SteamInventoryItem> items) {
			this.items = items;
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public Object getItem(int position) {
			return items.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null)
				v = activity().getLayoutInflater().inflate(R.layout.view_item_details, null);

			SteamInventoryItem item = (SteamInventoryItem) getItem(position);
			item.populateDetailView(v);
			return v;
		}
	}

	private class FetchInventoryTask extends AsyncTask<SteamID, Integer, SteamInventory> {
		@Override
		protected void onPreExecute() {
			loading_view.setVisibility(View.VISIBLE);
		}

		@Override
		protected SteamInventory doInBackground(SteamID... args) {
			return SteamInventory.fetchInventory(args[0], SteamUtil.apikey, false, activity());
		}

		@Override
		protected void onPostExecute(SteamInventory result) {
			inventory = result;
			// get rid of UI stuff,
			if (activity() != null) {
				loading_view.setVisibility(View.GONE);
				if (inventory == null || inventory.items == null || tabInventoryList == null) {
					inventory = null;
					error_view.setVisibility(View.VISIBLE);
					TextView error_text = (TextView) error_view.findViewById(R.id.craft_error_text);
					error_text.setText(activity().getString(R.string.inv_error_private));
					return;
				}

				if (craftingResults == null) {
					// initial inventory load
					Toast.makeText(activity(), "Loaded Inventory", Toast.LENGTH_LONG).show();
					tabInventoryList.setAdapter(tabInventoryListAdapter);
					tabInventoryListAdapter.filter("");
				} else {
					// loading the craft result
					Toast.makeText(activity(), "Loaded Crafted Items", Toast.LENGTH_LONG).show();
					ArrayList<SteamInventoryItem> list = new ArrayList<SteamInventoryItem>();
					for(long id : craftingResults)
						list.add(inventory.getItem(id));
					onCompleted(list);
				}
			}
		}
	}
}