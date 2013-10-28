package com.aegamesi.steamtrade.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import uk.co.thomasc.steamkit.types.steamid.SteamID;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamInventory;
import com.aegamesi.steamtrade.steam.SteamInventory.SteamInventoryItem;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.loopj.android.image.SmartImageView;

public class FragmentInventory extends FragmentBase implements OnItemClickListener {
	public SteamID id;
	public SteamInventory inventory = null;
	public boolean isGrid = true;

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

		loading_view = view.findViewById(R.id.inventory_loading);
		inventoryGrid = (GridView) view.findViewById(R.id.inventory_grid);
		inventoryList = (ListView) view.findViewById(R.id.inventory_list);
		error_view = view.findViewById(R.id.inventory_error_view);
		// adapter setup
		if (inventory != null) {
			inventoryGrid.setAdapter(adapter);
			inventoryList.setAdapter(adapter);
		}
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

	@Override
	public void onStart() {
		super.onStart();

		// yes, this *must* be after onCreateView
		if (inventory == null)
			new FetchInventoryTask().execute(id);
	}

	public class FetchInventoryTask extends AsyncTask<SteamID, Integer, SteamInventory> {
		public ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			loading_view.setVisibility(View.VISIBLE);
		}

		@Override
		protected SteamInventory doInBackground(SteamID... args) {
			return SteamInventory.fetchInventory(args[0], SteamUtil.apikey, true, activity());
		}

		@Override
		protected void onPostExecute(SteamInventory result) {
			inventory = result;
			// get rid of UI stuff,
			if (activity() != null) {
				loading_view.setVisibility(View.GONE);
				if (inventory != null && inventory.items != null && inventoryGrid != null && inventoryList != null) {
					Toast.makeText(activity(), "Loaded Inventory", Toast.LENGTH_LONG).show();
					inventoryGrid.setAdapter(adapter);
					inventoryList.setAdapter(adapter);
					adapter.filter("");
				} else {
					inventory = null;
					error_view.setVisibility(View.VISIBLE);
					TextView error_text = (TextView) error_view.findViewById(R.id.inventory_error_text);
					error_text.setText(activity().getString(R.string.inv_error_private));
				}
			}
		}
	}

	public class InventoryAdapter extends BaseAdapter {
		List<SteamInventoryItem> filteredList = new ArrayList<SteamInventoryItem>();

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
			SteamInventoryItem item = (SteamInventoryItem) getItem(position);
			if (isGrid) {
				if (v == null || !v.getTag().equals(isGrid))
					v = activity().getLayoutInflater().inflate(R.layout.inventory_grid_item, null);

				SmartImageView img = (SmartImageView) v.findViewById(R.id.item_image);
				img.setImageUrl("http://media.steampowered.com/apps/440/icons/" + item.def().image_url);
				img.setBackgroundColor(item.quality.bgColor);
				v.setBackgroundColor(item.quality.outlineColor);
			} else {
				if (v == null || !v.getTag().equals(isGrid))
					v = activity().getLayoutInflater().inflate(R.layout.inventory_item_item, null);

				item.populateListView(v);
			}
			v.setTag(isGrid);
			return v;
		}

		public void filter(String by) {
			filteredList.clear();
			if (inventory == null)
				return;
			if (by.trim().length() == 0) {
				filteredList.addAll(inventory.items);
			} else {
				for (SteamInventoryItem item : inventory.items)
					if (item.fullname().toLowerCase(Locale.ENGLISH).contains(by.toLowerCase(Locale.ENGLISH)))
						filteredList.add(item);
			}
			notifyDataSetChanged();
		}
	}

	public static void showItemInfo(Activity context, SteamInventoryItem item) {
		View itemInfoView = context.getLayoutInflater().inflate(R.layout.view_item_details, null);
		item.populateDetailView(itemInfoView);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(itemInfoView);
		builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		builder.show();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		SteamInventoryItem item = (SteamInventoryItem) parent.getAdapter().getItem(position);
		showItemInfo(activity(), item);
	}
}