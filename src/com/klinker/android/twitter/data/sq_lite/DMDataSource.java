package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.klinker.android.twitter.utils.HtmlUtils;

import twitter4j.DirectMessage;
import twitter4j.MediaEntity;
import twitter4j.URLEntity;

public class DMDataSource {

    // provides access to the database
    public static DMDataSource dataSource = null;

    /*

    This is used so that we don't have to open and close the database on different threads or fragments
    every time. This will facilitate it between all of them to avoid Illegal State Exceptions.

     */
    public static DMDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        try {
            if (dataSource == null || !dataSource.getDatabase().isOpen()) {
                dataSource = new DMDataSource(context); // create the database
                dataSource.open(); // open the database
            }
        } catch (Exception e) {
            dataSource = new DMDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    // Database fields
    private SQLiteDatabase database;
    private DMSQLiteHelper dbHelper;
    public String[] allColumns = {DMSQLiteHelper.COLUMN_ID, DMSQLiteHelper.COLUMN_TWEET_ID, DMSQLiteHelper.COLUMN_ACCOUNT, DMSQLiteHelper.COLUMN_TYPE,
            DMSQLiteHelper.COLUMN_TEXT, DMSQLiteHelper.COLUMN_NAME, DMSQLiteHelper.COLUMN_PRO_PIC,
            DMSQLiteHelper.COLUMN_SCREEN_NAME, DMSQLiteHelper.COLUMN_TIME, DMSQLiteHelper.COLUMN_PIC_URL, DMSQLiteHelper.COLUMN_RETWEETER,
            DMSQLiteHelper.COLUMN_URL, HomeSQLiteHelper.COLUMN_USERS, HomeSQLiteHelper.COLUMN_HASHTAGS, DMSQLiteHelper.COLUMN_EXTRA_ONE, DMSQLiteHelper.COLUMN_EXTRA_TWO };

    public DMDataSource(Context context) {
        dbHelper = new DMSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
        database = null;
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public DMSQLiteHelper getHelper() {
        return dbHelper;
    }

    public void createDirectMessage(DirectMessage status, int account) {
        ContentValues values = new ContentValues();
        long time = status.getCreatedAt().getTime();

        values.put(DMSQLiteHelper.COLUMN_ACCOUNT, account);
        values.put(DMSQLiteHelper.COLUMN_TEXT, HtmlUtils.getHtmlStatus(status)[0]);
        values.put(DMSQLiteHelper.COLUMN_TWEET_ID, status.getId());
        values.put(DMSQLiteHelper.COLUMN_NAME, status.getSender().getName());
        values.put(DMSQLiteHelper.COLUMN_PRO_PIC, status.getSender().getBiggerProfileImageURL());
        values.put(DMSQLiteHelper.COLUMN_SCREEN_NAME, status.getSender().getScreenName());
        values.put(DMSQLiteHelper.COLUMN_TIME, time);
        values.put(DMSQLiteHelper.COLUMN_RETWEETER, status.getRecipientScreenName());
        values.put(DMSQLiteHelper.COLUMN_EXTRA_ONE, status.getRecipient().getBiggerProfileImageURL());
        values.put(DMSQLiteHelper.COLUMN_EXTRA_TWO, status.getRecipient().getName());

        MediaEntity[] entities = status.getMediaEntities();

        if (entities.length > 0) {
            values.put(DMSQLiteHelper.COLUMN_PIC_URL, entities[0].getMediaURL());
        }

        URLEntity[] urls = status.getURLEntities();
        for (URLEntity url : urls) {
            Log.v("inserting_dm", "url here: " + url.getExpandedURL());
            values.put(DMSQLiteHelper.COLUMN_URL, url.getExpandedURL());
        }

        if (database == null) {
            open();
        } else if (!database.isOpen() || !database.isDbLockedByCurrentThread()) {
            open();
        }

        database.insert(DMSQLiteHelper.TABLE_DM, null, values);
    }

    public void deleteTweet(long tweetId) {
        long id = tweetId;
        if (database == null) {
            open();
        } else if (!database.isOpen() || !database.isDbLockedByCurrentThread()) {
            open();
        }

        database.delete(DMSQLiteHelper.TABLE_DM, DMSQLiteHelper.COLUMN_TWEET_ID
                + " = " + id, null);
    }

    public void deleteAllTweets(int account) {
        if (database == null) {
            open();
        } else if (!database.isOpen() || !database.isDbLockedByCurrentThread()) {
            open();
        }

        database.delete(DMSQLiteHelper.TABLE_DM, DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
    }

    public Cursor getCursor(int account) {
        if (database == null) {
            open();
        } else if (!database.isOpen() || !database.isDbLockedByCurrentThread()) {
            open();
        }
        Cursor cursor = database.query(true, DMSQLiteHelper.TABLE_DM,
                allColumns, DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC", null);

        return cursor;
    }

    public Cursor getConvCursor(String name, int account) {
        if (database == null) {
            open();
        } else if (!database.isOpen() || !database.isDbLockedByCurrentThread()) {
            open();
        }
        Cursor cursor = database.query(true, DMSQLiteHelper.TABLE_DM,
                allColumns, DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND (" + DMSQLiteHelper.COLUMN_SCREEN_NAME + " = ? OR " + DMSQLiteHelper.COLUMN_RETWEETER + " = ?)", new String[] {name, name}, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " DESC", null);

        return cursor;
    }

    public String getNewestName(int account) {

        Cursor cursor = getCursor(account);
        String name = "";

        try {
            if (cursor.moveToLast()) {
                name = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_SCREEN_NAME));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return name;
    }

    public String getNewestMessage(int account) {

        Cursor cursor = getCursor(account);
        String message = "";

        try {
            if (cursor.moveToLast()) {
                message = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_TEXT));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return message;
    }

    public void deleteDups(int account) {
        if (database == null) {
            open();
        } else if (!database.isOpen() || database.isDbLockedByOtherThreads()) {
            open();
        }
        database.execSQL("DELETE FROM " + DMSQLiteHelper.TABLE_DM + " WHERE _id NOT IN (SELECT MIN(_id) FROM " + DMSQLiteHelper.TABLE_DM + " GROUP BY " + DMSQLiteHelper.COLUMN_TWEET_ID + ") AND " + DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account);
    }
}
