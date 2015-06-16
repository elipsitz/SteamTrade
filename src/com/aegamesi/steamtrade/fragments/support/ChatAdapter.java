package com.aegamesi.steamtrade.fragments.support;

import android.database.Cursor;
import android.graphics.PorterDuff.Mode;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.lib.android.AndroidUtil;
import com.aegamesi.lib.android.CursorRecyclerAdapter;
import com.aegamesi.lib.android.UILImageGetter;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamUtil;

public class ChatAdapter extends CursorRecyclerAdapter<ChatAdapter.ViewHolder> implements OnLongClickListener {
	public long time_last_read = 0L;
	public int color_default = SteamUtil.colorOnline;

	public ChatAdapter(Cursor cursor) {
		super(cursor);
	}

	@Override
	public void onBindViewHolder(ChatAdapter.ViewHolder holder, Cursor cursor) {
		// get the information from the cursor
		long column_time = cursor.getLong(1);
		String column_message = cursor.getString(2);
		boolean column_sent_by_us = cursor.getInt(3) == 0;

		holder.itemView.setTag(column_message);

		// next adjust the layout
		holder.viewBubble.setBackgroundResource(column_sent_by_us ? R.drawable.chat_bubble_right : R.drawable.chat_bubble_left);
		FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) holder.viewBubble.getLayoutParams();
		bubbleParams.gravity = column_sent_by_us ? Gravity.RIGHT : Gravity.LEFT;
		bubbleParams.leftMargin = holder.itemView.getResources().getDimensionPixelSize(!column_sent_by_us ? R.dimen.chat_margin_minor : R.dimen.chat_margin_major);
		bubbleParams.rightMargin = holder.itemView.getResources().getDimensionPixelSize(column_sent_by_us ? R.dimen.chat_margin_minor : R.dimen.chat_margin_major);

		// coloring
		int bgColor = (column_time < time_last_read) ? SteamUtil.colorOffline : (!column_sent_by_us ? color_default : SteamUtil.colorOnline);
		holder.viewBubble.getBackground().setColorFilter(bgColor, Mode.MULTIPLY);

		Html.ImageGetter imageGetter = new UILImageGetter(holder.textMessage, holder.textMessage.getContext());
		String message = SteamUtil.parseEmoticons(column_message);
		holder.textMessage.setText(Html.fromHtml(message, imageGetter, null));
		//holder.textStatus.setText(DateFormat.format("h:mm a   yyyy-MM-dd", column_time));
		holder.textStatus.setText(AndroidUtil.getChatStyleTimeAgo(holder.textStatus.getContext(), column_time, System.currentTimeMillis()));
	}

	@Override
	public ChatAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new ViewHolder(parent);
	}

	@Override
	public boolean onLongClick(View view) {
		if (view.getTag() != null) {
			String message = (String) view.getTag();
			AndroidUtil.copyToClipboard(view.getContext(), message);
			Toast.makeText(view.getContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}

	class ViewHolder extends RecyclerView.ViewHolder {
		public TextView textMessage;
		public TextView textStatus;
		public View viewBubble;

		public ViewHolder(ViewGroup parent) {
			super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chat_item, parent, false));

			textMessage = (TextView) itemView.findViewById(R.id.chat_message);
			textStatus = (TextView) itemView.findViewById(R.id.chat_status);
			viewBubble = itemView.findViewById(R.id.chat_bubble);

			itemView.setTag(this);
			itemView.setOnLongClickListener(ChatAdapter.this);
		}
	}
}
