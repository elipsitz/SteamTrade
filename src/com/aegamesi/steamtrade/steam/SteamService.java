package com.aegamesi.steamtrade.steam;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.aegamesi.steamtrade.LoginActivity;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.TradeManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Timer;
import java.util.TimerTask;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EChatEntryType;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EUniverse;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendMsgCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.SteamUser;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.callbacks.LoggedOffCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.callbacks.LoggedOnCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.callbacks.LoginKeyCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.callbacks.UpdateMachineAuthCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.types.LogOnDetails;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.types.MachineAuthDetails;
import uk.co.thomasc.steamkit.steam3.steamclient.SteamClient;
import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;
import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.JobCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbacks.ConnectedCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbacks.DisconnectedCallback;
import uk.co.thomasc.steamkit.steam3.webapi.WebAPI;
import uk.co.thomasc.steamkit.types.keyvalue.KeyValue;
import uk.co.thomasc.steamkit.util.KeyDictionary;
import uk.co.thomasc.steamkit.util.WebHelpers;
import uk.co.thomasc.steamkit.util.cSharp.events.ActionT;
import uk.co.thomasc.steamkit.util.crypto.CryptoHelper;
import uk.co.thomasc.steamkit.util.crypto.RSACrypto;
import uk.co.thomasc.steamkit.util.logging.DebugLog;

public class SteamService extends Service {
	// This is the backbone of the app. Stores SteamClient connection, message, chat, and trade handlers, schemas...
	public static boolean running = false;
	public static SteamService singleton = null;

	public SteamClient steamClient = null;
	public Bundle extras = null;

	public TradeManager tradeManager = null;
	public SteamChatHandler chat = null;
	public SteamMessageHandler messageHandler = null;
	public SteamLogcatDebugListener debugListener = null;

	public String sessionID = null;
	public String token = null;
	public String tokenSecure = null;
	public String webapiKey = null;
	public String sentryHash = null;

	private Handler handler;
	Timer myTimer;
	private boolean timerRunning = false;
	private long POLL_TIME = 500; // 500 ms = 0.5 sec

	@Override
	public void onCreate() {
		super.onCreate();

		DebugLog.addListener(debugListener = new SteamLogcatDebugListener());
		handler = new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		steamClient = new SteamClient();
		chat = new SteamChatHandler(this);
		tradeManager = new TradeManager();

		if (!timerRunning) {
			myTimer = new Timer();
			myTimer.scheduleAtFixedRate(new CheckCallbacksTask(), 0, POLL_TIME);
			timerRunning = true;
		}

		running = true;
		singleton = this;

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		running = false;
		singleton = null;

		if (debugListener != null)
			DebugLog.removeListener(debugListener);
		if (myTimer != null)
			myTimer.cancel();
		timerRunning = false;
	}

	public static String generateSteamWebCookies() {
		String cookies = "";
		cookies += "sessionid=" + singleton.sessionID.trim() + ";";
		cookies += "steamLogin=" + singleton.token + ";";
		cookies += "steamLoginSecure=" + singleton.tokenSecure + ";";
		cookies += "webTradeEligibility=%7B%22allowed%22%3A0%2C%22reason%22%3A0%2C%22allowed_at_time%22%3A0%2C%22steamguard_required_days%22%3A15%2C%22sales_this_year%22%3A0%2C%22max_sales_per_year%22%3A200%2C%22forms_requested%22%3A0%2C%22new_device_cooldown_days%22%3A7%7D;";
		cookies += "steamMachineAuth" + singleton.steamClient.getSteamId().convertToLong() + "=" + singleton.sentryHash + ";";
		return cookies;
	}

