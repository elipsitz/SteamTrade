package com.aegamesi.steamtrade;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aegamesi.lib.AndroidUtil;
import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdTargetingOptions;
import com.google.android.gms.ads.AdRequest;

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AdFragment extends Fragment {
	private static final int AD_REFRESH_TIME = 60;
	private com.amazon.device.ads.AdLayout amazonAdView;
	private com.google.android.gms.ads.AdView admobAdView;
	private ScheduledExecutorService scheduledExecutorService;
	private ScheduledFuture<?> scheduledFuture = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		scheduledExecutorService = new ScheduledThreadPoolExecutor(5);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.ad_fragment, container, false);

		amazonAdView = (com.amazon.device.ads.AdLayout) view.findViewById(R.id.adview_amazon);
		admobAdView = (com.google.android.gms.ads.AdView) view.findViewById(R.id.adview_admob);

		amazonAdView.setTimeout(15000);
		amazonAdView.setListener(new AdListener() {
			@Override
			public void onAdLoaded(Ad ad, AdProperties adProperties) {
				if (amazonAdView != null)
					amazonAdView.setVisibility(View.VISIBLE);
				if (admobAdView != null)
					admobAdView.setVisibility(View.GONE);

			}

			@Override
			public void onAdFailedToLoad(Ad ad, AdError adError) {
				// Call AdMob SDK for backfill
				if (amazonAdView != null)
					amazonAdView.setVisibility(View.GONE);
				if (admobAdView != null)
					admobAdView.setVisibility(View.VISIBLE);

				AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
				admobAdView.loadAd(adRequest);
			}

			@Override
			public void onAdExpanded(Ad ad) {

			}

			@Override
			public void onAdCollapsed(Ad ad) {

			}

			@Override
			public void onAdDismissed(Ad ad) {

			}
		});

		return view;
	}

	public void refreshMainAd() {
		if (getView() == null)
			return;

		// if ads aren't enabled, leave...
		boolean adsEnabled = areAdsEnabled();
		if (!adsEnabled) {
			getView().setVisibility(View.GONE);
			return;
		}

		// otherwise, feel free to go ahead!
		getView().setVisibility(View.VISIBLE);
		AdTargetingOptions amazonAdOptions = new AdTargetingOptions();
		amazonAdView.loadAd(amazonAdOptions);
	}


	private boolean areAdsEnabled() {
		// user purchased and is removing ads via preferences
		boolean user_removed_ads = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_remove_ads", false);
		if (user_removed_ads)
			return false;

		// don't show ads on the first day of use.
		Date installTime = AndroidUtil.getInstallTime(getActivity().getPackageManager(), "com.aegamesi.steamtrade");
		long installTimeAgo = (new Date()).getTime() - installTime.getTime();
		if (installTimeAgo < 1)//1000 * 60 * 60 * 24)
			return false;

		return true;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (admobAdView != null)
			admobAdView.resume();

		scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			public void run() {
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						refreshMainAd();
					}
				});
			}
		}, 5, AD_REFRESH_TIME, TimeUnit.SECONDS);
	}

	@Override
	public void onPause() {
		super.onPause();

		if (admobAdView != null)
			admobAdView.pause();

		if (scheduledFuture != null)
			scheduledFuture.cancel(true);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (admobAdView != null)
			admobAdView.destroy();
		if (amazonAdView != null)
			amazonAdView.destroy();
	}
}
