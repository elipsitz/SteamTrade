package com.aegamesi.steamtrade.steam;

import android.util.Log;

import uk.co.thomasc.steamkit.util.logging.IDebugListener;

public class SteamLogcatDebugListener implements IDebugListener {
	@Override
	public void writeLine(String tag, String msg) {
		String[] ignore_tags = new String[]{"EMsg GET", "Connect"};
		for (String ignore_tag : ignore_tags)
			if (ignore_tag.equals(tag))
				return;

		String[] lines = msg.split(System.getProperty("line.separator"));
		for (String line : lines)
			Log.d("SteamKit", "[" + tag + "] " + line);

		/* debug mode */
		if (tag.equals("NEW_EX")) {
			for (StackTraceElement e : Thread.currentThread().getStackTrace())
				Log.d("StackTrace", e.toString());
		}
	}
}
