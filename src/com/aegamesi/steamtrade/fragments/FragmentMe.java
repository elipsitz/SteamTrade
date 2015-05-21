package com.aegamesi.steamtrade.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.Locale;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;

public class FragmentMe extends FragmentBase implements OnClickListener, OnItemSelectedListener {
	public ImageView avatarView;
	public TextView nameView;
	public Spinner statusSpinner;
	public Button changeNameButton;
	public Button changeGameButton;

	public int[] states = new int[]{1, 3, 2, 4, 5, 6}; // online, away, busy, snooze, lookingtotrade, lookingtoplay

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_me, container, false);
		avatarView = (ImageView) view.findViewById(R.id.profile_avatar);
		nameView = (TextView) view.findViewById(R.id.profile_name);
		statusSpinner = (Spinner) view.findViewById(R.id.profile_status_spinner);
		changeNameButton = (Button) view.findViewById(R.id.me_set_name);
		changeGameButton = (Button) view.findViewById(R.id.me_set_game);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity(), R.array.allowed_states, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		statusSpinner.setAdapter(adapter);
		statusSpinner.setOnItemSelectedListener(this);
		changeNameButton.setOnClickListener(this);
		changeGameButton.setOnClickListener(this);
		//changeGameButton.setEnabled(false);

		updateView();
		return view;
	}

	public void updateView() {
		activity().getSupportActionBar().setTitle(activity().steamFriends.getPersonaName());
		EPersonaState state = activity().steamFriends.getPersonaState();
		String name = activity().steamFriends.getPersonaName();
		String avatar = SteamUtil.bytesToHex(activity().steamFriends.getFriendAvatar(SteamService.singleton.steamClient.getSteamId())).toLowerCase(Locale.US);

		activity().getSupportActionBar().setTitle(name);
		nameView.setText(name);
		statusSpinner.setSelection(stateToIndex(state));

		avatarView.setImageResource(R.drawable.default_avatar);
		if (!avatar.equals("0000000000000000000000000000000000000000"))
			ImageLoader.getInstance().displayImage("http://media.steampowered.com/steamcommunity/public/images/avatars/" + avatar.substring(0, 2) + "/" + avatar + "_full.jpg", avatarView);

		nameView.setTextColor(SteamUtil.colorOnline);
	}

	public int stateToIndex(EPersonaState state) {
		for (int i = 0; i < states.length; i++)
			if (states[i] == state.v())
				return i;
		return 0;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (parent == statusSpinner) {
			activity().steamFriends.setPersonaState(EPersonaState.f(states[pos]));
			updateView();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	@Override
	public void onClick(View v) {
		if (v == changeNameButton) {
			//SteamFriends f = activity().steamFriends;
			//f.requestFriendInfo(new SteamID(76561198000739785L));
			AlertDialog.Builder alert = new AlertDialog.Builder(activity());
			alert.setTitle(activity().getString(R.string.change_display_name));
			alert.setMessage(activity().getString(R.string.change_display_name_prompt));
			final EditText input = new EditText(activity());
			input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			input.setText(activity().steamFriends.getPersonaName());
			alert.setView(input);
			alert.setPositiveButton(R.string.change, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String name = input.getText().toString().trim();
					if (name.length() != 0) {
						activity().steamFriends.setPersonaName(name);
						updateView();
					}
				}
			});
			alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			alert.show();
		}
		if (v == changeGameButton) {
			Toast.makeText(activity(), R.string.feature_not_implemented, Toast.LENGTH_LONG).show();
		}
	}
}