	public void attemptLogon(Bundle bundle) {
		abandonLogon();
		extras = bundle;
		steamClient.connect(true);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_notification).setContentTitle("Ice").setContentText("Connected");
		Intent notificationIntent = new Intent(this, LoginActivity.class);
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		builder.setContentIntent(contentIntent);
		startForeground(49716, builder.build());
	}

	public void abandonLogon() {
		SteamService.this.stopForeground(true);
	}

	public void disconnect() {

	}

	public class CheckCallbacksTask extends TimerTask {
		@Override
		public void run() {
			if (steamClient == null)
				return;
			while (true) {
				final CallbackMsg msg = steamClient.getCallback(true);
				if (msg == null)
					break;
				if (messageHandler != null) {
					// gotta run this on the ui thread
					handler.post(new Runnable() {
						@Override
						public void run() {
							messageHandler.handleSteamMessage(msg);
						}
					});
				}
				handleSteamMessage(msg);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public void handleSteamMessage(CallbackMsg msg) {
		msg.handle(DisconnectedCallback.class, new ActionT<DisconnectedCallback>() {
			@Override
			public void call(DisconnectedCallback obj) {
				Log.i("Steam", "Disconnected from Steam Network, new connection: " + obj.isNewconnection());
				SteamService.this.stopForeground(true);
			}
		});
		msg.handle(LoggedOffCallback.class, new ActionT<LoggedOffCallback>() {
			@Override
			public void call(LoggedOffCallback obj) {
				Log.i("Steam", "Logged off from Steam, " + obj.getResult());
				steamClient.disconnect();
				SteamService.this.stopForeground(true);
			}
		});
		msg.handle(ConnectedCallback.class, new ActionT<ConnectedCallback>() {
			@Override
			public void call(ConnectedCallback callback) {
				Log.i("Steam", "Connection Status " + callback.getResult());
				if (callback.getResult() == EResult.OK) {
					// log in
					LogOnDetails details = new LogOnDetails().username(extras.getString("username")).password(extras.getString("password"));
					if (extras.getString("steamguard") != null)
						details.authCode(extras.getString("steamguard"));

					// sentry files
					String prefSentry = PreferenceManager.getDefaultSharedPreferences(SteamService.this).getString("pref_machineauth", "");
					if (prefSentry != null && prefSentry.trim().length() > 0) {
						sentryHash = prefSentry.trim();
					} else {
						File file = new File(getFilesDir(), "sentry");
						if (!file.exists())
							file.mkdir();
						file = new File(file, extras.getString("username") + ".sentry");
						if (file.exists()) {
							try {
								RandomAccessFile raf = new RandomAccessFile(file, "r");
								byte[] data = new byte[(int) raf.length()];
								raf.readFully(data);
								raf.close();
								details.sentryFileHash = SteamUtil.calculateSHA1(data);
								details.authCode = "";

								sentryHash = SteamUtil.bytesToHex(details.sentryFileHash);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					steamClient.getHandler(SteamUser.class).logOn(details, com.aegamesi.steamtrade.Installation.id());
				} else {
					abandonLogon();
				}
			}
		});
		msg.handle(LoggedOnCallback.class, new ActionT<LoggedOnCallback>() {
			@Override
			public void call(LoggedOnCallback callback) {
				if (callback.getResult() != EResult.OK) {
					Log.i("Steam", "Login Failure: " + callback.getResult());
					abandonLogon();
				}
			}
		});
		msg.handle(JobCallback.class, new ActionT<JobCallback>() {
			@Override
			public void call(JobCallback callback) {
				if (callback.getCallbackType() == UpdateMachineAuthCallback.class) {
					UpdateMachineAuthCallback authCallback = (UpdateMachineAuthCallback) callback.getCallback();
					try {
						Log.i("Steam", "Received updated sentry file: " + authCallback.getFileName());
						File file = new File(getFilesDir(), "sentry");
						if (!file.exists())
							file.mkdir();
						file = new File(file, extras.getString("username") + ".sentry");
						FileOutputStream fos = new FileOutputStream(file);
						fos.write(authCallback.getData());
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

					MachineAuthDetails auth = new MachineAuthDetails();
					auth.jobId = callback.getJobId().getValue();
					auth.fileName = authCallback.getFileName();
					auth.bytesWritten = authCallback.getBytesToWrite();
					auth.fileSize = authCallback.getData().length;
					auth.offset = authCallback.getOffset();
					auth.result = EResult.OK;
					auth.lastError = 0;
					auth.oneTimePassword = authCallback.getOneTimePassword();
					auth.sentryFileHash = SteamUtil.calculateSHA1(authCallback.getData());

					sentryHash = SteamUtil.bytesToHex(auth.sentryFileHash);

					steamClient.getHandler(SteamUser.class).sendMachineAuthResponse(auth);
				}
			}
		});
		msg.handle(FriendMsgCallback.class, new ActionT<FriendMsgCallback>() {
			@Override
			public void call(FriendMsgCallback callback) {
				final EChatEntryType type = callback.getEntryType();

				if (!callback.getSender().equals(steamClient.getSteamId())) {
					if (type == EChatEntryType.ChatMsg) {
						chat.receiveMessage(callback.getSender(), callback.getMessage());
					}
				}
			}
		});
	}

	public boolean authenticate(LoginKeyCallback callback) {
		sessionID = Base64.encodeToString(String.valueOf(callback.getUniqueId()).getBytes(), Base64.DEFAULT);
		final WebAPI userAuth = new WebAPI("ISteamUserAuth", null);//SteamUtil.apikey); // this shouldn't require an api key
		// generate an AES session key
		byte[] sessionKey = CryptoHelper.GenerateRandomBlock(32);
		// rsa encrypt it with the public key for the universe we're on
		final byte[] cryptedSessionKey;
		EUniverse universe = steamClient == null ? EUniverse.Public : steamClient.getConnectedUniverse();
		final RSACrypto rsa = new RSACrypto(KeyDictionary.getPublicKey((universe == null || universe == EUniverse.Invalid) ? EUniverse.Public : universe));
		cryptedSessionKey = rsa.encrypt(sessionKey);
		final byte[] loginKey = new byte[20];
		System.arraycopy(webapiKey.getBytes(), 0, loginKey, 0, webapiKey.length());
		// aes encrypt the loginkey with our session key
		final byte[] cryptedLoginKey = CryptoHelper.SymmetricEncrypt(loginKey, sessionKey);

		new Thread(new Runnable() {
			@Override
			public void run() {
				int tries = 5;
				while (true) {
					try {
						Log.i("Steam", "Sending auth request...");
						KeyValue authResult = userAuth.authenticateUser(String.valueOf(steamClient.getSteamId().convertToLong()), WebHelpers.UrlEncode(cryptedSessionKey), WebHelpers.UrlEncode(cryptedLoginKey), "POST", "true");
						token = authResult.get("token").asString();
						tokenSecure = authResult.get("tokensecure").asString();

						Log.i("Steam", "Successfully authenticated: " + token + " secure: " + tokenSecure);
						break;
					} catch (final Exception e) {
						if (--tries == 0) {
							Log.e("Steam", "FATAL(ish): Unable to authenticate with SteamWeb. Tried several times");
							LoginActivity.result = EResult.ServiceUnavailable;
							break;
						}
						Log.e("Steam", "Error authenticating! Retrying...");
					}
				}
			}
		}).start();
		return true;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}