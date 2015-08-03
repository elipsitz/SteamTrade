package com.aegamesi.steamtrade;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aegamesi.steamtrade.steam.SteamLogcatDebugListener;
import com.aegamesi.steamtrade.steam.SteamWeb;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import uk.co.thomasc.steamkit.steam3.CMClient;
import uk.co.thomasc.steamkit.util.logging.DebugLog;

@ReportsCrashes(
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
	public static JSONObject webConfig = new JSONObject();

	@Override
	public void onCreate() {
		FixNoClassDefFoundError81083(); // code workaround for GPG bug #81083 -- remove later

		super.onCreate();
		ACRA.init(this);
		getTracker().enableAdvertisingIdCollection(true);
		filesDir = getFilesDir();

		DebugLog.addListener(new SteamLogcatDebugListener());

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.contains("cm_server_list")) {
			String serverString = prefs.getString("cm_server_list", "");
			String[] servers = serverString.split(",");
			if (servers.length > 0)
				CMClient.updateCMServers(servers);
		}

		// setup universal image loader
		DisplayImageOptions defaultImageOptions = new DisplayImageOptions.Builder()
				.cacheInMemory(true)
				.cacheOnDisk(true)
				.build();
		ImageLoaderConfiguration imageLoaderConfiguration = new ImageLoaderConfiguration.Builder(this)
				.defaultDisplayImageOptions(defaultImageOptions)
				.build();
		ImageLoader.getInstance().init(imageLoaderConfiguration);


		// set up web config
		// handleWebConfig();
	}

	private void FixNoClassDefFoundError81083() {
		try {
			Class.forName("android.os.AsyncTask");
		} catch (Throwable ignore) {
		}
	}

	public Tracker getTracker() {
		GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
		return analytics.newTracker(R.xml.tracker);
	}

	private void handleWebConfig() {
		final String url = "http://aegamesi.com/sites/ice/config.json";

		new Thread(new Runnable() {
			@Override
			public void run() {
				String response = SteamWeb.request(url, "GET", null, "", "");
				Log.i("SteamTrade", "Fetched webconfig");
				Log.i("SteamTrade", response);

				try {
					webConfig = new JSONObject(response);
				} catch (JSONException e) {
					e.printStackTrace();
					webConfig = new JSONObject();
				}
			}
		}).start();
	}
}