package com.sasi.readgmail;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.db.chart.view.animation.Animation;
import com.sasi.readgmail.data.CSVContract;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class ChartActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ChartActivity";

    public static final String STOCK_SYMBOL = "STOCK_SYMBOL";

    public static final String fileName = "gg_speed_test.xlsx";

    LineChartView linechart;
    TextView tvChartHeader;
    Animation anim;

    private static final int CHART_LOADER = 1;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        linechart = (LineChartView) findViewById(R.id.linechart);
        tvChartHeader = (TextView) findViewById(R.id.tvChartHeader);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Building xls file into DOWNLOADS folder ...");

        getSupportLoaderManager().initLoader(CHART_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = CSVContract.CSVEntry.CONTENT_URI;
        String[] projection = new String[]{CSVContract.CSVEntry.COLUMN_CSVP_ID, CSVContract.CSVEntry.COLUMN_CSVP_GG_FLAG, CSVContract.CSVEntry.COLUMN_CSVP_DATE,
                CSVContract.CSVEntry.COLUMN_CSVP_TIME, CSVContract.CSVEntry.COLUMN_CSVP_CONNECTION_TYPE, CSVContract.CSVEntry.COLUMN_CSVP_DOWNLOAD_SPEED, CSVContract.CSVEntry.COLUMN_CSVP_UPLOAD_SPEED};
        String selection = CSVContract.CSVEntry.COLUMN_CSVP_CONNECTION_TYPE + " != " + "'Wifi' ";
//                + "AND " + CSVContract.CSVEntry.COLUMN_CSVP_DATE + " = '2016-04-21' AND "
//                + CSVContract.CSVEntry.COLUMN_CSVP_GG_FLAG + " = 1";
//        String[] selectionArgs = new String[]{"1"};
//        String sortOrder = QuoteColumns._ID + " DESC LIMIT " + getResources().getInteger(R.integer.stock_query_limit);

        return new android.support.v4.content.CursorLoader(getBaseContext(), uri, projection, selection, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        Log.d(TAG, "CURSOR COUNT: " + data.getCount());

        boolean exportToExcel = true;

        if (exportToExcel) {
            try {
                exportToExcel(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            if (data != null && data.getCount() > 0) {

                ArrayList<Float> dspeeds = new ArrayList<>();
                ArrayList<Float> uspeeds = new ArrayList<>();
                ArrayList<String> times = new ArrayList<>();

                for (int i = 0; i < data.getCount(); i++) {
                    data.moveToPosition(i);

                    dspeeds.add(Float.valueOf(data.getString(data.getColumnIndex(CSVContract.CSVEntry.COLUMN_CSVP_DOWNLOAD_SPEED))));
                    uspeeds.add(Float.valueOf(data.getString(data.getColumnIndex(CSVContract.CSVEntry.COLUMN_CSVP_UPLOAD_SPEED))));

                    times.add(data.getString(data.getColumnIndex(CSVContract.CSVEntry.COLUMN_CSVP_TIME)));
                }

                float[] dspeedArr = new float[dspeeds.size()];
                String[] timeArr = new String[times.size()];

                int i = 0;

                for (Float dspeed : dspeeds) {

                    dspeedArr[i++] = i + 0.5f;
                }

                int j = 0;

                for (String time : timeArr) {

                    timeArr[j++] = "";
                }


//            drawMyChart(timeArr, dspeedArr);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    private void exportToExcel(Cursor cursor) throws IOException {

        mProgress.show();

        tvChartHeader.setText("Building xls file into DOWNLOADS folder ...");

        Log.d(TAG, "CURSOR COUNT EXPORT: " + cursor.getCount());

        //Saving file in external storage
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//        File directory = new File(sdCard.getAbsolutePath() + "/data");

//        ContextWrapper cw = new ContextWrapper(this);
//        java.io.File directory = cw.getDir("amedia", Context.MODE_PRIVATE);

        //create directory if not exist
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }

        //file path
        File file = new File(directory, fileName);

        WorkbookSettings wbSettings = new WorkbookSettings();
        wbSettings.setLocale(new Locale("en", "EN"));
        WritableWorkbook workbook;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currDateTime = sdf.format(new Date());

        try {

            workbook = Workbook.createWorkbook(file, wbSettings);

            //Excel sheet name. 0 represents first sheet
            WritableSheet sheet = workbook.createSheet(currDateTime, 0);

            try {
                sheet.addCell(new Label(0, 0, CSVContract.CSVEntry.COLUMN_CSVP_ID));
                sheet.addCell(new Label(1, 0, CSVContract.CSVEntry.COLUMN_CSVP_GG_FLAG));
                sheet.addCell(new Label(2, 0, CSVContract.CSVEntry.COLUMN_CSVP_DATE));
                sheet.addCell(new Label(3, 0, CSVContract.CSVEntry.COLUMN_CSVP_TIME));
                sheet.addCell(new Label(4, 0, CSVContract.CSVEntry.COLUMN_CSVP_CONNECTION_TYPE));
                sheet.addCell(new Label(5, 0, CSVContract.CSVEntry.COLUMN_CSVP_DOWNLOAD_SPEED));
                sheet.addCell(new Label(6, 0, CSVContract.CSVEntry.COLUMN_CSVP_UPLOAD_SPEED));

                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    int id = cursor.getInt(cursor.getColumnIndex(CSVContract.CSVEntry.COLUMN_CSVP_ID));
                    String flag = cursor.getString(cursor.getColumnIndex(CSVContract.CSVEntry.COLUMN_CSVP_GG_FLAG));
                    String date = cursor.getString(cursor.getColumnIndex(CSVContract.CSVEntry.COLUMN_CSVP_DATE));
                    String time = cursor.getString(cursor.getColumnIndex(CSVContract.CSVEntry.COLUMN_CSVP_TIME));
                    String conn = cursor.getString(cursor.getColumnIndex(CSVContract.CSVEntry.COLUMN_CSVP_CONNECTION_TYPE));
                    String dspeed = cursor.getString(cursor.getColumnIndex(CSVContract.CSVEntry.COLUMN_CSVP_DOWNLOAD_SPEED));
                    String uspeed = cursor.getString(cursor.getColumnIndex(CSVContract.CSVEntry.COLUMN_CSVP_UPLOAD_SPEED));

                    String flagStr = "";

                    switch (flag) {
                        case "1":
                            flagStr = "gg";
                            break;
                        case "0":
                            flagStr = "o2";
                            break;
                        default:
                            flagStr = "un";
                    }

                    sheet.addCell(new Label(0, i + 1, "" + id));
                    sheet.addCell(new Label(1, i + 1, flagStr));
                    sheet.addCell(new Label(2, i + 1, date));
                    sheet.addCell(new Label(3, i + 1, time));
                    sheet.addCell(new Label(4, i + 1, conn));
                    sheet.addCell(new Label(5, i + 1, dspeed));
                    sheet.addCell(new Label(6, i + 1, uspeed));
                }

                //closing cursor
//                cursor.close();
            } catch (RowsExceededException e) {
                e.printStackTrace();
            } catch (WriteException e) {
                e.printStackTrace();
            }
            workbook.write();
            try {
                workbook.close();
            } catch (WriteException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mProgress.dismiss();
        tvChartHeader.setText("Check your DOWNLOADS folder for the file named - " + fileName);

    }

    @Override
    protected void onPause() {

        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }

        super.onPause();
    }

    private void drawMyChart(String[] labels, float[] values) {

        tvChartHeader.setText("Comparison gg vs o2");

//        Log.d(TAG, "LABELS: " + labels.toString());
//        Log.d(TAG, "VALUES: " + values.toString());

        String tempLabels = "";

        for (String label : labels) {
            tempLabels = tempLabels + label;
        }

        String tempValue = "";

        for (float value : values) {
            tempValue = tempValue + String.valueOf(value);
        }

        Log.d(TAG, "LABELS: " + tempLabels);
        Log.d(TAG, "VALUES: " + tempValue);

        linechart.setYLabels(AxisController.LabelPosition.OUTSIDE);
        linechart.setXLabels(AxisController.LabelPosition.OUTSIDE);

        LineSet dataset = new LineSet(labels, values);
//        dataset.addPoint("sas", 750.0f);

        // Dots
        dataset.setDotsColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        dataset.setDotsRadius(6.0f);

        // Line
        dataset.setThickness(3.5f);
        dataset.setColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));

//        setBorderValues(linechart);

        linechart.setXAxis(true);
        linechart.addData(dataset);

        linechart.setLabelsFormat(new DecimalFormat());
        linechart.setAxisBorderValues(0, 50);

//        anim = new Animation();
//        anim.setDuration(500);
//        anim.setEasing(new CubicEase());
//        anim.setAlpha(3);
//        linechart.show(anim);

        linechart.show();
    }

//    public void goToDownloads(View view) {
//        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
//    }

    public void openSheet(View view) {
        File xls = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        Uri path = Uri.fromFile(xls);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "application/vnd.ms-excel");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No Application available to view XLS", Toast.LENGTH_SHORT).show();
        }
    }
}
