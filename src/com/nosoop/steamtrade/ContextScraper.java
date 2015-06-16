package com.nosoop.steamtrade;

import com.nosoop.steamtrade.inventory.AppContextPair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Private class that brutally scrapes the AppContextData JavaScript object from
 * the trade page. Without this, we would not know what inventories we have.
 *
 * @author nosoop
 */
public class ContextScraper {
	// TODO Uncouple this from the TradeSession class?
	static final List<AppContextPair> DEFAULT_APPCONTEXTDATA =
			new ArrayList<>();

	/**
	 * Initialize default AppContextPairs.
	 */
	static {
		DEFAULT_APPCONTEXTDATA.add(
				new AppContextPair(440, 2, "Team Fortress 2"));
	}

	/**
	 * Scrapes the page for the g_rgAppContextData variable and passes it to a
	 * private method for parsing, returning the list of named AppContextPair
	 * objects it generates. It's a bit of a hack...
	 *
	 * @param pageResult The page data fetched by the TradeSession object.
	 * @return A list of named AppContextPair objects representing the known
	 * inventories, or an empty list if not found.
	 */
	public static List<AppContextPair> scrapeContextData(String pageResult)
			throws JSONException {
		try {
			BufferedReader read;
			read = new BufferedReader(new StringReader(pageResult));

			String buffer;
			while ((buffer = read.readLine()) != null) {
				String input;
				input = buffer.trim();

				if (input.startsWith("var g_rgAppContextData")) {
					// Extract the JSON string from the JavaScript source.  Bleh
					return parseContextData(input.substring(input.indexOf('{'),
							input.length() - 1));
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// If we can't find it, return an empty one, I guess...
		return DEFAULT_APPCONTEXTDATA;
	}

	/**
	 * Parses the context data JSON feed and makes a bunch of AppContextPair
	 * instances.
	 *
	 * @param json The JSON String representing g_rgAppContextData.
	 * @return A list of named AppContextPair objects representing the available
	 * inventories.
	 */
	public static List<AppContextPair> parseContextData(String json)
			throws JSONException {
		List<AppContextPair> result = new ArrayList<>();

		JSONObject feedData = new JSONObject(json);
		Iterator<String> i = feedData.keys();
		while (i.hasNext()) {
			String on = i.next();
			JSONObject o = feedData.getJSONObject(on);
			if (o != null) {
				String gameName = o.getString("name");
				int appid = o.getInt("appid");

				JSONObject contextData = o.getJSONObject("rgContexts");
				Iterator<String> j = contextData.keys();
				while (j.hasNext()) {
					String bn = j.next();
					JSONObject b = contextData.getJSONObject(bn);
					String contextName = b.getString("name");
					long contextid = Long.parseLong(b.getString("id"));
					int assetCount = b.getInt("asset_count");

					// "Team Fortress 2 - Backpack (226)"
					String invNameFormat = String.format("%s - %s (%d)",
							gameName, contextName, assetCount);

					// Only include the inventory if it's not empty.
					if (assetCount > 0) {
						result.add(new AppContextPair(
								appid, contextid, invNameFormat));
					}
				}
			}
		}
		return result;
	}
}
