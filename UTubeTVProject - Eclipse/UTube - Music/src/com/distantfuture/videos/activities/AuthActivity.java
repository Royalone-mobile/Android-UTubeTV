package com.distantfuture.videos.activities;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.distantfuture.videos.misc.AppUtils;
import com.distantfuture.videos.misc.Auth;
import com.distantfuture.videos.misc.DUtils;
import com.distantfuture.videos.services.ListServiceRequest;
import com.distantfuture.videos.services.SubscriptionsServiceRequest;
import com.distantfuture.videos.services.YouTubeService;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public class AuthActivity extends Activity {
  private static final String REQUEST_AUTHORIZATION_REQUEST_PARAM = "com.distantfuture.utubetv.Request.param";
  private static final String REQUEST_AUTHORIZATION_INTENT_PARAM = "com.distantfuture.utubetv.Intent.param";
  private static final int INTENT_REQUEST_AUTHORIZATION = 3;
  private static final int INTENT_REQUEST_ACCOUNT_PICKER = 2;
  private GoogleAccountCredential credential;

  public static void show(Context context, Intent authIntent, Bundle requestBundle) {
    Intent intent = new Intent(context, AuthActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  // need this to start activity from service

    if (authIntent != null)
      intent.putExtra(AuthActivity.REQUEST_AUTHORIZATION_INTENT_PARAM, authIntent);

    intent.putExtra(AuthActivity.REQUEST_AUTHORIZATION_REQUEST_PARAM, requestBundle);

    context.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    credential = Auth.getCredentials(this, true);

    String accountName = credential.getSelectedAccountName();

    if (accountName == null) {
      chooseAccount();
    } else {
      showAuthIntent();
    }
  }

  private void showAuthIntent() {
    Intent authIntent = getIntent().getParcelableExtra(REQUEST_AUTHORIZATION_INTENT_PARAM);

    if (authIntent != null) {
      DUtils.log("Request auth received - executing the intent");

      startActivityForResult(authIntent, INTENT_REQUEST_AUTHORIZATION);
    } else
      doLastStep();
  }

  private void doLastStep() {
    ListServiceRequest listServiceRequest = ListServiceRequest.fromBundle(getIntent().getBundleExtra(REQUEST_AUTHORIZATION_REQUEST_PARAM));

    if (listServiceRequest != null) {
      // forcing a refresh since it is flagged as received in the service to avoid double attempts at a request
      YouTubeService.startListRequest(this, listServiceRequest, true);
    } else {
      SubscriptionsServiceRequest subscriptionsServiceRequest = SubscriptionsServiceRequest.fromBundle(getIntent().getBundleExtra(REQUEST_AUTHORIZATION_REQUEST_PARAM));

      if (subscriptionsServiceRequest != null) {
        YouTubeService.startSubscriptionRequest(this, subscriptionsServiceRequest);
      }
    }

    finish();
  }

  private void chooseAccount() {
    startActivityForResult(credential.newChooseAccountIntent(), INTENT_REQUEST_ACCOUNT_PICKER);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case INTENT_REQUEST_AUTHORIZATION:
        if (resultCode != Activity.RESULT_OK) {
          chooseAccount();
        } else {
          doLastStep();
        }
        break;

      case INTENT_REQUEST_ACCOUNT_PICKER:
        String accountName = null;
        if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
          accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
        }

        // save to preferences, listen to the pref change to get notified on change
        if (accountName != null) {
          AppUtils.instance(this).setAccountName(accountName);

          credential.setSelectedAccountName(accountName);

          showAuthIntent();
        } else
          finish();  // user hit cancel or back button
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

}
