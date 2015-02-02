package com.aegamesi.steamtrade.steam;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.SteamTrade;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EChatEntryType;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class SteamChatHandler {
	public ArrayList<ChatReceiver> receivers;
	public Pattern chatPattern;
	public SimpleDateFormat dateFormat;
	public SteamService service;
	public HashMap<SteamID, Integer> unreadMessages;

	public SteamChatHandler(SteamService service) {
		this.service = service;
		this.receivers = new ArrayList<ChatReceiver>();
		chatPattern = Pattern.compile("([t|c])\\[([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})\\] (You|Them): (.*)");
		dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US);
		unreadMessages = new HashMap<SteamID, Integer>();
	}

	public void receiveMessage(SteamID from, String message) {
		ChatLine chatline = new ChatLine();
		chatline.steamId = from; // null if it's local
		chatline.message = message;
		chatline.time = (new Date()).getTime();

		broadcastMessage(from, chatline);
	}

	public void sendMessage(SteamID to, String message) {
		ChatLine chatline = new ChatLine();
		chatline.steamId = null; // null if it's local
		chatline.message = message;
		chatline.time = (new Date()).getTime();

		SteamService.singleton.steamClient.getHandler(SteamFriends.class).sendChatMessage(to, EChatEntryType.ChatMsg, message);
		broadcastMessage(to, chatline);
	}

	private void broadcastMessage(SteamID key, ChatLine line) {
		// t[yyyy-MM-dd hh:mm:ss] You: blahblahblah
		// c[yyyy-MM-dd hh:mm:ss] Them: blahblahblah
		// <-- TRADE STARTED --> (it ignores lines beginning with "<--"
		logLine(line, key, "c");

		boolean delivered = false;
		for (ChatReceiver receiver : receivers)
			if (receiver != null)
				delivered = delivered || receiver.receiveChatLine(line, delivered);
		if (!delivered) {
			// use a notification
			unreadMessages.put(key, unreadMessages.containsKey(key) ? unreadMessages.get(key) + 1 : 1);
			updateNotification();
		}
	}

	public void logLine(ChatLine line, SteamID key, String tag) {
		String log = tag + "[" + dateFormat.format(line.time) + "] " + (line.steamId == null ? "You" : "Them") + ": " + line.message;
		appendToLog(key.convertToLong() + "", log);
	}

	public void updateNotification() {
		int unread = 0;
		String body = "";
		for (Map.Entry<SteamID, Integer> entry : unreadMessages.entrySet()) {
			unread += entry.getValue();
			body += (body.length() == 0 ? "" : ", ") + service.steamClient.getHandler(SteamFriends.class).getFriendPersonaName(entry.getKey());
		}
		String title = String.format(service.getString(R.string.x_new_messages), unread);
		body = String.format(service.getString(R.string.from), body);

		NotificationManager notificationManager = (NotificationManager) SteamService.singleton.getSystemService(Context.NOTIFICATION_SERVICE);
		if (unread == 0) {
			notificationManager.cancel(49717);
			return;
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(SteamService.singleton).setSmallIcon(R.drawable.ic_launcher);
		builder.setContentTitle(title).setContentText(body);
		// builder.setAutoCancel(true);
		// lights, sound, vibrate
		builder.setLights(0xFFAEDEDC, 750, 750);
		builder.setVibrate(new long[]{100, 300, 150, 300});
		//builder.setStyle(new NotificationCompat.InboxStyle());
		builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

		// and the intent
		Intent intent = new Intent(SteamService.singleton, MainActivity.class);
		intent.putExtra("fromNotification", true);
		intent.putExtra("notificationSteamID", unreadMessages.size() == 1 ? ((SteamID) unreadMessages.keySet().toArray()[0]).convertToLong() : 0);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(SteamService.singleton);
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(intent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		notificationManager.notify(49717, builder.build());
	}

	public ArrayList<ChatLine> getChatHistory(SteamID key, String filter, String startAt) {
		File logfolder = new File(SteamTrade.filesDir, "logs/" + SteamService.singleton.steamClient.getSteamId().convertToLong());
		logfolder.mkdirs();
		File file = new File(logfolder, key.convertToLong() + ".log");

		boolean started = startAt == null;
		ArrayList<ChatLine> list = new ArrayList<ChatLine>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				if (startAt != null) {
					if (line.equals("<-- " + startAt + " -->")) {
						started = true;
						list.clear();
						continue;
					}
				}
				if (!started)
					continue;
				Matcher m = chatPattern.matcher(line);
				if (!m.matches())
					continue;
				if (filter != null && !m.group(1).equals(filter)) // not fitting the filter
					continue;
				ChatLine chatLine = new ChatLine();
				try {
					chatLine.time = dateFormat.parse(m.group(2)).getTime();
				} catch (ParseException e) {
					continue;
				}
				chatLine.message = m.group(4);
				chatLine.steamId = m.group(3).equals("You") ? null : key;
				list.add(chatLine);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

	public ArrayList<SteamID> getRecentChats(long threshold) {
		class ChatTime implements Comparable<ChatTime> {
			public long time;
			public SteamID id;

			@Override
			public int compareTo(ChatTime other) {
				if (other.time == time)
					return 0;
				return time > other.time ? -1 : 1;
			}
		}
		ArrayList<SteamID> recentChats = new ArrayList<SteamID>();
		if (SteamService.singleton == null || SteamService.singleton.steamClient == null || SteamService.singleton.steamClient.getSteamId() == null)
			return recentChats;
		File logfolder = new File(SteamTrade.filesDir, "logs/" + SteamService.singleton.steamClient.getSteamId().convertToLong());
		logfolder.mkdirs();

		ArrayList<ChatTime> allChats = new ArrayList<ChatTime>();
		File[] files = logfolder.listFiles();
		for (File file : files) {
			if (file.isDirectory() || !file.getName().endsWith(".log"))
				continue;
			ChatTime time = new ChatTime();
			time.id = new SteamID(Long.parseLong((file.getName().substring(0, file.getName().length() - 4))));
			time.time = file.lastModified();
			allChats.add(time);
		}
		Collections.sort(allChats);

		for (ChatTime chat : allChats) {
			if (System.currentTimeMillis() - chat.time < threshold)
				recentChats.add(chat.id);
			else
				break;
		}
		return recentChats;
	}

	public void appendToLog(String key, String line) {
		File logfolder = new File(SteamTrade.filesDir, "logs/" + SteamService.singleton.steamClient.getSteamId().convertToLong());
		logfolder.mkdirs();
		File file = new File(logfolder, key + ".log");

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
			writer.append(line + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class ChatLine {
		public SteamID steamId;
		public long time;
		public String message;
	}

	public static interface ChatReceiver {
		public boolean receiveChatLine(ChatLine line, boolean delivered);
	}
}
