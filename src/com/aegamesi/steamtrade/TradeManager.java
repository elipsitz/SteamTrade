package com.aegamesi.steamtrade;

import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.fragments.FragmentTrade;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.trade2.Trade;
import com.google.android.gms.analytics.HitBuilders;

import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.SessionStartCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.TradeProposedCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.TradeResultCallback;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class TradeManager implements OnClickListener {
	public Trade currentTrade = null;

	public View tradeStatus = null;
	public ImageButton yesButton;
	public ImageButton noButton;

	public SteamID pendingTradeRequest = null;
	public int tradeRequestID;
	public boolean tradeRequestSentByUs = false;

	public void trade(SteamID with) {
		if (pendingTradeRequest != null) {
			if (pendingTradeRequest.equals(with) && !tradeRequestSentByUs)
				activity().steamTrade.respondToTrade(tradeRequestID, true);
			if (!pendingTradeRequest.equals(with)) {
				activity().steamTrade.respondToTrade(tradeRequestID, false);
				activity().steamTrade.trade(with);
				pendingTradeRequest = with;
				tradeRequestSentByUs = true;
			}
		} else {
			activity().steamTrade.trade(with);
			pendingTradeRequest = with;
			tradeRequestSentByUs = true;
		}
		updateTradeStatus();
	}

	public void callbackSessionStart(SessionStartCallback obj) {
		Log.i("Trade", "Starting trade with " + obj.getOtherClient().convertToLong());
		SteamService.singleton.chat.appendToLog(obj.getOtherClient().convertToLong() + "", "<-- Trade Started -->");
		activity().tracker().send(new HitBuilders.EventBuilder().setCategory("Steam").setAction("Trade_Start").build());

		SteamID myID = SteamService.singleton.steamClient.getSteamId();
		String sessionID = SteamService.singleton.sessionID;
		String token = SteamService.singleton.token;

		// XXX HORRIBLE
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		currentTrade = new Trade();
		currentTrade.ourSteamId = myID.convertToLong();
		currentTrade.otherSteamId = obj.getOtherClient().convertToLong();
		currentTrade.sessionid = sessionID;
		currentTrade.token = token;
		currentTrade.beginTrade();

		pendingTradeRequest = null;
		updateTradeStatus();
		// browseToFragment(new FragmentTrade(), true);
	}

	public void callbackTradeProposed(TradeProposedCallback obj) {
		if (currentTrade != null || pendingTradeRequest != null) {
			activity().steamTrade.respondToTrade(obj.getTradeID(), false);
			return;
		}
		pendingTradeRequest = obj.getOtherClient();
		tradeRequestID = obj.getTradeID();
		tradeRequestSentByUs = false;
		updateTradeStatus();

		Toast.makeText(activity(), String.format(activity().getString(R.string.trade_got_request), obj.getOtherName()), Toast.LENGTH_LONG).show();
	}

	public void callbackTradeResult(TradeResultCallback obj) {
		if (obj == null || obj.getResponse() == null) {
			Log.e("Steam", "Unexpected null TradeResultCallback...");
			return;
		}
		String name = activity().steamFriends.getFriendPersonaName(obj.getOtherClient());
		switch (obj.getResponse()) {
			case Accepted:
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_accepted), name), Toast.LENGTH_LONG).show();
				break;
			case TargetAlreadyTrading:
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_alreadytrading), name), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case NoResponse:
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_timeout), name), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case Declined:
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_declined), name), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case Cancel:
				Toast.makeText(activity(), activity().getString(R.string.trade_result_cancelled), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case TargetAccountCannotTrade:
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_targetcannotrade), name), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case InitiatorSteamGuardDuration:
				Toast.makeText(activity(), activity().getString(R.string.trade_result_steamguard_duration), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case InitiatorNewDeviceCooldown:
				Toast.makeText(activity(), activity().getString(R.string.trade_result_steamguard_newdevice), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case InitiatorNeedsSteamGuard:
				Toast.makeText(activity(), activity().getString(R.string.trade_result_steamguard), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case InitiatorPasswordResetProbation:
				Toast.makeText(activity(), activity().getString(R.string.trade_result_password_reset), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case InitiatorNeedsVerifiedEmail:
				Toast.makeText(activity(), activity().getString(R.string.trade_result_require_email), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case TradeBannedInitiator:
				Toast.makeText(activity(), activity().getString(R.string.trade_result_tradeban_initiator), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case TradeBannedTarget:
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_tradeban_target), name), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			default:
				// otherwise unable to trade.
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_unknown), name, obj.getResponse()), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
		}
		updateTradeStatus();
	}

	public void cancelTrade() {
		if (currentTrade != null) {
			currentTrade.cancelTrade();
		}
	}

	public void notifyTradeHasEnded() {
		currentTrade = null;
		updateTradeStatus();
	}

	public MainActivity activity() {
		return MainActivity.instance;
	}

	public void setupTradeStatus() {
		tradeStatus = activity().findViewById(R.id.trade_status);
		tradeStatus.setOnClickListener(this);
		yesButton = (ImageButton) tradeStatus.findViewById(R.id.trade_status_yes);
		noButton = (ImageButton) tradeStatus.findViewById(R.id.trade_status_no);
		yesButton.setOnClickListener(this);
		noButton.setOnClickListener(this);
	}

	public void updateTradeStatus() {
		if (tradeStatus == null)
			setupTradeStatus();
		if (currentTrade == null && pendingTradeRequest == null) {
			tradeStatus.setVisibility(View.GONE);
			return;
		}
		TextView statusText = (TextView) tradeStatus.findViewById(R.id.trade_status_text);
		ProgressBar progress = (ProgressBar) tradeStatus.findViewById(R.id.trade_status_progress);

		tradeStatus.setVisibility(View.VISIBLE);
		yesButton.setVisibility(View.GONE);
		noButton.setVisibility(View.GONE);
		progress.setVisibility(View.GONE);
		yesButton.setEnabled(true);

		if (pendingTradeRequest != null) {
			String name = activity().steamFriends.getFriendPersonaName(pendingTradeRequest);
			if (tradeRequestSentByUs) {
				statusText.setText(String.format(activity().getString(R.string.trade_sent_request), name)); // also add CANCEL button
				noButton.setVisibility(View.VISIBLE);
			} else {
				statusText.setText(String.format(activity().getString(R.string.trade_got_request), name)); // also add YES/NO button
				yesButton.setVisibility(View.VISIBLE);
				noButton.setVisibility(View.VISIBLE);
			}
		}
		if (currentTrade != null) {
			String name = activity().steamFriends.getFriendPersonaName(new SteamID(currentTrade.otherSteamId));
			String text = String.format(activity().getString(R.string.trade_currently_trading), name);
			if (currentTrade != null && currentTrade.listener != null && currentTrade.listener.loaded)
				text += "\n" + activity().getString(R.string.trade_tap_to_view);
			else
				progress.setVisibility(View.VISIBLE);
			statusText.setText(text);
		}
	}

	@Override
	public void onClick(View v) {
		if (v == tradeStatus && v.getVisibility() == View.VISIBLE) {
			if (currentTrade != null && currentTrade.listener != null && currentTrade.listener.loaded) {
				Fragment fragment = new FragmentTrade();
				activity().browseToFragment(fragment, false);
			}
		}
		if (v == yesButton) {
			if (pendingTradeRequest != null) {
				activity().steamTrade.respondToTrade(tradeRequestID, true);
				updateTradeStatus();
			}
		}
		if (v == noButton) {
			if (pendingTradeRequest != null) {
				if (tradeRequestSentByUs) {
					activity().steamTrade.cancelTrade(pendingTradeRequest);
				} else {
					activity().steamTrade.respondToTrade(tradeRequestID, false);
				}
				pendingTradeRequest = null;
				updateTradeStatus();
			}
		}
	}
}
