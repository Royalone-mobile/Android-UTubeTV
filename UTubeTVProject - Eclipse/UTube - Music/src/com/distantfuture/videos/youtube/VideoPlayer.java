package com.distantfuture.videos.youtube;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.distantfuture.videos.R;
import com.distantfuture.videos.imageutils.ToolbarIcons;
import com.distantfuture.videos.misc.AppUtils;
import com.distantfuture.videos.misc.Utils;

public class VideoPlayer {
  private final int mAnimationDuration = 300;
  private final int mIconSize = 32;
  private View mVideoBox;
  private Context mContext;
  private VideoPlayerFragment mVideoFragment;
  private VideoPlayerStateListener mListener;
  private ImageView mMuteButton;
  private TextView mSeekFlashTextView;
  private TextView mTimeRemainingTextView;
  private View mTopBar;

  public VideoPlayer(Activity activity, int fragmentContainerResID, VideoPlayerStateListener l) {
    super();

    mListener = l;
    mContext = activity.getApplicationContext();

    // install video fragment
    // will already exist if restoring Activity
    mVideoFragment = (VideoPlayerFragment) activity.getFragmentManager()
        .findFragmentById(fragmentContainerResID);

    // hide top bar when going fullscreen
    mTopBar = activity.findViewById(R.id.top_bar);
    mVideoFragment.setVideoFragmentListener(new VideoPlayerFragment.VideoFragmentListener() {

      @Override
      public void onFullScreen(boolean fullscreen) {
        mTopBar.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
      }

      @Override
      public void playerInitialized() {
        if (AppUtils.instance(mContext).alwaysPlayFullscreen())
          mVideoFragment.setFullscreen(true);

        // we avoid showing the view until after fullscreen is set after the player is setup
        mVideoBox.setVisibility(View.VISIBLE);
      }
    });

    mVideoBox = activity.findViewById(R.id.video_player_box);

    setupToolbar();
  }

  public void open(final PlayerParams params) {
    if (visible())
      playerShown(params);
    else {
      Utils.vibrate(mContext);

      if (AppUtils.instance(mContext).alwaysPlayFullscreen()) {
        playerShown(params);
      } else {
        // update mute button since it could still be in mute mode
        updateMuteButton();

        boolean animate = Utils.isPortrait(mContext);
        if (animate) {
          // Initially translate off the screen so that it can be animated in from below.
          mVideoBox.setTranslationY(-mVideoBox.getHeight());
          mVideoBox.setAlpha(0f);

          mVideoBox.setVisibility(View.VISIBLE);

          mVideoBox.animate()
              .translationY(0)
              .alpha(1f)
              .setInterpolator(new AccelerateDecelerateInterpolator())
              .setDuration(mAnimationDuration)
              .withEndAction(new Runnable() {
                @Override
                public void run() {
                  playerShown(params);
                }
              });
        } else {
          mVideoBox.setVisibility(View.VISIBLE);

          playerShown(params);
        }
      }
    }
  }

  public PlayerParams playerParams() {
    return mVideoFragment.playerParams();
  }

  public void close() {
    if (visible()) {
      Utils.vibrate(mContext);

      mVideoFragment.closingPlayer();

      boolean animate = Utils.isPortrait(mContext);

      if (animate) {
        mVideoBox.animate()
            .translationYBy(-mVideoBox.getHeight())
            .alpha(.3f)
            .setInterpolator(new AccelerateInterpolator())
            .setDuration(animate ? mAnimationDuration : 0)
            .withEndAction(new Runnable() {
              @Override
              public void run() {
                playerClosed();
              }
            });
      } else
        playerClosed();
    }
  }

  public String title() {
    return mVideoFragment.getTitle();
  }

  public String videoId() {
    return mVideoFragment.getVideoId();
  }

  public boolean visible() {
    return (mVideoBox.getVisibility() == View.VISIBLE);
  }

  public void toggleMute() {
    mVideoFragment.mute(!mVideoFragment.isMute());

    updateMuteButton();
  }

  public void toggleFullscreen() {
    mVideoFragment.setFullscreen(true);
  }

  public void skip(int seconds) {
    mVideoFragment.seekRelativeSeconds(seconds);
  }

  private void updateMuteButton() {
    if (mVideoFragment.isMute())
      mMuteButton.setImageDrawable(ToolbarIcons.icon(mContext, ToolbarIcons.IconID.SOUND, Color.RED, mIconSize));
    else
      mMuteButton.setImageDrawable(ToolbarIcons.icon(mContext, ToolbarIcons.IconID.SOUND, Color.WHITE, mIconSize));
  }

  private void playerShown(PlayerParams playerParams) {
    // action bar menu needs to update
    Activity host = (Activity) mVideoBox.getContext();
    if (host != null)
      host.invalidateOptionsMenu();

    mVideoFragment.setVideo(playerParams);

    // actionbar subtitle needs a refresh when new video starts playing, so it's not just open/close events
    // that need state changed messages

    if (mListener != null)
      mListener.stateChanged();
  }

  // ------------------------------------------------------------------------------------------------
  // private

  private void playerClosed() {
    // action bar menu needs to update
    Activity host = (Activity) mVideoBox.getContext();
    if (host != null)
      host.invalidateOptionsMenu();

    mVideoBox.setVisibility(View.INVISIBLE);

    // reset this so it's 00:00 next time it's shown
    mTimeRemainingTextView.setText("00:00");

    if (mListener != null) {
      mListener.stateChanged();
    }
  }

