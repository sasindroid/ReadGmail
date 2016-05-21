package com.sasi.readgmail.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by sasikumarlakshmanan on 12/04/16.
 */
public class CSVDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 6;
    static final String DATABASE_NAME = "csv.db";


    /**
     * Create a helper object to create, open, and/or manage a database.
     * This method always returns very quickly.  The database is not actually
     * created or opened until one of {@link #getWritableDatabase} or
     * {@link #getReadableDatabase} is called.
     *
     * @param context to use to open or create the database
     * @param name    of the database file, or null for an in-memory database
     * @param factory to use for creating cursor objects, or null for the default
     * @param version number of the database (starting at 1); if the database is older,
     *                {@link #onUpgrade} will be used to upgrade the database; if the database is
     *                newer, {@link #onDowngrade} will be used to downgrade the database
     */
    public CSVDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
//        super(context, name, factory, version);
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public CSVDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        final String SQL_CREATE_CSV_ALL_TABLE = "CREATE TABLE " + CSVContract.CSVEntry.TABLE_NAME_CSV_ALL
                + " ("
                + CSVContract.CSVEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + CSVContract.CSVEntry.COLUMN_CSV_ID + " INTEGER UNIQUE NOT NULL, "
                + CSVContract.CSVEntry.COLUMN_CSV_GG_FLAG + " INTEGER DEFAULT 0, "
                + CSVContract.CSVEntry.COLUMN_CSV_CSV + " TEXT );";

        db.execSQL(SQL_CREATE_CSV_ALL_TABLE);

        final String SQL_CREATE_CSV_PARSED_TABLE = "CREATE TABLE " + CSVContract.CSVEntry.TABLE_NAME_CSV_PARSED
                + " ("
                + CSVContract.CSVEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + CSVContract.CSVEntry.COLUMN_CSVP_ID + " TEXT NOT NULL, "
                + CSVContract.CSVEntry.COLUMN_CSVP_GG_FLAG + " INTEGER DEFAULT 1, "
                + CSVContract.CSVEntry.COLUMN_CSVP_DATE + " TEXT NOT NULL, "
                + CSVContract.CSVEntry.COLUMN_CSVP_TIME + " TEXT, "
                + CSVContract.CSVEntry.COLUMN_CSVP_SERVER_NAME + " TEXT, "
                + CSVContract.CSVEntry.COLUMN_CSVP_CONNECTION_TYPE + " TEXT, "
                + CSVContract.CSVEntry.COLUMN_CSVP_DOWNLOAD_SPEED + " TEXT NOT NULL, "
                + CSVContract.CSVEntry.COLUMN_CSVP_UPLOAD_SPEED + " TEXT NOT NULL );";

        db.execSQL(SQL_CREATE_CSV_PARSED_TABLE);
    }

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     * <p>
     * <p>
     * The SQLite ALTER TABLE documentation can be found
     * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     * </p><p>
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + CSVContract.CSVEntry.TABLE_NAME_CSV_ALL);
        db.execSQL("DROP TABLE IF EXISTS " + CSVContract.CSVEntry.TABLE_NAME_CSV_PARSED);
        onCreate(db);
    }
}
