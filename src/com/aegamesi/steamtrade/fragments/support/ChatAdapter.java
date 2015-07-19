package com.aegamesi.steamtrade.fragments.support;

import android.database.Cursor;
import android.graphics.PorterDuff.Mode;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
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
	private static final String compactLineFormat = "<font color=\"#%06X\"><i>[%s]</i> <b>%s</b>:</font> %s";
	private static final String compactDateFormat = "yyyy-MM-dd HH:mm:ss a";
	public final boolean compact;
	public long time_last_read = 0L;
	public int color_default = 0;
	private String name_us = null;
	private String name_them = null;

	public ChatAdapter(Cursor cursor, boolean compact) {
		super(cursor);
		this.compact = compact;
	}

	public void setPersonaNames(String name_us, String name_them) {
		this.name_us = name_us;
		this.name_them = name_them;
	}

	@Override
	public void onBindViewHolder(ChatAdapter.ViewHolder holder, Cursor cursor) {
		// get the information from the cursor
		long column_time = cursor.getLong(1);
		String column_message = cursor.getString(2);
		boolean column_sent_by_us = cursor.getInt(3) == 0;

		final long expandedDividerTimeCutoff = 1000 * 60 * 10; // 10 minutes
		boolean expandedDivider = false;
		if (!cursor.isFirst()) {
			cursor.moveToPrevious();
			long previous_column_time = cursor.getLong(1);
			boolean previous_column_sent_by_us = cursor.getInt(3) == 0;

			expandedDivider = (previous_column_sent_by_us != column_sent_by_us) || (column_time - previous_column_time > expandedDividerTimeCutoff);
			cursor.moveToNext();
		}
		boolean hideTime = false;
		if (!cursor.isLast()) {
			cursor.moveToNext();
			long next_column_time = cursor.getLong(1);
			boolean next_column_sent_by_us = cursor.getInt(3) == 0;

			hideTime = (next_column_sent_by_us == column_sent_by_us) && (next_column_time - column_time <= expandedDividerTimeCutoff);
		}

		holder.itemView.setTag(column_message);
		// coloring
		int colorOffline = holder.itemView.getResources().getColor(R.color.steam_offline);
		int colorOnline = holder.itemView.getResources().getColor(R.color.steam_online);
		int bgColor = (column_time < time_last_read) ? colorOffline : (!column_sent_by_us ? color_default : colorOnline);

		if (compact) {
			String personName;
			if (name_us != null && name_them != null)
				personName = column_sent_by_us ? name_us : name_them;
			else
				personName = holder.itemView.getResources().getString(column_sent_by_us ? R.string.chat_you : R.string.chat_them);

			column_message = TextUtils.htmlEncode(column_message); // escape html
			String date = AndroidUtil.getChatStyleTimeAgo(holder.itemView.getContext(), column_time, System.currentTimeMillis()).toString();
			//String date = DateFormat.format(compactDateFormat, column_time);
			String chatLine = String.format(compactLineFormat, bgColor & 0xFFFFFF, date, personName, column_message);

			holder.viewDivider.setVisibility(expandedDivider ? View.VISIBLE : View.GONE);

			Html.ImageGetter imageGetter = new UILImageGetter(holder.textMessage, holder.textMessage.getContext());
			String message = SteamUtil.parseEmoticons(chatLine);
			holder.textMessage.setText(Html.fromHtml(message, imageGetter, null));
		} else {
			// next adjust the layout
			int bubbleDrawable = 0;
			if (column_sent_by_us)
				bubbleDrawable = expandedDivider ? R.drawable.chat_bubble_right : R.drawable.chat_bubble_right_min;
			else
				bubbleDrawable = expandedDivider ? R.drawable.chat_bubble_left : R.drawable.chat_bubble_left_min;

			holder.viewBubble.setBackgroundResource(bubbleDrawable);
			FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) holder.viewBubble.getLayoutParams();
			bubbleParams.gravity = column_sent_by_us ? Gravity.RIGHT : Gravity.LEFT;
			bubbleParams.leftMargin = holder.itemView.getResources().getDimensionPixelSize(!column_sent_by_us ? R.dimen.chat_margin_minor : R.dimen.chat_margin_major);
			bubbleParams.rightMargin = holder.itemView.getResources().getDimensionPixelSize(column_sent_by_us ? R.dimen.chat_margin_minor : R.dimen.chat_margin_major);
			bubbleParams.topMargin = holder.itemView.getResources().getDimensionPixelSize(expandedDivider ? R.dimen.chat_vertical_margin_major : R.dimen.chat_vertical_margin_minor);

			holder.viewBubble.getBackground().setColorFilter(bgColor, Mode.MULTIPLY);

			Html.ImageGetter imageGetter = new UILImageGetter(holder.textMessage, holder.textMessage.getContext());
			column_message = TextUtils.htmlEncode(column_message); // escape html
			String message = SteamUtil.parseEmoticons(column_message);
			holder.textMessage.setText(Html.fromHtml(message, imageGetter, null));
			//holder.textStatus.setText(DateFormat.format("h:mm a   yyyy-MM-dd", column_time));
			if (!hideTime) {
				holder.textStatus.setText(AndroidUtil.getChatStyleTimeAgo(holder.textStatus.getContext(), column_time, System.currentTimeMillis()));
				holder.textStatus.setVisibility(View.VISIBLE);
			} else {
				holder.textStatus.setVisibility(View.GONE);
			}
		}
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
		public View viewDivider;

		public ViewHolder(ViewGroup parent) {
			super(LayoutInflater.from(parent.getContext()).inflate(compact ? R.layout.fragment_chat_item_compact : R.layout.fragment_chat_item, parent, false));

			textMessage = (TextView) itemView.findViewById(R.id.chat_message);
			if (!compact) {
				textStatus = (TextView) itemView.findViewById(R.id.chat_status);
				viewBubble = itemView.findViewById(R.id.chat_bubble);
			} else {
				viewDivider = itemView.findViewById(R.id.chat_divider);
			}

			itemView.setTag(this);
			itemView.setOnLongClickListener(ChatAdapter.this);
		}
	}
}
