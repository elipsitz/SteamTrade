package com.aegamesi.steamtrade.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.util.Log;
import android.widget.Toast;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamItemUtil;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.github.machinarius.preferencefragment.PreferenceFragment;

public class FragmentSettings extends PreferenceFragment {
	public static final String IAP_REMOVEADS = "ice.removeads";

	private RingtonePreference pref_notification_sound = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		// more actions for removing ads
		findPreference("pref_remove_ads").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				MainActivity activity = ((MainActivity) getActivity());

				if ((boolean) newValue) {
					boolean override = SteamService.singleton != null && SteamService.singleton.steamClient != null && SteamService.singleton.steamClient.getSteamId().convertToLong() == 76561198000739785L;
					if (activity.billingProcessor.listOwnedProducts().contains(IAP_REMOVEADS) || override) {
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

		// reset pricing cache when you change preferred currency
		findPreference("pref_currency").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				SteamItemUtil.marketCache.clear();
				return true;
			}
		});

		// allow webapi key to be per-user
		final EditTextPreference pref_webapikey = (EditTextPreference) findPreference("pref_webapikey");
		pref_webapikey.setText(SteamUtil.webApiKey);
		pref_webapikey.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String key = newValue.toString().trim();
				if (key.length() > 32)
					key = key.substring(0, 32);

				SteamUtil.webApiKey = key;
				pref_webapikey.setText(SteamUtil.webApiKey);
				Log.d("Preferences", "Accepted new webapi key: " + SteamUtil.webApiKey);

				if (getActivity() != null && SteamService.singleton != null && SteamService.singleton.steamClient != null) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
					prefs.edit().putString("webapikey_" + SteamService.singleton.steamClient.getSteamId().convertToLong(), SteamUtil.webApiKey).apply();
				}
				return false;
			}
		});

		pref_notification_sound = (RingtonePreference) findPreference("pref_notification_sound");
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getActivity() != null && getActivity() instanceof MainActivity) {
			((MainActivity) getActivity()).toolbar.setTitle(getString(R.string.nav_settings));
		}
	}


	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		// workaround for Ringtone Fragment not saving data
		if (pref_notification_sound != null && pref_notification_sound.onActivityResult(requestCode, resultCode, data))
			return true;

		return false;
	}
}
