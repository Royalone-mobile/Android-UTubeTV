package com.sickboots.sickvideos.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.sickboots.sickvideos.misc.Util;
import com.sickboots.sickvideos.services.YouTubeServiceRequest;

import java.util.ArrayList;
import java.util.List;

public class DatabaseAccess {
  private Database mDB;
  private DatabaseTables.DatabaseTable mTable;
  private String mRequestIdentifier;
  private Context mContext;

  public DatabaseAccess(Context context, YouTubeServiceRequest request) {
    this(context, request.databaseTable());
  }

  public DatabaseAccess(Context context, DatabaseTables.DatabaseTable table) {
    super();

    mDB = Database.instance(context);
    mContext = context.getApplicationContext();
    mTable = table;
  }

  public void deleteAllRows(String requestIdentifier) {
    SQLiteDatabase db = mDB.getWritableDatabase();

    try {
      int result = db.delete(mTable.tableName(), mTable.whereClause(DatabaseTables.ALL_ITEMS, requestIdentifier), mTable.whereArgs(DatabaseTables.ALL_ITEMS, requestIdentifier));

      if (result > 0)
        notifyProviderOfChange();

    } catch (Exception e) {
      Util.log("deleteAllRows exception: " + e.getMessage());
    } finally {
    }
  }

  public void insertItems(List<YouTubeData> items) {
    if (items != null) {
      // Gets the data repository in write mode
      SQLiteDatabase db = mDB.getWritableDatabase();

      db.beginTransaction();
      try {
        for (YouTubeData item : items)
          db.insert(mTable.tableName(), null, mTable.contentValuesForItem(item));

        db.setTransactionSuccessful();

        notifyProviderOfChange();
      } catch (Exception e) {
        Util.log("Insert item exception: " + e.getMessage());
      } finally {
        db.endTransaction();
      }
    }
  }

  public YouTubeData getItemWithID(Long id) {
    YouTubeData result = null;

    Database.DatabaseQuery query = new Database.DatabaseQuery(mTable.tableName(), whereClauseForID(), whereArgsForID(id), mTable.projection(0));
    Cursor cursor = mDB.getCursor(query);

    if (cursor.moveToFirst()) {
      result = mTable.cursorToItem(cursor, null);
    } else {
      Util.log("getItemWithID not found or too many results?");
    }

    cursor.close();

    return result;
  }

  public Cursor getCursor(int flags, String requestIdentifier) {
    return getCursor(mTable.whereClause(flags, requestIdentifier), mTable.whereArgs(flags, requestIdentifier), mTable.projection(flags));
  }

  public Cursor getCursor(String whereClause, String[] whereArgs, String[] projection) {
    Database.DatabaseQuery query = new Database.DatabaseQuery(mTable.tableName(), whereClause, whereArgs, projection);

    return mDB.getCursor(query);
  }

  public List<YouTubeData> getItems(int flags, String requestIdentifier, int maxResults) {
    Cursor cursor = getCursor(flags, requestIdentifier);

    List<YouTubeData> result = getItems(cursor, maxResults);

    cursor.close();

    return result;
  }

  public void updateItem(YouTubeData item) {
    SQLiteDatabase db = mDB.getWritableDatabase();

    try {
      int result = db.update(mTable.tableName(), mTable.contentValuesForItem(item), whereClauseForID(), whereArgsForID(item.mID));

      if (result != 1)
        Util.log("updateItem didn't return 1");
      else
        notifyProviderOfChange();

    } catch (Exception e) {
      Util.log("updateItem exception: " + e.getMessage());
    } finally {
    }
  }

  // -----------------------------------------------------------------------------
  // private

  private void notifyProviderOfChange() {
    mContext.getContentResolver().notifyChange(YouTubeContentProvider.URI_CONTENTS, null);
  }

  private String whereClauseForID() {
    return "_id=?";
  }

  private String[] whereArgsForID(Long id) {
    return new String[]{id.toString()};
  }

  // pass 0 to maxResults if you don't care
  private List<YouTubeData> getItems(Cursor cursor, int maxResults) {
    List<YouTubeData> result = new ArrayList<YouTubeData>();
    boolean stopOnMaxResults = maxResults > 0;

    try {
      int cnt = 0;

      if (cursor.moveToFirst()) {
        while (!cursor.isAfterLast()) {
          result.add(mTable.cursorToItem(cursor, null));

          if (stopOnMaxResults) {
            if (++cnt == maxResults)
              break;
          }

          cursor.moveToNext();
        }
      }
    } catch (Exception e) {
      Util.log("getItems exception: " + e.getMessage());
    } finally {
    }

    return result;
  }
}
