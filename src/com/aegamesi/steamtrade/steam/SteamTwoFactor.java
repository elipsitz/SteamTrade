package com.aegamesi.steamtrade.steam;

import android.content.Context;
import android.util.Base64;

import com.aegamesi.lib.android.AndroidUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class SteamTwoFactor {
	private static final String DEVICE_ID_SALT = "a8cd56564282db67";
	private static final String TOTP_CODE_CHARS = "23456789BCDFGHJKMNPQRTVWXY";

	/**
	 * Generates a consistent device ID for Steam two factor authentication.
	 *
	 * @param steamID The steamID to base the deviceID off of.
	 * @return A String representing an android-specific device ID.
	 */
	public static String generateDeviceID(SteamID steamID) {
		String hash = steamID.render() + DEVICE_ID_SALT;
		hash = SteamUtil.bytesToHex(SteamUtil.calculateSHA1(hash.getBytes()));
		return "android:" + hash;
	}

	/**
	 * Return the current local time.
	 *
	 * @return Current local time, in seconds.
	 */
	public static long getCurrentTime() {
		return getCurrentTime(0);
	}

	/**
	 * Return the current local time, offset by a number of seconds.
	 *
	 * @param time_offset The number of seconds to offset the current time by.
	 * @return Local time with offset, in seconds.
	 */
	public static long getCurrentTime(int time_offset) {
		return (System.currentTimeMillis() / 1000L) + time_offset;
	}

	/**
	 * Generate a Steam-style TOTP authentication code.
	 *
	 * @param shared_secret - the TOTP shared_secret
	 * @param time          - The time to generate the code for.
	 * @return authentication code
	 */
	public static String generateAuthCodeForTime(byte[] shared_secret, long time) {
		time = time / 30L;
		ByteBuffer byteBuffer = ByteBuffer.allocate(8);
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		byteBuffer.putLong(time);

		byte[] hash;
		try {
			SecretKeySpec secretkeyspec = new SecretKeySpec(shared_secret, "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(secretkeyspec);
			hash = mac.doFinal(byteBuffer.array());
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}

		int start = hash[19] & 0x0F;
		int fullcode = (hash[start] & 0x7f) << 24 | (hash[start + 1] & 0xff) << 16 | (hash[start + 2] & 0xff) << 8 | hash[start + 3] & 0xff;

		String code = "";
		for (int i = 0; i <= 4; i++) {
			code += TOTP_CODE_CHARS.charAt(fullcode % TOTP_CODE_CHARS.length());
			fullcode /= TOTP_CODE_CHARS.length();
		}
		return code;
	}

	/**
	 * Generate a base64 confirmation key for use with mobile trade confirmations. The key can only be used once.
	 *
	 * @param identity_secret - The identity_secret that you received when enabling two-factor authentication
	 * @param time            - The Unix time for which you are generating this secret. Generally should be the current time.
	 * @param tag             - The tag which identifies what this request (and therefore key) will be for.
	 *                        "conf" to load the confirmations page,
	 *                        "details" to load details about a trade,
	 *                        "allow" to confirm a trade,
	 *                        "cancel" to cancel it.
	 * @return String key
	 */
	public static String generateConfirmationKey(byte[] identity_secret, long time, String tag) {
		if (tag == null)
			tag = "";
		ByteBuffer byteBuffer = ByteBuffer.allocate(8 + Math.min(tag.length(), 32));
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		byteBuffer.putLong(time);
		byteBuffer.put(tag.getBytes());

		try {
			SecretKeySpec secretkeyspec = new SecretKeySpec(identity_secret, "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(secretkeyspec);
			byte[] hash = mac.doFinal(byteBuffer.array());
			return Base64.encodeToString(hash, Base64.NO_WRAP);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Generate confirmation tag parameters
	 *
	 * @return
	 */
	public static String generateConfirmationParameters(Context context, String tag) {
		String username = SteamService.singleton.username;
		AccountLoginInfo info = AccountLoginInfo.readAccount(context, username);
		if (info != null && info.has_authenticator) {
			long time = SteamTwoFactor.getCurrentTime();
			SteamID steamID = SteamService.singleton.steamClient.getSteamId();

			Map<String, Object> params = new HashMap<>();
			params.put("p", SteamTwoFactor.generateDeviceID(steamID));
			params.put("a", steamID.convertToLong());
			params.put("k", SteamTwoFactor.generateConfirmationKey(info.tfa_identitySecret, time, tag));
			params.put("t", time);
			params.put("m", "android");
			params.put("tag", tag);
			return AndroidUtil.createURIDataString(params);
		}
		return "";
	}

	public static double getCodeValidityTime() {
		return 30.0 - ((System.currentTimeMillis() / 1000.0) % 30.0);
	}
}
