package com.distantfuture.videos.introactivity;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

public class IntroPagerAdapter extends FragmentPagerAdapter {
  private static int sChangeCount = 0;
  private List<IntroXMLParser.IntroPage> pages;

  public IntroPagerAdapter(Context context, FragmentManager fm) {
    super(fm);
  }

  public void setPages(List<IntroXMLParser.IntroPage> pages) {
    this.pages = pages;
    notifyDataSetChanged();
  }

  public IntroXMLParser.IntroPage pageAtIndex(int position) {
    if (pages != null)
      return pages.get(position);

    return null;
  }

  @Override
  public Fragment getItem(int position) {
    return IntroPageFragment.newInstance(position);
  }

  @Override
  public int getCount() {
    if (pages != null)
      return pages.size();

    return 0;
  }

  public void notifyDataSetChanged() {
    sChangeCount += 100;

    super.notifyDataSetChanged();
  }

  public long getItemId(int position) {
    return position + sChangeCount;
  }
}
