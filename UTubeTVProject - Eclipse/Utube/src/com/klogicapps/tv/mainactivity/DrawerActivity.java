package com.klogicapps.tv.mainactivity;

// import org.codechimp.apprater.AppRater;

import java.util.Random;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
// import com.distantfuture.videos.cast.CastActivity;
// import com.distantfuture.videos.misc.ColorPickerFragment;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.inscription.WhatsNewDialog;
import com.keyes.youtube.OpenYouTubePlayerActivity;
import com.klogicapps.tv.R;
import com.klogicapps.tv.activities.SettingsActivity;
import com.klogicapps.tv.activities.ViewServerActivity;
import com.klogicapps.tv.channellookup.ChannelLookupActivity;
import com.klogicapps.tv.content.Content;
import com.klogicapps.tv.introactivity.IntroActivity;
import com.klogicapps.tv.misc.ActionBarSpinnerAdapter;
import com.klogicapps.tv.misc.BusEvents;
import com.klogicapps.tv.misc.Constants;
import com.klogicapps.tv.misc.MainApplication;
import com.klogicapps.tv.misc.Utils;
import com.klogicapps.tv.miscellaneous.AdViewListenerAmazon;
import com.klogicapps.tv.miscellaneous.AdViewListenerGoogle;
import com.klogicapps.tv.miscellaneous.AmazonInterstitialAd;
import com.klogicapps.tv.miscellaneous.KeyVariables;
import com.klogicapps.tv.miscellaneous.Network;
import com.klogicapps.tv.youtube.VideoPlayer;
import com.klogicapps.tv.youtube.YouTubeAPI;

import de.greenrobot.event.EventBus;

public class DrawerActivity extends ViewServerActivity implements DrawerActivitySupport {
	VideoPlayer mPlayer;
	private int mCurrentSection = -1;
	private DrawerManager mDrawerMgr;
	private Toast backButtonToast;
	private long lastBackPressTime = 0;
	private Content mContent;
	private ActionBarSpinnerAdapter mActionBarSpinnerAdapter;
	private boolean mSpinnerSucksBalls;

