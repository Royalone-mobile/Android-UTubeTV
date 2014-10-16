package com.klogicapps.tv.misc;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.klogicapps.tv.R;
import com.klogicapps.tv.imageutils.ToolbarIcons;

public class VideoMenuView extends ImageView {
  private static Drawable sharedDrawable;
  public Long mId;
  private VideoMenuViewListener mListener;

  public VideoMenuView(Context context, AttributeSet attrs) {
    super(context, attrs);

    if (sharedDrawable == null) {
      sharedDrawable = ToolbarIcons.iconBitmap(getContext(), ToolbarIcons.IconID.OVERFLOW, 0xffdddddd, 30);
      sharedDrawable.setAlpha(200);  // 0-255
    }

    setImageDrawable(sharedDrawable);

    final PopupMenu popupMenu = new PopupMenu(getContext(), this);
    popupMenu.inflate(R.menu.video_menu);
    setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        popupMenu.show();
      }
    });

    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
		if (itemId == R.id.video_menu_youtube) {
			if (mListener != null)
              mListener.showVideoOnYouTube(mId);
		} else if (itemId == R.id.video_menu_hide) {
			if (mListener != null)
              mListener.hideVideo(mId);
		}
        return true;
      }
    });
  }

  public void setListener(VideoMenuViewListener listener) {
    mListener = listener;
  }

  public interface VideoMenuViewListener {
    public void showVideoInfo(Long itemId);

    public void showVideoOnYouTube(Long itemId);

    public void hideVideo(Long itemId);
  }

}

