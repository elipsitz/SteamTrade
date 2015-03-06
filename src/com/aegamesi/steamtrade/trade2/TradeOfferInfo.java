package com.aegamesi.steamtrade.trade2;

import com.nosoop.steamtrade.SteamWeb;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class TradeOfferInfo {
	public long id = -1;
	public boolean active = false;
	public String senderName = "";
	public String message = "";
	public int numItemsThem = 0;
	public int numItemsUs = 0;

	public static List<TradeOfferInfo> getTradeOffers() {
		String response = SteamWeb.request("http://steamcommunity.com/my/tradeoffers", "GET", null, TradeOffer.generateCookies(), "http://steamcommunity.com/");
		Document document = Jsoup.parse(response);

		Elements tradeOfferElements = document.getElementsByClass("tradeoffer");
		ArrayList<TradeOfferInfo> offers = new ArrayList<TradeOfferInfo>();

		for (Element tradeOfferElement : tradeOfferElements) {
			TradeOfferInfo info = new TradeOfferInfo();
			info.id = Integer.parseInt(tradeOfferElement.id().substring(13)); // strip off tradeofferid_
			info.active = tradeOfferElement.getElementsByClass("tradeoffer_items_ctn").get(0).hasClass("active");
			Elements headerElements = tradeOfferElement.getElementsByClass("tradeoffer_header");
			info.senderName = headerElements.size() > 0 ? headerElements.get(0).text().trim() : "";
			if (info.senderName.endsWith(" offered you a trade:"))
				info.senderName = info.senderName.substring(0, info.senderName.length() - " offered you a trade:".length());
			Elements messageElements = tradeOfferElement.getElementsByClass("tradeoffer_message");
			info.message = messageElements.size() > 0 ? messageElements.get(0).text().trim() : "";
			if (info.message.endsWith(" (?)"))
				info.message = info.message.substring(0, info.message.length() - " (?)".length());
			Elements itemListElements = tradeOfferElement.getElementsByClass("tradeoffer_item_list");
			if (itemListElements.size() >= 2) {
				info.numItemsThem = itemListElements.get(0).getElementsByClass("trade_item").size();
				info.numItemsUs = itemListElements.get(1).getElementsByClass("trade_item").size();
			}
			offers.add(info);
		}

		return offers;
	}
}
