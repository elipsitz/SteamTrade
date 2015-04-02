package com.aegamesi.steamtrade.fragments.support;

import android.graphics.PorterDuff;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamChatHandler.ChatLine;
import com.aegamesi.steamtrade.steam.SteamUtil;

import java.util.ArrayList;

public class ChatAdapter extends BaseAdapter {
	public long last_read = -1;
	public int color_default = SteamUtil.colorOffline;
	public ArrayList<ChatLine> chatLines = new ArrayList<ChatLine>();

	public void addChatLine(ChatLine line) {
		chatLines.add(line);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return chatLines.size();
	}

	@Override
	public Object getItem(int position) {
		return chatLines.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ChatLine line = chatLines.get(position);
		boolean left = line.steamId != null;

		View v = convertView;
		if (v == null || v.getTag() == null || !(v.getTag() instanceof ChatLine) || (((ChatLine) v.getTag()).steamId != null) != left)
			v = MainActivity.instance.getLayoutInflater().inflate(left ? R.layout.chat_item_left : R.layout.chat_item_right, null);
		v.setTag(line);

		int bgColor = 0;
		if(line.time < last_read) {
			bgColor = SteamUtil.colorOffline;
		} else {
			if(left) {
				bgColor = color_default;
			} else {
				bgColor = SteamUtil.colorOnline;
			}
		}

		TextView message = (TextView) v.findViewById(R.id.chat_message);
		message.setText(line.message);
		message.getBackground().setColorFilter(bgColor, PorterDuff.Mode.MULTIPLY);
		TextView timestamp = (TextView) v.findViewById(R.id.chat_timestamp);
		timestamp.setText(DateFormat.format("yyyy-MM-dd - h:mm a", line.time));

		return v;
	}
}