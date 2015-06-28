package com.aegamesi.steamtrade.fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
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

public class FragmentWeb extends FragmentBase {
	public WebView web_view;
	public String url = null;
	public ProgressBar loading_bar;
	private boolean loaded_page = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		if (getArguments() != null)
			url = getArguments().getString("url");
	}

	@Override
	public void onResume() {
		super.onResume();
		setTitle(getString(R.string.nav_browser));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_web, container, false);
		web_view = (WebView) view.findViewById(R.id.web_view);
		loading_bar = (ProgressBar) view.findViewById(R.id.web_progress);

		web_view.setWebViewClient(new SteamWebViewClient());
		web_view.setWebChromeClient(new SteamWebChromeClient());
		WebSettings web_settings = web_view.getSettings();
		web_settings.setJavaScriptEnabled(true);
		web_settings.setBuiltInZoomControls(true);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		if (web_view != null) {
			CookieSyncManager.createInstance(activity());
			CookieManager cookieManager = CookieManager.getInstance();
			String[] cookies = SteamService.generateSteamWebCookies().split(";");
			for (String cookie : cookies) {
				cookieManager.setCookie("store.steampowered.com", cookie);
				cookieManager.setCookie("steamcommunity.com", cookie);
			}
			boolean desktop_mode = PreferenceManager.getDefaultSharedPreferences(activity()).getBoolean("pref_desktop_mode", false);
			updateDesktopViewCookie(desktop_mode);
			CookieSyncManager.getInstance().sync();

			if (!loaded_page) {
				if (url == null)
					web_view.loadUrl("https://steamcommunity.com/profiles/" + SteamService.singleton.steamClient.getSteamId().convertToLong());
				else
					web_view.loadUrl(url);
				loaded_page = true;
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_web, menu);

		boolean desktop_mode = PreferenceManager.getDefaultSharedPreferences(activity()).getBoolean("pref_desktop_mode", false);
		menu.findItem(R.id.web_toggle_view).setChecked(desktop_mode);
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
				updateDesktopViewCookie(item.isChecked());
				PreferenceManager.getDefaultSharedPreferences(activity()).edit().putBoolean("pref_desktop_mode", item.isChecked()).apply();
				web_view.reload();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void updateDesktopViewCookie(boolean enable) {
		CookieManager cookieManager = CookieManager.getInstance();
		//cookieManager.removeSessionCookie();
		String cookieValue = "forceMobile=" + (enable ? 0 : 1);
		cookieManager.setCookie("store.steampowered.com", cookieValue);
		cookieManager.setCookie("steamcommunity.com", cookieValue);
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
			view.loadUrl(url);
			return true;
		}
	}

	private class SteamWebChromeClient extends WebChromeClient {
		public void onProgressChanged(WebView view, int progress) {
			if (progress < 100 && loading_bar.getVisibility() != View.VISIBLE)
				loading_bar.setVisibility(View.VISIBLE);
			if (progress == 100)
				loading_bar.setVisibility(View.GONE);

			loading_bar.setProgress(progress);
		}
	}
}