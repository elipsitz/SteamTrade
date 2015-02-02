package com.aegamesi.steamtrade.fragments;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.aegamesi.steamtrade.R;

public class FragmentAbout extends FragmentBase {
	public WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		fragmentName = "FragmentAbout";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_about, container, false);
		String version = "v0.5.0";
		try {
			version = activity().getPackageManager().getPackageInfo(activity().getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		webView = (WebView) v.findViewById(R.id.news_webview);
		webView.loadUrl("http://aegamesi.com/ice/news.php?v=" + version);
		webView.setBackgroundColor(0x00000000);

		return v;
	}
}