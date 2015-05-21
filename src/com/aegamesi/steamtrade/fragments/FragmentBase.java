package com.aegamesi.steamtrade.fragments;

import android.support.v4.app.Fragment;

import com.aegamesi.steamtrade.MainActivity;
import com.google.android.gms.analytics.HitBuilders;

public class FragmentBase extends Fragment {

	@Override
	public void onStart() {
		super.onStart();

		setAnalyticsScreen(getClass().getName());
	}

	public void setAnalyticsScreen(String name) {
		activity().tracker().setScreenName(name);
		activity().tracker().send(new HitBuilders.AppViewBuilder().build());
	}

	public void sendAnalyticsEvent(String category, String action, String label, Long value) {
		activity().tracker().send(new HitBuilders.EventBuilder()
				.setCategory(category)
				.setAction(action)
				.setLabel(label)
				.setValue(value)
				.build());

	}

	public final MainActivity activity() {
		return (MainActivity) getActivity();
	}
}
