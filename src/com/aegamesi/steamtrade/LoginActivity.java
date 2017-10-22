package com.aegamesi.steamtrade;

import android.app.Activity;
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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import com.aegamesi.steamtrade.fragments.support.SteamGuardCodeView;
import com.aegamesi.steamtrade.steam.AccountLoginInfo;
import com.aegamesi.steamtrade.steam.SteamConnectionListener;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamTwoFactor;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EUniverse;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;

public class LoginActivity extends AppCompatActivity {
	private static final int REQUEST_CODE_LOAD_MAFILE = 48399;
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
		handleLegacy();
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

		Button buttonOverflow = (Button) findViewById(R.id.button_overflow);
		buttonOverflow.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				handleOverflowMenu(view);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_LOAD_MAFILE) {
			if (resultCode == Activity.RESULT_OK) {
				try {
					StringBuilder b = new StringBuilder();
					InputStream is = getContentResolver().openInputStream(data.getData());
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String s;
					while ((s = reader.readLine()) != null) {
						b.append(s);
						b.append("\n");
					}

					final AccountLoginInfo acc = new AccountLoginInfo();
					acc.importFromJson(b.toString());
					AccountLoginInfo existing_acc = AccountLoginInfo.readAccount(this, acc.tfa_accountName);
					if (existing_acc != null) {
						existing_acc.importFromJson(b.toString());
						existing_acc.has_authenticator = true;
						AccountLoginInfo.writeAccount(LoginActivity.this, existing_acc);
						Toast.makeText(LoginActivity.this, R.string.action_successful, Toast.LENGTH_LONG).show();
					} else {
						acc.username = acc.tfa_accountName;
						AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setTitle(R.string.steamguard_mobile_authenticator);
						builder.setMessage(String.format(getString(R.string.steamguard_import_password), acc.username));
						builder.setCancelable(true);
						final EditText passwordInput = new EditText(this);
						builder.setView(passwordInput);
						passwordInput.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD);
						builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								acc.password = passwordInput.getText().toString();
								acc.has_authenticator = true;
								AccountLoginInfo.writeAccount(LoginActivity.this, acc);
								Toast.makeText(LoginActivity.this, R.string.action_successful, Toast.LENGTH_LONG).show();
								recreate();
							}
						});
						builder.show();
					}

				} catch (IOException | JSONException | NumberFormatException e) {
					e.printStackTrace();
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(R.string.error);
					builder.setMessage(e.toString());
					builder.setCancelable(true);
					builder.setNeutralButton(R.string.ok, null);
					builder.show();
				}
			}
		}
	}

	private void handleOverflowMenu(View view) {
		PopupMenu popup = new PopupMenu(this, view);
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
					case R.id.item_import_authenticator:
						SteamTwoFactor.promptForMafile(LoginActivity.this, REQUEST_CODE_LOAD_MAFILE);
						break;
				}
				return true;
			}
		});
		popup.inflate(R.menu.login);
		popup.show();
	}

	private void handleLegacy() {
		// from version 0.10.4 onwards:
		// convert from old account storage to new account storage
		Set<String> savedAccounts = new HashSet<>();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
		Map<String, ?> allPrefs = sharedPreferences.getAll();
		for (String key : allPrefs.keySet()) {
			if (key.startsWith("loginkey_"))
				savedAccounts.add(key.substring("loginkey_".length()));
			if (key.startsWith("password_"))
				savedAccounts.add(key.substring("password_".length()));
		}
		SharedPreferences.Editor editor = sharedPreferences.edit();
		for (String savedAccount : savedAccounts) {
			AccountLoginInfo loginInfo = new AccountLoginInfo();
			loginInfo.username = savedAccount;
			loginInfo.password = sharedPreferences.getString("password_" + savedAccount, null);
			loginInfo.loginkey = sharedPreferences.getString("loginkey_" + savedAccount, null);
			loginInfo.avatar = sharedPreferences.getString("avatar_" + savedAccount, null);
			AccountLoginInfo.writeAccount(this, loginInfo);
			editor.remove("loginkey_" + savedAccount);
			editor.remove("password_" + savedAccount);
		}
		editor.apply();
		// end
	}

	public void loginWithSavedAccount(AccountLoginInfo account) {
		// start the logging in progess
		Bundle bundle = new Bundle();
		bundle.putString("username", account.username);
		bundle.putBoolean("remember", true);

		if (account.loginkey != null) {
			bundle.putString("loginkey", account.loginkey);
		} else {
			bundle.putString("password", account.password);
		}

		connectionListener.handle_result = true;
		SteamService.attemptLogon(LoginActivity.this, connectionListener, bundle, true);
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

	private void showAndFillManualLogin(String username) {
		headerNew.setVisibility(View.GONE);
		viewNew.setVisibility(View.VISIBLE);
		((LayoutParams) cardNew.getLayoutParams()).weight = 1;

		headerSaved.setVisibility(View.VISIBLE);
		viewSaved.setVisibility(View.GONE);
		((LayoutParams) cardSaved.getLayoutParams()).weight = 0;


		AccountLoginInfo info = AccountLoginInfo.readAccount(this, username);
		if (info != null) {
			textUsername.setText(info.username);
			textPassword.setText(info.password);

			if (info.has_authenticator) {
				String code = SteamTwoFactor.generateAuthCodeForTime(info.tfa_sharedSecret, SteamTwoFactor.getCurrentTime());
				textSteamguard.setText(code);
			}
		}
		rememberInfoCheckbox.setChecked(true);
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

					if (!handle_result)
						return;
					handle_result = false;

					if (result == EResult.InvalidPassword) {
						// maybe change error to "login key expired, log in again" if using loginkey
						if (SteamService.extras != null && SteamService.extras.getString("loginkey") != null) {
							Toast.makeText(LoginActivity.this, R.string.error_loginkey_expired, Toast.LENGTH_LONG).show();
							textPassword.setError(getString(R.string.error_loginkey_expired));

							String username = SteamService.extras.getString("username");
							showAndFillManualLogin(username);
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

						String username = SteamService.extras.getString("username");
						showAndFillManualLogin(username);

						need_twofactor = result == EResult.AccountLoginDeniedNeedTwoFactor;
					} else if (result == EResult.InvalidLoginAuthCode || result == EResult.TwoFactorCodeMismatch) {
						textSteamguard.setVisibility(View.VISIBLE);
						textSteamguard.setError(getString(R.string.error_incorrect_steamguard));
						textSteamguard.requestFocus();

						String username = SteamService.extras.getString("username");
						showAndFillManualLogin(username);

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
							progressDialog.setCancelable(true);
							progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
								@Override
								public void onCancel(DialogInterface dialog) {
									if (SteamService.singleton != null) {
										SteamService.singleton.kill();
									}
								}
							});
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
		public List<AccountLoginInfo> accounts = new ArrayList<>();

		public AccountListAdapter() {
			accounts = AccountLoginInfo.getAccountList(LoginActivity.this);
			notifyDataSetChanged();
		}

		@Override
		public AccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return new AccountViewHolder(parent);
		}

		@Override
		public void onBindViewHolder(AccountViewHolder holder, int position) {
			AccountLoginInfo account = accounts.get(position);
			holder.name.setText(account.username);

			holder.buttonKey.setVisibility(account.has_authenticator ? View.VISIBLE : View.GONE);
			holder.avatar.setImageResource(R.drawable.default_avatar);
			if (account.avatar != null)
				ImageLoader.getInstance().displayImage(account.avatar, holder.avatar);

			holder.buttonRemove.setTag(position);
			holder.itemView.setTag(position);
			holder.buttonKey.setTag(position);
		}

		@Override
		public int getItemCount() {
			return accounts == null ? 0 : accounts.size();
		}

		class AccountViewHolder extends ViewHolder implements OnClickListener {
			public CircleImageView avatar;
			public TextView name;
			public ImageButton buttonRemove;
			public ImageButton buttonKey;

			public AccountViewHolder(ViewGroup parent) {
				super(LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_login_account, parent, false));

				name = (TextView) itemView.findViewById(R.id.account_name);
				avatar = (CircleImageView) itemView.findViewById(R.id.account_avatar);
				buttonRemove = (ImageButton) itemView.findViewById(R.id.account_delete);
				buttonKey = (ImageButton) itemView.findViewById(R.id.account_key);

				itemView.setOnClickListener(this);
				buttonRemove.setOnClickListener(this);
				buttonKey.setOnClickListener(this);
			}

			@Override
			public void onClick(View view) {
				final AccountLoginInfo account = accounts.get(getAdapterPosition());

				if (view.getId() == R.id.account_delete) {
					AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
					builder.setNegativeButton(R.string.cancel, null);
					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							accounts.remove(getAdapterPosition());
							notifyItemRemoved(getAdapterPosition());

							AccountLoginInfo.removeAccount(LoginActivity.this, account.username);
						}
					});
					builder.setMessage(String.format(getString(R.string.login_confirm_delete_account), account.username));
					builder.show();
				}
				if (view.getId() == R.id.account) {
					loginWithSavedAccount(account);
				}
				if (view.getId() == R.id.account_key) {
					SteamGuardCodeView codeView = new SteamGuardCodeView(LoginActivity.this);
					codeView.setSharedSecret(account.tfa_sharedSecret);

					AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
					builder.setNeutralButton(R.string.ok, null);
					builder.setCancelable(true);
					builder.setView(codeView);
					builder.show();
				}
			}
		}
	}

}
