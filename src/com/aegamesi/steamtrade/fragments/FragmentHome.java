package com.aegamesi.steamtrade.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.aegamesi.steamtrade.R;

public class FragmentHome extends FragmentBase {
	public WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		fragmentName = "FragmentHome";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_home, container, false);

		webView = (WebView) v.findViewById(R.id.news_webview);
		webView.loadUrl("http://aegamesi.com/steamtrade/news.html");
		webView.setBackgroundColor(0x00000000);

		return v;
	}
}