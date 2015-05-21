package com.aegamesi.steamtrade.trade2;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aegamesi.lib.UILImageGetter;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.ItemListAdapter;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TradeUtil {
	public static String generateAssetDescription(TradeInternalAsset asset) {
		String descriptions = "";
		for (TradeInternalAsset.Description desc : asset.getDescriptions()) {
			if (desc.getValue() == null || desc.getValue().length() == 0)
				continue;

			Matcher matcher = Pattern.compile("\\[date\\](\\d*)\\[/date\\]").matcher(desc.getValue());
			StringBuffer result = new StringBuffer();
			while (matcher.find())
				matcher.appendReplacement(result, DateFormat.getDateInstance().format(new Date(Long.parseLong(matcher.group(1)))));
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

	public static void showItemInfo(Context context, TradeInternalAsset asset, List<AppContextPair> appContextPairs) {
		View view = LayoutInflater.from(context).inflate(R.layout.view_item_info, null);
		populateItemInfo(view, asset, appContextPairs);

		ScrollView scrollView = new ScrollView(context);
		scrollView.addView(view);
		buildDismissableModal(context, scrollView);
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

	public static void showItemListModal(Context context, String title, List<TradeInternalAsset> items) {
		GridView grid = (GridView) LayoutInflater.from(context).inflate(R.layout.item_grid, null);
		ItemListAdapter adapter = new ItemListAdapter(context, grid, false, null);
		adapter.setItemList(items);
		grid.setAdapter(adapter);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setView(grid);
		builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		builder.show();
	}
}
