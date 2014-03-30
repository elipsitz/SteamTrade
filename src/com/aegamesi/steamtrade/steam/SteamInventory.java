package com.aegamesi.steamtrade.steam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import uk.co.thomasc.steamkit.types.steamid.SteamID;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.Schema.SchemaPaint;
import com.aegamesi.steamtrade.steam.Schema.SchemaParticle;
import com.aegamesi.steamtrade.steam.Schema.SchemaQuality;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.loopj.android.image.SmartImageView;

public class SteamInventory {
	public int status;
	public int num_backpack_slots;
	public List<SteamInventoryItem> items;

	public static class SteamInventoryAttribute {
		public int defindex;
		public long long_value;
		public float float_value;
		public String string_value;
		public SteamInventoryAccountInfo account_info = null;
	}

	public static class SteamInventoryAccountInfo {
		public long steamid;
		public String personaname;
	}

	public static class SteamInventoryItem {
		public long id;
		public long original_id;
		public int defindex;
		public int level;
		public int quantity;
		public boolean flag_cannot_trade = false;
		public boolean flag_cannot_craft = false;
		public SchemaQuality quality;
		public String custom_name = null;
		public String custom_desc = null;
		public SteamInventoryItem contained_item = null;
		public SteamInventoryAttribute[] attributes = null;

		public Schema.SchemaItem def() {
			if (SteamService.singleton == null || SteamService.singleton.schema == null)
				return null;
			return SteamService.singleton.schema.items.get(defindex);
		}

		public String fullname() {
			SteamInventoryAttribute seriesAttribute = getAttribute(187);
			String series_str = seriesAttribute == null ? "" : " Series #" + ((int) seriesAttribute.float_value);//187
			String proper_str = (def().proper_name ? "The " : "");
			String quality_str = (quality.prefix ? quality.name + " " : "");
			String name_str = (custom_name != null ? "\"" + custom_name + "\"" : def().item_name);
			return proper_str + quality_str + name_str + series_str;
		}

		public SteamInventoryAttribute getAttribute(int defindex) {
			if (attributes == null)
				return null;
			for (SteamInventoryAttribute attribute : attributes)
				if (attribute.defindex == defindex)
					return attribute;
			return null;
		}

		public void populateListView(View v) {
			TextView itemName = (TextView) v.findViewById(R.id.inventory_item_name);
			itemName.setText(fullname());
			itemName.setTextColor(quality.color);
			v.findViewById(R.id.inventory_item_gifted).setVisibility(getAttribute(186) == null ? View.GONE : View.VISIBLE); // gifted 186
			v.findViewById(R.id.inventory_item_notcraftable).setVisibility(!flag_cannot_craft ? View.GONE : View.VISIBLE);
			v.findViewById(R.id.inventory_item_nottradable).setVisibility(!flag_cannot_trade ? View.GONE : View.VISIBLE);

			// craft number
			TextView itemNum = (TextView) v.findViewById(R.id.inventory_item_num);
			if (getAttribute(229) == null) {
				itemNum.setVisibility(View.GONE);
			} else {
				itemNum.setVisibility(View.VISIBLE);
				itemNum.setText("#" + getAttribute(229).long_value);
			}

			// EUGH PAINT
			SchemaPaint paint = getAttribute(261) == null ? null : SchemaPaint.getByColor((int) getAttribute(261).float_value);// painted 261 floatvalue
			if (paint != null) {
				ImageView view_paint1 = (ImageView) v.findViewById(R.id.inventory_item_paint1);
				view_paint1.setVisibility(paint.color1 != -1 ? View.VISIBLE : View.GONE);
				((GradientDrawable) view_paint1.getDrawable()).setColor(0xFF000000 + paint.color1);
				ImageView view_paint2 = (ImageView) v.findViewById(R.id.inventory_item_paint2);
				view_paint2.setVisibility(paint.color2 != -1 ? View.VISIBLE : View.GONE);
				((GradientDrawable) view_paint2.getDrawable()).setColor(0xFF000000 + paint.color2);
			} else {
				v.findViewById(R.id.inventory_item_paint1).setVisibility(View.GONE);
				v.findViewById(R.id.inventory_item_paint2).setVisibility(View.GONE);
			}
			// end paint
		}

