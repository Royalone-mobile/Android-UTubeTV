package com.distantfuture.videos.channellookup;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.distantfuture.videos.R;
import com.distantfuture.videos.database.YouTubeData;
import com.squareup.picasso.Picasso;

public class ChannelDetailsActivity extends Activity {
  private final float mAspectRatio = 4f / 5f;

  public static void show(Activity activity, YouTubeData data) {
    // add animation, see finish below for the back transition
    ActivityOptions opts = ActivityOptions.makeCustomAnimation(activity, android.R.anim.fade_in, android.R.anim.fade_out);

    Bundle paramsBundle = YouTubeData.toBundle(data);

    Intent intent = new Intent();
    intent.putExtra("params", paramsBundle);
    intent.setClass(activity, ChannelDetailsActivity.class);
    activity.startActivity(intent, opts.toBundle());
  }

  @Override
  public void finish() {
    super.finish();

    // animate out
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_channel_details);
    getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    ImageView imageView = (ImageView) findViewById(R.id.channel_thumbnail);
    TextView titleView = (TextView) findViewById(R.id.channel_title);
    TextView channelIdView = (TextView) findViewById(R.id.channel_id);
    TextView descriptionView = (TextView) findViewById(R.id.channel_description);

    Bundle paramsBundle = getIntent().getBundleExtra("params");
    YouTubeData data = YouTubeData.fromBundle(paramsBundle);

    Picasso.with(this).load(data.mThumbnail)
        //          .noFade()
        //          .resize(250, 250) // put into dimens for dp values
        .into(imageView);

    titleView.setText(data.mTitle);
    descriptionView.setText(data.mDescription);
    channelIdView.setText(data.mChannel);

    imageView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      // Respond to the action bar's Up/Home button
      case android.R.id.home:
        finish();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
