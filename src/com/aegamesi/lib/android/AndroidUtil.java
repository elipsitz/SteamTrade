package com.aegamesi.lib.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.util.Base64;

import com.aegamesi.steamtrade.R;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class AndroidUtil {
	public static final int STORE_UNKNOWN = 0;
	public static final int STORE_GOOGLEPLAY = 1;
	public static final int STORE_AMAZON = 2;

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

	public static int getAppStore(Context context) {
		PackageManager pkgManager = context.getPackageManager();
		String installerPackageName = pkgManager.getInstallerPackageName(context.getPackageName());

		if (installerPackageName.startsWith("com.amazon")) {
			return STORE_AMAZON;
		} else if ("com.android.vending".equals(installerPackageName)) {
			return STORE_GOOGLEPLAY;
		}

		return 0;
	}

	public static String createURIDataString(Map<String, Object> data) {

		StringBuilder dataStringBuffer = new StringBuilder();
		if (data != null) {
			try {
				for (Map.Entry<String, Object> entry : data.entrySet()) {
					dataStringBuffer.append(
							URLEncoder.encode(entry.getKey(), "UTF-8"))
							.append("=").append(
							URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"))
							.append("&");
				}
			} catch (UnsupportedEncodingException e) {
				return "";
			}
		}
		return dataStringBuffer.toString();
	}

	public static void createCachedFile(Context context, String fileName, String content) throws IOException {
		File cacheFile = new File(context.getCacheDir() + File.separator + fileName);
		cacheFile.getParentFile().mkdirs();
		cacheFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(cacheFile);
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
		PrintWriter pw = new PrintWriter(osw);
		pw.println(content);
		pw.flush();
		pw.close();
	}

	public static void showBasicAlert(Context context, String title, String message, DialogInterface.OnClickListener callback) {
		AlertDialog.Builder alert = new AlertDialog.Builder(context);
		alert.setTitle(title);
		alert.setMessage(message);
		alert.setNeutralButton(R.string.ok, callback);
		alert.show();
	}


	// From https://gist.github.com/orip/3635246
	public static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
		public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return Base64.decode(json.getAsString(), Base64.NO_WRAP);
		}

		public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(Base64.encodeToString(src, Base64.NO_WRAP));
		}
	}

}
