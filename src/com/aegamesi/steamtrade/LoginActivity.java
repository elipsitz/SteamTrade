package com.aegamesi.steamtrade;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.fragments.FragmentEula;
import com.aegamesi.steamtrade.steam.SteamConnectionListener;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EUniverse;

public class LoginActivity extends AppCompatActivity {
	// Values for email and password at the time of the login attempt.
	public static String username;
	public static String password;
	private boolean need_twofactor = false;
	// UI references.
	private CheckBox rememberInfoCheckbox;
	private EditText textUsername;
	private EditText textPassword;
	private EditText textSteamguard;

	private View headerSaved;
	private View headerNew;
	private View viewSaved;
	private View viewNew;
	private View cardSaved;
	private View cardNew;
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

		// prepare "drawers"
		headerNew = findViewById(R.id.header_new);
		headerSaved = findViewById(R.id.header_saved);
		viewNew = findViewById(R.id.layout_new);
		viewSaved = findViewById(R.id.layout_saved);
		cardNew = findViewById(R.id.card_new);
		cardSaved = findViewById(R.id.card_saved);
		OnClickListener cardListener = new OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean isNew = view == headerNew;
				boolean isSaved = view == headerSaved;

				headerNew.setVisibility(isNew ? View.GONE : View.VISIBLE);
				viewNew.setVisibility(isNew ? View.VISIBLE : View.GONE);
				((LayoutParams) cardNew.getLayoutParams()).weight = isNew ? 1 : 0;

