package com.aegamesi.steamtrade.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.widget.Toast;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.R;
import com.github.machinarius.preferencefragment.PreferenceFragment;

public class FragmentSettings extends PreferenceFragment {
	public static final String IAP_REMOVEADS = "ice.removeads";

	private RingtonePreference pref_notification_sound = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		Preference pref_remove_ads = findPreference("pref_remove_ads");
		pref_remove_ads.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				MainActivity activity = ((MainActivity) getActivity());

				if ((boolean) newValue) {
					if (activity.billingProcessor.listOwnedProducts().contains(IAP_REMOVEADS)) {
						// okay, remove ads
						return true;
					} else {
						Toast.makeText(getActivity(), R.string.purchase_pending, Toast.LENGTH_LONG).show();
						activity.billingProcessor.purchase(activity, IAP_REMOVEADS);
						return false;
					}
				}
				return true;
			}
		});

		pref_notification_sound = (RingtonePreference)findPreference("pref_notification_sound");
	}


	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		// workaround for Ringtone Fragment not saving data
		if(pref_notification_sound != null && pref_notification_sound.onActivityResult(requestCode, resultCode, data))
			return true;

		return false;
	}
}
