package com.aegamesi.steamtrade.fragments;

import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.steam.SteamMessageHandler;
import com.google.android.gms.analytics.HitBuilders;

import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;

public class FragmentBase extends Fragment implements SteamMessageHandler {

	@Override
	public void onStart() {
		super.onStart();

		setAnalyticsScreen(getClass().getName());
	}

	public void setAnalyticsScreen(String name) {
		activity().tracker().setScreenName(name);
		activity().tracker().send(new HitBuilders.AppViewBuilder().build());
	}

	public final MainActivity activity() {
		return (MainActivity) getActivity();
	}

	public void sendAnalyticsEvent(String category, String action, String label, Long value) {
		activity().tracker().send(new HitBuilders.EventBuilder()
				.setCategory(category)
				.setAction(action)
				.setLabel(label)
				.setValue(value)
				.build());

	}

	public void setTitle(CharSequence title) {
		if (activity() != null) {
			ActionBar actionBar = activity().getSupportActionBar();
			if (actionBar != null)
				actionBar.setTitle(title);
		}
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		// by default, do nothing
	}
}
