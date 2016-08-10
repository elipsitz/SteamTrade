package com.aegamesi.steamtrade.steam;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.aegamesi.lib.android.AndroidUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class AccountLoginInfo {
	private static final String pref_key = "accountinfo_";
	private static Gson gson;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeHierarchyAdapter(byte[].class, new AndroidUtil.ByteArrayToBase64TypeAdapter());
		gson = gsonBuilder.create();
	}

	// saved variables
	public String username;
	public String password;
	public String loginkey;
	public String avatar;
	public int unique_id = -1;

	public boolean has_authenticator = false;
	public byte[] tfa_sharedSecret;
	public long tfa_serialNumber;
	public String tfa_revocationCode;
	public String tfa_uri;
	public long tfa_serverTime;
	public String tfa_accountName;
	public String tfa_tokenGid;
	public byte[] tfa_identitySecret;
	public byte[] tfa_secret1;

	private static SharedPreferences getSharedPreferences(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static AccountLoginInfo readAccount(Context context, String name) {
		String json = getSharedPreferences(context).getString(pref_key + name, null);
		//Log.d("AccountLoginInfo_READ", json);
		if (json == null)
			return null;

		return gson.fromJson(json, AccountLoginInfo.class);
	}

	public static void writeAccount(Context context, AccountLoginInfo obj) {
		String json = gson.toJson(obj, AccountLoginInfo.class);
		SharedPreferences.Editor editor = getSharedPreferences(context).edit();
		editor.putString(pref_key + obj.username, json);
		//Log.d("AccountLoginInfo_WRITE", json);
		editor.apply();
	}

	public static void removeAccount(Context context, String name) {
		SharedPreferences.Editor editor = getSharedPreferences(context).edit();
		editor.remove(pref_key + name);
		editor.apply();
	}

	public static List<AccountLoginInfo> getAccountList(Context context) {
		List<AccountLoginInfo> accountList = new ArrayList<>();
		for (String key : getSharedPreferences(context).getAll().keySet()) {
			if (key.startsWith(pref_key)) {
				String account = key.substring(pref_key.length());
				accountList.add(readAccount(context, account));
			}
		}
		return accountList;
	}

	public String exportToJson(SteamID steamID) {
		try {
			JSONObject obj = new JSONObject();
			obj.put("steamguard_scheme", "2"); // mobile
			obj.put("steamid", Long.toString(steamID.convertToLong()));
			obj.put("account_name", tfa_accountName);
			obj.put("shared_secret", Base64.encodeToString(tfa_sharedSecret, Base64.NO_WRAP));
			obj.put("serial_number", Long.toString(tfa_serialNumber));
			obj.put("revocation_code", tfa_revocationCode);
			obj.put("uri", tfa_uri);
			obj.put("server_time", Long.toString(tfa_serverTime));
			obj.put("token_gid", tfa_tokenGid);
			obj.put("identity_secret", Base64.encodeToString(tfa_identitySecret, Base64.NO_WRAP));
			obj.put("secret_1", Base64.encodeToString(tfa_secret1, Base64.NO_WRAP));
			obj.put("status", 1);
			return obj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}

	public void importFromJson(String json) {
		try {
			JSONObject obj = new JSONObject(json);
			tfa_accountName = obj.getString("account_name");
			tfa_sharedSecret = Base64.decode(obj.getString("shared_secret"), Base64.DEFAULT);
			tfa_serialNumber = Long.valueOf(obj.getString("serial_number"));
			tfa_revocationCode = obj.getString("revocation_code");
			tfa_uri = obj.getString("uri");
			tfa_serverTime = Long.valueOf(obj.getString("server_time"));
			tfa_tokenGid = obj.getString("token_gid");
			tfa_identitySecret = Base64.decode(obj.getString("identity_secret"), Base64.DEFAULT);
			tfa_secret1 = Base64.decode(obj.getString("secret_1"), Base64.DEFAULT);
		} catch (JSONException | NumberFormatException e) {
			e.printStackTrace();
		}
	}
}
