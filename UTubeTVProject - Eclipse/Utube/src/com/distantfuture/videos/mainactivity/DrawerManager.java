package com.distantfuture.videos.mainactivity;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.distantfuture.videos.R;
import com.distantfuture.videos.content.Content;
import com.distantfuture.videos.imageutils.ToolbarIcons;

import java.util.Map;

public class DrawerManager {

  private DrawerLayout mDrawerLayout;
  private View mDrawerContainer;
  private ListView mDrawerList;
  private ActionBarDrawerToggle mDrawerToggle;
  private DrawerManagerListener mListener;
  private FragmentManager mFragmentManager;

  public DrawerManager(Activity activity, Content content, DrawerManagerListener listener) {
    super();

    mListener = listener;

    mDrawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
    mDrawerContainer = activity.findViewById(R.id.drawer_container);
    mDrawerList = (ListView) activity.findViewById(R.id.left_drawer);

    // set a custom shadow that overlays the main content when the drawer opens
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

    // set up the drawer's list view with items and click listener
    mDrawerList.setAdapter(new DrawerAdapter(activity, content));

    mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mListener.onDrawerClick(position);
        closeDrawer();
      }
    });

    // seems insane, is this the best way of having a variable drawable resource by theme?
    int[] attrs = new int[]{R.attr.nav_drawer_menu_drawable};
    TypedArray ta = activity.obtainStyledAttributes(attrs);
    int resID = ta.getResourceId(0, 0);
    ta.recycle();

    // ActionBarDrawerToggle ties together the the proper interactions
    // between the sliding drawer and the action bar app icon
    mDrawerToggle = new ActionBarDrawerToggle(activity,                  /* host Activity */
        mDrawerLayout,         /* DrawerLayout object */
        resID,  /* nav drawer image to replace 'Up' caret */
        R.string.drawer_open,  /* "open drawer" description for accessibility */
        R.string.drawer_close  /* "close drawer" description for accessibility */) {
      public void onDrawerClosed(View view) {
        mListener.onDrawerOpen(false);
      }

      public void onDrawerOpened(View drawerView) {
        mListener.onDrawerOpen(true);
      }
    };

    mDrawerLayout.setDrawerListener(mDrawerToggle);

    mFragmentManager = activity.getFragmentManager();

    mFragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
      @Override
      public void onBackStackChanged() {
        int backStackEntryCount = mFragmentManager.getBackStackEntryCount();

        mDrawerToggle.setDrawerIndicatorEnabled(backStackEntryCount == 0);
      }
    });
  }

  public void setDrawerIndicatorEnabled(boolean set) {
    mDrawerToggle.setDrawerIndicatorEnabled(set);
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    if (mDrawerToggle.isDrawerIndicatorEnabled() && mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    } else if (item.getItemId() == android.R.id.home && mFragmentManager.popBackStackImmediate()) {
      return true;
    }

    return false;
  }

  public void openDrawer() {
    mDrawerLayout.openDrawer(mDrawerContainer);
  }

  public void closeDrawer() {
    mDrawerLayout.closeDrawer(mDrawerContainer);
  }

  public void setItemChecked(int position, boolean checked) {
    mDrawerList.setItemChecked(position, checked);
  }

  public void syncState() {
    mDrawerToggle.syncState();
  }

  public void onConfigurationChanged(Configuration newConfig) {
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  public interface DrawerManagerListener {
    public void onChannelClick();

    public void onDrawerClick(int position);

    public void onDrawerOpen(boolean openOrClose);
  }

  private static class DrawerAdapter extends ArrayAdapter<Map> {
    private LayoutInflater inflater;
    private Content mContent;
    private int mIconColor;

    public DrawerAdapter(Context context, Content content) {
      super(context, R.layout.drawer_list_item);

      inflater = LayoutInflater.from(context);
      mIconColor = context.getResources().getColor(R.color.drawer_icon_color);

      mContent = content;

      for (Map item : mContent.drawerTitles()) {
        add(item);
      }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View result = convertView;

      if (result == null)
        result = inflater.inflate(R.layout.drawer_list_item, parent, false);

      TextView textView = (TextView) result.findViewById(android.R.id.text1);

      Map item = getItem(position);
      ToolbarIcons.IconID icon = ToolbarIcons.IconID.NONE;

      Object iconInt = item.get("icon");
      if (iconInt != null)
        icon = (ToolbarIcons.IconID) iconInt; // cast Integer to Enum, better way?

      textView.setText((String) item.get("title"));

      ImageView imageView = (ImageView) result.findViewById(android.R.id.icon);

      Drawable sharedDrawable = ToolbarIcons.iconBitmap(getContext(), icon, mIconColor, 30);
      imageView.setImageDrawable(sharedDrawable);

      return result;
    }
  }

}
