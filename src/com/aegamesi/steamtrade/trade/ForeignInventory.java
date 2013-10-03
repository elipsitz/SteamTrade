package com.aegamesi.steamtrade.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.aegamesi.steamtrade.steam.Schema.SchemaQuality;
import com.aegamesi.steamtrade.steam.SteamInventory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ForeignInventory {
	private ArrayList<ForeignItem> rgInventory = new ArrayList<ForeignItem>();
	private HashMap<String, ForeignDescription> rgDescriptions = new HashMap<String, ForeignDescription>();

	public static ForeignInventory fromJSON(String json) {
		JsonParser parser = new JsonParser();
		JsonObject root = parser.parse(json).getAsJsonObject();
		if (!root.get("success").getAsBoolean())
			return null;
		ForeignInventory inv = new ForeignInventory();

		JsonObject inventory = root.get("rgInventory").getAsJsonObject();
		Iterator<Entry<String, JsonElement>> i = inventory.entrySet().iterator();
		while (i.hasNext()) {
			// TODO figure out why there's a NullPointerException round here, fix up ForeignInventory
			ForeignItem item = new ForeignItem();
			JsonObject obj = i.next().getValue().getAsJsonObject();
			item.id = Long.parseLong(obj.get("id").getAsString());
			item.description_id = obj.get("classid").getAsString() + "_" + obj.get("descriptionid").getAsString();
			inv.rgInventory.add(item);
		}

		JsonObject descriptions = root.get("rgDescriptions").getAsJsonObject();
		i = descriptions.entrySet().iterator();
		while (i.hasNext()) {
			Entry<String, JsonElement> entry = i.next();
			JsonObject obj = entry.getValue().getAsJsonObject();
			JsonObject app_data = obj.get("app_data").getAsJsonObject();

			ForeignDescription desc = new ForeignDescription();
			desc.defindex = Integer.parseInt(app_data.get("def_index").getAsString());
			desc.quanity = app_data.has("quantity") ? Integer.parseInt(app_data.get("quantity").getAsString()) : 1;
			desc.quality = SchemaQuality.values[Integer.parseInt(app_data.get("quality").getAsString())];
			desc.custom_name = obj.get("name").getAsString().equals(obj.get("market_name").getAsString()) ? null : obj.get("name").getAsString();
			desc.custom_desc = null;
			desc.level = 1; // XXX todo get the level
			desc.craftable = true;
			// begin test if craftable
			JsonArray desc_strings = obj.get("descriptions").getAsJsonArray();
			if (desc_strings != null && desc_strings.size() > 0)
				desc.craftable = !desc_strings.get(desc_strings.size() - 1).getAsJsonObject().get("value").getAsString().equals("( Not Usable in Crafting )");
			// end test if craftable
			inv.rgDescriptions.put(entry.getKey(), desc);
		}

		return inv;
	}

	public SteamInventory asInventory() {
		SteamInventory inv = new SteamInventory();
		for (ForeignItem item : rgInventory) {
			SteamInventory.SteamInventoryItem steamItem = new SteamInventory.SteamInventoryItem();
			ForeignDescription desc = rgDescriptions.get(item.description_id);
			steamItem.id = item.id;
			steamItem.defindex = desc.defindex;
			steamItem.quantity = desc.quanity;
			steamItem.quality = desc.quality;
			steamItem.custom_name = desc.custom_name;
			steamItem.custom_desc = desc.custom_desc;
			steamItem.level = desc.level;
			steamItem.flag_cannot_craft = !desc.craftable;
			steamItem.contained_item = null;
		}
		return inv;
	}

	public static class ForeignDescription {
		public int defindex;
		public int level;
		public int quanity;
		public boolean craftable;
		public SchemaQuality quality;
		public String custom_name = null;
		public String custom_desc = null;
	}

	public static class ForeignItem {
		public long id;
		public String description_id;
	}
}
