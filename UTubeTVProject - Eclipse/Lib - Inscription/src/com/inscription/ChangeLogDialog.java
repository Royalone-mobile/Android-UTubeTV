/*
 * (c) 2012 Martin van Zuilekom (http://martin.cubeactive.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.inscription;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.webkit.WebView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChangeLogDialog {
  private static final String TAG = "ChangeLogDialog";

  protected final Activity mActivity;
  private String mStyle = "h1 { margin-left: 0px; font-size: 12pt; }" + "li { margin-left: 0px; font-size: 9pt; }" + "ul { padding-left: 30px; }" + ".summary { font-size: 9pt; color: #606060; display: block; clear: left; }" + ".date { font-size: 9pt; color: #606060;  display: block; }";

  protected ChangeLogDialog(final Activity activity) {
    super();
    mActivity = activity;
  }

  public static void showChangeLog(Activity activity) {
    new ChangeLogDialog(activity).showDialog(0);
  }

  private static String parseDate(final Context context, final String dateString) {
    final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
    try {
      final Date parsedDate = dateFormat.parse(dateString);
      return DateFormat.getDateFormat(context).format(parsedDate);
    } catch (ParseException ignored) {
      return dateString;
    }
  }

  private static void parseReleaseTag(Context context, final StringBuilder changelogBuilder, final XmlPullParser resourceParser) throws XmlPullParserException, IOException {
    changelogBuilder.append("<h1>Release: ")
        .append(resourceParser.getAttributeValue(null, "version"))
        .append("</h1>");

    if (resourceParser.getAttributeValue(null, "date") != null) {
      changelogBuilder.append("<span class='date'>")
          .append(parseDate(context, resourceParser.getAttributeValue(null, "date")))
          .append("</span>");
    }

    if (resourceParser.getAttributeValue(null, "summary") != null) {
      changelogBuilder.append("<span class='summary'>")
          .append(resourceParser.getAttributeValue(null, "summary"))
          .append("</span>");
    }

    changelogBuilder.append("<ul>");

    int eventType = resourceParser.getEventType();
    while ((eventType != XmlPullParser.END_TAG) || (resourceParser.getName().equals("change"))) {
      if ((eventType == XmlPullParser.START_TAG) && (resourceParser.getName().equals("change"))) {
        eventType = resourceParser.next();
        changelogBuilder.append("<li>" + resourceParser.getText() + "</li>");
      }
      eventType = resourceParser.next();
    }
    changelogBuilder.append("</ul>");
  }

  private String getAppVersion() {
    String versionName = "";
    try {
      final PackageInfo packageInfo = mActivity.getPackageManager()
          .getPackageInfo(mActivity.getPackageName(), 0);
      versionName = packageInfo.versionName;
    } catch (NameNotFoundException e) {
      Log.e(TAG, e.getMessage(), e);
    }
    return versionName;
  }

  private String getStyle() {
    return String.format("<style type=\"text/css\">%s</style>", mStyle);
  }

  private void setStyle(final String style) {
    mStyle = style;
  }

  private String getHTMLChangelog(final int resourceId, final Resources resources, final int version) {
    boolean releaseFound = false;

    final StringBuilder changelogBuilder = new StringBuilder();
    changelogBuilder.append("<html><head>").append(getStyle()).append("</head><body>");

    final XmlResourceParser xml = resources.getXml(resourceId);
    try {
      int eventType = xml.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if ((eventType == XmlPullParser.START_TAG) && (xml.getName().equals("release"))) {
          // Check if the version matches the release tag.
          // When version is 0 every release tag is parsed.
          final int versioncode = Integer.parseInt(xml.getAttributeValue(null, "versioncode"));
          if ((version == 0) || (versioncode == version)) {
            parseReleaseTag(mActivity, changelogBuilder, xml);
            releaseFound = true; //At lease one release tag has been parsed.
          }
        }
        eventType = xml.next();
      }
    } catch (XmlPullParserException e) {
      Log.e(TAG, e.getMessage(), e);
      return "";
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
      return "";
    } finally {
      xml.close();
    }
    changelogBuilder.append("</body></html>");

    if (releaseFound) {
      return changelogBuilder.toString();
    } else {
      return "";
    }
  }

  private void getHTML(final Resources resources, final int version) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        final String result = getHTMLChangelog(R.xml.changelog, resources, version);

        if (!TextUtils.isEmpty(result)) {
          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              showDialogWithHTML(resources, result);
            }
          });
        }
      }
    }).start();
  }

  private Resources getResources() {
    String packageName = mActivity.getPackageName();
    Resources resources = null;
    try {
      resources = mActivity.getPackageManager().getResourcesForApplication(packageName);
    } catch (NameNotFoundException ignored) {
    }
    return resources;
  }

  protected void showDialog(final int version) {
    final Resources resources = getResources();
    if (resources == null)
      return;

    // this creates a thread which shows the dialog if successful
    getHTML(resources, version);
  }

  private void showDialogWithHTML(Resources resources, String htmlChangelog) {
    CharSequence title = resources.getString(R.string.title_changelog);
    title = String.format("%s v%s", title, getAppVersion());

    final WebView webView = new WebView(mActivity);
    webView.loadDataWithBaseURL(null, htmlChangelog, "text/html", "utf-8", null);

    final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity).setTitle(title)
        .setView(webView)
        .setPositiveButton(resources.getString(R.string.changelog_close), new Dialog.OnClickListener() {
          public void onClick(final DialogInterface dialogInterface, final int i) {
            dialogInterface.dismiss();
          }
        })
        .setOnCancelListener(new OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            dialog.dismiss();
          }
        });
    builder.create().show();
  }

}

