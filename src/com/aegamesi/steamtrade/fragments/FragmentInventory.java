package com.aegamesi.steamtrade.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.ItemListAdapter;
import com.aegamesi.steamtrade.fragments.support.ItemListView;
import com.aegamesi.steamtrade.fragments.support.ItemListView.IItemListProvider;
import com.aegamesi.steamtrade.steam.SteamItemUtil;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamWeb;
import com.nosoop.steamtrade.ContextScraper;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nosoop.steamtrade.inventory.TradeInternalInventories;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.co.thomasc.steamkit.base.gc.tf2.ECraftingRecipe;
import uk.co.thomasc.steamkit.steam3.handlers.steamgamecoordinator.callbacks.CraftResponseCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;
import uk.co.thomasc.steamkit.types.steamid.SteamID;
import uk.co.thomasc.steamkit.util.cSharp.events.ActionT;

public class FragmentInventory extends FragmentBase implements AdapterView.OnItemSelectedListener, View.OnClickListener {
	public SteamID steamId;
	public TradeInternalInventories inventories;
	public List<AppContextPair> appContextPairs;

	// crafting support
	public Set<TradeInternalAsset> craftingItems = new HashSet<TradeInternalAsset>();
	public long[] craftingResult = null;

	public Spinner selectInventory;
	public ArrayAdapter<AppContextPair> inventorySelectAdapter;
	public View viewLoading;
	public View viewError;
	public Button buttonCraft;
	public CheckBox checkBoxEnableCrafting;
	public View viewCraftingMenu;
	public TextView viewSearchResult;

	public ItemListView itemList;
	public IItemListProvider itemListProvider;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(abort)
			return;

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
		if (getArguments() != null) {
			if (getArguments().containsKey("steamId"))
				steamId = new SteamID(getArguments().getLong("steamId", myID));

			if (getArguments().containsKey("craftingResult"))
				craftingResult = getArguments().getLongArray("craftingResult");
		} else {
			steamId = new SteamID(myID);
		}