	// Interstitial Ad Stuff
	protected static InterstitialAd mInterstitial_Google;
	protected static AmazonInterstitialAd mInterstitial_Amazon;
	protected static Random mRandom;
	protected static int mRandomInt;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// we set the activity to NoActionBar in the manifest to avoid the title flickering in the actionbar
		setTheme(R.style.DrawerActivityTheme);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_drawer);

		mContent = Content.instance(this);

		MainApplication.mContext = this; 

		if (MainApplication.hasGooglePlayServices()){ KeyVariables.KEY_GOOGLE_PLAY = true; }
		else { KeyVariables.KEY_GOOGLE_PLAY = false; }

		if (MainApplication.OVERIDE_AD_SERVICES == 0){ KeyVariables.KEY_AD_CURRENT = KeyVariables.KEY_AD_NORMAL; }
		else if (MainApplication.OVERIDE_AD_SERVICES == 1){ KeyVariables.KEY_AD_CURRENT = KeyVariables.KEY_AD_GOOGLE; }
		else if (MainApplication.OVERIDE_AD_SERVICES == 2){ KeyVariables.KEY_AD_CURRENT = KeyVariables.KEY_AD_AMAZON; }
		else { KeyVariables.KEY_AD_CURRENT = KeyVariables.KEY_AD_NORMAL; }

		Log.i("GOOGLE PLAY SERVICES AVAILABILITY", "Is google play services available: " + Boolean.valueOf(KeyVariables.KEY_GOOGLE_PLAY) );
		Log.i("OVERIDE AD SERVICES Integer", "Value is: " + Integer.valueOf(MainApplication.OVERIDE_AD_SERVICES) );
		Log.i("OVERIDE AD SERVICES String", "Value is: " + KeyVariables.KEY_AD_CURRENT );

		MainApplication.NETWORK_AVAILABLE = Network.isNetworkAvailable(MainApplication.mContext);

		if (mContent.needsChannelSwitcher()) {
			mActionBarSpinnerAdapter = new ActionBarSpinnerAdapter(this, mContent);
			ActionBar.OnNavigationListener listener = new ActionBar.OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int position, long itemId) {

					// be aware that this call back gets called when the spinner contents are built
					// we need to ignore that one, so not going to do anything if channel not changing
					if (!mSpinnerSucksBalls) {
						mSpinnerSucksBalls = true;

						// ## taking advantage of this feature/bug to set the real value of the actionbar spinner
						// if we don't do this, the spinner defaults to value 0, so selecting the first item
						// in the list will not work since it doesn't respond when selecting the same index as the current value
						getActionBar().setSelectedNavigationItem(mContent.currentChannelIndex());
					} else {
						if (mContent.changeChannel(position))
							updateSectionForChannel();
					}
					return true;
				}
			};

			getActionBar().setDisplayShowTitleEnabled(false);
			getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			getActionBar().setListNavigationCallbacks(mActionBarSpinnerAdapter, listener);
		}

		setupDrawer();

		// enable ActionBar app icon to behave as action to toggle nav drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);

		selectSection(mContent.savedSectionIndex(), false);

		if (Constants.showAppRater) {
			// MARKER REMOVED APPRATER   AppRater.app_launched(this);
		}

		// general app tweaks
		//    Debug.activateStrictMode();
		Utils.ignoreObsoleteCapacitiveMenuButton(this);

		// show player if activity was destroyed and recreated
		if (savedInstanceState != null) {
			VideoPlayer.PlayerParams params = savedInstanceState.getParcelable("player_params");

			if (params != null)
				playVideo(params);
		}

		WhatsNewDialog.showWhatsNew(this, false);

		// only show intro for multi channels
		if (mContent.needsChannelSwitcher())
			IntroActivity.showIntroDelayed(this, false);
	}

	@Override
	public void onStop() {
		super.onStop();

		// for AppUtils.THEME_CHANGED
		EventBus.getDefault().unregister(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		// for AppUtils.THEME_CHANGED
		EventBus.getDefault().register(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mPlayer != null) {
			if (mPlayer.visible()) {
				VideoPlayer.PlayerParams params = mPlayer.playerParams();

				if (params != null) {
					outState.putParcelable("player_params", params);
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item;

		getMenuInflater().inflate(R.menu.main, menu);

		// MARKER Removed chrome cast manager
		// MainApplication.getCastManager(this).addMediaRouterButton(menu, R.id.action_cast, this, true);

		if (!mContent.supportsDonate()) {
			// marker removed donate    item = menu.findItem(R.id.action_donate);
			// marker removed donate    item.setVisible(false);
		}

		if (!mContent.supportsChannelEditing()) {
			item = menu.findItem(R.id.action_channel_lookup);
			item.setVisible(false);
		}

		return super.onCreateOptionsMenu(menu);
	}

	private boolean closePlayerIfOpen() {
		if (mPlayer != null && mPlayer.visible()) {
			mPlayer.close();
			return true;
		}

		return false;
	}

	public void onEventMainThread(BusEvents.ThemeChanged event) {
		// animate doesn't work, puts new activity in the background.  use recreate instead
		boolean animate = false;
		if (animate) {
			ActivityOptions opts = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out);

			startActivity(getIntent(), opts.toBundle());

			finish();
		} else {
			// not sure how to to get recreate to animate, so we use the above code when animating which is like a recreate
			recreate();
		}
	}

	// FragmentActivity uses some supportFragment garbage
	// backbutton will close the activity rather than popBack a fragment
	public void superOnBackPressedHack() {
		if (!getFragmentManager().popBackStackImmediate()) {
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		// hides the video player if visible
		if (!closePlayerIfOpen()) {
			if (getFragmentManager().getBackStackEntryCount() == 0) {
				if (this.lastBackPressTime < System.currentTimeMillis() - 4000) {
					backButtonToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT);
					backButtonToast.show();
					this.lastBackPressTime = System.currentTimeMillis();
				} else {
					if (backButtonToast != null) {
						backButtonToast.cancel();
					}
					// this works around FragmentActivity incorrect behavior
					superOnBackPressedHack();
				}
			} else {
				// this works around FragmentActivity incorrect behavior
				superOnBackPressedHack();
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		// called when playing a movie, could fail and this dialog shows the user how to fix it
		case YouTubeAPI.REQ_PLAYER_CODE:
			if (resultCode != RESULT_OK) {
				YouTubeInitializationResult errorReason = YouTubeStandalonePlayer.getReturnedInitializationResult(data);
				if (errorReason.isUserRecoverableError()) {
					errorReason.getErrorDialog(this, 0).show();
				} else {
					String errorMessage = String.format("PLAYER ERROR!! - %s", errorReason.toString());
					Utils.toast(this, errorMessage);
				}
			}

			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		/* marker removed    MenuItem item = menu.findItem(R.id.action_show_hidden);

    if (item != null) {
      boolean showHidden = AppUtils.instance(this).showHiddenItems();

      item.setTitle((showHidden ? R.string.action_hide_hidden : R.string.action_show_hidden));
    }

    boolean showDevTools = AppUtils.instance(this).showDevTools();
    menu.setGroupVisible(R.id.dev_tools_group, showDevTools);

		 */ 

		if (MainApplication.deviceOSName.equals(KeyVariables.BLACKBERRY) ) {
			MenuItem item = menu.findItem(R.id.action_settings);
			item.setVisible(false); 
		}   



		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		// close player if open
		if (item.getItemId() == android.R.id.home) {
			// close player if back button in action bar hit
			if (closePlayerIfOpen())
				return true;
		}

		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerMgr.onOptionsItemSelected(item)) {
			return true;
		}
		int itemId = item.getItemId();
		if (itemId == R.id.action_settings) {
			SettingsActivity.show(DrawerActivity.this);
			return true;
		} else if (itemId == R.id.action_channel_lookup) {
			ChannelLookupActivity.show(this);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateSectionForChannel() {
		mCurrentSection = -1; // force it to reload fragment if same position
		selectSection(mContent.savedSectionIndex(), true);
	}

	private void setupDrawer() {
		mDrawerMgr = new DrawerManager(this, mContent, new DrawerManager.DrawerManagerListener() {
			@Override
			public void onChannelClick() {
				updateSectionForChannel();
			}

			@Override
			public void onDrawerClick(int position) {
				selectSection(position, true);
			}

			@Override
			public void onDrawerOpen(boolean opened) {
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		});

		// open on first launch?
		// mDrawerMgr.openDrawer();
	}

	private void selectSection(int position, boolean animate) {
		// short curcuit trying to select the same position
		if (mCurrentSection == position)
			return;

		mCurrentSection = position;

		mDrawerMgr.setItemChecked(position, true);

		// clear back stack when using drawer
		getFragmentManager().popBackStack();
		Utils.showFragment(this, mContent.fragmentForIndex(position), R.id.fragment_holder, animate ? 3 : 0, false);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerMgr.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		mDrawerMgr.onConfigurationChanged(newConfig);

		// the spinners title and subtitle change on different orientations
		// must tell the adaptor it's views need to be refreshed
		if (mActionBarSpinnerAdapter != null) {
			mActionBarSpinnerAdapter.notifyDataSetChanged();
		}
	}

	private void syncActionBarTitle() {
		YouTubeGridFragment fragment = currentYouTubeFragment();

		if (fragment != null)
			fragment.syncActionBarTitle();
	}

	private YouTubeGridFragment currentYouTubeFragment() {
		YouTubeGridFragment result = null;

		Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_holder);

		if (fragment instanceof YouTubeGridFragment)
			result = (YouTubeGridFragment) fragment;

		return result;
	}

	private VideoPlayer videoPlayer(boolean createIfNeeded) {
		if (createIfNeeded) {
			if (mPlayer == null) {
				mPlayer = new VideoPlayer(this, R.id.youtube_fragment, new VideoPlayer.VideoPlayerStateListener() {
					// called when the video player opens or closes, adjust the action bar title
					@Override
					public void stateChanged() {
						syncActionBarTitle();
					}
				});
			}
		}

		return mPlayer;
	}

	// DrawerActivitySupport
	@Override
	public void showDefaultFragment() {
		selectSection(1, true);
	}

	// DrawerActivitySupport
	@Override
	public void installFragment(Fragment fragment, boolean animate) {
		Utils.showFragment(this, fragment, R.id.fragment_holder, animate ? 1 : 0, true);
	}

	// DrawerActivitySupport
	@Override
	public void setActionBarTitle(CharSequence title, CharSequence subtitle) {
		if (mActionBarSpinnerAdapter != null) {
			mActionBarSpinnerAdapter.setTitleAndSubtitle(title, subtitle);
		} else {
			ActionBar bar = getActionBar();

			if (bar != null) {
				bar.setTitle(title);
				bar.setSubtitle(subtitle);
			}
		}
	}

	// DrawerActivitySupport
	@Override
	public boolean actionBarTitleHandled() {
		// if video player is up, show the video title
		VideoPlayer player = videoPlayer(false);
		if (player != null && player.visible()) {
			setActionBarTitle(getResources().getString(R.string.now_playing), player.title());

			return true;
		}

		return false;
	}

	// DrawerActivitySupport
	@Override
	public boolean isPlayerVisible() {
		VideoPlayer player = videoPlayer(false);
		return (player != null && player.visible());
	}

	// DrawerActivitySupport
	@Override
	public void playVideo(VideoPlayer.PlayerParams params) {
		// could use this, but we have no control for mute commericals and play next automatically etc.
		// YouTubeAPI.playMovie(this, videoId, true);

		mRandom = new Random();
		mRandomInt = mRandom.nextInt(3 - 1 + 1) + 1;
		Log.i("RANDOM INT", "CURRENT RANOM INT: " + Integer.toString(mRandomInt));


		if (!MainApplication.TESTING && MainApplication.NETWORK_AVAILABLE) {
			if (mRandomInt == 1) {
				Log.i("AD SERVICE", "ATTEMPTING TO LOAD");
				if (KeyVariables.KEY_AD_CURRENT.equals(KeyVariables.KEY_AD_NORMAL)) {
					if ( KeyVariables.KEY_GOOGLE_PLAY ) { loadGoogleAd(MainApplication.mContext); }
					else { loadAmazonAd(MainApplication.mContext); }
				}
				else if (KeyVariables.KEY_AD_CURRENT.equals(KeyVariables.KEY_AD_GOOGLE)) { loadGoogleAd(MainApplication.mContext); }
				else if (KeyVariables.KEY_AD_CURRENT.equals(KeyVariables.KEY_AD_AMAZON)) { loadAmazonAd(MainApplication.mContext); }
				else { Log.i("AD SERVICE", "NONE : SOMETHING HAPPENED!!!!"); }
			}
		}

		if (MainApplication.deviceOSName.equals(KeyVariables.BLACKBERRY) ) {
			Intent lVideoIntent = new Intent(null, Uri.parse("ytv://"+params.videoId), MainApplication.mContext, OpenYouTubePlayerActivity.class);
			((Activity) MainApplication.mContext).startActivity(lVideoIntent);
		} else {
			videoPlayer(true).open(params);
		}  
	}

	public static void loadGoogleAd(Context context) {
		Log.i("AD SERVICE", "GOOGLE INTERSTITIAL ADS ARE LOADING");
		mInterstitial_Google = new InterstitialAd(context);
		mInterstitial_Google.setAdUnitId(context.getResources().getString(R.string.ad_unit_id_interstitial));
		mInterstitial_Google.setAdListener(new AdViewListenerGoogle(context, mInterstitial_Google));
		mInterstitial_Google.loadAd(new AdRequest.Builder().build());
	}

	public static void loadGoogleAdBB(Context context) {
		Log.i("AD SERVICE BB", "GOOGLE INTERSTITIAL ADS ARE LOADING");
		mInterstitial_Google = new InterstitialAd(context);
		mInterstitial_Google.setAdUnitId(context.getResources().getString(R.string.ad_unit_id_interstitial_bb));
		mInterstitial_Google.setAdListener(new AdViewListenerGoogle(context, mInterstitial_Google));
		mInterstitial_Google.loadAd(new AdRequest.Builder().build());
	}

	public static void loadAmazonAd(Context context) {
		Log.i("AD SERVICE", "AMAZON INTERSTITIAL ADS ARE LOADING");
		mInterstitial_Amazon = new AmazonInterstitialAd((Activity) context);
		mInterstitial_Amazon.setListener(new AdViewListenerAmazon(context, mInterstitial_Amazon));
		mInterstitial_Amazon.loadAd();
	}
}

