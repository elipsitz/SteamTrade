package com.aegamesi.steamtrade.steam;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;

public class SteamUtil {
	public static String apikey = ""; // kept in secret.xml
	public final static int colorGame = Color.parseColor("#AED04E");
	public final static int colorOnline = Color.parseColor("#9CC6FF");
	public final static int colorOffline = Color.parseColor("#CFD2D3");
	public final static long recentChatThreshold = 7 * 24 * 60 * 60 * 1000; // 7 days

	public static byte[] calculateSHA1(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			return md.digest(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	final protected static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static String bytesToHex(byte[] bytes) {
		if (bytes == null)
			return "0000000000000000000000000000000000000000";
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static void disconnectWithDialog(final Context context, final String message) {
		class SteamDisconnectTask extends AsyncTask<Void, Void, Void> {
			private ProgressDialog dialog;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				dialog = new ProgressDialog(context);
				dialog.setCancelable(false);
				dialog.setMessage(message);
				dialog.show();
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				try {
					dialog.dismiss();
				} catch (IllegalArgumentException e) {
				}
			}

			@Override
			protected Void doInBackground(Void... params) {
				SteamService.singleton.steamClient.disconnect();
				return null;
			}
		}
		new SteamDisconnectTask().execute();
	}

	public static int getQuantizedPersonaState(EPersonaState state, String game) {
		if (game != null && game.length() != 0)
			return 1;
		if (state == EPersonaState.Offline || state == null)
			return -1;
		return 0;
	}
}
