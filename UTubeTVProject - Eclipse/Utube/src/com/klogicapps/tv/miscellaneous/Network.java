package com.klogicapps.tv.miscellaneous;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Network {

	private static ConnectivityManager manager;
	private static NetworkInfo networkInfo;
	private static boolean isAvailable = false;

	// Checks to see if Network is available
	public static boolean isNetworkAvailable(Context context) {
		manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		networkInfo = manager.getActiveNetworkInfo();
		if (networkInfo != null  && networkInfo.isConnected()) { isAvailable = true; }
		else { isAvailable = false; }
		return isAvailable;
	}

}
