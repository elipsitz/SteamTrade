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
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.ItemListAdapter;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamWeb;
import com.aegamesi.steamtrade.trade2.TradeUtil;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nosoop.steamtrade.inventory.TradeInternalInventories;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.co.thomasc.steamkit.base.ClientMsgProtobuf;
import uk.co.thomasc.steamkit.base.gc.tf2.ECraftingRecipe;
import uk.co.thomasc.steamkit.base.generated.SteammessagesClientserver;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EMsg;
import uk.co.thomasc.steamkit.steam3.handlers.steamgamecoordinator.callbacks.CraftResponseCallback;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentInventory extends FragmentBase implements AdapterView.OnItemSelectedListener, View.OnClickListener {
	public SteamID steamId;
	public TradeInternalInventories inventories;
	public List<AppContextPair> appContextPairs;

	// crafting support
	public Set<TradeInternalAsset> craftingItems = new HashSet<TradeInternalAsset>();
	public long[] craftingResult = null;

	public Spinner inventorySelect;
	public ArrayAdapter<AppContextPair> inventorySelectAdapter;
	public GridView inventoryGrid;
	public View loading_view;
	public ItemListAdapter adapter;
	public View error_view;
	public Button buttonCraft;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		// TODO support fetching the inventories (rather than hardcoded list)
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

		new Thread(new Runnable() {
			@Override
			public void run() {
				// we need to be in tf2
				int gameId = 440;

				/* ewwww protobuuufs */
				ClientMsgProtobuf<SteammessagesClientserver.CMsgClientGamesPlayed> playGame;
				playGame = new ClientMsgProtobuf<SteammessagesClientserver.CMsgClientGamesPlayed>(SteammessagesClientserver.CMsgClientGamesPlayed.class, EMsg.ClientGamesPlayed);
				SteammessagesClientserver.CMsgClientGamesPlayed.GamePlayed gamePlayed = new SteammessagesClientserver.CMsgClientGamesPlayed.GamePlayed();
				gamePlayed.gameId = gameId;
				playGame.getBody().gamesPlayed = new SteammessagesClientserver.CMsgClientGamesPlayed.GamePlayed[]{gamePlayed};
				SteamService.singleton.steamClient.send(playGame);
			}
		}).start();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_inventory, container, false);

		// create inventory select spinner
		inventorySelect = (Spinner) view.findViewById(R.id.inventory_select);
		inventorySelect.setOnItemSelectedListener(this);
		inventorySelectAdapter = new ArrayAdapter<AppContextPair>(activity(), android.R.layout.simple_spinner_item);
		inventorySelectAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		for (AppContextPair pair : appContextPairs)
			inventorySelectAdapter.add(pair);
		inventorySelectAdapter.notifyDataSetChanged();
		inventorySelect.setAdapter(inventorySelectAdapter);
		//end
		loading_view = view.findViewById(R.id.inventory_loading);
		inventoryGrid = (GridView) view.findViewById(R.id.inventory_grid);
		error_view = view.findViewById(R.id.inventory_error_view);
		// adapter setup
		adapter = new ItemListAdapter(activity(), inventoryGrid, true, new ItemListAdapter.IItemListProvider() {
			@Override
			public void onItemChecked(TradeInternalAsset item, boolean checked) {
				if (checked)
					craftingItems.add(item);
				else
					craftingItems.remove(item);

				buttonCraft.setEnabled(craftingItems.size() > 0);
				buttonCraft.setText(String.format(getString(R.string.craft_x_items), craftingItems.size()));
			}

			@Override
			public boolean shouldItemBeChecked(TradeInternalAsset item) {
				return craftingItems.contains(item);
			}
		});
		inventoryGrid.setAdapter(adapter);
		// inventory search
		EditText inventorySearch = (EditText) view.findViewById(R.id.inventory_search);
		inventorySearch.setVisibility(View.GONE);
		//
		buttonCraft = (Button) view.findViewById(R.id.inventory_craft);
		buttonCraft.setVisibility(View.GONE);
		buttonCraft.setOnClickListener(this);

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
				adapter.filter(newText);
				return true;
			}
		});
	}

	@Override
	public void onStop() {
		super.onStop();

		/*new Thread(new Runnable() {
			@Override
			public void run() {
				// we need to be in tf2
				ClientMsgProtobuf<SteammessagesClientserver.CMsgClientGamesPlayed.Builder> playGame;
				playGame = new ClientMsgProtobuf<SteammessagesClientserver.CMsgClientGamesPlayed.Builder>(SteammessagesClientserver.CMsgClientGamesPlayed.class, EMsg.ClientGamesPlayed);
				playGame.getBody().addGamesPlayed(SteammessagesClientserver.CMsgClientGamesPlayed.GamePlayed.newBuilder().setGameId(0).build());
				SteamService.singleton.steamClient.send(playGame);
			}
		}).start();*/
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_inventory_toggle_view:
				adapter.setListMode(adapter.getListMode() == ItemListAdapter.MODE_GRID ? ItemListAdapter.MODE_LIST : ItemListAdapter.MODE_GRID);
				item.setIcon((adapter.getListMode() == ItemListAdapter.MODE_GRID) ? R.drawable.ic_view_list : R.drawable.ic_view_module);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (parent == inventorySelect) {
			AppContextPair pair = (AppContextPair) inventorySelect.getSelectedItem();
			new FetchInventoryTask().execute(pair);

			// we only support tf2 crafting for now...
			// look into other types of crafting (if they even exist?)
			boolean enableCrafting = (pair.getAppid() == 440) && steamId.equals(SteamService.singleton.steamClient.getSteamId());
			craftingItems.clear();
			craftingResult = null;
			buttonCraft.setText(String.format(getString(R.string.craft_x_items), craftingItems.size()));
			buttonCraft.setVisibility(enableCrafting ? View.VISIBLE : View.GONE);
			adapter.hasCheckboxes = enableCrafting;

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
		AppContextPair pair = (AppContextPair) inventorySelect.getSelectedItem();
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
		protected void onPostExecute(JSONObject result) {
			try {
				if (result != null) {
					inventories.addInventory(appContext, result);

					if (craftingResult != null && appContext.getAppid() == 440) {
						// show the crafting results
						List<TradeInternalAsset> items = new ArrayList<TradeInternalAsset>();
						for (long item : craftingResult) {
							TradeInternalAsset asset = inventories.getInventory(appContext).getItem(item);
							if (asset != null)
								items.add(asset);
						}
						if (activity() != null)
							TradeUtil.showItemListModal(activity(), getString(R.string.craft_completed), items);

						craftingResult = null;
					}
				}

				// get rid of UI stuff,
				if (activity() != null) {
					loading_view.setVisibility(View.GONE);
					adapter.setItemList(inventories.getInventory(appContext).getItemList());
				}
			} catch (Exception e) {
				if (activity() != null) {
					error_view.setVisibility(View.VISIBLE);
					TextView error_text = (TextView) error_view.findViewById(R.id.inventory_error_text);
					error_text.setText(e.getMessage() == null ? getString(R.string.inv_error_loading) : e.getMessage());
				}
			}
		}
	}
}