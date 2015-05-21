package com.aegamesi.lib;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.lang.reflect.Field;
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

	public static int intCompare(int x, int y) {
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
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
		} catch (IllegalAccessException e) {
		} catch (NoSuchFieldException e) {
		} catch (IllegalArgumentException e) {
		} catch (SecurityException e) {
		}
		// field wasn't found
		return null;
	}

	private static Date firstNonNull(Date... dates) {
		for (Date date : dates)
			if (date != null)
				return date;
		return null;
	}
}
