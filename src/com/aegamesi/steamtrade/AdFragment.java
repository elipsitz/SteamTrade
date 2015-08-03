package com.aegamesi.steamtrade;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.aegamesi.lib.android.AndroidUtil;
import com.aegamesi.steamtrade.fragments.FragmentSettings;
import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.AdTargetingOptions;
import com.google.android.gms.ads.AdRequest;

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AdFragment extends Fragment implements View.OnClickListener {
	private static final int AD_REFRESH_TIME = 60;
	private static final int AD_MIN_TIME = 10;
	private long last_load = 0;
	private com.amazon.device.ads.AdLayout amazonAdView;
	private com.google.android.gms.ads.AdView admobAdView;
	private ScheduledExecutorService scheduledExecutorService;
	private ScheduledFuture<?> scheduledFuture = null;
	private Runnable refreshAdRunnable = null;

	public static boolean areAdsEnabled(Context context) {
		// user purchased and is removing ads via preferences
		boolean user_removed_ads = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_remove_ads", false);
		if (user_removed_ads)
			return false;

		// don't show ads on the first day of use.
		Date installTime = AndroidUtil.getInstallTime(context.getPackageManager(), "com.aegamesi.steamtrade");
		long installTimeAgo = (new Date()).getTime() - installTime.getTime();
		if (installTimeAgo < 1000 * 60 * 60 * 24)
			return false;

		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AdRegistration.setAppKey(getString(R.string.amazon_ad_key));

		scheduledExecutorService = new ScheduledThreadPoolExecutor(5);
		refreshAdRunnable = new Runnable() {
			@Override
			public void run() {
				if ((System.currentTimeMillis() - last_load) < (AD_MIN_TIME * 1000)) {
					scheduleAdRefresh(AD_REFRESH_TIME - ((System.currentTimeMillis() - last_load) / 1000));
				} else {
					// refresh ad
					refreshMainAd();
				}
			}
		};
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.ad_fragment, container, false);

		amazonAdView = (com.amazon.device.ads.AdLayout) view.findViewById(R.id.adview_amazon);
		admobAdView = (com.google.android.gms.ads.AdView) view.findViewById(R.id.adview_admob);

		amazonAdView.setTimeout(10000);
		amazonAdView.setListener(new com.amazon.device.ads.AdListener() {
			@Override
			public void onAdLoaded(Ad ad, AdProperties adProperties) {
				Log.d("Amazon", "Amazon ad loaded.");
				if (amazonAdView != null)
					amazonAdView.setVisibility(View.VISIBLE);
				if (admobAdView != null)
					admobAdView.setVisibility(View.GONE);

				last_load = System.currentTimeMillis();
				scheduleAdRefresh(AD_REFRESH_TIME);
			}

			@Override
			public void onAdFailedToLoad(Ad ad, AdError adError) {
				Log.d("Amazon", "Amazon ad failed to load: " + adError.getMessage());
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

		admobAdView.setAdListener(new com.google.android.gms.ads.AdListener() {
			@Override
			public void onAdFailedToLoad(int errorCode) {
				super.onAdFailedToLoad(errorCode);
				last_load = System.currentTimeMillis(); // eh... fuck it, let's try again
				scheduleAdRefresh(AD_MIN_TIME);
			}

			@Override
			public void onAdLoaded() {
				super.onAdLoaded();
				last_load = System.currentTimeMillis();
				scheduleAdRefresh(AD_REFRESH_TIME);
			}
		});

		view.setOnClickListener(this);

		scheduleAdRefresh(0);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (admobAdView != null)
			admobAdView.resume();

		scheduleAdRefresh(AD_REFRESH_TIME);
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

	private void scheduleAdRefresh(final long seconds) {
		scheduledExecutorService.schedule(refreshAdRunnable, seconds, TimeUnit.SECONDS);
	}

	public void refreshMainAd() {
		if (getView() == null)
			return;

		// if ads aren't enabled, leave...
		boolean adsEnabled = areAdsEnabled(getActivity());
		if (!adsEnabled) {
			getView().setVisibility(View.GONE);
			return;
		}

		// otherwise, feel free to go ahead!
		getView().setVisibility(View.VISIBLE);
		AdTargetingOptions amazonAdOptions = new AdTargetingOptions();
		amazonAdView.loadAd(amazonAdOptions);
	}

	@Override
	public void onClick(View view) {
		if (view == getView()) {
			// remove ads
			MainActivity activity = (MainActivity) getActivity();
			if (activity.billingProcessor.listOwnedProducts().contains(FragmentSettings.IAP_REMOVEADS)) {
				// okay, remove ads -- redirect the user to the settings fragment
				activity.browseToFragment(new FragmentSettings(), true);
			} else {
				// otherwise start the purchase
				Toast.makeText(getActivity(), R.string.purchase_pending, Toast.LENGTH_LONG).show();
				activity.billingProcessor.purchase(activity, FragmentSettings.IAP_REMOVEADS);
			}
		}
	}
}
