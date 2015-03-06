package com.aegamesi.steamtrade.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.loopj.android.image.SmartImageView;

import java.util.Locale;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentProfile extends FragmentBase implements View.OnClickListener {
	public FriendListAdapter adapter;

	public SteamID id;
	public EPersonaState state;
	public String name;
	public String game;
	public String avatar;

	public SmartImageView avatarView;
	public TextView nameView;
	public TextView statusView;
	public Button chatButton;
	public Button tradeButton;
	public Button tradeOfferButton;
	public Button inventoryButton;
	public Button removeFriendButton;
	public Button viewSteamButton;
	public Button viewSteamRepButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		id = new SteamID(getArguments().getLong("steamId"));
		// http://steamcommunity.com/id/therealpickleman/?xml=1&l=english

		fragmentName = "FragmentProfile";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_profile, container, false);

		avatarView = (SmartImageView) view.findViewById(R.id.profile_avatar);
		nameView = (TextView) view.findViewById(R.id.profile_name);
		statusView = (TextView) view.findViewById(R.id.profile_status);
		chatButton = (Button) view.findViewById(R.id.profile_button_chat);
		tradeButton = (Button) view.findViewById(R.id.profile_button_trade);
		tradeOfferButton = (Button) view.findViewById(R.id.profile_button_tradeoffer);
		inventoryButton = (Button) view.findViewById(R.id.profile_button_inventory);
		removeFriendButton = (Button) view.findViewById(R.id.profile_button_remove_friend);
		viewSteamButton = (Button) view.findViewById(R.id.profile_button_viewsteam);
		viewSteamRepButton = (Button) view.findViewById(R.id.profile_button_viewsteamrep);

		chatButton.setOnClickListener(this);
		tradeButton.setOnClickListener(this);
		tradeOfferButton.setOnClickListener(this);
		inventoryButton.setOnClickListener(this);
		removeFriendButton.setOnClickListener(this);
		viewSteamButton.setOnClickListener(this);
		viewSteamRepButton.setOnClickListener(this);

		nameView.setSelected(true);
		statusView.setSelected(true);

		updateView();
		return view;
	}

	@Override
	public void onClick(View view) {
		if (view == removeFriendButton) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity());
			builder.setMessage(String.format(getString(R.string.friend_remove_message), activity().steamFriends.getFriendPersonaName(id)));
			builder.setTitle(R.string.friend_remove);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					activity().steamFriends.removeFriend(FragmentProfile.this.id);
					Toast.makeText(activity(), String.format(getString(R.string.friend_removed), activity().steamFriends.getFriendPersonaName(FragmentProfile.this.id)), Toast.LENGTH_LONG).show();
					activity().browseToFragment(new FragmentFriends(), false);
				}
			});
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				}
			});
			builder.create().show();
		}
		if (view == chatButton) {
			Fragment fragment = new FragmentChat();
			Bundle bundle = new Bundle();
			bundle.putLong("steamId", id.convertToLong());
			bundle.putBoolean("fromProfile", true);
			fragment.setArguments(bundle);
			activity().browseToFragment(fragment, true);
		}
		if (view == tradeButton) {
			SteamService.singleton.tradeManager.trade(id);
		}
		if (view == tradeOfferButton) {
			Fragment fragment = new FragmentOffer();
			Bundle bundle = new Bundle();
			bundle.putBoolean("from_existing", false);
			bundle.putLong("user_id", id.getAccountID()); // getAccountID *NOT* convertToLong
			bundle.putString("token", null);
			fragment.setArguments(bundle);
			activity().browseToFragment(fragment, true);
		}
		if (view == inventoryButton) {
			Fragment fragment = new FragmentInventory();
			Bundle bundle = new Bundle();
			bundle.putLong("steamId", id.convertToLong());
			fragment.setArguments(bundle);
			activity().browseToFragment(fragment, true);
		}
		if (view == viewSteamButton) {
			Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(SteamUtil.generateCommunityURL(id)));
			startActivity(browse);
		}
		if (view == viewSteamRepButton) {
			String steamRepUrl = "http://steamrep.com/profiles/" + id.convertToLong() + "/";
			Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(steamRepUrl));
			startActivity(browse);
		}
	}

	public void updateView() {
		if(activity() == null || activity().steamFriends == null)
			return;

		state = activity().steamFriends.getFriendPersonaState(id);
		name = activity().steamFriends.getFriendPersonaName(id);
		game = activity().steamFriends.getFriendGamePlayedName(id);
		avatar = SteamUtil.bytesToHex(activity().steamFriends.getFriendAvatar(id)).toLowerCase(Locale.US);

		activity().getSupportActionBar().setTitle(name);
		nameView.setText(name);

		avatarView.setImageResource(R.drawable.default_avatar);
		if (avatar != null && avatar.length() == 40 && !avatar.equals("0000000000000000000000000000000000000000"))
			avatarView.setImageUrl("http://media.steampowered.com/steamcommunity/public/images/avatars/" + avatar.substring(0, 2) + "/" + avatar + "_full.jpg");

		if (game != null && game.length() > 0)
			statusView.setText("Playing " + game);
		else
			statusView.setText(state.toString());
		if (game != null && game.length() > 0) {
			// 8BC53F (AED04E ?) game
			nameView.setTextColor(SteamUtil.colorGame);
			statusView.setTextColor(SteamUtil.colorGame);
			avatarView.setBackgroundColor(SteamUtil.colorGame);
		} else if (state == EPersonaState.Offline || state == null) {
			// 898989 (CFD2D3 ?) offline
			nameView.setTextColor(SteamUtil.colorOffline);
			statusView.setTextColor(SteamUtil.colorOffline);
			avatarView.setBackgroundColor(SteamUtil.colorOffline);
		} else {
			// 86B5D9 (9CC6FF ?) online
			nameView.setTextColor(SteamUtil.colorOnline);
			statusView.setTextColor(SteamUtil.colorOnline);
			avatarView.setBackgroundColor(SteamUtil.colorOnline);
		}
	}
}