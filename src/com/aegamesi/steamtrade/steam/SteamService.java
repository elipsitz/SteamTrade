package com.aegamesi.steamtrade.steam;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.aegamesi.steamtrade.LoginActivity;
import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Timer;
import java.util.TimerTask;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EChatEntryType;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EFriendRelationship;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EUniverse;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendMsgCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendMsgEchoCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendMsgHistoryCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.FriendMsgHistoryCallback.FriendMsg;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.PersonaStateCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamnotifications.SteamNotifications;
import uk.co.thomasc.steamkit.steam3.handlers.steamnotifications.callbacks.NotificationOfflineMsgCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamnotifications.callbacks.NotificationUpdateCallback;
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
import uk.co.thomasc.steamkit.steam3.steamclient.callbacks.CMListCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbacks.ConnectedCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbacks.DisconnectedCallback;
import uk.co.thomasc.steamkit.steam3.webapi.WebAPI;
import uk.co.thomasc.steamkit.types.keyvalue.KeyValue;
import uk.co.thomasc.steamkit.types.steamid.SteamID;
import uk.co.thomasc.steamkit.util.KeyDictionary;
import uk.co.thomasc.steamkit.util.WebHelpers;
import uk.co.thomasc.steamkit.util.cSharp.events.ActionT;
import uk.co.thomasc.steamkit.util.crypto.CryptoHelper;
import uk.co.thomasc.steamkit.util.crypto.RSACrypto;


// This is the backbone of the app. Stores SteamClient connection, message, chat, and trade handlers, schemas...
public class SteamService extends Service {
	public static final int NOTIFICATION_ID = 49716;

	public static boolean attemptReconnect = false;
	public static boolean running = false;
	public static SteamService singleton = null;
	public static Bundle extras = null;
	public static SteamConnectionListener connectionListener = null;
	public SteamClient steamClient = null;
	public SteamTradeManager tradeManager = null;
	public SteamChatManager chatManager = null;
	public SteamMessageHandler messageHandler = null;
	public String sessionID = null;
	public String token = null;
	public String tokenSecure = null;
	public String webapiUserNonce = null;
	public String sentryHash = null;
	public long timeLogin = 0L;
	Timer myTimer;
	private DBHelper db_helper = null;
	private SQLiteDatabase _db = null;
	private Handler handler;
	private boolean timerRunning = false;

	public static String generateSteamWebCookies() {
		String cookies = "";
		if (singleton != null) {
			cookies += "sessionid=" + singleton.sessionID + ";";
			cookies += "steamLogin=" + singleton.token + ";";
			cookies += "steamLoginSecure=" + singleton.tokenSecure + ";";
			//cookies += "webTradeEligibility=%7B%22allowed%22%3A0%2C%22reason%22%3A0%2C%22allowed_at_time%22%3A0%2C%22steamguard_required_days%22%3A15%2C%22sales_this_year%22%3A0%2C%22max_sales_per_year%22%3A200%2C%22forms_requested%22%3A0%2C%22new_device_cooldown_days%22%3A7%7D;";
			cookies += "webTradeEligibility=%7B%22allowed%22%3A1%2C%22allowed_at_time%22%3A0%2C%22steamguard_required_days%22%3A15%2C%22sales_this_year%22%3A0%2C%22max_sales_per_year%22%3A-1%2C%22forms_requested%22%3A0%2C%22new_device_cooldown_days%22%3A7%7D;";
			cookies += "steamMachineAuth" + singleton.steamClient.getSteamId().convertToLong() + "=" + singleton.sentryHash + "";
		}
		return cookies;
	}

