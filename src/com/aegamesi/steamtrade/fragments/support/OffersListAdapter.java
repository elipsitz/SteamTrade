package com.aegamesi.steamtrade.fragments.support;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.aegamesi.lib.android.ExpandableHeightGridView;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.FragmentOffersList;
import com.aegamesi.steamtrade.fragments.support.OffersListAdapter.ViewHolderOffer;
import com.aegamesi.steamtrade.steam.tradeoffers.TradeOfferInfo;
import com.aegamesi.steamtrade.steam.tradeoffers.TradeOfferInfo.ETradeOfferState;

import java.util.List;

import uk.co.thomasc.steamkit.base.generated.steamlanguage.EAccountType;
import uk.co.thomasc.steamkit.base.generated.steamlanguage.EUniverse;
import uk.co.thomasc.steamkit.types.steamid.SteamID;

public class OffersListAdapter extends RecyclerView.Adapter<ViewHolderOffer> {
	public FragmentOffersList fragment;
	public List<TradeOfferInfo> offers = null;

	public OffersListAdapter(FragmentOffersList fragment) {
		this.fragment = fragment;
	}


	@Override
	public ViewHolderOffer onCreateViewHolder(ViewGroup parent, int viewType) {
		return new ViewHolderOffer(parent);
	}

	@Override
	public void onBindViewHolder(ViewHolderOffer h, int position) {
		TradeOfferInfo info = offers.get(position);
		h.itemView.setTag(info.getTradeofferid());
		String partner_name = fragment.activity().steamFriends.getFriendPersonaName(new SteamID((int) info.getAccountid_other(), EUniverse.Public, EAccountType.Individual));

		h.textHeading.setText(String.format(fragment.getString(info.is_our_offer() ? R.string.offer_sent_heading : R.string.offer_received_heading), partner_name));
		h.textSubtitle.setText(fragment.getResources().getStringArray(R.array.offer_status)[info.getTrade_offer_state().v()]);
		if (info.getMessage() != null) {
			h.textMessage.setVisibility(View.VISIBLE);
			h.textMessage.setText("\"" + info.getMessage() + "\"");
		} else {
			h.textMessage.setVisibility(View.GONE);
		}

		// buttons
		h.buttonRespond.setTag(position);
		h.buttonCancel.setTag(position);
		h.buttonDecline.setTag(position);
		h.buttonProfile.setTag(position);
		h.buttonRespond.setOnClickListener(fragment);
		h.buttonCancel.setOnClickListener(fragment);
		h.buttonDecline.setOnClickListener(fragment);
		h.buttonProfile.setOnClickListener(fragment);
		h.buttonRespond.setVisibility((info.is_our_offer() || info.getTrade_offer_state() != ETradeOfferState.Active) ? View.GONE : View.VISIBLE);
		h.buttonDecline.setVisibility(info.is_our_offer() ? View.GONE : View.VISIBLE);
		h.buttonCancel.setVisibility(info.is_our_offer() ? View.VISIBLE : View.GONE);


		ItemListAdapter adapter_will_give = new ItemListAdapter(fragment.activity(), h.itemListWillGive, false, null);
		adapter_will_give.setItemList(info.getItems_to_give());
		adapter_will_give.setListMode(ItemListAdapter.MODE_GRID);
		h.itemListWillGive.setAdapter(adapter_will_give);
		h.itemListWillGive.setExpanded(true);
		ItemListAdapter adapter_will_receive = new ItemListAdapter(fragment.activity(), h.itemListWillReceive, false, null);
		adapter_will_receive.setItemList(info.getItems_to_receive());
		adapter_will_receive.setListMode(ItemListAdapter.MODE_GRID);
		h.itemListWillReceive.setAdapter(adapter_will_receive);
		h.itemListWillReceive.setExpanded(true);
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public int getItemCount() {
		return offers == null ? 0 : offers.size();
	}


	public class ViewHolderOffer extends ViewHolder {
		public TextView textHeading;
		public TextView textSubtitle;
		public TextView textMessage;
		public Button buttonRespond;
		public Button buttonCancel;
		public Button buttonDecline;
		public Button buttonProfile;
		public ExpandableHeightGridView itemListWillGive;
		public ExpandableHeightGridView itemListWillReceive;

		public ViewHolderOffer(ViewGroup parent) {
			super(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_offerslist_card, parent, false));

			textHeading = (TextView) itemView.findViewById(R.id.offer_heading);
			textSubtitle = (TextView) itemView.findViewById(R.id.offer_subtitle);
			textMessage = (TextView) itemView.findViewById(R.id.offer_message);
			buttonRespond = (Button) itemView.findViewById(R.id.offer_button_respond);
			buttonCancel = (Button) itemView.findViewById(R.id.offer_button_cancel);
			buttonDecline = (Button) itemView.findViewById(R.id.offer_button_decline);
			buttonProfile = (Button) itemView.findViewById(R.id.offer_button_profile);
			itemListWillGive = (ExpandableHeightGridView) itemView.findViewById(R.id.offer_items_give);
			itemListWillReceive = (ExpandableHeightGridView) itemView.findViewById(R.id.offer_items_receive);
		}
	}
}
