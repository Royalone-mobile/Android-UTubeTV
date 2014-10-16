package com.klogicapps.tv.mainactivity;

import android.app.Fragment;

import com.klogicapps.tv.youtube.VideoPlayer;

// also used by about fragment, but had to put it somewhere
public interface DrawerActivitySupport {
  public boolean actionBarTitleHandled();

  // used in the about fragment when clicking on Watch Now or image
  public void showDefaultFragment();

  public void playVideo(VideoPlayer.PlayerParams params);

  public boolean isPlayerVisible();

  public void installFragment(Fragment fragment, boolean animate);

  public void setActionBarTitle(CharSequence title, CharSequence subtitle);
}
