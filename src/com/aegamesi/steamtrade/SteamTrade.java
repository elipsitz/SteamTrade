package com.aegamesi.steamtrade;

import java.io.File;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import com.aegamesi.steamtrade.steam.SteamUtil;

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
		filesDir = getFilesDir();

		SteamUtil.apikey = this.getString(R.string.steam_apikey);
	}
}