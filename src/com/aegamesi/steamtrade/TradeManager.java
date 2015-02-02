package com.aegamesi.steamtrade;

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
import com.aegamesi.steamtrade.trade.Trade;
import com.aegamesi.steamtrade.trade.UserTradeListener;
import com.google.android.gms.analytics.HitBuilders;

import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.SessionStartCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.TradeProposedCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamtrading.callbacks.TradeResultCallback;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class TradeManager implements OnClickListener {
	public Trade currentTrade = null;
	public UserTradeListener listener = null;

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
		currentTrade = new Trade(myID, obj.getOtherClient(), sessionID, token, listener = new UserTradeListener());
		currentTrade.start();

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
	}

	public void callbackTradeResult(TradeResultCallback obj) {
		if (obj == null || obj.getResponse() == null) {
			Log.e("Steam", "Unexpected null TradeResultCallback...");
			return;
		}
		String name = SteamService.singleton.steamClient.getHandler(SteamFriends.class).getFriendPersonaName(obj.getOtherClient());
		switch (obj.getResponse()) {
			case Accepted:
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_accepted), name), Toast.LENGTH_LONG).show();
				break;
			case TargetAlreadyTrading:
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_alreadytrading), name), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case Timeout:
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_timeout), name), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case Declined:
				Toast.makeText(activity(), String.format(activity().getString(R.string.trade_result_declined), name), Toast.LENGTH_LONG).show();
				pendingTradeRequest = null;
				break;
			case Cancel:
				Toast.makeText(activity(), "Cancelled", Toast.LENGTH_LONG).show();
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
			if (!currentTrade.die) {
				currentTrade.toRun.add(new Runnable() {
					@Override
					public void run() {
						if (currentTrade == null)
							return;
						currentTrade.cancelTrade();
						currentTrade = null;
					}
				});
			}
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
				if (SteamService.singleton.schema == null)
					yesButton.setEnabled(false);
			}
		}
		if (currentTrade != null) {
			String name = activity().steamFriends.getFriendPersonaName(currentTrade.otherID);
			String text = String.format(activity().getString(R.string.trade_currently_trading), name);
			if (currentTrade.initiated)
				text += "\n" + activity().getString(R.string.trade_tap_to_view);
			else
				progress.setVisibility(View.VISIBLE);
			statusText.setText(text);
		}
	}

	@Override
	public void onClick(View v) {
		if (v == tradeStatus && v.getVisibility() == View.VISIBLE && currentTrade != null && currentTrade.initiated) {
			Fragment fragment = new FragmentTrade();
			activity().browseToFragment(fragment, false);
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
