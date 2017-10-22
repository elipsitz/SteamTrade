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
import com.appodeal.ads.Appodeal;
import com.appodeal.ads.BannerCallbacks;

import java.util.Date;

public class AdFragment extends Fragment implements View.OnClickListener {

	public static boolean areAdsEnabled(Context context) {
		// user purchased and is removing ads via preferences
		boolean user_removed_ads = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_remove_ads", false);
		if (user_removed_ads)
			return false;

		// don't show ads on the 12 hours of use.
		Date installTime = AndroidUtil.getInstallTime(context.getPackageManager(), "com.aegamesi.steamtrade");
		long installTimeAgo = (new Date()).getTime() - installTime.getTime();
		return installTimeAgo >= 1000 * 60 * 60 * 12;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.ad_fragment, container, false);

		Appodeal.setBannerCallbacks(new BannerCallbacks() {
			private Toast mToast;

			@Override
			public void onBannerLoaded(int height, boolean isPrecache) {
				showToast(String.format("onBannerLoaded, %ddp", height));
			}

			@Override
			public void onBannerFailedToLoad() {
				showToast("onBannerFailedToLoad");
			}

			@Override
			public void onBannerShown() {
				showToast("onBannerShown");
			}

			@Override
			public void onBannerClicked() {
				showToast("onBannerClicked");
			}

			void showToast(final String text) {
				Log.i("Appodeal--AdFragment", text);
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (AdFragment.areAdsEnabled(getActivity())) {
			Appodeal.show(getActivity(), Appodeal.BANNER_VIEW);
			if (getView() != null) {
				getView().setVisibility(View.VISIBLE);
			}
		} else {
			if (getView() != null) {
				getView().setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
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
