package com.aegamesi.steamtrade.steam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.Schema.SchemaAttribute;
import com.aegamesi.steamtrade.steam.Schema.SchemaItem;
import com.aegamesi.steamtrade.steam.Schema.SchemaParticle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SteamSchemaDownloader extends AsyncTask<String, Integer, Schema> {
	public Context context;

	public SteamSchemaDownloader(Context context) {
		this.context = context;
	}

	@Override
	protected void onPreExecute() {
		// setup UI
	}

	@Override
	protected Schema doInBackground(String... args) {
		Schema schema = null;
		int tries = 5;
		while (schema == null && tries > 0) {
			tries--;
			try {
				File cachedSchema = new File(context.getCacheDir(), "schema440.cache");
				URL url = new URL("http://api.steampowered.com/IEconItems_440/GetSchema/v0001/?format=json&language=en&key=" + args[0]);

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setIfModifiedSince(cachedSchema.exists() ? cachedSchema.lastModified() : 0);
				conn.connect();
				boolean cache = conn.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED;

				// liars!
				InputStream in = cache ? new FileInputStream(cachedSchema) : conn.getInputStream();
				schema = readJsonStream(new BufferedInputStream(in), cache);

				if (!cache) {
					BufferedWriter bw = new BufferedWriter(new FileWriter(cachedSchema));
					GsonBuilder builder = new GsonBuilder();
					builder.registerTypeAdapter(new TypeToken<Map<Integer, SchemaItem>>() {
					}.getType(), new JsonSerializer<Map<Integer, SchemaItem>>() {
						@Override
						public JsonElement serialize(Map<Integer, SchemaItem> src, Type typeOfSrc, JsonSerializationContext context) {
							return context.serialize(src.values().toArray());
						}
					});
					builder.registerTypeAdapter(new TypeToken<Map<Integer, SchemaAttribute>>() {
					}.getType(), new JsonSerializer<Map<Integer, SchemaAttribute>>() {
						@Override
						public JsonElement serialize(Map<Integer, SchemaAttribute> src, Type typeOfSrc, JsonSerializationContext context) {
							return context.serialize(src.values().toArray());
						}
					});
					builder.registerTypeAdapter(new TypeToken<Map<Integer, SchemaParticle>>() {
					}.getType(), new JsonSerializer<Map<Integer, SchemaParticle>>() {
						@Override
						public JsonElement serialize(Map<Integer, SchemaParticle> src, Type typeOfSrc, JsonSerializationContext context) {
							return context.serialize(src.values().toArray());
						}
					});
					builder.create().toJson(schema, Schema.class, new JsonWriter(bw));
					bw.close();
				}
				in.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return schema;
	}

	@SuppressLint("UseSparseArrays")
	public Schema readJsonStream(InputStream in, boolean cache) throws IOException {
		Gson gson = new Gson();
		Schema schema = new Schema();

		try {
			JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
			reader.beginObject(); // root
			if (!cache) {
				String result = reader.nextName();
				if (!result.equals("result")) {
					reader.close();
					return null;
				}
				reader.beginObject(); // result
			}
			// <-- START MAIN PART
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("items")) {
					schema.items = new HashMap<Integer, SchemaItem>();
					reader.beginArray();
					while (reader.hasNext()) {
						SchemaItem item = gson.fromJson(reader, SchemaItem.class);
						if (!cache && item.image_url != null && item.image_url_large != null && item.image_url.length() > 45 && item.image_url_large.length() > 45) {
							item.image_url = item.image_url.substring(45);
							item.image_url_large = item.image_url_large.substring(45);
							// remove (45 chars)
							// http://media.steampowered.com/apps/440/icons/
						}
						schema.items.put(item.defindex, item);
					}
					reader.endArray();
				} else if (name.equals("attributes")) {
					schema.attributes = new HashMap<Integer, SchemaAttribute>();
					reader.beginArray();
					while (reader.hasNext()) {
						SchemaAttribute attribute = gson.fromJson(reader, SchemaAttribute.class);
						schema.attributes.put(attribute.defindex, attribute);
					}
					reader.endArray();
				} else if (name.equals("attribute_controlled_attached_particles") || name.equals("particles")) {
					reader.beginArray();
					schema.particles = new HashMap<Integer, SchemaParticle>();
					while (reader.hasNext()) {
						SchemaParticle particle = gson.fromJson(reader, SchemaParticle.class);
						schema.particles.put(particle.id, particle);
					}
					reader.endArray();
				} else {
					reader.skipValue();
				}
			}
			// END MAIN PART -->
			if (!cache)
				reader.endObject(); // end result
			reader.endObject(); // end root
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return schema;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		// update UI
	}

	@Override
	protected void onPostExecute(Schema result) {
		// get rid of UI stuff,
		if (result == null) {
			Toast.makeText(context, R.string.schema_load_failed, Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(context, R.string.schema_loaded, Toast.LENGTH_LONG).show();
			SteamService.singleton.schema = result;
		}
	}

}
