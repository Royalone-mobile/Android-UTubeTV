package com.klogicapps.tv.miscellaneous;

import android.content.Context;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.DefaultAdListener;

public class AdViewListenerAmazon extends DefaultAdListener {
	protected Context mContext;
	protected AmazonInterstitialAd mInterstitialAd;
	protected AdLayout mAdLayout;

	public AdViewListenerAmazon(Context context, AmazonInterstitialAd adview) {
		this.mContext = context;
		this.mInterstitialAd = adview;
	}

	public AdViewListenerAmazon(Context context, AdLayout adview) {
		this.mContext = context;
		this.mAdLayout = adview;
	}

	@Override
	public void onAdLoaded(Ad ad, AdProperties adProperties) {
		super.onAdLoaded(ad, adProperties);
		if (mInterstitialAd != null) {
			if (ad == mInterstitialAd) {
				mInterstitialAd.showAd();
			}
		}
		if ( mAdLayout != null) {
			if (ad == mAdLayout) { }
		}
	}


	@Override
	public void onAdFailedToLoad(Ad ad, AdError error) {
		if (mInterstitialAd != null) {
			if (ad == mInterstitialAd) {
				//ActivityImagesViewPager.loadGoogleAdBB(mContext);
			}
		}
		if ( mAdLayout != null) {
			if (ad == mAdLayout) {
				//ActivityTip.loadGoogleAdBB(mContext);
			}
		}
	}

	/*
	@Override
	public void onAdCollapsed(Ad ad) {
		super.onAdCollapsed(ad);
	}

	@Override
	public void onAdDismissed(Ad ad) {
		super.onAdDismissed(ad);
	}

	@Override
	public void onAdExpanded(Ad ad) {
		super.onAdExpanded(ad);
	}
	 */
}