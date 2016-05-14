package com.sasi.readgmail.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by sasikumarlakshmanan on 12/04/16.
 */
public class CSVContract {

    public static final String CONTENT_AUTHORITY = "com.sasi.readgmail";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_CSV = "csv";
    public static final String PATH_CSV_PARSED = "csv_parsed";

    public static final class CSVEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_CSV).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
                + CONTENT_AUTHORITY + "/" + PATH_CSV;
        public static final String CONTENT_ITEM_BASE_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/" + CONTENT_AUTHORITY + "/" + PATH_CSV;

        public static final String TABLE_NAME_CSV_ALL = "csv_all";

        public static final String COLUMN_CSV_ID = "id";
        public static final String COLUMN_CSV_GG_FLAG = "gg_flag";
        public static final String COLUMN_CSV_CSV = "csv";


        public static final String TABLE_NAME_CSV_PARSED = "csv_parsed";

        public static final String COLUMN_CSVP_ID = "id";
        public static final String COLUMN_CSVP_GG_FLAG = "gg_flag";
        public static final String COLUMN_CSVP_DATE = "date_run";
        public static final String COLUMN_CSVP_TIME = "time_run";
        public static final String COLUMN_CSVP_CONNECTION_TYPE = "con_type";
        public static final String COLUMN_CSVP_DOWNLOAD_SPEED = "dspeed";
        public static final String COLUMN_CSVP_UPLOAD_SPEED = "uspeed";

        public static Uri buildCSVUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

    }

}
