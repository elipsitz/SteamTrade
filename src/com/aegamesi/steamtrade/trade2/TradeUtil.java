package com.aegamesi.steamtrade.trade2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;
import com.loopj.android.image.SmartImageView;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;

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
			if (desc.getValue().equals("image"))
				element += "<img src='" + desc.getValue() + "'>";
			else if (desc.getValue().equals("html"))
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
		Spanned item_description = Html.fromHtml(generateAssetDescription(asset));
		// get the application name
		String appName = null;
		if (appContextPairs != null)
			for (AppContextPair pair : appContextPairs)
				if (pair.getAppid() == asset.getAppid())
					appName = pair.getName();

		String image_url = "https://steamcommunity-a.akamaihd.net/economy/image/" + asset.getIconURL() + "/144x144";
		((SmartImageView) view.findViewById(R.id.item_image)).setImageUrl(image_url);
		((TextView) view.findViewById(R.id.item_name)).setText(asset.getName());
		if (asset.getNameColor() != 0) {
			((TextView) view.findViewById(R.id.item_name)).setTextColor(asset.getNameColor());
		}
		((TextView) view.findViewById(R.id.item_type)).setText((appName == null ? "" : (appName + "\n")) + asset.getType());
		((TextView) view.findViewById(R.id.item_description)).setText(item_description);
	}

	public static void showItemInfo(Activity activity, TradeInternalAsset asset, List<AppContextPair> appContextPairs) {
		View view = activity.getLayoutInflater().inflate(R.layout.view_item_info, null);
		populateItemInfo(view, asset, appContextPairs);

		ScrollView scrollView = new ScrollView(activity);
		scrollView.addView(view);
		showDismissableModal(activity, scrollView);
	}

	public static void showDismissableModal(Context context, View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(view);
		builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		builder.show();
	}
}
