package com.distantfuture.videos.mainactivity;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.SearchView;

import com.cocosw.undobar.UndoBarController;
import com.distantfuture.videos.R;
import com.distantfuture.videos.content.Content;
import com.distantfuture.videos.database.Database;
import com.distantfuture.videos.database.DatabaseAccess;
import com.distantfuture.videos.database.DatabaseTables;
import com.distantfuture.videos.database.YouTubeContentProvider;
import com.distantfuture.videos.database.YouTubeData;
import com.distantfuture.videos.imageutils.ToolbarIcons;
import com.distantfuture.videos.misc.AppUtils;
import com.distantfuture.videos.misc.BusEvents;
import com.distantfuture.videos.misc.ContractFragment;
import com.distantfuture.videos.misc.EmptyListHelper;
import com.distantfuture.videos.misc.ScrollTriggeredAnimator;
import com.distantfuture.videos.misc.Utils;
import com.distantfuture.videos.services.ListServiceRequest;
import com.distantfuture.videos.services.YouTubeService;
import com.distantfuture.videos.youtube.VideoPlayer;
import com.google.common.primitives.Longs;
import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.SwipeDismissAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class YouTubeGridFragment extends ContractFragment<DrawerActivitySupport> implements OnRefreshListener, OnDismissCallback, YouTubeCursorAdapter.YouTubeCursorAdapterListener, LoaderManager.LoaderCallbacks<Cursor> {

  private EmptyListHelper mEmptyListHelper;
  private ListServiceRequest mRequest;
  private YouTubeCursorAdapter mAdapter;
  private PullToRefreshLayout mPullToRefreshLayout;
  private boolean mCachedHiddenPref;
  private BroadcastReceiver mBroadcastReceiver;
  private String mFilter;
  private MenuItem mSearchItem;
  private SearchView mSearchView;
  private Drawable mSearchDrawable;
  private boolean mSearchSubmitted = false;

  public static YouTubeGridFragment newInstance(ListServiceRequest request) {
    YouTubeGridFragment fragment = new YouTubeGridFragment();

    Bundle args = new Bundle();

    args.putBundle("request", request.toBundle());

    fragment.setArguments(args);

    return fragment;
  }

  public void syncActionBarTitle() {
    DrawerActivitySupport provider = getContract();
    if (provider != null) {
      // activity can control the actionbar title, for example the player sets the Now Playing title
      if (!provider.actionBarTitleHandled()) {
        String subtitle = null;
        String title = null;

        if (mRequest != null) {
          int cnt = mAdapter.getCount();

          subtitle = String.format("%d ", cnt) + mRequest.unitName(cnt > 1 || cnt == 0);

          String channelName = Content.instance().channelName();
          if (channelName != null)
            title = channelName;

          String containerName = mRequest.containerName();
          if (containerName != null)
            subtitle += ": " + containerName;
        }

        provider.setActionBarTitle(title, subtitle);
      }
    }
  }

  private boolean endSearchActionBar() {
    mSearchSubmitted = true;  // had to add this for KitKat?  Maybe I'm doing something slightly non standard?
    if (mSearchItem.isActionViewExpanded())
      return mSearchItem.collapseActionView();

    return false;
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);

    // hide the search item if player visible
    DrawerActivitySupport provider = getContract();
    if (provider != null) {
      if (provider.isPlayerVisible()) {
        mSearchItem.setVisible(false);
      } else {
        mSearchItem.setVisible(true);
      }
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.youtube_fragment_menu, menu);
    setupSearchItem(menu);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
    EventBus.getDefault().register(this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    GridView gridView;
    mRequest = ListServiceRequest.fromBundle(getArguments().getBundle("request"));
    mAdapter = YouTubeCursorAdapter.newAdapter(getActivity(), mRequest, this);

    ViewGroup rootView = mAdapter.rootView(container);

    // Now find the PullToRefreshLayout to setup
    mPullToRefreshLayout = (PullToRefreshLayout) rootView.findViewById(R.id.grid_frame_layout);

    Options.Builder options = new Options.Builder();
    options.scrollDistance(0.4f);

    // Now setup the PullToRefreshLayout
    ActionBarPullToRefresh.from(this.getActivity())
        // Mark All Children as pullable
        .allChildrenArePullable()
            // Set the OnRefreshListener
        .listener(this)
        .options(options.build())
            // Finally commit the setup to our PullToRefreshLayout
        .setup(mPullToRefreshLayout);

    gridView = (GridView) rootView.findViewById(R.id.gridview);
    gridView.setOnItemClickListener(mAdapter);

    SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(new SwipeDismissAdapter(mAdapter, this));
    swingBottomInAnimationAdapter.setInitialDelayMillis(200);
    swingBottomInAnimationAdapter.setAbsListView(gridView);

    gridView.setAdapter(swingBottomInAnimationAdapter);

    // setup empty view
    mEmptyListHelper = new EmptyListHelper(rootView.findViewById(R.id.empty_view));
    gridView.setEmptyView(mEmptyListHelper.view());

    // create the loader
    getLoaderManager().initLoader(0, null, this);

    // dimmer only exists for dark mode
    View dimmerView = rootView.findViewById(R.id.dimmer);
    if (dimmerView != null)
      new ScrollTriggeredAnimator(gridView, dimmerView);

    return rootView;
  }

  // OnDismissCallback
  @Override
  public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
    ArrayList<Long> ids = new ArrayList<Long>();

    for (int position : reverseSortedPositions) {
      Cursor cursor = (Cursor) mAdapter.getItem(position);
      YouTubeData itemMap = mRequest.databaseTable().cursorToItem(cursor, null);

      DatabaseAccess database = new DatabaseAccess(getActivity(), mRequest);

      ids.add(itemMap.mID);

      if (itemMap != null) {
        boolean deleteOnHide = false; // true for debugging only

        if (deleteOnHide)
          database.deleteItem(itemMap.mID);
        else {
          itemMap.setHidden(!itemMap.isHidden());
          database.updateItem(itemMap);
        }
      }
    }

    UndoBarController.UndoListener listener = new UndoBarController.UndoListener() {
      @Override
      public void onUndo(Parcelable parcelable) {
        // was getting crashes here, this fixed it.  I think quickly double tapping the undo button triggers this
        if (parcelable == null) {
          return;
        }

        DatabaseAccess database = new DatabaseAccess(getActivity(), mRequest);

        Bundle info = (Bundle) parcelable;
        long ids[] = info.getLongArray("id_array");

        for (long id : ids) {
          YouTubeData itemMap = database.getItemWithID(id);
          if (itemMap != null) {
            itemMap.setHidden(!itemMap.isHidden());
            database.updateItem(itemMap);
          }
        }
      }
    };
    Bundle info = new Bundle();
    info.putLongArray("id_array", Longs.toArray(ids));
    UndoBarController.show(getActivity(), mRequest.unitName(false) + " hidden", listener, info);
  }

  public String getFilter() {
    return mFilter;
  }

  public boolean setFilter(String filter) {
    if (!TextUtils.equals(mFilter, filter)) {
      mFilter = filter;
      getLoaderManager().restartLoader(0, null, this);

      return true;
    }

    return false;
  }

  // YouTubeCursorAdapterListener
  @Override
  public void adapterDataChanged() {
    syncActionBarTitle();

    if (!TextUtils.isEmpty(getFilter()) && mSearchItem.isActionViewExpanded()) {
      int cnt = mAdapter.getCount();

      Utils.message(getActivity(), String.format("Found: %d items", cnt));
    }
  }

  // YouTubeCursorAdapterListener
  @Override
  public void handleClickFromAdapter(int position, YouTubeData itemMap) {
    DrawerActivitySupport provider = getContract();

    // get rid of the search if still open.  Someone could search
    // and click a visible item rather than hitting the search button on keyboard
    endSearchActionBar();

    switch (mRequest.type()) {
      case RELATED:
      case SEARCH:
      case LIKED:
      case VIDEOS:
        VideoPlayer.PlayerParams params = new VideoPlayer.PlayerParams(itemMap.mVideo, itemMap.mTitle, position);

        if (provider != null)
          provider.playVideo(params);

        break;
      case PLAYLISTS: {
        String playlistID = itemMap.mPlaylist;

        if (playlistID != null) {
          Fragment frag = YouTubeGridFragment.newInstance(ListServiceRequest.videosRequest(playlistID, itemMap.mTitle));

          if (provider != null)
            provider.installFragment(frag, true);
        }
      }
      break;
    }
  }

  // YouTubeCursorAdapterListener
  @Override
  public Activity accesActivity() {
    return getActivity();
  }

  @Override
  public void onDestroy() {
    EventBus.getDefault().unregister(this);

    super.onDestroy();
  }

  @Override
  public void onResume() {
    super.onResume();

    // reload if the hidden pref is now how we remember it
    reloadForPrefChange();

    syncActionBarTitle();
  }

  // for EventBus
  public void onEventMainThread(BusEvents.PlayNextEvent event) {
    int index = event.params.index;
    int cnt = mAdapter.getCount();

    index++;  // go to next

    if (index > cnt) {
      index = 0; // loop around
    }

    if (index < cnt) {
      Cursor cursor = (Cursor) mAdapter.getItem(index);
      YouTubeData itemMap = mRequest.databaseTable().cursorToItem(cursor, null);

      if (itemMap != null) {
        DrawerActivitySupport provider = getContract();

        if (provider != null) {
          VideoPlayer.PlayerParams params = new VideoPlayer.PlayerParams(itemMap.mVideo, itemMap.mTitle, index);

          provider.playVideo(params);
        }
      }
    }
  }

  public void onEventMainThread(BusEvents.YouTubeFragmentDataReady event) {
    // stop the pull to refresh indicator
    // Notify PullToRefreshLayout that the refresh has finished
    mPullToRefreshLayout.setRefreshComplete();

    // in the case of no results, we need to update the emptylist view to reflect that
    // This only shows up at launch, or the first time a list is requested
    mEmptyListHelper.updateEmptyListView("List is Empty", true);
  }

  // eventbus event
  public void onEventMainThread(BusEvents.ContentEvent event) {
    // need to update the channel name in the action bar once ready
    syncActionBarTitle();
  }


  // OnRefreshListener
  @Override
  public void onRefreshStarted(View view) {
    YouTubeService.startListRequest(getActivity(), mRequest, true);
  }

  // called by activity on menu item action for show hidden files toggle
  public void reloadForPrefChange() {
    boolean showHidden = AppUtils.instance(getActivity()).showHiddenItems();

    if (mCachedHiddenPref != showHidden) {
      mCachedHiddenPref = showHidden;
      getLoaderManager().restartLoader(0, null, this);
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    DatabaseTables.DatabaseTable table = mRequest.databaseTable();

    // Debug.log(mRequest.toString());

    String sortOrder = (DatabaseTables.videoTable() == table) ? "vi" : "pl"; // stupid hack

    mCachedHiddenPref = AppUtils.instance(getActivity()).showHiddenItems();
    int queryID = DatabaseTables.VISIBLE_ITEMS;
    if (mCachedHiddenPref)
      queryID = DatabaseTables.ALL_ITEMS;

    Database.DatabaseQuery queryParams = table.queryParams(queryID, mRequest.requestIdentifier(), mFilter);

    // startRequest below will notify when done and we hide the progress bar
    mEmptyListHelper.updateEmptyListView("Talking to YouTube...", false);

    YouTubeService.startListRequest(getActivity(), mRequest, false);

    return new CursorLoader(getActivity(), YouTubeContentProvider.contentsURI(getActivity()), queryParams.mProjection, queryParams.mSelection, queryParams.mSelectionArgs, sortOrder);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
    mAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mAdapter.swapCursor(null);
  }

  private void setupSearchItem(Menu menu) {
    mSearchItem = menu.findItem(R.id.action_search);
    if (mSearchItem != null) {
      if (mSearchDrawable == null) {
        // seems insane, is this the best way of having a variable drawable resource by theme?
        int[] attrs = new int[]{R.attr.action_bar_icon_color};
        TypedArray ta = getActivity().obtainStyledAttributes(attrs);
        int color = ta.getColor(0, 0);
        ta.recycle();

        mSearchDrawable = ToolbarIcons.icon(getActivity(), ToolbarIcons.IconID.SEARCH, color, 32);
        mSearchDrawable.setAlpha(150);
      }
      mSearchItem.setIcon(mSearchDrawable);

      mSearchView = (SearchView) mSearchItem.getActionView();
      //      mSearchView.setSubmitButtonEnabled(true);
      mSearchView.setQueryHint("Search");

      // not sure if this is needed or not yet....
      SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
      SearchableInfo searchableInfo = searchManager.getSearchableInfo(getActivity().getComponentName());
      mSearchView.setSearchableInfo(searchableInfo);

      mSearchItem.setActionView(mSearchView);

      // change the text color inside the searchView, There is no way to theme this shitty thing
      int textColor = getActivity().getResources().getColor(android.R.color.primary_text_dark);
      int hintColor = getActivity().getResources().getColor(android.R.color.secondary_text_dark);

      Utils.textViewColorChanger(mSearchView, textColor, hintColor);

      mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
          // return false and clear the filter if clicked when a filter is set
          if (!TextUtils.isEmpty(getFilter())) {
            // this is needed since if the action bar refreshes, it will restore the query, kitkat seems to clear this though
            mSearchView.setQuery(null, true);

            // added for KitKat, previously setQuery to null would trigger the text listener
            setFilter(null);

            return false;
          }
          return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
          return true;
        }
      });

      mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
          if (mSearchItem != null) {
            return endSearchActionBar();
          }

          return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
          // Called when the action bar search text has changed.  Update
          // the search filter, and restart the loader to do a new query
          // with this filter.
          String filter = !TextUtils.isEmpty(newText) ? newText : null;
          boolean setFilter = true;

          if (mSearchSubmitted && filter == null) {
            // on KitKat collapseActionView call above on submit, sends null for newText, so preventing this from changing
            // the filter here
            setFilter = false;
          }

          mSearchSubmitted = false;  // had to add this for KitKat?  Maybe I'm doing something slightly non standard?

          return (setFilter && setFilter(filter));
        }
      });
    }
  }

}

