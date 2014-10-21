package com.klogicapps.tv.misc;

import android.app.Application;
import android.content.Context;
// import com.distantfuture.castcompanionlibrary.lib.cast.VideoCastManager;
// import com.distantfuture.castcompanionlibrary.lib.utils.CastUtils;
import android.os.Build;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class MainApplication extends Application {

	// Context
	public static Context mContext;

	// Variables
	public static int androidSDK = Build.VERSION.SDK_INT; 
	public static String deviceOSName = System.getProperty("os.name");

	public static boolean TESTING = false;

	public static boolean NETWORK_AVAILABLE;

	public static int OVERIDE_AD_SERVICES = 0;

	public static final double VOLUME_INCREMENT = 0.05;
	private static String sApplicationID;
	// MARKER - REMOVED CAST SCRIPTS
	//  private static VideoCastManager sCastManager = null;
	/* 
  public static VideoCastManager getCastManager(Context context) {
    if (null == sCastManager) {
      sCastManager = VideoCastManager.initialize(context, sApplicationID, null);
      sCastManager.enableFeatures(VideoCastManager.FEATURE_NOTIFICATION |
          VideoCastManager.FEATURE_LOCKSCREEN |
          VideoCastManager.FEATURE_DEBUGGING);

    }
    sCastManager.setContext(context);

    //      add pref later
    //      String destroyOnExitStr = CastUtils.getStringFromPreference(context, CastPreference.TERMINATION_POLICY_KEY);
    //      mCastMgr.setStopOnDisconnect(null != destroyOnExitStr && CastPreference.STOP_ON_DISCONNECT.equals(destroyOnExitStr));

    sCastManager.setStopOnDisconnect(true);

    return sCastManager;
  }
	 */ 

	@Override
	public void onCreate() {
		super.onCreate();

		mContext = this;

		// sApplicationID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID; // "6142AE0B"; // "5A3D7A5C";
		// CastUtils.saveFloatToPreference(getApplicationContext(), VideoCastManager.PREFS_KEY_VOLUME_INCREMENT, (float) VOLUME_INCREMENT);
	}

	// Used to check if devices has Google Play Services
	public static boolean hasGooglePlayServices() {
		boolean isAvailable = false;
		int statusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
		if (statusCode == ConnectionResult.SUCCESS) {
			isAvailable = true;
		} else {
			isAvailable = false;
		}
		return isAvailable;
	}

}