		// scrape the inventory page for fun and profit
		// (and by fun and profit I mean, fetch app/context pairs, and clear item notifications)
		new ScrapInventoryPage().execute();
	}

	@Override
	public void onResume() {
		super.onResume();
		setTitle(getString(R.string.nav_inventory));
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		// our crafting was completed (or failed)
		msg.handle(CraftResponseCallback.class, new ActionT<CraftResponseCallback>() {
			@Override
			public void call(CraftResponseCallback obj) {
				onCraftingCompleted(obj);
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_inventory, container, false);

		// create inventory select spinner
		selectInventory = (Spinner) view.findViewById(R.id.inventory_select);
		selectInventory.setOnItemSelectedListener(this);
		inventorySelectAdapter = new ArrayAdapter<AppContextPair>(activity(), android.R.layout.simple_spinner_item);
		inventorySelectAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		for (AppContextPair pair : appContextPairs)
			inventorySelectAdapter.add(pair);
		inventorySelectAdapter.notifyDataSetChanged();
		selectInventory.setAdapter(inventorySelectAdapter);
		//end
		viewLoading = view.findViewById(R.id.inventory_loading);
		viewError = view.findViewById(R.id.inventory_error_view);
		// adapter setup
		itemList = (ItemListView) view.findViewById(R.id.itemlist);
		itemListProvider = new ItemListView.IItemListProvider() {
			@Override
			public void onItemChecked(TradeInternalAsset item, boolean checked) {
				if (checked)
					craftingItems.add(item);
				else
					craftingItems.remove(item);

				buttonCraft.setEnabled(craftingItems.size() > 0 && checkBoxEnableCrafting.isChecked());
				buttonCraft.setText(String.format(getString(R.string.craft_x_items), craftingItems.size()));
			}

			@Override
			public boolean shouldItemBeChecked(TradeInternalAsset item) {
				return craftingItems.contains(item);
			}
		};
		// inventory search (hide here, still used in trading)
		viewSearchResult = (TextView) view.findViewById(R.id.inventory_search_result);
		//
		buttonCraft = (Button) view.findViewById(R.id.inventory_craft);
		buttonCraft.setOnClickListener(this);
		checkBoxEnableCrafting = (CheckBox) view.findViewById(R.id.inventory_craft_enable);
		checkBoxEnableCrafting.setOnClickListener(this);
		viewCraftingMenu = view.findViewById(R.id.inventory_menu_craft);
		viewCraftingMenu.setVisibility(View.GONE);

		// select last selected inventory
		SharedPreferences prefs = activity().getPreferences(Context.MODE_PRIVATE);
		int pref_appid = prefs.getInt("inv_last_appid", -1);
		long pref_context = prefs.getLong("inv_last_context", -1);
		int pref_index = -1;
		for (int i = 0; i < appContextPairs.size(); i++)
			if (appContextPairs.get(i).getAppid() == pref_appid && appContextPairs.get(i).getContextid() == pref_context)
				pref_index = i;
		if (pref_index != -1)
			selectInventory.setSelection(pref_index);
		// end

		return view;
	}

	@Override
	public void onStop() {
		super.onStop();

		if (checkBoxEnableCrafting.isChecked()) {
			activity().steamUser.setPlayingGame(0);
			checkBoxEnableCrafting.setChecked(false);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_inventory, menu);
		inflater.inflate(R.menu.item_list, menu);

		SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return onQueryTextChange(query);
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				itemList.filter(newText);

				boolean filtering = newText != null && newText.trim().length() > 0;
				if (filtering) {
					viewSearchResult.setVisibility(View.VISIBLE);
					viewSearchResult.setText(String.format(getString(R.string.search_result_count), itemList.getFilteredItemCount(), itemList.getTotalItemCount()));
				} else {
					viewSearchResult.setVisibility(View.GONE);
				}
				return true;
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_inventory_toggle_view:
				itemList.setListMode(itemList.getListMode() == ItemListAdapter.MODE_GRID ? ItemListAdapter.MODE_LIST : ItemListAdapter.MODE_GRID);
				item.setIcon((itemList.getListMode() == ItemListAdapter.MODE_GRID) ? R.drawable.ic_view_list : R.drawable.ic_view_module);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (parent == selectInventory) {
			AppContextPair pair = (AppContextPair) selectInventory.getSelectedItem();
			new FetchInventoryTask().execute(pair);

			// we only support tf2 crafting for now...
			// look into other types of crafting (if they even exist?)
			boolean enableCrafting = (pair.getAppid() == 440) && steamId.equals(SteamService.singleton.steamClient.getSteamId());
			craftingItems.clear();
			craftingResult = null;
			itemList.setProvider(null);
			buttonCraft.setEnabled(false);
			buttonCraft.setText(String.format(getString(R.string.craft_x_items), craftingItems.size()));
			viewCraftingMenu.setVisibility(enableCrafting ? View.VISIBLE : View.GONE);
			if (checkBoxEnableCrafting.isChecked())
				activity().steamUser.setPlayingGame(0);
			checkBoxEnableCrafting.setChecked(false);

			SharedPreferences.Editor prefs = activity().getPreferences(Context.MODE_PRIVATE).edit();
			prefs.putInt("inv_last_appid", pair.getAppid());
			prefs.putLong("inv_last_context", pair.getContextid());
			prefs.apply();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {

	}

	@Override
	public void onClick(View view) {
		if (view == buttonCraft && buttonCraft.isEnabled()) {
			doCrafting(false);
		}
		if (view == checkBoxEnableCrafting) {
			buttonCraft.setEnabled(craftingItems.size() > 0 && checkBoxEnableCrafting.isChecked());
			itemList.setProvider(checkBoxEnableCrafting.isChecked() ? itemListProvider : null);

			activity().steamUser.setPlayingGame(checkBoxEnableCrafting.isChecked() ? 440 : 0);
		}
	}

	private boolean doCrafting(boolean force) {
		// "Not Usable in Crafting"
		// "Not Tradable"
		for (TradeInternalAsset asset : craftingItems) {
			for (TradeInternalAsset.Description desc : asset.getDescriptions()) {
				if (desc.getValue().contains("Not Usable in Crafting") && !force) {
					Toast.makeText(activity(), R.string.craft_noncraftable, Toast.LENGTH_LONG).show();
					return false;
				}
				if (desc.getValue().contains("Not Tradable") && !force) {
					(new AlertDialog.Builder(activity()))
							.setMessage(R.string.craft_nontradable)
							.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									doCrafting(true);
								}
							})
							.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
								}
							})
							.show();
					return false;
				}
			}
		}

		// otherwise, keep going! craaaaaft
		Toast.makeText(activity(), R.string.craft_crafting, Toast.LENGTH_LONG).show();
		new Thread(new Runnable() {
			@Override
			public void run() {
				int index = 0;
				long[] craftItems = new long[craftingItems.size()];
				for (TradeInternalAsset asset : craftingItems)
					craftItems[index++] = asset.getAssetid();
				craftingResult = null;
				activity().steamGC.craft(440, ECraftingRecipe.BestFit, craftItems);
			}
		}).start();
		return true;
	}

	public void onCraftingCompleted(CraftResponseCallback obj) {
		if (obj.getItems().size() > 0)
			Toast.makeText(activity(), String.format(getString(R.string.craft_successful), obj.getItems().size()), Toast.LENGTH_LONG).show();
		else
			Toast.makeText(activity(), String.format(getString(R.string.craft_failed), obj.getItems().size()), Toast.LENGTH_LONG).show();

		// set the craft results for later...
		int index = 0;
		craftingResult = new long[obj.getItems().size()];
		for (Long l : obj.getItems())
			craftingResult[index++] = l;

		// and reload the inventory
		// XXX for some reason, you CANNOT get the new inventory (from steam) if you don't reload the activity().. FIND OUT WHY!
		/*craftingItems.clear();
		AppContextPair pair = (AppContextPair) selectInventory.getSelectedItem();
		new FetchInventoryTask().execute(pair);*/
		Fragment fragment = new FragmentInventory();
		Bundle bundle = new Bundle();
		bundle.putLong("steamId", steamId.convertToLong());
		bundle.putLongArray("craftingResult", craftingResult);
		fragment.setArguments(bundle);
		activity().browseToFragment(fragment, true);
	}

	private class FetchInventoryTask extends AsyncTask<AppContextPair, Integer, JSONObject> {
		public AppContextPair appContext;

		@Override
		protected JSONObject doInBackground(AppContextPair... args) {
			// arg 0: steamid
			// arg 1: appid
			// arg 2: contextid
			//http://steamcommunity.com/profiles/76561197960422183/inventory/json/440/2/

			final String url, response;
			appContext = args[0];

			if (inventories.hasInventory(appContext) && craftingResult == null)
				return null; // no reason to load it again... unless we're reloading cause of crafting

			// TODO Add support for large inventories ourselves.
			url = String.format("http://steamcommunity.com/profiles/%d/inventory/json/%d/%d/",
					steamId.convertToLong(), appContext.getAppid(), appContext.getContextid());

			response = SteamWeb.fetch(url, "GET", null, null);

			try {
				return new JSONObject(response);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPreExecute() {
			viewLoading.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			try {
				if (result != null) {
					inventories.addInventory(appContext, result);
					boolean success = result.optBoolean("success", false);

					if (success) {
						if (craftingResult != null && appContext.getAppid() == 440) {
							// show the crafting results
							List<TradeInternalAsset> items = new ArrayList<TradeInternalAsset>();
							for (long item : craftingResult) {
								TradeInternalAsset asset = inventories.getInventory(appContext).getItem(item);
								if (asset != null)
									items.add(asset);
							}
							if (activity() != null)
								SteamItemUtil.showItemListModal(activity(), getString(R.string.craft_completed), items);

							craftingResult = null;
						}

						// select the proper inventory
						int pos = -1;
						for (int i = 0; i < appContextPairs.size(); i++) {
							if (appContextPairs.get(i).equals(appContext)) {
								pos = i;
								break;
							}
						}
						if (pos != -1)
							selectInventory.setSelection(pos);
					} else if (result.has("Error")) {
						String errorText = result.getString("Error");
						Toast.makeText(activity(), errorText, Toast.LENGTH_LONG).show();
					}
				}

				// get rid of UI stuff,
				if (activity() != null) {
					viewLoading.setVisibility(View.GONE);
					itemList.setItems(inventories.getInventory(appContext).getItemList());
				}
			} catch (Exception e) {
				if (activity() != null) {
					viewError.setVisibility(View.VISIBLE);
					TextView error_text = (TextView) viewError.findViewById(R.id.inventory_error_text);
					error_text.setText(e.getMessage() == null ? getString(R.string.inv_error_loading) : e.getMessage());
				}
			}
		}
	}

	private class ScrapInventoryPage extends AsyncTask<Void, Void, List<AppContextPair>> {
		@Override
		protected List<AppContextPair> doInBackground(Void... voids) {
			//g_rgAppContextData
			//http://steamcommunity.com/profiles/76561197960422183/inventory/

			final String url, response;

			// TODO Add support for large inventories ourselves.
			url = String.format("http://steamcommunity.com/profiles/%d/inventory/",
					steamId.convertToLong());

			response = SteamWeb.fetch(url, "GET", null, null);

			try {
				return ContextScraper.scrapeContextData(response);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<AppContextPair> contexts) {
			if (contexts != null && contexts.size() > 0) {
				appContextPairs = contexts;

				if (inventorySelectAdapter != null) {
					inventorySelectAdapter.clear();
					for (AppContextPair pair : appContextPairs)
						inventorySelectAdapter.add(pair);
				}
			}
		}
	}
}