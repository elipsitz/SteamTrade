package com.aegamesi.steamtrade.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.OnTabSelectedListener;
import android.support.design.widget.TabLayout.Tab;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FragmentWeb extends FragmentBase {
	public WebView web_view;
	public String url = null;
	private boolean loaded_page = false;
	private boolean headless = false;
	private boolean forceDesktop = false;
	private boolean hasTabs = false;
	private int last_tab = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(abort)
			return;

		setHasOptionsMenu(true);

		Bundle args = getArguments();
		if (args != null) {
			if (args.containsKey("url"))
				url = getArguments().getString("url");
			headless = args.getBoolean("headless", false);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		setTitle(getString(R.string.nav_browser));

		Bundle args = getArguments();
		if(args != null) {
			String[] tabNames = args.getStringArray("tabs");
			if (tabNames != null) {
				hasTabs = true;
				TabLayout tabs = activity().tabs;
				tabs.removeAllTabs();
				tabs.setVisibility(View.VISIBLE);
				tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
				for(int i = 0; i < tabNames.length; i++) {
					Tab newTab = tabs.newTab();
					newTab.setText(tabNames[i]);
					tabs.addTab(newTab, i == last_tab);
				}
				final String[] tabUrls = args.getStringArray("tabUrls");
				if(tabUrls != null && tabUrls.length == tabNames.length) {
					tabs.setOnTabSelectedListener(new OnTabSelectedListener() {
						@Override
						public void onTabSelected(Tab tab) {
							String url = tabUrls[tab.getPosition()];
							web_view.loadUrl(url);
							last_tab = tab.getPosition();
						}

						@Override
						public void onTabUnselected(Tab tab) {
						}

						@Override
						public void onTabReselected(Tab tab) {
							onTabSelected(tab);
						}
					});
				}
			}
		}

		if (web_view != null) {
			CookieSyncManager.createInstance(activity());
			forceDesktop = PreferenceManager.getDefaultSharedPreferences(activity()).getBoolean("pref_desktop_mode", false);
			updateCookies();

			if (!loaded_page) {
				if(hasTabs) {
					activity().tabs.getTabAt(0).select();
				} else {
					if (url == null)
						web_view.loadUrl("https://steamcommunity.com/profiles/" + SteamService.singleton.steamClient.getSteamId().convertToLong());
					else
						web_view.loadUrl(url);
				}
				loaded_page = true;
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_web, container, false);
		web_view = (WebView) view.findViewById(R.id.web_view);

		web_view.setWebViewClient(new SteamWebViewClient());
		web_view.setWebChromeClient(new SteamWebChromeClient());
		WebSettings web_settings = web_view.getSettings();
		web_settings.setJavaScriptEnabled(true);
		web_settings.setBuiltInZoomControls(true);

		return view;
	}

	@Override
	public void onPause() {
		super.onPause();

		if(activity() != null && activity().tabs != null) {
			activity().tabs.setVisibility(View.GONE);
			activity().tabs.setOnTabSelectedListener(null);
			activity().tabs.removeAllTabs();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_web, menu);

		if (headless) {
			menu.findItem(R.id.web_toggle_view).setVisible(false);
			menu.findItem(R.id.web_community).setVisible(false);
			menu.findItem(R.id.web_store).setVisible(false);
		} else {
			boolean desktop_mode = PreferenceManager.getDefaultSharedPreferences(activity()).getBoolean("pref_desktop_mode", false);
			menu.findItem(R.id.web_toggle_view).setChecked(desktop_mode);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.web_back:
				web_view.goBack();
				return true;
			case R.id.web_forward:
				web_view.goForward();
				return true;
			case R.id.web_refresh:
				web_view.reload();
				return true;
			case R.id.web_community:
				web_view.loadUrl("https://steamcommunity.com/profiles/" + SteamService.singleton.steamClient.getSteamId().convertToLong());
				return true;
			case R.id.web_store:
				web_view.loadUrl("https://store.steampowered.com/");
				return true;
			case R.id.web_toggle_view:
				// switch
				item.setChecked(!item.isChecked());
				forceDesktop = item.isChecked();
				updateCookies();
				PreferenceManager.getDefaultSharedPreferences(activity()).edit().putBoolean("pref_desktop_mode", forceDesktop).apply();
				web_view.reload();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void updateCookies() {
		List<String> cookies = new ArrayList<String>();
		Collections.addAll(cookies, SteamService.generateSteamWebCookies().split(";"));
		cookies.add("forceMobile=" + ((!forceDesktop || headless) ? 1 : 0));
		cookies.add("dob=1"); // age check
		cookies.add("mobileClient=" + (headless ? "android" : ""));

		CookieManager cookieManager = CookieManager.getInstance();
		for (String cookie : cookies) {
			cookieManager.setCookie("store.steampowered.com", cookie);
			cookieManager.setCookie("steamcommunity.com", cookie);
		}
		CookieSyncManager.getInstance().sync();
	}

	public boolean onBackPressed() {
		if (web_view != null && web_view.canGoBack()) {
			web_view.goBack();
			return true;
		}

		return false;
	}

	private class SteamWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Uri uri = Uri.parse(url);
			if (uri.getScheme().equalsIgnoreCase("steammobile")) {
				Log.d("FragmentWeb", "Captured url: " + url);
				String command = uri.getHost();

				if (command.equalsIgnoreCase("settitle")) {
					setTitle(uri.getQueryParameter("title"));
				}
				if (command.equalsIgnoreCase("openurl")) {
					view.loadUrl(uri.getQueryParameter("url"));
				}
				if (command.equalsIgnoreCase("reloadpage")) {
					view.reload();
				}
				return true;
			}

			view.loadUrl(url);
			return true;
		}
	}

	private class SteamWebChromeClient extends WebChromeClient {
		public void onProgressChanged(WebView view, int progress) {
			if (activity() != null && activity().progressBar != null) {
				ProgressBar loading_bar = activity().progressBar;
				if (progress < 100 && loading_bar.getVisibility() != View.VISIBLE)
					loading_bar.setVisibility(View.VISIBLE);
				if (progress == 100)
					loading_bar.setVisibility(View.GONE);

				loading_bar.setProgress(progress);
			}
		}
	}
}