		public void populateDetailView(View v) {
			// TODO move these to strings.xml
			// #FF4040 negative, #99CCFF positive
			String description = "<b>Level " + level + " " + def().item_type_name + "</b><br>";
			if (getAttribute(187) != null)
				description += "<br><font color=\"#99CCFF\">Crate Series #" + ((int) getAttribute(187).float_value + "</font>");
			if (custom_desc != null)
				description += "<br>&quot;" + custom_desc + "&quot;";
			else if (def().item_description != null)
				description += "<br>" + def().item_description;
			description += "<br>";

			if (getAttribute(186) != null)
				description += "<br><font color=\"#99CCFF\">Gift from: " + getAttribute(186).account_info.personaname + "</font>";
			if (getAttribute(228) != null)
				description += "<br><font color=\"#99CCFF\">Crafted by " + getAttribute(228).account_info.personaname + "</font>";
			if (getAttribute(229) != null)
				description += "<br><font color=\"#99CCFF\">Craft #" + getAttribute(229).long_value + "</font>";
			description += "<br>";

			SchemaParticle particle = getAttribute(134) == null ? null : SteamService.singleton.schema.particles.get((int) getAttribute(134).float_value);
			SchemaPaint paint = getAttribute(261) == null ? null : SchemaPaint.getByColor((int) getAttribute(261).float_value);
			if (particle != null)
				description += "<br><font color=\"#8650AC\">Effect: " + particle.name + "</font>";
			if (paint != null)
				description += "<br>Color: " + paint.name;
			if (flag_cannot_craft)
				description += "<br><i>( Not Usable in Crafting )</i>";
			if (flag_cannot_trade)
				description += "<br><i>( Not Tradable )</i>";

			SmartImageView itemImage = (SmartImageView) v.findViewById(R.id.item_image);
			itemImage.setImageUrl("http://media.steampowered.com/apps/440/icons/" + def().image_url);
			TextView itemName = (TextView) v.findViewById(R.id.item_name);
			itemName.setText(fullname());
			itemName.setTextColor(quality.color);
			TextView itemDescription = (TextView) v.findViewById(R.id.item_description);
			itemDescription.setText(Html.fromHtml(description));
		}
	}

	public SteamInventoryItem getItem(long id) {
		if (items == null)
			return null;
		for (final SteamInventoryItem item : items)
			if (item != null && item.id == id)
				return item;
		return null;
	}

	// *in* is most certainly closed
	@SuppressWarnings("resource")
	public static SteamInventory fetchInventory(SteamID id, String apikey, boolean cache, Context context) {
		try {
			//InputStream in = new BufferedInputStream(conn.getInputStream());
			File cachedInv = null;
			InputStream in = null;
			if (cache) {
				File invCacheDir = new File(context.getCacheDir(), "inv/");
				invCacheDir.mkdirs();
				cachedInv = new File(context.getCacheDir(), "inv/" + id.convertToLong() + ".cache");
				long cacheTime = 3 * 60 * 1000;// 3 minutes;
				if (cachedInv.exists() && System.currentTimeMillis() - cachedInv.lastModified() < cacheTime)
					in = new BufferedInputStream(new FileInputStream(cachedInv));
			}
			Log.d("cache", "Inventory cache is: " + (in == null ? " null" : " not null"));
			if (in == null) {
				URL url = new URL("http://api.steampowered.com/IEconItems_440/GetPlayerItems/v0001/?key=" + apikey + "&SteamID=" + id.convertToLong() + "&format=json");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				in = conn.getInputStream();
				// cache didn't work, read http
				if (cache) {
					// read http, and write to the cache...then return an input stream to the cache
					// I don't know why
					cachedInv.delete();
					cachedInv.createNewFile();
					OutputStream out = new BufferedOutputStream(new FileOutputStream(cachedInv));
					int bufferSize = 2048;
					byte[] buffer = new byte[bufferSize];
					int len = 0;
					while ((len = in.read(buffer)) != -1)
						out.write(buffer, 0, len);
					if (out != null)
						out.close();
					in.close();
					in = new BufferedInputStream(new FileInputStream(cachedInv));
				}
			}

			GsonBuilder builder = new GsonBuilder();
			builder.registerTypeAdapter(SchemaQuality.class, new JsonDeserializer<SchemaQuality>() {
				@Override
				public SchemaQuality deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
					return SchemaQuality.values[element.getAsInt()];
				}
			});
			builder.registerTypeAdapter(SteamInventoryAttribute.class, new JsonDeserializer<SteamInventoryAttribute>() {
				@Override
				public SteamInventoryAttribute deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
					SteamInventoryAttribute attribute = new SteamInventoryAttribute();
					JsonObject obj = element.getAsJsonObject();
					attribute.defindex = obj.get("defindex").getAsInt();
					if (obj.has("float_value"))
						attribute.float_value = obj.get("float_value").getAsFloat();
					if (obj.has("value")) {
						if (obj.get("value").getAsJsonPrimitive().isNumber())
							attribute.long_value = obj.get("value").getAsLong();
						else
							attribute.string_value = obj.get("value").getAsString();
					}
					attribute.account_info = context.deserialize(obj.get("account_info"), SteamInventoryAccountInfo.class);
					return attribute;
				}
			});
			Gson gson = builder.create();

			JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
			reader.beginObject(); // root
			String result = reader.nextName();
			if (!result.equals("result")) {
				reader.close();
				return null;
			}
			SteamInventory inventory = gson.fromJson(reader, SteamInventory.class);
			reader.endObject(); // end root
			reader.close();
			in.close();

			return inventory;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void filterTradable() {
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).flag_cannot_trade) {
				items.remove(i);
				i--;
			}
		}
	}
}