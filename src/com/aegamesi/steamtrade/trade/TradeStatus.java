package com.aegamesi.steamtrade.trade;

import java.util.ArrayList;
import java.util.List;

public class TradeStatus {
	public String error;
	public boolean newversion;
	public boolean success;
	public long trade_status = -1;
	public int version;
	public int logpos;
	public TradeUserObj me;
	public TradeUserObj them;
	public List<TradeEvent> events = new ArrayList<TradeEvent>();

	public class TradeUserObj {
		public short ready;
		public short confirmed;
		public int sec_since_touch;
	}

	public class TradeEvent {
		public String steamid;
		public int action;
		public long timestamp;
		public int appid;
		public String text;
		public int contextid;
		public long assetid;
	}
}