	public static void attemptLogon(Context context, final SteamConnectionListener listener, Bundle bundle, boolean start_service) {
		extras = bundle;

		if (start_service) {
			// start the steam service (stop if it's already started)
			Intent intent = new Intent(context, SteamService.class);
			context.stopService(intent);
			SteamService.singleton = null;
			context.startService(intent);
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				if (listener != null)
					listener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_INITIALIZING);

				// busy-wait for the service to start...
				while (SteamService.singleton == null) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
						if (listener != null) {
							listener.onConnectionResult(EResult.Fail);
							listener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_FAILURE);
						}
					}
				}

				SteamService.singleton.processLogon(listener);
			}
		}).start();
	}

	public void resetAuthentication() {
		sessionID = null;
		token = null;
		tokenSecure = null;
	}

	private void buildNotification(int code, boolean update) {
		// then, update our notification
		boolean persist_notification = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_persist_notification", true);
		if (persist_notification) {
			Intent notificationIntent = new Intent(this, LoginActivity.class);
			notificationIntent.setAction(Intent.ACTION_MAIN);
			notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			builder.setSmallIcon(R.drawable.ic_notify_online);
			builder.setContentTitle(getString(R.string.app_name));
			builder.setContentText(getResources().getStringArray(R.array.connection_status)[code]);
			builder.setContentIntent(contentIntent);
			builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
			builder.setOnlyAlertOnce(true);
			builder.setPriority(NotificationCompat.PRIORITY_MIN);
			//builder.setOngoing(true);

			if (steamClient != null) {
				SteamNotifications steamNotifications = steamClient.getHandler(SteamNotifications.class);
				if (steamNotifications != null)
					builder.setContentInfo(steamNotifications.getTotalNotificationCount() + "");
			}

			if (update) {
				NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.notify(NOTIFICATION_ID, builder.build());
			} else {
				startForeground(NOTIFICATION_ID, builder.build());
			}
		}
	}

	// this needs to take place in a non-main thread
	private void processLogon(SteamConnectionListener listener) {
		// now we wait.
		if (listener != null)
			listener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_CONNECTING);
		buildNotification(SteamConnectionListener.STATUS_CONNECTING, false);
		attemptReconnect = false;

		if (listener != null)
			connectionListener = listener;
		db();
		SteamUtil.webApiKey = null; // reset webApiKey
		steamClient.connect(true);
	}

	public SQLiteDatabase db() {
		if (_db == null)
			_db = db_helper.getWritableDatabase();
		return _db;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		handler = new Handler();
		db_helper = new DBHelper(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		steamClient = new SteamClient();
		chatManager = new SteamChatManager();
		tradeManager = new SteamTradeManager();

		if (!timerRunning) {
			myTimer = new Timer();
			myTimer.scheduleAtFixedRate(new CheckCallbacksTask(), 0, 1000);
			timerRunning = true;
		}

		running = true;
		singleton = this;

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopForeground(true);
		running = false;
		singleton = null;

		if (_db != null)
			_db.close();
		_db = null;

		if (myTimer != null)
			myTimer.cancel();
		timerRunning = false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@SuppressWarnings("rawtypes")
	public void handleSteamMessage(CallbackMsg msg) {
		msg.handle(DisconnectedCallback.class, new ActionT<DisconnectedCallback>() {
			@Override
			public void call(DisconnectedCallback obj) {
				if (connectionListener != null) {
					connectionListener.onConnectionResult(EResult.ConnectFailed);
					connectionListener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_FAILURE);
				}
				buildNotification(SteamConnectionListener.STATUS_FAILURE, true);

				// now, attempt reconnect?
				ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
				boolean connected = activeNetwork != null && activeNetwork.isConnected();
				if (attemptReconnect && connected) {
					boolean pref_reconnect = PreferenceManager.getDefaultSharedPreferences(SteamService.this).getBoolean("pref_reconnect", true);
					if (pref_reconnect) {
						// first make sure that we are connected to the internet
						// and the last failure was not "connectfailed"

						// do the reconnection process
						if (extras != null) {
							// steam guard key will no longer be valid-- and, provided we had a successful login, we shouldn't need it anyway
							if (extras.containsKey("steamguard"))
								extras.remove("steamguard");

							// this might not work using the own service that will be stopped as the context
							SteamService.attemptLogon(SteamService.this, null, extras, true);
						}
					}
				}

				Log.i("Steam", "Disconnected from Steam Network, new connection: " + obj.isNewconnection());
				disconnect();
			}
		});
		msg.handle(LoggedOffCallback.class, new ActionT<LoggedOffCallback>() {
			@Override
			public void call(LoggedOffCallback obj) {
				Log.i("Steam", "Logged off from Steam, " + obj.getResult());
				disconnect();
			}
		});
		msg.handle(ConnectedCallback.class, new ActionT<ConnectedCallback>() {
			@Override
			public void call(ConnectedCallback callback) {
				Log.i("Steam", "Connection Status " + callback.getResult());
				if (callback.getResult() == EResult.OK) {
					//  notify listener
					if (connectionListener != null) {
						connectionListener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_LOGON);
					}
					buildNotification(SteamConnectionListener.STATUS_LOGON, true);

					// log in
					LogOnDetails details = new LogOnDetails();
					details.username(extras.getString("username"));
					if (extras.getBoolean("loginkey", false)) {
						// login key
						String loginkey = PreferenceManager.getDefaultSharedPreferences(SteamService.this).getString("loginkey_" + extras.getString("username"), "");
						if (loginkey.length() > 0)
							details.loginkey = loginkey;
					} else {
						details.password(extras.getString("password"));
					}
					details.shouldRememberPassword = extras.getBoolean("remember", false);
					if (extras.getString("steamguard") != null) {
						//details.authCode(extras.getString("steamguard"));
						if (extras.getBoolean("twofactor", false))
							details.twoFactorCode(extras.getString("steamguard"));
						else
							details.authCode(extras.getString("steamguard"));
					}


					// sentry files
					String prefSentry = PreferenceManager.getDefaultSharedPreferences(SteamService.this).getString("pref_machineauth", "");
					if (prefSentry.trim().length() > 0) {
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
								//details.authCode = "";

								sentryHash = SteamUtil.bytesToHex(details.sentryFileHash);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					SteamUser steamUser = steamClient.getHandler(SteamUser.class);
					if (steamUser != null)
						steamUser.logOn(details, com.aegamesi.steamtrade.Installation.id());
				} else {
					if (connectionListener != null) {
						connectionListener.onConnectionResult(EResult.ConnectFailed);
						connectionListener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_FAILURE);
					}
					buildNotification(SteamConnectionListener.STATUS_FAILURE, true);
					attemptReconnect = false;

					disconnect();
				}
			}
		});
		msg.handle(LoggedOnCallback.class, new ActionT<LoggedOnCallback>() {
			@Override
			public void call(LoggedOnCallback callback) {
				if (callback.getResult() != EResult.OK) {
					// if there's a loginkey saved and it's an InvalidPassword, scrap it
					if(callback.getResult() == EResult.InvalidPassword) {
						if (extras != null && extras.containsKey("username")) {
							SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(SteamService.this).edit();
							prefs.remove("loginkey_" + extras.getString("username"));
							prefs.apply();
						}
					}

					if (connectionListener != null) {
						connectionListener.onConnectionResult(callback.getResult());
						connectionListener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_FAILURE);
					}
					buildNotification(SteamConnectionListener.STATUS_FAILURE, true);
					attemptReconnect = false;
					Log.i("Steam", "Login Failure: " + callback.getResult());
					disconnect();
				} else {
					// okay! :)
					webapiUserNonce = callback.getWebAPIUserNonce();

					// save password (it's valid!)
					if (extras != null && extras.getBoolean("remember", false) && extras.containsKey("password")) {
						Log.d("SteamService", "Saving password.");
						SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(SteamService.this).edit();
						prefs.putString("password_" + extras.getString("username"), extras.getString("password"));
						prefs.apply();
					}

					if (connectionListener != null)
						connectionListener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_AUTH);
					buildNotification(SteamConnectionListener.STATUS_AUTH, true);

					doSteamWebAuthentication();
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

					SteamUser steamUser = steamClient.getHandler(SteamUser.class);
					if (steamUser != null)
						steamUser.sendMachineAuthResponse(auth);

					if (extras != null)
						extras.putBoolean("alertSteamGuard", true);
				}
			}
		});
		msg.handle(FriendMsgCallback.class, new ActionT<FriendMsgCallback>() {
			@Override
			public void call(FriendMsgCallback callback) {
				final EChatEntryType type = callback.getEntryType();

				if (!callback.getSender().equals(steamClient.getSteamId())) {
					if (type == EChatEntryType.ChatMsg) {
						chatManager.receiveMessage(callback.getSender(), callback.getMessage(), System.currentTimeMillis());
					}
				}
			}
		});
		// echoed message from another instance
		msg.handle(FriendMsgEchoCallback.class, new ActionT<FriendMsgEchoCallback>() {
			@Override
			public void call(FriendMsgEchoCallback obj) {
				// we log it:
				if (obj.getEntryType() == EChatEntryType.ChatMsg) {
					chatManager.broadcastMessage(
							System.currentTimeMillis(),
							steamClient.getSteamId(),
							obj.getRecipient(),
							true,
							SteamChatManager.CHAT_TYPE_CHAT,
							obj.getMessage()
					);
				}
			}
		});
		msg.handle(PersonaStateCallback.class, new ActionT<PersonaStateCallback>() {
			@Override
			public void call(PersonaStateCallback obj) {
				// handle notifications for friend requests
				SteamFriends steamFriends = steamClient.getHandler(SteamFriends.class);
				if (steamFriends != null) {
					if (steamFriends.getFriendRelationship(obj.getFriendID()) == EFriendRelationship.RequestRecipient) {
						// create a notification
						String partnerName = obj.getName();
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SteamService.this);
						NotificationCompat.Builder builder = new NotificationCompat.Builder(SteamService.this);
						builder.setSmallIcon(R.drawable.ic_notify_friend);
						builder.setContentTitle(getString(R.string.friend_request));
						builder.setContentText(String.format(getString(R.string.friend_request_text), partnerName));
						builder.setPriority(NotificationCompat.PRIORITY_MAX);
						builder.setVibrate(prefs.getBoolean("pref_vibrate", true) ? new long[]{0, 500, 200, 500, 1000} : new long[]{0});
						builder.setSound(Uri.parse(prefs.getString("pref_notification_sound", "DEFAULT_SOUND")));
						builder.setOnlyAlertOnce(true);
						builder.setAutoCancel(true);

						Bundle bundle = new Bundle();
						bundle.putLong("steamId", obj.getFriendID().convertToLong());
						Intent intent = new Intent(SteamService.this, MainActivity.class);
						intent.putExtra("fragment", "com.aegamesi.steamtrade.fragments.FragmentProfile");
						intent.putExtra("arguments", bundle);

						TaskStackBuilder stackBuilder = TaskStackBuilder.create(SteamService.this);
						stackBuilder.addParentStack(MainActivity.class);
						stackBuilder.addNextIntent(intent);
						PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
						builder.setContentIntent(resultPendingIntent);
						NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
						mNotificationManager.notify(2995, builder.build());
					}
				}
			}
		});

		msg.handle(CMListCallback.class, new ActionT<CMListCallback>() {
			@Override
			public void call(CMListCallback obj) {
				if (obj.getServerList().length > 0) {
					String serverString = "";
					for (String entry : obj.getServerList()) {
						if (serverString.length() > 0)
							serverString += ",";
						serverString += entry;
					}

					SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(SteamService.this).edit();
					prefs.putString("cm_server_list", serverString);
					prefs.apply();
				}
			}
		});
		msg.handle(LoginKeyCallback.class, new ActionT<LoginKeyCallback>() {
			@Override
			public void call(LoginKeyCallback callback) {
				Log.d("SteamService", "Got loginkey " + callback.getLoginKey() + "| uniqueid: " + callback.getUniqueId());
				if (extras != null && extras.getBoolean("remember", false)) {
					Log.d("SteamService", "Saving loginkey.");
					SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(SteamService.this).edit();
					prefs.putString("loginkey_" + extras.getString("username"), callback.getLoginKey());
					prefs.putInt("uniqueid_" + extras.getString("username"), callback.getUniqueId());
					prefs.commit();
				}
			}
		});
		msg.handle(FriendMsgHistoryCallback.class, new ActionT<FriendMsgHistoryCallback>() {
			@Override
			public void call(FriendMsgHistoryCallback obj) {
				// add all messages that are "unread" to our internal database
				// problem though... Steam send us *all messages* as unread since we last
				// requested history... perhaps we should request history
				// when we get a message. that way we confirm we read the message
				// SOLUTION: record the time that we log in. If this time is after that, ignore it
				if (obj.getSuccess() > 0) {
					SteamID otherId = obj.getSteamId();
					SteamID ourId = steamClient.getSteamId();
					for (FriendMsg msg : obj.getMessages()) {
						if (!msg.isUnread())
							continue;
						if ((msg.getTimestamp() * 1000L) > timeLogin)
							continue;
						boolean sent_by_us = !msg.getSender().equals(otherId);
						// potentially check for if it's been read already
						chatManager.broadcastMessage(
								msg.getTimestamp() * 1000, // seconds --> millis
								ourId,
								otherId,
								sent_by_us,
								SteamChatManager.CHAT_TYPE_CHAT,
								msg.getMessage()
						);
					}
				}
			}
		});
		msg.handle(NotificationOfflineMsgCallback.class, new ActionT<NotificationOfflineMsgCallback>() {
			@Override
			public void call(NotificationOfflineMsgCallback callback) {
				Log.d("SteamService", "Notification offline msg: " + callback.getOfflineMessages());

				chatManager.unreadMessages.addAll(callback.getFriendsWithOfflineMessages());
			}
		});
		msg.handle(NotificationUpdateCallback.class, new ActionT<NotificationUpdateCallback>() {
			@Override
			public void call(NotificationUpdateCallback obj) {
				buildNotification(SteamConnectionListener.STATUS_CONNECTED, true);
			}
		});
	}

	public void disconnect() {
		stopSelf();
		steamClient.disconnect();
		resetAuthentication();
	}

	private boolean doSteamWebAuthentication() {
		sessionID = SteamUtil.bytesToHex(CryptoHelper.GenerateRandomBlock(4));
		final WebAPI userAuth = new WebAPI("ISteamUserAuth", null);//SteamUtil.webApiKey); // this shouldn't require an api key
		// generate an AES session key
		byte[] sessionKey = CryptoHelper.GenerateRandomBlock(32);
		// rsa encrypt it with the public key for the universe we're on
		final byte[] cryptedSessionKey;
		EUniverse universe = steamClient == null ? EUniverse.Public : steamClient.getConnectedUniverse();
		final RSACrypto rsa = new RSACrypto(KeyDictionary.getPublicKey((universe == null || universe == EUniverse.Invalid) ? EUniverse.Public : universe));
		cryptedSessionKey = rsa.encrypt(sessionKey);
		final byte[] loginKey = new byte[20];
		System.arraycopy(webapiUserNonce.getBytes(), 0, loginKey, 0, webapiUserNonce.length());
		// aes encrypt the loginkey with our session key
		final byte[] cryptedLoginKey = CryptoHelper.SymmetricEncrypt(loginKey, sessionKey);

		new Thread(new Runnable() {
			@Override
			public void run() {
				int tries = 3;
				while (true) {
					try {
						Log.i("Steam", "Sending auth request...");
						KeyValue authResult = userAuth.authenticateUser(String.valueOf(steamClient.getSteamId().convertToLong()), WebHelpers.UrlEncode(cryptedSessionKey), WebHelpers.UrlEncode(cryptedLoginKey), "POST", "true");
						token = authResult.get("token").asString();
						tokenSecure = authResult.get("tokensecure").asString();
						Log.i("Steam", "Successfully authenticated: " + token + " secure: " + tokenSecure);

						// tell our listener and start fetching the webapi key
						if (connectionListener != null)
							connectionListener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_APIKEY);
						buildNotification(SteamConnectionListener.STATUS_APIKEY, true);

						fetchAPIKey();

						// now we're done! tell our listener
						if (connectionListener != null) {
							connectionListener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_CONNECTED);
							connectionListener.onConnectionResult(EResult.OK);
						}
						buildNotification(SteamConnectionListener.STATUS_CONNECTED, true);

						finalizeConnection();

						break;
					} catch (final Exception e) {
						if (--tries == 0) {
							Log.e("Steam", "FATAL(ish): Unable to authenticate with SteamWeb. Tried several times");
							if (connectionListener != null) {
								connectionListener.onConnectionResult(EResult.ServiceUnavailable);
								connectionListener.onConnectionStatusUpdate(SteamConnectionListener.STATUS_FAILURE);
							}
							buildNotification(SteamConnectionListener.STATUS_FAILURE, true);
							attemptReconnect = false;
							break;
						}
						Log.e("Steam", "Error authenticating! Retrying...");
					}
				}
			}
		}).start();
		return true;
	}

	private void fetchAPIKey() {
		// fetch api key
		String apikey = SteamWeb.requestWebAPIKey("localhost"); // hopefully this keeps working
		SteamUtil.webApiKey = apikey == null ? "" : apikey;
		Log.d("Steam", "Fetched api key: " + SteamUtil.webApiKey);
	}

	private void finalizeConnection() {
		timeLogin = System.currentTimeMillis();
		attemptReconnect = true;

		SteamFriends steamFriends = steamClient.getHandler(SteamFriends.class);
		if (steamFriends != null) {
			steamFriends.setPersonaState(EPersonaState.Online);
		}

		SteamNotifications steamNotifications = steamClient.getHandler(SteamNotifications.class);
		if (steamNotifications != null) {
			steamNotifications.requestNotificationItem();
			steamNotifications.requestNotificationComments();
			steamNotifications.requestNotificationOfflineMessages();
			steamNotifications.requestNotificationGeneric();
		}
	}

	private class CheckCallbacksTask extends TimerTask {
		@Override
		public void run() {
			if (steamClient == null)
				return;
			while (true) {
				final CallbackMsg msg = steamClient.getCallback(true);
				if (msg == null)
					break;
				handleSteamMessage(msg);
				if (messageHandler != null) {
					// gotta run this on the ui thread
					handler.post(new Runnable() {
						@Override
						public void run() {
							messageHandler.handleSteamMessage(msg);
						}
					});
				}
			}
		}
	}
}