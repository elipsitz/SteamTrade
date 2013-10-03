package com.aegamesi.steamtrade.steam;

import uk.co.thomasc.steamkit.util.logging.IDebugListener;
import android.util.Log;

public class SteamLogcatDebugListener implements IDebugListener {
	@Override
	public void writeLine(String tag, String msg) {
		String[] ignore_tags = new String[] { "EMsg GET", "Connect" };
		for (String ignore_tag : ignore_tags)
			if (ignore_tag.equals(tag))
				return;

		String[] lines = msg.split(System.getProperty("line.separator"));
		for (String line : lines)
			Log.d("SteamKit", "[" + tag + "] " + line);
	}
}
