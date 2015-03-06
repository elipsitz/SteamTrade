package com.aegamesi.steamtrade.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;

public class FragmentEula extends DialogFragment {
	private String EULA_PREFIX = "eula___ae-ice__";

	private PackageInfo getPackageInfo(Context context) {
		PackageInfo pi = null;
		try {
			pi = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return pi;
	}

	public boolean shouldCreateDialog(Context context) {
		PackageInfo versionInfo = getPackageInfo(context);
		// the eulaKey changes every time you increment the version number in
		// the AndroidManifest.xml
		final String eulaKey = EULA_PREFIX + versionInfo.versionCode;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return !prefs.getBoolean(eulaKey, false);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		PackageInfo versionInfo = getPackageInfo(getActivity());
		final String eulaKey = EULA_PREFIX + versionInfo.versionCode;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		// Show the Eula
		String title = getActivity().getString(R.string.app_name) + " v" + versionInfo.versionName;
		// Includes the updates as well so users know what changed.
		String message = getActivity().getString(R.string.eula);

		AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(title).setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				// Mark this version as read.
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean(eulaKey, true);
				editor.commit();
				dialogInterface.dismiss();
			}
		}).setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Close the activity as they have declined the EULA
				getActivity().finish();
			}
		}).create();

		TextView msg = new TextView(getActivity());
		msg.setMovementMethod(new ScrollingMovementMethod());
		float scale = getResources().getDisplayMetrics().density;
		msg.setPadding((int) (10 * scale), (int) (10 * scale), (int) (10 * scale), (int) (10 * scale));
		msg.setText(Html.fromHtml(message));
		dialog.setView(msg);

		setCancelable(false);
		return dialog;
	}
}