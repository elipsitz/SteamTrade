package com.aegamesi.steamtrade.steam;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;

public interface SteamConnectionListener {
	int STATUS_UNKNOWN = 0;
	int STATUS_INITIALIZING = 1;
	int STATUS_CONNECTING = 2;
	int STATUS_LOGON = 3;
	int STATUS_AUTH = 4;
	int STATUS_APIKEY = 5;
	int STATUS_CONNECTED = 6;
	int STATUS_FAILURE = 7;

	// warning: this will be called from a non-main thread
	void onConnectionResult(EResult result);


	// warning: this will be called from a non-main thread
	void onConnectionStatusUpdate(int status);
}
