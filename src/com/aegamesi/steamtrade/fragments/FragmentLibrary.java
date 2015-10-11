package com.aegamesi.steamtrade.fragments;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.LibraryAdapter;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.aegamesi.steamtrade.steam.SteamWeb;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentLibrary extends FragmentBase implements View.OnClickListener {
	public LibraryAdapter adapterLibrary;
	public RecyclerView listGames;
	public long steamID = 0;

	public View loading_view;
	public List<LibraryEntry> games;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (abort)
			return;

		setHasOptionsMenu(true);

		if (getArguments() != null && getArguments().containsKey("id"))
			steamID = getArguments().getLong("id");
		else
			steamID = SteamService.singleton.steamClient.getSteamId().convertToLong();
	}

	@Override
	public void onResume() {
		super.onResume();
		setTitle(getString(R.string.nav_games));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_library, container, false);

		loading_view = view.findViewById(R.id.offers_loading);

		listGames = (RecyclerView) view.findViewById(R.id.games_list);
		adapterLibrary = new LibraryAdapter(this);
		adapterLibrary.setGames(games, adapterLibrary.currentSort);
		RecyclerView.LayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
		listGames.setHasFixedSize(true);
		listGames.setLayoutManager(layoutManager);
		listGames.setAdapter(adapterLibrary);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		if (games == null) {
			if (SteamUtil.webApiKey != null && SteamUtil.webApiKey.length() > 0)
				new FetchLibraryTask().execute();
			else
				Toast.makeText(activity(), R.string.api_key_not_loaded, Toast.LENGTH_LONG).show();
		}
		//activity().getSupportActionBar().setTitle(R.string.trade_offer);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_library, menu);
		inflater.inflate(R.menu.search, menu);

		SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return onQueryTextChange(query);
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				adapterLibrary.filter(newText);
				return true;
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_library_sort_name:
				if (adapterLibrary != null && adapterLibrary.currentSort != LibraryAdapter.SORT_ALPHABETICAL)
					adapterLibrary.setGames(games, LibraryAdapter.SORT_ALPHABETICAL);
				return true;

			case R.id.menu_library_sort_playtime:
				if (adapterLibrary != null && adapterLibrary.currentSort != LibraryAdapter.SORT_PLAYTIME)
					adapterLibrary.setGames(games, LibraryAdapter.SORT_PLAYTIME);
				return true;

			case R.id.menu_library_add:
				AlertDialog.Builder alert = new AlertDialog.Builder(activity());
				alert.setTitle(activity().getString(R.string.library_activate_product));
				alert.setMessage(activity().getString(R.string.library_enter_key));
				final EditText input = new EditText(activity());
				input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				alert.setView(input);
				alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String key = input.getText().toString().toUpperCase().trim();
						if (key.length() != 0 && activity() != null) {
							// send this to steam for activation
							activity().steamUser.registerProductKey(key);
							// show a toast
							Toast.makeText(activity(), R.string.library_validating_key, Toast.LENGTH_SHORT).show();
						}
					}
				});
				alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});
				alert.show();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.game_card) {
			int appid = (Integer) view.getTag();
			String url = String.format("http://store.steampowered.com/app/%d/", appid);
			Fragment fragment = new FragmentWeb();
			Bundle bundle = new Bundle();
			bundle.putString("url", url);
			bundle.putBoolean("headless", true);
			fragment.setArguments(bundle);
			activity().browseToFragment(fragment, true);
		}
	}

	private List<LibraryEntry> fetchLibrary() {
		String webapi_url = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=%s&format=json&steamid=%d&include_appinfo=1&include_played_free_games=1";
		webapi_url = String.format(webapi_url, SteamUtil.webApiKey, steamID);
		String response = SteamWeb.fetch(webapi_url, "GET", null, "");

		try {
			List<LibraryEntry> games = new ArrayList<LibraryEntry>();
			JSONObject json = new JSONObject(response);
			if (json.has("response")) {
				JSONArray json_games = json.getJSONObject("response").getJSONArray("games");
				for (int i = 0; i < json_games.length(); i++) {
					games.add(new LibraryEntry(json_games.getJSONObject(i)));
				}
			}
			return games;
		} catch (JSONException e) {
			e.printStackTrace();
			return new ArrayList<LibraryEntry>();
		}
	}

	public static class LibraryEntry {
		public int appid;
		public String name;
		public double playtime_2weeks;
		public double playtime_forever;
		public String img_icon_url;
		public String img_logo_url;
		public boolean has_community_visible_stats;

		public LibraryEntry(JSONObject json) {
			appid = json.optInt("appid", 0);
			name = json.optString("name", "Unknown");
			playtime_2weeks = ((double) json.optInt("playtime_2weeks", 0)) / 60.0;
			playtime_forever = ((double) json.optInt("playtime_forever", 0)) / 60.0;
			img_icon_url = json.optString("img_icon_url");
			img_logo_url = json.optString("img_logo_url");
			has_community_visible_stats = json.optBoolean("has_community_visible_stats", false);
		}
	}

	private class FetchLibraryTask extends AsyncTask<Void, Void, List<LibraryEntry>> {
		@Override
		protected List<LibraryEntry> doInBackground(Void... args) {
			return fetchLibrary();
		}

		@Override
		protected void onPreExecute() {
			loading_view.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onPostExecute(List<LibraryEntry> result) {
			if (activity() == null)
				return;

			games = result;
			adapterLibrary.setGames(games, adapterLibrary.currentSort);

			// get rid of UI stuff,
			loading_view.setVisibility(View.GONE);
		}
	}
}