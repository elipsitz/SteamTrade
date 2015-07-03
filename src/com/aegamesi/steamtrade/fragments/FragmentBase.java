package com.aegamesi.steamtrade.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.steam.SteamMessageHandler;
import com.google.android.gms.analytics.HitBuilders;

import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;

public class FragmentBase extends Fragment implements SteamMessageHandler {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// make sure we're actually connected to steam...
		activity().assertSteamConnection();
	}

	@Override
	public void onResume() {
		super.onResume();

		setAnalyticsScreen(getClass().getName());
		// make sure we're actually connected to steam...
		activity().assertSteamConnection();
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
		if (activity() != null && activity().toolbar != null) {
			activity().toolbar.setTitle(title);
		}
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		// by default, do nothing
	}
}
