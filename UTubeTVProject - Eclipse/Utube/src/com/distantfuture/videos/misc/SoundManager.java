package com.distantfuture.videos.misc;

import android.content.Context;
import android.media.AudioManager;

public class SoundManager {
  private boolean mMuteState = false;

  private boolean mSavedMute;
  private int mSavedVolume;
  private boolean mWasModified;
  private Context mContext;

  public SoundManager(Context context) {
    super();

    mContext = context.getApplicationContext();
  }

  // call when owning fragment or activity onPause
  public void restoreMuteIfNeeded() {
    if (mMuteState) {
      mute(false);
    }
  }

  public boolean isMute() {
    return mMuteState;
  }

  public void mute(boolean mute) {
    if (mMuteState != mute) {
      mMuteState = mute;

      AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

      // setStreamMute is broken on my 4.1 (16) galaxy nexus, so using volume instead
      if (Utils.isJellyBean()) {
        int saved = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

        manager.setStreamVolume(AudioManager.STREAM_MUSIC, (mMuteState ? 0 : mSavedVolume), 0);

        mSavedVolume = saved;
      } else
        manager.setStreamMute(AudioManager.STREAM_MUSIC, mMuteState);
    }
  }

}


