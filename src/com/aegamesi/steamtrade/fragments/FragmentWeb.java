package com.aegamesi.steamtrade.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamService;

public class FragmentWeb extends FragmentBase implements View.OnClickListener {
	public WebView web_view;
	public String url = null;
	public ProgressBar loading_bar;
	public ImageButton button_back;
	public ImageButton button_forward;
	public ImageButton button_home;
	public ImageButton button_refresh;
	public ImageButton button_store;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		if (getArguments() != null)
			url = getArguments().getString("url");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_web, container, false);
		web_view = (WebView) view.findViewById(R.id.web_view);
		loading_bar = (ProgressBar) view.findViewById(R.id.web_progress);
		button_back = (ImageButton) view.findViewById(R.id.web_button_back);
		button_forward = (ImageButton) view.findViewById(R.id.web_button_forward);
		button_home = (ImageButton) view.findViewById(R.id.web_button_home);
		button_refresh = (ImageButton) view.findViewById(R.id.web_button_refresh);
		button_store = (ImageButton) view.findViewById(R.id.web_button_store);

		button_back.setOnClickListener(this);
		button_forward.setOnClickListener(this);
		button_home.setOnClickListener(this);
		button_refresh.setOnClickListener(this);
		button_store.setOnClickListener(this);

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

			if (url == null)
				web_view.loadUrl("https://steamcommunity.com/profiles/" + SteamService.singleton.steamClient.getSteamId().convertToLong());
			else
				web_view.loadUrl(url);
		}
	}

	@Override
	public void onClick(View view) {
		if (view == button_back) {
			web_view.goBack();
		}
		if (view == button_forward) {
			web_view.goForward();
		}
		if (view == button_refresh) {
			web_view.reload();
		}
		if (view == button_home) {
			web_view.loadUrl("https://steamcommunity.com/profiles/" + SteamService.singleton.steamClient.getSteamId().convertToLong());
		}
		if (view == button_store) {
			web_view.loadUrl("https://store.steampowered.com/");
		}
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