				headerSaved.setVisibility(isSaved ? View.GONE : View.VISIBLE);
				viewSaved.setVisibility(isSaved ? View.VISIBLE : View.GONE);
				((LayoutParams) cardSaved.getLayoutParams()).weight = isSaved ? 1 : 0;
			}
		};
		headerNew.setOnClickListener(cardListener);
		headerSaved.setOnClickListener(cardListener);
		RecyclerView accountsList = (RecyclerView) findViewById(R.id.accounts_list);
		AccountListAdapter accountListAdapter = new AccountListAdapter();
		accountsList.setAdapter(accountListAdapter);
		accountsList.setLayoutManager(new LinearLayoutManager(this));
		if (accountListAdapter.getItemCount() == 0) {
			// only show the new one
			cardListener.onClick(headerNew);
			cardSaved.setVisibility(View.GONE);
		} else {
			cardListener.onClick(headerSaved);
		}

		// prepare login form
		rememberInfoCheckbox = ((CheckBox) findViewById(R.id.remember));
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

		// show legacy information
		if (accountListAdapter.getItemCount() == 0) {
			if (getPreferences(MODE_PRIVATE).getBoolean("rememberDetails", true)) {
				textUsername.setText(getPreferences(MODE_PRIVATE).getString("username", ""));
				textPassword.setText(getPreferences(MODE_PRIVATE).getString("password", ""));
			}
		}

		Button buttonSignIn = (Button) findViewById(R.id.sign_in_button);
		buttonSignIn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});
	}

	public void loginWithSavedAccount(String username) {
		// start the logging in progess
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
		if(prefs.contains("loginkey_" + username)) {
			Bundle bundle = new Bundle();
			bundle.putString("username", username);
			bundle.putBoolean("loginkey", true);
			bundle.putBoolean("remember", true);
			connectionListener.handle_result = true;
			SteamService.attemptLogon(LoginActivity.this, connectionListener, bundle, true);
		} else {
			String password = prefs.getString("password_" + username, "");
			Bundle bundle = new Bundle();
			bundle.putString("username", username);
			bundle.putString("password", password);
			bundle.putBoolean("remember", true);
			connectionListener.handle_result = true;
			SteamService.attemptLogon(LoginActivity.this, connectionListener, bundle, true);
		}
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
			// log in
			if (progressDialog != null)
				progressDialog.dismiss();

			// start the logging in progess
			Bundle bundle = new Bundle();
			bundle.putString("username", username);
			bundle.putString("password", password);
			bundle.putString("steamguard", steamGuard);
			bundle.putBoolean("remember", rememberInfoCheckbox.isChecked());
			bundle.putBoolean("twofactor", need_twofactor);
			connectionListener.handle_result = true;
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
		private boolean handle_result = true;

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

					if(!handle_result)
						return;
					handle_result = false;

					if (result == EResult.InvalidPassword) {
						// maybe change error to "login key expired, log in again" if using loginkey
						if(SteamService.extras != null && SteamService.extras.getBoolean("loginkey", false)) {
							headerNew.performClick();
							SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
							String password = prefs.getString("password_" + SteamService.extras.getString("username"), "");

							Toast.makeText(LoginActivity.this, R.string.error_loginkey_expired, Toast.LENGTH_LONG).show();
							textPassword.setError(getString(R.string.error_loginkey_expired));
							textUsername.setText(SteamService.extras.getString("username"));
							textPassword.setText(password);
							rememberInfoCheckbox.setChecked(true);
						} else {
							textPassword.setError(getString(R.string.error_incorrect_password));
							textPassword.requestFocus();
						}
					} else if (result == EResult.ConnectFailed) {
						Toast.makeText(LoginActivity.this, R.string.cannot_connect_to_steam, Toast.LENGTH_SHORT).show();
					} else if (result == EResult.ServiceUnavailable) {
						Toast.makeText(LoginActivity.this, R.string.cannot_auth_with_steamweb, Toast.LENGTH_LONG).show();
					} else if (result == EResult.AccountLogonDenied || result == EResult.AccountLogonDeniedNoMail || result == EResult.AccountLogonDeniedVerifiedEmailRequired || result == EResult.AccountLoginDeniedNeedTwoFactor) {
						textSteamguard.setVisibility(View.VISIBLE);
						textSteamguard.setError(getString(R.string.error_steamguard_required));
						textSteamguard.requestFocus();
						Toast.makeText(LoginActivity.this, "SteamGuard: " + result.name(), Toast.LENGTH_LONG).show();

						need_twofactor = result == EResult.AccountLoginDeniedNeedTwoFactor;
					} else if (result == EResult.InvalidLoginAuthCode || result == EResult.TwoFactorCodeMismatch) {
						textSteamguard.setVisibility(View.VISIBLE);
						textSteamguard.setError(getString(R.string.error_incorrect_steamguard));
						textSteamguard.requestFocus();

						need_twofactor = result == EResult.TwoFactorCodeMismatch;
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

	private class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.AccountViewHolder> {
		public List<String> accountNames = new ArrayList<String>();
		private SharedPreferences prefs;

		public AccountListAdapter() {
			prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
			Map<String, ?> allPrefs = prefs.getAll();
			for (String key : allPrefs.keySet()) {
				if (key.startsWith("loginkey_")) {
					String accountName = key.substring("loginkey_".length());
					if(!accountNames.contains(accountName))
						accountNames.add(accountName);
				}
				if (key.startsWith("password_")) {
					String accountName = key.substring("password_".length());
					if(!accountNames.contains(accountName))
						accountNames.add(accountName);
				}
			}


			notifyDataSetChanged();
		}

		@Override
		public AccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return new AccountViewHolder(parent);
		}

		@Override
		public void onBindViewHolder(AccountViewHolder holder, int position) {
			String name = accountNames.get(position);
			holder.name.setText(name);

			String avatarURL = prefs.getString("avatar_" + name, null);
			holder.avatar.setImageResource(R.drawable.default_avatar);
			if (avatarURL != null)
				ImageLoader.getInstance().displayImage(avatarURL, holder.avatar);

			holder.buttonRemove.setTag(position);
			holder.itemView.setTag(position);
		}

		@Override
		public int getItemCount() {
			return accountNames == null ? 0 : accountNames.size();
		}

		class AccountViewHolder extends ViewHolder implements OnClickListener {
			public CircleImageView avatar;
			public TextView name;
			public ImageButton buttonRemove;

			public AccountViewHolder(ViewGroup parent) {
				super(LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_login_account, parent, false));

				name = (TextView) itemView.findViewById(R.id.account_name);
				avatar = (CircleImageView) itemView.findViewById(R.id.account_avatar);
				buttonRemove = (ImageButton) itemView.findViewById(R.id.account_delete);

				itemView.setOnClickListener(this);
				buttonRemove.setOnClickListener(this);
			}

			@Override
			public void onClick(View view) {
				String name = accountNames.get(getAdapterPosition());

				if (view.getId() == R.id.account_delete) {
					AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
					builder.setNegativeButton(R.string.cancel, null);
					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							String name = accountNames.remove(getAdapterPosition());
							notifyItemRemoved(getAdapterPosition());

							PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().remove("loginkey_" + name).apply();
						}
					});
					builder.setMessage(String.format(getString(R.string.login_confirm_delete_account), name));
					builder.show();
				}
				if (view.getId() == R.id.account) {
					loginWithSavedAccount(name);
				}
			}
		}
	}
}