  private void showSeekPopupWindow(View anchorView) {
    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View popupContentsView = inflater.inflate(R.layout.video_seek_popup, null);
    final PopupWindow pw = new PopupWindow(popupContentsView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);  // if false, clicks to dismiss window also get passed to views below (should be true)

    // hack_alert: must set some kind of background so that clicking outside the view will dismiss the popup (known bug in Android)
    pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    pw.setOutsideTouchable(true);
    pw.setAnimationStyle(-1);
    pw.showAsDropDown(anchorView);

    float time = mVideoFragment.getCurrentTimeMillis();
    final float duration = mVideoFragment.getDurationMillis();
    float currentPercent = time / duration;
    int startValue = (int) (currentPercent * 100);

    SeekBar sb = (SeekBar) popupContentsView.findViewById(R.id.video_seek_bar);
    sb.setMax(100);   // using max like a percent
    sb.setProgress(startValue);

    sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        float progressPercent = progress / 100.0f;
        int seekTo = (int) (progressPercent * duration);

        mTimeRemainingTextView.setText(Utils.millisecondsToDuration(seekTo));
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        // seek when released
        float progressPercent = seekBar.getProgress() / 100.0f;
        int seekTo = (int) (progressPercent * duration);

        mVideoFragment.seekToMillis(seekTo);

        pw.dismiss();
      }
    });

  }

  private void setupToolbar() {
    ImageView b;

    // close button
    b = (ImageView) mVideoBox.findViewById(R.id.close_button);
    b.setImageDrawable(ToolbarIcons.icon(mContext, ToolbarIcons.IconID.CLOSE, Color.WHITE, mIconSize));
    b.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        close();
      }
    });

    // Mute button
    mMuteButton = (ImageView) mVideoBox.findViewById(R.id.mute_button);
    mMuteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        toggleMute();
      }
    });
    updateMuteButton();

    // Skip back button
    b = (ImageView) mVideoBox.findViewById(R.id.skip_back_button);
    b.setImageDrawable(ToolbarIcons.icon(mContext, ToolbarIcons.IconID.STEP_BACK, Color.WHITE, mIconSize));
    b.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        skip(-10);
      }
    });

    // Skip ahead button
    b = (ImageView) mVideoBox.findViewById(R.id.skip_ahead_button);
    b.setImageDrawable(ToolbarIcons.icon(mContext, ToolbarIcons.IconID.STEP_FORWARD, Color.WHITE, mIconSize));
    b.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        skip(10);
      }
    });

    mTimeRemainingTextView = (TextView) mVideoBox.findViewById(R.id.time_remaining);
    mTimeRemainingTextView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // put it below toolbar
        View tb = mVideoBox.findViewById(R.id.video_toolbar_view);

        showSeekPopupWindow(tb);
      }
    });

    mSeekFlashTextView = (TextView) mVideoBox.findViewById(R.id.seek_flash);

    // we let the video fragment update us in it's own timer
    mVideoFragment.setTimeRemainingListener(new VideoPlayerFragment.TimeRemainingListener() {

      // call this on the main thread
      @Override
      public void setTimeRemainingText(final String timeRemaining) {
        mTimeRemainingTextView.setText(timeRemaining);
      }

      @Override
      public void setSeekFlashText(final String seekFlash) {
        mSeekFlashTextView.setText(seekFlash);

        int duration = 300;

        // fade old one out
        mTimeRemainingTextView.animate().setDuration(duration).alpha(0.0f);

        // start off off the screen, make visible
        mSeekFlashTextView.setTranslationY(-60.0f);
        mSeekFlashTextView.setVisibility(View.VISIBLE);

        // run animation, new time slides in from top, old time slides off
        mSeekFlashTextView.animate()
            .setDuration(duration)
            .translationY(0)
            .setInterpolator(new BounceInterpolator())
            .withEndAction(new Runnable() {
              @Override
              public void run() {
                mSeekFlashTextView.setVisibility(View.INVISIBLE);

                mTimeRemainingTextView.setText(seekFlash);
                mTimeRemainingTextView.setAlpha(1.0f);
              }
            });
      }

    });
  }

  abstract public interface VideoPlayerStateListener {
    abstract public void stateChanged();
  }

  public static class PlayerParams implements Parcelable {
    public static final Parcelable.Creator<PlayerParams> CREATOR = new Parcelable.Creator<PlayerParams>() {
      public PlayerParams createFromParcel(Parcel in) {
        return new PlayerParams(in);
      }

      public PlayerParams[] newArray(int size) {
        return new PlayerParams[size];
      }
    };
    public final String videoId;
    public final String title;
    public final int index;  // used for the play_next feature if enabled

    public PlayerParams(String videoId, String title, int index) {
      this.videoId = videoId;
      this.title = title;
      this.index = index;
    }

    // ===================================================================
    //  Parcelable - we send this to the service inside an intent

    private PlayerParams(Parcel in) {
      videoId = in.readString();
      title = in.readString();
      index = in.readInt();
    }

    public boolean equals(PlayerParams inParams) {
      if (inParams == null)
        return false;

      if (index != inParams.index)
        return false;
      if (!videoId.equals(inParams.videoId))
        return false;
      if (!title.equals(inParams.title))
        return false;

      return true;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(videoId);
      dest.writeString(title);
      dest.writeInt(index);
    }
  }

}
