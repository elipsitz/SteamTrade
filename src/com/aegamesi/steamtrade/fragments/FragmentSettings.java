package com.aegamesi.steamtrade.fragments;

import android.os.Bundle;

import com.aegamesi.steamtrade.R;
import com.github.machinarius.preferencefragment.PreferenceFragment;

public class FragmentSettings extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
	}
}
