package com.sasi.readgmail.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class CSVContentProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private CSVDBHelper mOpenHelper;

    static final int CSV = 100;

    static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        final String authority = CSVContract.CONTENT_AUTHORITY;

        // For each type of Uri create a corresponding code.
        matcher.addURI(authority, CSVContract.PATH_CSV, CSV);

        return matcher;
    }


    public CSVContentProvider() {
    }

    @Override
    public boolean onCreate() {
        // Implement this to initialize your content provider on startup.
        mOpenHelper = new CSVDBHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        // Implement this to handle requests for the MIME type of the data
        // at the given URI.

        // Use the Uri matcher to determine what kind of Uri this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case CSV:
                return CSVContract.CSVEntry.CONTENT_TYPE;

            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Implement this to handle requests to insert a new row.

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case CSV:

                // In case of a conflict replace it.
                long _id = 0;

                try {
                    _id = db.insert(CSVContract.CSVEntry.TABLE_NAME_CSV_PARSED, null, values);
                } catch (Exception e) {

                }

                if (_id > -1) {
                    returnUri = CSVContract.CSVEntry.buildCSVUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }

                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        int rowsDeleted = 0;

        // This makes delete all rows.
        if (null == selection) {
            selection = "1";
        }

        switch (match) {
            case CSV:

                rowsDeleted = db.delete(CSVContract.CSVEntry.TABLE_NAME_CSV_PARSED, selection, selectionArgs);
                break;

            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // Implement this to handle requests to update one or more rows.

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        int rowsUpdated = 0;

        switch (match) {

            case CSV:

                rowsUpdated = db.update(CSVContract.CSVEntry.TABLE_NAME_CSV_PARSED, values, selection, selectionArgs);
                break;

            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // Implement this to handle query requests from clients.

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        Cursor retCursor;

        switch (match) {

            case CSV:

                retCursor = db.query(CSVContract.CSVEntry.TABLE_NAME_CSV_PARSED, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case CSV:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(CSVContract.CSVEntry.TABLE_NAME_CSV_PARSED, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;

            default:
                return super.bulkInsert(uri, values);
        }
    }
}
