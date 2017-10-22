package com.aegamesi.steamtrade.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.lib.android.AndroidUtil;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.SteamGuardCodeView;
import com.aegamesi.steamtrade.steam.AccountLoginInfo;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamTwoFactor;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import uk.co.thomasc.steamkit.base.generated.SteammessagesTwofactorSteamclient.CTwoFactor_AddAuthenticator_Response;
import uk.co.thomasc.steamkit.base.generated.SteammessagesTwofactorSteamclient.CTwoFactor_FinalizeAddAuthenticator_Response;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EResult;
import uk.co.thomasc.steamkit.steam3.handlers.steamunifiedmessages.callbacks.UnifiedMessageResponseCallback;
import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;
import uk.co.thomasc.steamkit.types.steamid.SteamID;
import uk.co.thomasc.steamkit.util.cSharp.events.ActionT;

public class FragmentSteamGuard extends FragmentBase implements OnClickListener {
	public static final int REQUEST_CODE_LOAD = 48235;
	public static final int REQUEST_CODE_SAVE = 48236;

	public SteamGuardCodeView steamGuardCodeView;
	public View steamGuardCodeCard;
	public TextView textSteamGuardStatus;
	public Button buttonSteamGuardManage;
	public Button buttonRevocationCode;
	public Button buttonPort;

	private int authenticator_finalize_attempts = 30;
	private long authenticator_finalize_time = 0;
	private String authenticator_finalize_code;
	private byte[] authenticator_finalize_secret;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (abort)
			return;
		Log.i("FragmentSteamGuard", "created");
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		msg.handle(UnifiedMessageResponseCallback.class, new ActionT<UnifiedMessageResponseCallback>() {
			@Override
			public void call(UnifiedMessageResponseCallback obj) {
				/*if (obj.getMethodName().equals("TwoFactor.Status#1")) {
					CTwoFactor_Status_Response response = obj.getProtobuf(CTwoFactor_Status_Response.class);
				}*/

				if (obj.getMethodName().equals("TwoFactor.AddAuthenticator#1")) {
					CTwoFactor_AddAuthenticator_Response response = obj.getProtobuf(CTwoFactor_AddAuthenticator_Response.class);
					Log.d("FragmentSteamGuard", response.toString());

					EResult status = EResult.f(response.status);
					if (status == EResult.OK) {
						// don't set "has authenticator" but save all of the data
						AccountLoginInfo info = getAccountLoginInfo();
						info.tfa_sharedSecret = response.sharedSecret;
						info.tfa_serialNumber = Long.toString(response.serialNumber);
						info.tfa_revocationCode = response.revocationCode;
						info.tfa_uri = response.uri;
						info.tfa_serverTime = response.serverTime;
						info.tfa_accountName = response.accountName;
						info.tfa_tokenGid = response.tokenGid;
						info.tfa_identitySecret = response.identitySecret;
						info.tfa_secret1 = response.secret1;
						saveAccountLoginInfo(info);

						authenticator_finalize_secret = response.sharedSecret;

						AlertDialog.Builder alert = new AlertDialog.Builder(activity());
						alert.setTitle(R.string.steamguard_mobile_authenticator);
						alert.setMessage(R.string.steamguard_instructions_finalize);
						final EditText input = new EditText(activity());
						input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
						alert.setView(input);
						alert.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								authenticator_finalize_code = input.getText().toString().trim();
								authenticator_finalize_attempts = 30;
								authenticator_finalize_time = SteamTwoFactor.getCurrentTime();
								finalizeAuthenticator();
							}
						});
						alert.setNegativeButton(R.string.cancel, null);
						alert.show();
					} else {
						String errorMessage = String.format(getString(R.string.steamguard_enable_failure), status.name());
						if (status == EResult.DuplicateRequest) {
							errorMessage = getString(R.string.steamguard_duplicate);
						}

						AndroidUtil.showBasicAlert(activity(), getString(R.string.error), errorMessage, null);
					}
				}

