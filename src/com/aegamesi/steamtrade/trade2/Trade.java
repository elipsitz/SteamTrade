/*
 * The MIT License
 *
 * Copyright 2014 nosoop < nosoop at users.noreply.github.com >.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.aegamesi.steamtrade.trade2;

import com.nosoop.steamtrade.TradeSession;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Excerpt snippet to show use of SteamTrade-Java. In this case, we receive a
 * notification to start a trade from somewhere and we use the provided
 * TradeListener instance to start a trade.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public class Trade {
	/**
	 * To access the trading page, we must know the following... -- The SteamID
	 * of our currently signed-in user (in long value format) -- The
	 * Base64-encoded current session identifier from Steam. -- The Steam login
	 * token used for Steam Web services.
	 * <p/>
	 * Again, you'll probably want to use a reverse-engineered Steam library to
	 * access this information and the notification to know when a trade session
	 * is starting. It's probably the only way, actually, so. Eh.
	 */
	public long ourSteamId;
	public long otherSteamId;
	public String sessionid;
	public String token;

	public TradeSession session;
	public UserTradeListener listener;

	private List<Runnable> runnables = new ArrayList<Runnable>();

	/**
	 * Receives a callback notifying us that a trade has started.
	 */
	public void beginTrade() {
		// TradeSession currentTrade; // The current trade.

		// Opens a new trade session to be handled by the given TradeListener.
		(new Thread(new Runnable() {
			@Override
			public void run() {
				listener = new UserTradeListener();
				session = new TradeSession(
						ourSteamId, otherSteamId,
						sessionid, token, listener);

				// Start a new thread in the background that polls the thread for updates.
				(new Thread(new TradePoller(session))).start();
			}
		})).start();
	}

	public void cancelTrade() {
		runnables.add(new Runnable() {
			@Override
			public void run() {
				try {
					session.getCmds().cancelTrade();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void run(Runnable runnable) {
		runnables.add(runnable);
	}

	private class TradePoller implements Runnable {
		public TradeSession session;

		public TradePoller(TradeSession session) {
			this.session = session;
		}

		@Override
		public void run() {
			while (true) {
				if (session == null)
					break;
				session.run();
				while (runnables.size() > 0)
					runnables.remove(0).run();
				try {
					Thread.sleep(1500);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
				if (!listener.active)
					break;
			}
		}
	}
}