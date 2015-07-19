package com.aegamesi.steamtrade.steam;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aegamesi.lib.android.UILImageGetter;
import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.FragmentWeb;
import com.aegamesi.steamtrade.fragments.support.ItemListView;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SteamItemUtil {
	public static Map<String, MarketInfo> marketCache = new HashMap<String, MarketInfo>();

	private static void fetchMarketInfo(final Context context, final TradeInternalAsset asset, final View populateView) {
		final String cache_key = asset.getAppid() + "|" + asset.getMarketHashName();
		if (marketCache.containsKey(cache_key)) {
			MarketInfo info = marketCache.get(cache_key);
			populateMarketView(context, asset, info, populateView);
			return;
		}

		new Thread(
				new Runnable() {
					@Override
					public void run() {
						String currency = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_currency", "1");
						Map<String, String> parameters = new HashMap<String, String>();
						parameters.put("currency", currency);
						parameters.put("appid", "" + asset.getAppid());
						parameters.put("market_hash_name", asset.getMarketHashName());
						String result = SteamWeb.fetch(
								"http://steamcommunity.com/market/priceoverview/",
								"GET",
								parameters,
								""
						);

						MarketInfo info = null;
						try {
							JSONObject json = new JSONObject(result);

							if (json.optBoolean("success", false)) {
								info = new MarketInfo();
								info.lowest_price = Html.fromHtml(json.optString("lowest_price", "")).toString();
								info.median_price = Html.fromHtml(json.optString("median_price", "")).toString();
								info.volume = Html.fromHtml(json.optString("volume", "0")).toString();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

						final MarketInfo finalizedInfo = info;
						marketCache.put(cache_key, info);
						populateView.post(new Runnable() {
							@Override
							public void run() {
								populateMarketView(context, asset, finalizedInfo, populateView);
							}
						});
					}
				}
		).start();
	}

	private static void populateMarketView(final Context context, final TradeInternalAsset asset, MarketInfo info, View view) {
		if (info != null) {
			((TextView) view.findViewById(R.id.item_market_lowprice)).setText(String.format(view.getResources().getString(R.string.market_low), info.lowest_price));
			((TextView) view.findViewById(R.id.item_market_volume)).setText(String.format(view.getResources().getString(R.string.market_volume), info.volume));
			ImageButton button = (ImageButton) view.findViewById(R.id.item_market_button);
			button.setVisibility(View.VISIBLE);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					if (context instanceof MainActivity) {
						Fragment fragment = new FragmentWeb();
						Bundle bundle = new Bundle();
						bundle.putString("url", "http://steamcommunity.com/market/listings/" + asset.getAppid() + "/" + Uri.encode(asset.getMarketHashName()));
						fragment.setArguments(bundle);
						((MainActivity) context).browseToFragment(fragment, true);
					}
				}
			});
		} else {
			((TextView) view.findViewById(R.id.item_market_lowprice)).setText(R.string.market_item_notfound);
		}
	}

	public static void showItemInfo(Context context, TradeInternalAsset asset, List<AppContextPair> appContextPairs) {
		View view = LayoutInflater.from(context).inflate(R.layout.view_item_info, null);
		populateItemInfo(view, asset, appContextPairs);

		View marketView = view.findViewById(R.id.item_market_view);
		if (asset.isMarketable() && asset.getMarketHashName() != null) {
			fetchMarketInfo(context, asset, marketView);
		} else {
			marketView.setVisibility(View.GONE);
		}

		ScrollView scrollView = new ScrollView(context);
		scrollView.addView(view);
		buildDismissableModal(context, scrollView);
	}

	public static void populateItemInfo(View view, TradeInternalAsset asset, List<AppContextPair> appContextPairs) {
		// get the application name
		String appName = null;
		if (appContextPairs != null)
			for (AppContextPair pair : appContextPairs)
				if (pair.getAppid() == asset.getAppid())
					appName = pair.getName();

		String image_url = "https://steamcommunity-a.akamaihd.net/economy/image/" + asset.getIconURL() + "/144x144";
		ImageLoader.getInstance().displayImage(image_url, (ImageView) view.findViewById(R.id.item_image));
		((TextView) view.findViewById(R.id.item_name)).setText(asset.getName());
		if (asset.getNameColor() != 0) {
			((TextView) view.findViewById(R.id.item_name)).setTextColor(asset.getNameColor());
		}
		((TextView) view.findViewById(R.id.item_type)).setText((appName == null ? "" : (appName + "\n")) + asset.getType());

		TextView item_description = (TextView) view.findViewById(R.id.item_description);
		Html.ImageGetter imageGetter = new UILImageGetter(item_description, view.getContext());
		Spanned description_text = Html.fromHtml(generateAssetDescription(asset), imageGetter, null);
		item_description.setText(description_text);
	}

	public static AlertDialog buildDismissableModal(Context context, View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(view);
		builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		return builder.show();
	}

	public static String generateAssetDescription(TradeInternalAsset asset) {
		String descriptions = "";
		for (TradeInternalAsset.Description desc : asset.getDescriptions()) {
			if (desc.getValue() == null || desc.getValue().length() == 0)
				continue;

			Matcher matcher = Pattern.compile("\\[date\\](\\d*)\\[/date\\]").matcher(desc.getValue());
			StringBuffer result = new StringBuffer();
			while (matcher.find())
				matcher.appendReplacement(result, DateFormat.getDateInstance().format(new Date(Long.parseLong(matcher.group(1)) * 1000L)));
			matcher.appendTail(result);
			String parsedDescription = result.toString();

			String element = "";
			if (desc.getColor() != 0)
				element += "<font color='" + String.format("#%06X", (0xFFFFFF & desc.getColor())) + "'>";
			if (desc.getType().equals("image"))
				element += "<img src='" + desc.getValue() + "'>";
			else if (desc.getType().equals("html"))
				element += parsedDescription;
			else
				element += TextUtils.htmlEncode(parsedDescription).replaceAll("\n", "<br>");
			if (desc.getColor() != 0)
				element += "</font>";

			if (descriptions.length() > 0)
				descriptions += "<br>";
			descriptions += element;
		}
		return descriptions;
	}

	public static void showItemListModal(Context context, String title, List<TradeInternalAsset> items) {
		ItemListView view = new ItemListView(context, null);
		view.setItems(items);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setView(view);
		builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		builder.show();
	}

	public static class MarketInfo {
		public String lowest_price;
		public String median_price;
		public String volume;
	}
}