				if (obj.getMethodName().equals("TwoFactor.FinalizeAddAuthenticator#1")) {
					CTwoFactor_FinalizeAddAuthenticator_Response response = obj.getProtobuf(CTwoFactor_FinalizeAddAuthenticator_Response.class);

					if (response.serverTime != 0)
						authenticator_finalize_time = response.serverTime;

					EResult status = EResult.f(response.status);
					Log.i("FragmentSteamGuard", "FinalizeAddAuthenticator: " + status);
					if (response.success) {
						if (response.wantMore) {
							authenticator_finalize_attempts--;
							authenticator_finalize_time += 30;
							finalizeAuthenticator();
						} else {
							// success!
							AccountLoginInfo info = getAccountLoginInfo();
							info.has_authenticator = true;
							saveAccountLoginInfo(info);
							updateView();


							AndroidUtil.showBasicAlert(activity(),
									getString(R.string.steamguard_mobile_authenticator),
									String.format(getString(R.string.steamguard_enable_success), info.tfa_revocationCode),
									null);
						}
					} else {
						AndroidUtil.showBasicAlert(activity(),
								getString(R.string.error),
								String.format(getString(R.string.steamguard_enable_failure), status.name()),
								null);
					}
				}
			}
		});
	}

	private void finalizeAuthenticator() {
		String code = SteamTwoFactor.generateAuthCodeForTime(authenticator_finalize_secret, authenticator_finalize_time);
		Log.i("FragmentSteamGuard", "Attempting finalization with code " + code);
		activity().steamUser.finalizeTwoFactor(authenticator_finalize_code, code);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		final View view = inflater.inflate(R.layout.fragment_steamguard, container, false);

		steamGuardCodeView = (SteamGuardCodeView) view.findViewById(R.id.steamguard_code_view);
		textSteamGuardStatus = (TextView) view.findViewById(R.id.steamguard_status);
		buttonSteamGuardManage = (Button) view.findViewById(R.id.steamguard_manage);
		steamGuardCodeCard = view.findViewById(R.id.steamguard_code_card);
		buttonRevocationCode = (Button) view.findViewById(R.id.steamguard_revocation);
		buttonPort = (Button) view.findViewById(R.id.steamguard_port);

		buttonSteamGuardManage.setOnClickListener(this);
		buttonRevocationCode.setOnClickListener(this);
		buttonPort.setOnClickListener(this);

		updateView();
		return view;
	}

	public void updateView() {
		if (activity() == null || activity().steamUser == null)
			return;
		if (SteamService.singleton == null || SteamService.singleton.steamClient == null)
			return;

		if (hasAuthenticator()) {
			textSteamGuardStatus.setVisibility(View.GONE);
			steamGuardCodeCard.setVisibility(View.VISIBLE);
			buttonSteamGuardManage.setText(R.string.steamguard_manage_authenticator);
			buttonPort.setText(R.string.steamguard_export_authenticator);
			buttonRevocationCode.setVisibility(View.VISIBLE);

			steamGuardCodeView.setSharedSecret(getAccountLoginInfo().tfa_sharedSecret);
		} else {
			textSteamGuardStatus.setVisibility(View.VISIBLE);
			steamGuardCodeCard.setVisibility(View.GONE);
			buttonSteamGuardManage.setText(R.string.steamguard_enable_authenticator);
			buttonPort.setText(R.string.steamguard_import_authenticator);
			buttonRevocationCode.setVisibility(View.GONE);
		}
	}

	private AccountLoginInfo getAccountLoginInfo() {
		String username = SteamService.singleton.username;
		return AccountLoginInfo.readAccount(activity(), username);
	}

	private void saveAccountLoginInfo(AccountLoginInfo info) {
		AccountLoginInfo.writeAccount(activity(), info);
	}

	private boolean hasAuthenticator() {
		AccountLoginInfo accountLoginInfo = getAccountLoginInfo();
		return accountLoginInfo != null && accountLoginInfo.has_authenticator;
	}

	@Override
	public void onClick(View v) {
		if (v == buttonSteamGuardManage) {
			if (hasAuthenticator()) {
				// https://store.steampowered.com/twofactor/manage
				// go to that webpage
				String url = "https://store.steampowered.com/twofactor/manage/";
				FragmentWeb.openPage(activity(), url, true);
			} else {
				String deviceId = SteamTwoFactor.generateDeviceID(SteamService.singleton.steamClient.getSteamId());
				activity().steamUser.enableTwoFactor(deviceId);
				// activity().steamUser.requestTwoFactorStatus();
			}
		}
		if (v == buttonRevocationCode) {
			AccountLoginInfo info = getAccountLoginInfo();

			if (info != null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(activity());
				builder.setTitle(R.string.steamguard_mobile_authenticator);
				builder.setMessage(String.format(getString(R.string.steamguard_revocation_code), info.tfa_revocationCode));
				builder.setCancelable(true);
				builder.setNeutralButton(R.string.ok, null);
				builder.show();
			}
		}
		if (v == buttonPort) {
			if (hasAuthenticator()) {
				AccountLoginInfo info = getAccountLoginInfo();
				SteamID id = SteamService.singleton.steamClient.getSteamId();

				String json = info.exportToJson(id);
				String filename = "mafiles/" + id.convertToLong() + ".mafile";
				try {
					AndroidUtil.createCachedFile(activity(), filename, json);
				} catch (IOException e) {
					e.printStackTrace();
				}

				File mafile = new File(activity().getCacheDir(), filename);
				Uri contentUri = FileProvider.getUriForFile(getContext(), "com.aegamesi.steamtrade.fileprovider", mafile);

				// export
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
				sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				sendIntent.setType("application/json");
				startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.steamguard_export_authenticator)));
			} else {
				SteamTwoFactor.promptForMafile(activity(), REQUEST_CODE_LOAD);
			}
		}
	}

	@Override
	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_LOAD) {
			boolean success = false;
			if (resultCode == Activity.RESULT_OK) {

				try {
					StringBuilder b = new StringBuilder();
					InputStream is = activity().getContentResolver().openInputStream(data.getData());
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String s;
					while ((s = reader.readLine()) != null) {
						b.append(s);
						b.append("\n");
					}

					AccountLoginInfo accountLoginInfo = getAccountLoginInfo();
					if (accountLoginInfo != null) {
						accountLoginInfo.importFromJson(b.toString());

						if (accountLoginInfo.tfa_accountName.equals(accountLoginInfo.username)) {
							accountLoginInfo.has_authenticator = true;
							saveAccountLoginInfo(accountLoginInfo);

							AlertDialog.Builder builder = new AlertDialog.Builder(activity());
							builder.setTitle(R.string.steamguard_mobile_authenticator);
							builder.setMessage(String.format(getString(R.string.steamguard_enable_success), accountLoginInfo.tfa_revocationCode));
							builder.setCancelable(true);
							builder.setNeutralButton(R.string.ok, null);
							builder.show();
							success = true;

							updateView();
						} else {
							AlertDialog.Builder builder = new AlertDialog.Builder(activity());
							builder.setTitle(R.string.steamguard_mobile_authenticator);
							builder.setMessage(String.format(getString(R.string.steamguard_import_error_wrong_account), accountLoginInfo.tfa_accountName));
							builder.setCancelable(true);
							builder.setNeutralButton(R.string.ok, null);
							builder.show();
						}
					}
				} catch (IOException | JSONException | NumberFormatException e) {
					e.printStackTrace();
					AlertDialog.Builder builder = new AlertDialog.Builder(activity());
					builder.setTitle(R.string.error);
					builder.setMessage(e.toString());
					builder.setCancelable(true);
					builder.setNeutralButton(R.string.ok, null);
					builder.show();
				}
			}
			if (!success) {
				Toast.makeText(activity(), R.string.error, Toast.LENGTH_SHORT).show();
				Log.d("ImportAuthenticator", "import failed? " + resultCode);
			}
		}
		return false;
	}
}