package com.aegamesi.steamtrade;

import android.app.Application;

import com.aegamesi.steamtrade.steam.SteamUtil;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.File;
import java.util.Random;

@ReportsCrashes(
		formKey = "",
		formUri = "https://pickleman.cloudant.com/acra-steamtrade/_design/acra-storage/_update/report",
		reportType = org.acra.sender.HttpSender.Type.JSON,
		httpMethod = org.acra.sender.HttpSender.Method.PUT,
		formUriBasicAuthLogin = "erandstuntryouldertencer",
		formUriBasicAuthPassword = "Ht7cJlLh28flt5SnBlX0Dr0u",
		mode = ReportingInteractionMode.TOAST,
		forceCloseDialogAfterToast = false,
		resToastText = R.string.app_crashed_toast)
public class SteamTrade extends Application {
	public static File filesDir;

	@Override
	public void onCreate() {
		super.onCreate();
		ACRA.init(this);
		getTracker().enableAdvertisingIdCollection(true);
		filesDir = getFilesDir();

		Random r = new Random();
		r.setSeed(System.currentTimeMillis());
		String[] keys = getResources().getStringArray(R.array.steam_apikey);
		SteamUtil.apikey = keys[r.nextInt(keys.length)];
	}

	private Tracker tracker;

	public Tracker getTracker() {
		GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
		return analytics.newTracker(R.xml.tracker);
	}
}