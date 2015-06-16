package com.aegamesi.steamtrade;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.fragments.FragmentEula;
import com.aegamesi.steamtrade.steam.SteamConnectionListener;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;

import java.util.Locale;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EUniverse;

public class LoginActivity extends AppCompatActivity {
	// Values for email and password at the time of the login attempt.
	public static String username;
	public static String password;
	private EResult lastResult = null;
	// UI references.
	private EditText textUsername;
	private EditText textPassword;
	private EditText textSteamguard;
	//
	private ConnectionListener connectionListener = null;
	private ProgressDialog progressDialog = null;
	private boolean active = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		if (getSupportActionBar() != null)
			getSupportActionBar().hide();

		// show the eula
		FragmentEula eula = new FragmentEula();
		if (eula.shouldCreateDialog(this))
			eula.show(getSupportFragmentManager(), "tag");

		connectionListener = new ConnectionListener();

		// prepare login form
		textSteamguard = (EditText) findViewById(R.id.steamguard);
		textSteamguard.setVisibility(View.GONE);
		textUsername = (EditText) findViewById(R.id.username);
		textPassword = (EditText) findViewById(R.id.password);
		textPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		if (getPreferences(MODE_PRIVATE).getBoolean("rememberDetails", true)) {
			textUsername.setText(getPreferences(MODE_PRIVATE).getString("username", ""));
			textPassword.setText(getPreferences(MODE_PRIVATE).getString("password", ""));
		}

		Button buttonSignIn = (Button) findViewById(R.id.sign_in_button);
		buttonSignIn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});
	}

	public void attemptLogin() {
		// TODO do not do this if we're already trying to connect-- fix this
		if (progressDialog != null)
			return;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork == null || !activeNetwork.isConnected())
			Toast.makeText(this, R.string.not_connected_to_internet, Toast.LENGTH_LONG).show();

		textUsername.setError(null);
		textPassword.setError(null);
		textSteamguard.setError(null);

		// Store values at the time of the login attempt.
		username = textUsername.getText().toString();
		password = textPassword.getText().toString();
		String steamGuard = textSteamguard.getText().toString();
		steamGuard = steamGuard.trim().toUpperCase(Locale.US);

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(password)) {
			textPassword.setError(getString(R.string.error_field_required));
			focusView = textPassword;
			cancel = true;
		}
		// Check for a valid username.
		if (TextUtils.isEmpty(username)) {
			textUsername.setError(getString(R.string.error_field_required));
			focusView = textUsername;
			cancel = true;
		}
		if (TextUtils.isEmpty(steamGuard))
			steamGuard = null;

		if (cancel) {
			focusView.requestFocus();
		} else {
			if (((CheckBox) findViewById(R.id.remember)).isChecked()) {
				SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
				editor.putString("username", username);
				editor.putString("password", password);
				editor.apply();
			}

			// log in
			if (progressDialog != null)
				progressDialog.dismiss();

			// start the logging in progess
			Bundle bundle = new Bundle();
			bundle.putString("username", username);
			bundle.putString("password", password);
			bundle.putString("steamguard", steamGuard);
			bundle.putBoolean("twofactor", lastResult == EResult.AccountLoginDeniedNeedTwoFactor || lastResult == EResult.TwoFactorCodeMismatch);
			SteamService.attemptLogon(LoginActivity.this, connectionListener, bundle, true);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		active = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		active = true;

		// go to main activity if already logged in
		if (SteamService.singleton != null && SteamService.singleton.steamClient != null && SteamService.singleton.steamClient.getConnectedUniverse() != null && SteamService.singleton.steamClient.getConnectedUniverse() != EUniverse.Invalid) {
			Intent intent = new Intent(this, MainActivity.class);
			if (getIntent() != null) {
				// forward our intent (there may be a better way to do this)
				intent.setAction(getIntent().getAction());
				intent.setData(getIntent().getData());
			}
			startActivity(intent);
			finish();
			active = false;
			return;
		}

		// show any potential warnings...
		int show_warning = -1;
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork == null || !activeNetwork.isConnected()) {
			show_warning = R.string.not_connected_to_internet;
		}
		// if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
		//	show_warning = R.string.connected_via_mobiledata;

		TextView textLoginWarning = (TextView) findViewById(R.id.login_warning_text);
		if (show_warning != -1) {
			textLoginWarning.setVisibility(View.VISIBLE);
			textLoginWarning.setText(show_warning);
		} else {
			textLoginWarning.setVisibility(View.GONE);
		}

		// set self as the steam connection listener
		SteamService.connectionListener = connectionListener;
	}

	private class ConnectionListener implements SteamConnectionListener {
		@Override
		public void onConnectionResult(final EResult result) {
			Log.i("ConnectionListener", "Connection result: " + result);

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (!active)
						return;

					if (progressDialog != null && progressDialog.isShowing())
						progressDialog.dismiss();
					progressDialog = null;

					lastResult = result;
					if (result == EResult.InvalidPassword) {
						textPassword.setError(getString(R.string.error_incorrect_password));
						textPassword.requestFocus();
					} else if (result == EResult.ConnectFailed) {
						Toast.makeText(LoginActivity.this, R.string.cannot_connect_to_steam, Toast.LENGTH_LONG).show();
					} else if (result == EResult.ServiceUnavailable) {
						Toast.makeText(LoginActivity.this, R.string.cannot_auth_with_steamweb, Toast.LENGTH_LONG).show();
					} else if (result == EResult.AccountLogonDenied || result == EResult.AccountLogonDeniedNoMail || result == EResult.AccountLogonDeniedVerifiedEmailRequired || result == EResult.AccountLoginDeniedNeedTwoFactor) {
						textSteamguard.setVisibility(View.VISIBLE);
						textSteamguard.setError(getString(R.string.error_steamguard_required));
						textSteamguard.requestFocus();
						Toast.makeText(LoginActivity.this, "SteamGuard: " + result.name(), Toast.LENGTH_LONG).show();
					} else if (result == EResult.InvalidLoginAuthCode || result == EResult.TwoFactorCodeMismatch) {
						textSteamguard.setVisibility(View.VISIBLE);
						textSteamguard.setError(getString(R.string.error_incorrect_steamguard));
						textSteamguard.requestFocus();
					} else if (result != EResult.OK) {
						// who knows what this is. perhaps a bug report will reveal
						Toast.makeText(LoginActivity.this, "Cannot Login: " + result.toString(), Toast.LENGTH_LONG).show();
					} else {
						if (SteamUtil.webApiKey.length() == 0) {
							Toast.makeText(LoginActivity.this, R.string.error_getting_key, Toast.LENGTH_LONG).show();
						}

						Intent intent = new Intent(LoginActivity.this, MainActivity.class);
						intent.putExtra("isLoggingIn", true);
						LoginActivity.this.startActivity(intent);
						finish();
					}
				}
			});
		}

		@Override
		public void onConnectionStatusUpdate(final int status) {
			Log.i("ConnectionListener", "Status update: " + status);

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (!active)
						return;

					if (status != STATUS_CONNECTED && status != STATUS_FAILURE) {
						if (progressDialog == null || !progressDialog.isShowing()) {
							progressDialog = new ProgressDialog(LoginActivity.this);
							progressDialog.setCancelable(false);
							progressDialog.show();
						}
					}

					String[] statuses = getResources().getStringArray(R.array.connection_status);
					if (progressDialog != null)
						progressDialog.setMessage(statuses[status]);
				}
			});
		}
	}
}
