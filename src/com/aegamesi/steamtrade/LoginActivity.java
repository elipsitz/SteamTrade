package com.aegamesi.steamtrade;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.fragments.FragmentEula;
import com.aegamesi.steamtrade.steam.SteamMessageHandler;
import com.aegamesi.steamtrade.steam.SteamService;

import java.util.Locale;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EUniverse;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.SteamFriends;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.callbacks.LoggedOnCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamuser.callbacks.LoginKeyCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;
import uk.co.thomasc.steamkit.steam3.steamclient.callbacks.ConnectedCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbacks.DisconnectedCallback;
import uk.co.thomasc.steamkit.util.cSharp.events.ActionT;

public class LoginActivity extends ActionBarActivity {
	private UserLoginTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	public static String username;
	public static String password;
	private String steamGuard;

	// UI references.
	private EditText textUsername;
	private EditText textPassword;
	private EditText textSteamguard;

	public static EResult result = null;
	public boolean attemptReconnect = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		getSupportActionBar().hide();

		// show the eula
		FragmentEula eula = new FragmentEula();
		if (eula.shouldCreateDialog(this))
			eula.show(getSupportFragmentManager(), "tag");

		// go to main activity if already logged in
		if (SteamService.singleton != null && SteamService.singleton.steamClient != null && SteamService.singleton.steamClient.getConnectedUniverse() != null && SteamService.singleton.steamClient.getConnectedUniverse() != EUniverse.Invalid) {
			Intent intent = new Intent(LoginActivity.this, MainActivity.class);
			LoginActivity.this.startActivity(intent);
			finish();
			return;
		}

		if (getIntent().getExtras() != null) {
			attemptReconnect = getIntent().getExtras().getBoolean("attemptReconnect");
		}

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

		findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		// show any potential warnings...
		int show_warning = -1;
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork == null || !activeNetwork.isConnected())
			show_warning = R.string.not_connected_to_internet;
		else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
			show_warning = R.string.connected_via_mobiledata;
		if (show_warning != -1) {
			findViewById(R.id.login_warning).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.login_warning_text)).setText(show_warning);
		} else {
			findViewById(R.id.login_warning).setVisibility(View.GONE);
		}

		if (!SteamService.running || SteamService.singleton == null) {
			Intent intent = new Intent(getApplicationContext(), SteamService.class);
			startService(intent);
		}

		// attempt to reconnect if disconnected
		if (attemptReconnect) {
			Log.d("Login", "Attempting to reconnect");
			attemptLogin();
		}
	}

	public void attemptLogin() {
		if (mAuthTask != null)
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
		steamGuard = textSteamguard.getText().toString();
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
				editor.commit();
			}

			mAuthTask = new UserLoginTask();
			mAuthTask.execute();
		}
	}

	// asynchronous login
	public class UserLoginTask extends AsyncTask<Void, String, EResult> implements SteamMessageHandler {
		private ProgressDialog dialog;

		@Override
		protected EResult doInBackground(Void... params) {
			SteamService.singleton.messageHandler = this;
			Bundle bundle = new Bundle();
			bundle.putString("username", username);
			bundle.putString("password", password);
			bundle.putString("steamguard", steamGuard);
			publishProgress(getString(R.string.connecting));
			SteamService.singleton.attemptLogon(bundle);

			// busy-waiting
			result = null;
			while (true) {
				String message = "";

				if (result != null && (result != EResult.OK || (result == EResult.OK && SteamService.singleton.token != null)))
					return result;
				if (SteamService.singleton.steamClient.getConnectedUniverse() != null && SteamService.singleton.steamClient.getConnectedUniverse() != EUniverse.Invalid)
					message = getString(R.string.logging_on);
				if (result == EResult.OK && SteamService.singleton.token == null)
					message = getString(R.string.authenticating);

				if (message.length() > 0)
					publishProgress(message);
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		protected void onProgressUpdate(String... progress) {
			dialog.setMessage(progress[0]);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = new ProgressDialog(LoginActivity.this);
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected void onPostExecute(final EResult status) {
			mAuthTask = null;
			if (dialog != null)
				dialog.dismiss();

			if (status == EResult.InvalidPassword) {
				textPassword.setError(getString(R.string.error_incorrect_password));
				textPassword.requestFocus();
			} else if (status == EResult.ConnectFailed) {
				Toast.makeText(LoginActivity.this, R.string.cannot_connect_to_steam, Toast.LENGTH_LONG).show();
			} else if (status == EResult.ServiceUnavailable) {
				Toast.makeText(LoginActivity.this, R.string.cannot_auth_with_steamweb, Toast.LENGTH_LONG).show();
			} else if (status == EResult.AccountLogonDenied || status == EResult.AccountLogonDeniedNoMailSent || status == EResult.AccountLogonDeniedVerifiedEmailRequired) {
				textSteamguard.setVisibility(View.VISIBLE);
				textSteamguard.setError(getString(R.string.error_steamguard_required));
				textSteamguard.requestFocus();
				Toast.makeText(LoginActivity.this, "SteamGuard", Toast.LENGTH_LONG).show();
			} else if (status == EResult.InvalidLoginAuthCode) {
				textSteamguard.setVisibility(View.VISIBLE);
				textSteamguard.setError(getString(R.string.error_incorrect_steamguard));
				textSteamguard.requestFocus();
			} else if (status != EResult.OK) {
				// who knows what this is. perhaps a bug report will reveal
				Toast.makeText(LoginActivity.this, "Cannot Login: " + status.toString(), Toast.LENGTH_LONG).show();
			} else {
				SteamService.singleton.steamClient.getHandler(SteamFriends.class).setPersonaState(EPersonaState.Online);
				Intent intent = new Intent(LoginActivity.this, MainActivity.class);
				intent.putExtra("isLoggingIn", true);
				LoginActivity.this.startActivity(intent);
				finish();
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
		}

		@Override
		public void handleSteamMessage(CallbackMsg msg) {
			msg.handle(LoggedOnCallback.class, new ActionT<LoggedOnCallback>() {
				@Override
				public void call(LoggedOnCallback callback) {
					result = callback.getResult();
					if (result == EResult.OK)
						SteamService.singleton.webapiKey = callback.getWebAPIUserNonce();
				}
			});
			msg.handle(ConnectedCallback.class, new ActionT<ConnectedCallback>() {
				@Override
				public void call(ConnectedCallback callback) {
					if (callback.getResult() != EResult.OK)
						result = EResult.ConnectFailed;
				}
			});
			msg.handle(DisconnectedCallback.class, new ActionT<DisconnectedCallback>() {
				@Override
				public void call(DisconnectedCallback callback) {
					if (result == null)
						result = EResult.ConnectFailed;
				}
			});
			msg.handle(LoginKeyCallback.class, new ActionT<LoginKeyCallback>() {
				@Override
				public void call(LoginKeyCallback callback) {
					SteamService.singleton.authenticate(callback);
				}
			});
		}
	}
}
