package com.aegamesi.steamtrade.fragments;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;

public class FragmentAbout extends FragmentBase {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (abort)
			return;
	}

	@Override
	public void onResume() {
		super.onResume();
		setTitle(getString(R.string.nav_about));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View v = inflater.inflate(R.layout.fragment_about, container, false);

		TextView textAbout = (TextView) v.findViewById(R.id.ice_text_about);
		textAbout.setMovementMethod(LinkMovementMethod.getInstance());
		textAbout.setText(Html.fromHtml(getString(R.string.ice_about)));

		return v;
	}
}