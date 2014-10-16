package com.klogicapps.tv.services;

import android.content.Context;
import android.content.Intent;

import com.klogicapps.tv.activities.AuthActivity;
import com.klogicapps.tv.database.YouTubeData;
import com.klogicapps.tv.misc.BusEvents;
import com.klogicapps.tv.youtube.YouTubeAPI;

import java.util.List;

import de.greenrobot.event.EventBus;

public class SubscriptionsServiceTask {

  public SubscriptionsServiceTask(final Context context, final SubscriptionsServiceRequest request) {
    YouTubeAPI helper = new YouTubeAPI(context, true, false, new YouTubeAPI.YouTubeAPIListener() {
      @Override
      public void handleAuthIntent(final Intent authIntent) {
        AuthActivity.show(context, authIntent, request.toBundle());
      }
    });

    YouTubeAPI.SubscriptionListResults results = helper.subscriptionListResults(true);

    List<YouTubeData> items = results.getItems(0);

    List<String> channelIds = YouTubeData.contentIdsList(items);

    // notify that we handled an intent so pull to refresh can stop it's animation and other stuff
    EventBus.getDefault().post(new BusEvents.SubscriptionServiceResult(channelIds));
  }

}
