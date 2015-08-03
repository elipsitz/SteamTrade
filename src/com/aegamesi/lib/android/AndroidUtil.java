package com.aegamesi.lib.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.text.format.DateFormat;

import com.aegamesi.steamtrade.R;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

public class AndroidUtil {
	// return install time from package manager, or apk file modification time,
	// or null if not found
	public static Date getInstallTime(
			PackageManager packageManager, String packageName) {
		return firstNonNull(
				installTimeFromPackageManager(packageManager, packageName),
				apkUpdateTime(packageManager, packageName));
	}


	public static int numCompare(double x, double y) {
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}

	public static int numCompare(long x, long y) {
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@SuppressWarnings("deprecation")
	public static void copyToClipboard(Context context, String str) {
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData.newPlainText("Chat Line", str);
			clipboard.setPrimaryClip(clip);
		} else {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(str);
		}
	}

	private static Date apkUpdateTime(
			PackageManager packageManager, String packageName) {
		try {
			ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
			File apkFile = new File(info.sourceDir);
			return apkFile.exists() ? new Date(apkFile.lastModified()) : null;
		} catch (PackageManager.NameNotFoundException e) {
			return null; // package not found
		}
	}

	private static Date installTimeFromPackageManager(
			PackageManager packageManager, String packageName) {
		// API level 9 and above have the "firstInstallTime" field.
		// Check for it with reflection and return if present.
		try {
			PackageInfo info = packageManager.getPackageInfo(packageName, 0);
			Field field = PackageInfo.class.getField("firstInstallTime");
			long timestamp = field.getLong(info);
			return new Date(timestamp);
		} catch (PackageManager.NameNotFoundException e) {
			return null; // package not found
		} catch (Exception e) {
			e.printStackTrace();
		}
		// field wasn't found
		return null;
	}

	private static <T> T firstNonNull(T... objs) {
		for (T obj : objs)
			if (obj != null)
				return obj;
		return null;
	}

	public static CharSequence getChatStyleTimeAgo(Context context, long time_then, long time_now) {
		Calendar cal_then = Calendar.getInstance();
		Calendar cal_now = Calendar.getInstance();
		cal_then.setTimeInMillis(time_then);
		cal_now.setTimeInMillis(time_now);

		// todo update this to show exact time, too.
		int time_diff = (int) ((time_now - time_then) / 1000);
		Resources resources = context.getResources();

		// in the future
		if (time_diff < 0) {
			return resources.getString(R.string.time_in_future);
		}

		// less than 1 minute -> "Now"
		if (time_diff < 60) {
			return resources.getString(R.string.time_now);
		}

		// between 1 minute and 60 minutes -> "x mins"
		if (time_diff < 60 * 60) {
			int mins = time_diff / 60;
			return resources.getQuantityString(R.plurals.time_mins, mins, mins);
		}


		if (cal_then.get(Calendar.YEAR) == cal_now.get(Calendar.YEAR)) {
			// more than 60 minutes, but on the same day -> "X:XX P/AM"
			if (cal_then.get(Calendar.DAY_OF_YEAR) == cal_now.get(Calendar.DAY_OF_YEAR)) {
				return DateFormat.format("h:mm a", time_then);
			}

			// not the same day, but the same week --> "DAY X:XX P/AM"
			if (cal_then.get(Calendar.WEEK_OF_YEAR) == cal_now.get(Calendar.WEEK_OF_YEAR)) {
				return DateFormat.format("EEE h:mm a", time_then);
			}

			// same year --> "Month Day, time"
			return DateFormat.format("MMM d, h:mm a", time_then);
		}

		// default to "month day year, time"
		return DateFormat.format("MMM d yyyy, h:mm a", time_then);
	}

	public static final int STORE_UNKNOWN = 0;
	public static final int STORE_GOOGLEPLAY = 1;
	public static final int STORE_AMAZON = 2;
	public int getAppStore(Context context) {
		PackageManager pkgManager = context.getPackageManager();
		String installerPackageName = pkgManager.getInstallerPackageName(context.getPackageName());

		if(installerPackageName.startsWith("com.amazon")) {
			return STORE_AMAZON;
		} else if ("com.android.vending".equals(installerPackageName)) {
			return STORE_GOOGLEPLAY;
		}

		return 0;
	}
}
