package com.klogicapps.tv.miscellaneous;

import android.content.Context;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

public class AdViewListenerGoogle extends AdListener {
	protected Context mContext;
	protected AdView mAdView;
	protected InterstitialAd mInterstitialAd;

	public AdViewListenerGoogle(Context context, AdView adview) {
		this.mContext = context;
		this.mAdView = adview;
	}

	public AdViewListenerGoogle(Context context, InterstitialAd adview) {
		this.mContext = context;
		this.mInterstitialAd = adview;
	}

	@Override
	public void onAdLoaded() {
		//  Toast.makeText(mContext, "onAdLoaded()", Toast.LENGTH_SHORT).show();

		if (mAdView != null) {mAdView.setVisibility(View.VISIBLE);}

		if (mInterstitialAd != null) {mInterstitialAd.show();}

	}

	@Override
	public void onAdFailedToLoad(int errorCode) {
		@SuppressWarnings("unused")
		String errorReason = "";
		switch(errorCode) {
		case AdRequest.ERROR_CODE_INTERNAL_ERROR:
			errorReason = "Internal error";
			break;
		case AdRequest.ERROR_CODE_INVALID_REQUEST:
			errorReason = "Invalid request";
			break;
		case AdRequest.ERROR_CODE_NETWORK_ERROR:
			errorReason = "Network Error";
			break;
		case AdRequest.ERROR_CODE_NO_FILL:
			errorReason = "No fill";
			break;
		}
		// Toast.makeText(mContext, String.format("onAdFailedToLoad(%s)", errorReason),
		//     Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onAdOpened() {

	}

	@Override
	public void onAdClosed() { 

	}

	@Override
	public void onAdLeftApplication() { 

	}
}