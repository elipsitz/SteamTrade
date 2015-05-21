package com.aegamesi.steamtrade.steam;

import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;

public interface SteamMessageHandler {
	void handleSteamMessage(CallbackMsg msg);
}
