package com.aegamesi.steamtrade.fragments.support;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamTwoFactor;

public class SteamGuardCodeView extends LinearLayout {
	public double time_until_next_code;
	private TextView textCode;
	private ProgressBar progressBar;
	private byte[] shared_secret = null;

	public SteamGuardCodeView(Context context) {
		super(context);
		init();
	}

	public SteamGuardCodeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public void setSharedSecret(byte[] shared_secret) {
		this.shared_secret = shared_secret;
	}

	private void init() {
		inflate(getContext(), R.layout.view_steamguard_code, this);
		this.textCode = (TextView) findViewById(R.id.steamguard_code);
		this.progressBar = (ProgressBar) findViewById(R.id.steamguard_time);

		time_until_next_code = 10000;
		progressBar.setMax(3000);
		post(new Runnable() {
			@Override
			public void run() {
				// update here, change code if necessary...
				if (shared_secret != null) {
					double validityTime = 30.0 - SteamTwoFactor.getCodeValidityTime();

					int progress = (int) (3000.0 * (validityTime / 30.0));
					progressBar.setProgress(progress);

					boolean new_code = time_until_next_code > validityTime;
					time_until_next_code = validityTime;
					if (new_code) {
						long time = SteamTwoFactor.getCurrentTime();
						String authCode = SteamTwoFactor.generateAuthCodeForTime(shared_secret, time);
						textCode.setText(authCode);
					}
				}

				postDelayed(this, 50);
			}
		});
	}
}
