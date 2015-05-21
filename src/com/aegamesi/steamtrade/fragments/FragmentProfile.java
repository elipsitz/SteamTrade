package com.aegamesi.steamtrade.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.lib.UILImageGetter;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.steam.SteamService;
import com.aegamesi.steamtrade.steam.SteamUtil;
import com.aegamesi.steamtrade.steam.SteamWeb;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EFriendRelationship;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EPersonaState;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.PersonaStateCallback;
import uk.co.thomasc.steamkit.steam3.handlers.steamfriends.callbacks.ProfileInfoCallback;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class FragmentProfile extends FragmentBase implements View.OnClickListener {
	public SteamID id;
	public int steam_level = -1;
	public EFriendRelationship relationship;
	public EPersonaState state;
	public String name;
	public String game;
	public String avatar;

	public CircleImageView avatarView;
	public TextView nameView;
	public TextView statusView;
	public TextView summaryView;
	public TextView levelView;
	public Button chatButton;
	public Button tradeButton;
	public Button tradeOfferButton;
	public Button inventoryButton;
	public Button addFriendButton;
	public Button removeFriendButton;
	public Button viewSteamButton;
	public Button viewSteamRepButton;

	public ProfileInfoCallback profile_info = null;
	public PersonaStateCallback persona_info = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		if (getArguments().containsKey("steamId")) {
			id = new SteamID(getArguments().getLong("steamId"));
		} else {
			String url = getArguments().getString("url");

			Matcher matcher = Pattern.compile("steamcommunity.com/id/([a-zA-Z0-9]+)").matcher(url);
			if (matcher.find()) {
				id = null;
				String vanity = matcher.group(1);
				(new ResolveVanityURLTask()).execute(vanity);
			} else {
				matcher = Pattern.compile("steamcommunity.com/profiles/([0-9]+)").matcher(url);
				if (matcher.find())
					id = new SteamID(Long.parseLong(matcher.group(1)));
				else
					id = null;
			}
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_profile, container, false);

		avatarView = (CircleImageView) view.findViewById(R.id.profile_avatar);
		nameView = (TextView) view.findViewById(R.id.profile_name);
		statusView = (TextView) view.findViewById(R.id.profile_status);
		summaryView = (TextView) view.findViewById(R.id.profile_summary);
		levelView = (TextView) view.findViewById(R.id.profile_level);
		chatButton = (Button) view.findViewById(R.id.profile_button_chat);
		tradeButton = (Button) view.findViewById(R.id.profile_button_trade);
		tradeOfferButton = (Button) view.findViewById(R.id.profile_button_tradeoffer);
		inventoryButton = (Button) view.findViewById(R.id.profile_button_inventory);
		removeFriendButton = (Button) view.findViewById(R.id.profile_button_remove_friend);
		addFriendButton = (Button) view.findViewById(R.id.profile_button_add_friend);
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
		summaryView.setMovementMethod(LinkMovementMethod.getInstance());

		updateView();
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		requestInfo();
		// fetch the data
		//(new ProfileFetchTask()).execute();
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
		if (view == addFriendButton) {
			activity().steamFriends.addFriend(id);
			Toast.makeText(activity(), R.string.friend_add_success, Toast.LENGTH_LONG).show();
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
		if (activity() == null || activity().steamFriends == null)
			return;

		relationship = activity().steamFriends.getFriendRelationship(id);
		if (relationship == EFriendRelationship.Friend || persona_info == null) {
			state = activity().steamFriends.getFriendPersonaState(id);
			relationship = activity().steamFriends.getFriendRelationship(id);
			name = activity().steamFriends.getFriendPersonaName(id);
			game = activity().steamFriends.getFriendGamePlayedName(id);
			avatar = SteamUtil.bytesToHex(activity().steamFriends.getFriendAvatar(id)).toLowerCase(Locale.US);
		} else {
			// use the found persona info stuff
			state = persona_info.getState();
			relationship = activity().steamFriends.getFriendRelationship(id);
			name = persona_info.getName();
			game = persona_info.getGameName();
			avatar = SteamUtil.bytesToHex(persona_info.getAvatarHash()).toLowerCase(Locale.US);
		}

		if (profile_info != null) {
			String summary = profile_info.getSummary();
			summary = SteamUtil.parseBBCode(summary);
			Html.ImageGetter imageGetter = new UILImageGetter(summaryView, summaryView.getContext());
			summaryView.setText(Html.fromHtml(summary, imageGetter, null));
			//Linkify.addLinks(summaryView, Linkify.WEB_URLS);
		}

		if (steam_level == -1) {
			levelView.setText(R.string.unknown);
		} else {
			levelView.setText(steam_level + "");
		}

		activity().getSupportActionBar().setTitle(name);
		nameView.setText(name);

		avatarView.setImageResource(R.drawable.default_avatar);
		if (avatar != null && avatar.length() == 40 && !avatar.equals("0000000000000000000000000000000000000000"))
			ImageLoader.getInstance().displayImage("http://media.steampowered.com/steamcommunity/public/images/avatars/" + avatar.substring(0, 2) + "/" + avatar + "_full.jpg", avatarView);

		if (game != null && game.length() > 0)
			statusView.setText("Playing " + game);
		else
			statusView.setText(state.toString());

		int color = SteamUtil.colorOnline;
		if (relationship == EFriendRelationship.Blocked || relationship == EFriendRelationship.Ignored || relationship == EFriendRelationship.IgnoredFriend)
			color = SteamUtil.colorBlocked;
		else if (game != null && game.length() > 0)
			color = SteamUtil.colorGame;
		else if (state == EPersonaState.Offline || state == null)
			color = SteamUtil.colorOffline;
		nameView.setTextColor(color);
		statusView.setTextColor(color);
		avatarView.setBorderColor(color);

		// things to do if we are not friends
		boolean isFriend = relationship == EFriendRelationship.Friend;
		if (!isFriend) {
			statusView.setText(relationship.toString());
			addFriendButton.setEnabled(id != null);
		}

		// visibility of buttons and stuff
		addFriendButton.setVisibility(!isFriend ? View.VISIBLE : View.GONE);
		removeFriendButton.setVisibility(isFriend ? View.VISIBLE : View.GONE);
		chatButton.setVisibility(isFriend ? View.VISIBLE : View.GONE);
		tradeButton.setVisibility(isFriend ? View.VISIBLE : View.GONE);
		tradeOfferButton.setVisibility(isFriend ? View.VISIBLE : View.GONE);
	}

	public void updateProfile(ProfileInfoCallback obj) {
		profile_info = obj;
		updateView();
	}

	public void updatePersona(PersonaStateCallback obj) {
		persona_info = obj;
		updateView();
	}

	public void updateLevel(int level) {
		steam_level = level;
		updateView();
	}

	private void requestInfo() {
		if (id != null) {
			if (profile_info == null)
				activity().steamFriends.requestProfileInfo(id);
			int request_flags = 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512 | 1024;
			relationship = activity().steamFriends.getFriendRelationship(id);
			if (relationship != EFriendRelationship.Friend && persona_info == null)
				activity().steamFriends.requestFriendInfo(id, request_flags);
			if (steam_level == -1)
				activity().steamFriends.requestSteamLevel(id);
		}
	}

	private class ResolveVanityURLTask extends AsyncTask<String, Void, SteamID> {
		@Override
		protected SteamID doInBackground(String... args) {
			String vanity = args[0];

			String api_url = "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=" + SteamUtil.apikey + "&format=json&vanityurl=" + vanity;
			String response = SteamWeb.fetch(api_url, "GET", null, "");
			if (response.length() == 0)
				return null;
			JSONObject response_obj;
			try {
				response_obj = new JSONObject(response);
				response_obj = response_obj.getJSONObject("response");
				int result = response_obj.getInt("success");
				if (result != 1)
					return null;

				String steam_id = response_obj.getString("steamid");
				long long_value = Long.parseLong(steam_id);
				return new SteamID(long_value);
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(SteamID id) {
			FragmentProfile.this.id = id;
			requestInfo();
			updateView();
		}
	}
}