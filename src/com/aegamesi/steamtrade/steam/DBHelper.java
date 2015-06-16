package com.aegamesi.steamtrade.steam;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.aegamesi.steamtrade.SteamTrade;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBHelper extends SQLiteOpenHelper {
	// If you change the database schema, you must increment the database version.
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "SteamTrade.db";

	// predefined sql query
	private static final String SQL_CREATE_ENTRIES =
			"CREATE TABLE " + ChatEntry.TABLE + " (" + //
					ChatEntry._ID + " INTEGER PRIMARY KEY," +
					ChatEntry.COLUMN_TIME + " INTEGER," +
					ChatEntry.COLUMN_OUR_ID + " INTEGER," +
					ChatEntry.COLUMN_OTHER_ID + " INTEGER," +
					ChatEntry.COLUMN_SENDER + " INTEGER," +
					ChatEntry.COLUMN_TYPE + " INTEGER," +
					ChatEntry.COLUMN_MESSAGE + " TEXT" +
					" )";

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_ENTRIES);

		// if we have data from the previous version of the app, load it here
		// load chat data
		Pattern chatPattern = Pattern.compile("([t|c])\\[([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})\\] (You|Them): (.*)");
		Matcher chatMatcher = chatPattern.matcher("");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US);
		File logfolder = new File(SteamTrade.filesDir, "logs");
		if (logfolder.exists() && logfolder.isDirectory()) {
			File[] id_folders = logfolder.listFiles();
			for (File id_folder : id_folders) {
				long user_id = Long.parseLong(id_folder.getName());
				File[] chats = id_folder.listFiles();
				for (File chat : chats) {
					if (chat.isFile() && chat.getName().endsWith(".log")) {
						long other_id = Long.parseLong(chat.getName().substring(0, chat.getName().length() - 4));
						// now we can start reading and parsing this file
						try {
							BufferedReader reader = new BufferedReader(new FileReader(chat));
							String line;
							while ((line = reader.readLine()) != null) {
								chatMatcher.reset(line);
								if (!chatMatcher.matches())
									continue;
								int type = chatMatcher.group(1).equals("c") ? SteamChatManager.CHAT_TYPE_CHAT : SteamChatManager.CHAT_TYPE_TRADE;
								long time;
								try {
									time = dateFormat.parse(chatMatcher.group(2)).getTime();
								} catch (ParseException e) {
									continue;
								}
								boolean sent_by_us = chatMatcher.group(3).equals("You");
								String message = chatMatcher.group(4);

								ChatEntry.insert(db, time, user_id, other_id, sent_by_us, type, message);
							}
							reader.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// no upgrades yet...
		// adapted from http://blog.adamsbros.org/2012/02/28/upgrade-android-sqlite-database/
		int upgradeTo = oldVersion + 1;
		while (upgradeTo <= newVersion) {
			switch (upgradeTo) {
				case 1:
					break;
			}
			upgradeTo++;
		}
	}

	public static abstract class ChatEntry implements BaseColumns {
		public static final String TABLE = "chat";
		public static final String COLUMN_TIME = "time";
		public static final String COLUMN_OUR_ID = "id_us";
		public static final String COLUMN_OTHER_ID = "id_other";
		public static final String COLUMN_SENDER = "sender"; // 0 - us, 1 - them
		public static final String COLUMN_TYPE = "type"; // 0 - chat, 1 - trade
		public static final String COLUMN_MESSAGE = "message";

		// inserts a new row, returns the new row id
		public static long insert(SQLiteDatabase db, long time, long id_us, long id_them, boolean sent_by_us, int type, String message) {
			ContentValues values = new ContentValues();
			values.put(COLUMN_TIME, time);
			values.put(COLUMN_OUR_ID, id_us);
			values.put(COLUMN_OTHER_ID, id_them);
			values.put(COLUMN_SENDER, sent_by_us ? 0 : 1);
			values.put(COLUMN_TYPE, type);
			values.put(COLUMN_MESSAGE, message.trim());

			return db.insert(TABLE, null, values);
		}
	}
}