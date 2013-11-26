package com.aegamesi.steamtrade.fragments;

import com.actionbarsherlock.app.SherlockFragment;
import com.aegamesi.steamtrade.MainActivity;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;

public class FragmentBase extends SherlockFragment {
	public String fragmentName = "FragmentBase";
	
	@Override
	public void onStart() {
		super.onStart();
		
		setAnalyticsScreen(fragmentName);
	}

	public void setAnalyticsScreen(String name) {
		activity().tracker().set(Fields.SCREEN_NAME, name);
		activity().tracker().send(MapBuilder.createAppView().build());
	}

	public void sendAnalyticsEvent(String category, String action, String label, Long value) {
		activity().tracker().send(MapBuilder.createEvent(category, action, label, value).build());

	}

	public final MainActivity activity() {
		return (MainActivity) getActivity();
	}
}
