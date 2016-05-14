package com.sasi.readgmail;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.sasi.readgmail.data.CSVContract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import au.com.bytecode.opencsv.CSVReadProc;
import au.com.bytecode.opencsv.CSVReader;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MainActivity";
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "GET DATA FROM GMAIL";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {GmailScopes.GMAIL_LABELS};

    TextView mOutputText, mStatus;
    Button mCallApiButton, mCallChartButton;

    List<Message> messageList;

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mOutputText = (TextView) findViewById(R.id.mOutputText);
        mCallApiButton = (Button) findViewById(R.id.mCallApiButton);
        mCallChartButton = (Button) findViewById(R.id.mCallChartButton);
        mStatus = (TextView) findViewById(R.id.mStatus);

        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
            }
        });

        mCallChartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ChartActivity.class));
            }
        });

        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        mOutputText.setText(
                "Click the \'" + BUTTON_TEXT + "\' button to get the updated feed from Gmail. \nThen click on the EXPORT TO EXCEL button to generate " +
                        "xls file which can be found in the DOWNLOAD folder.");

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Gmail API ...");

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Gmail API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Gmail API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
//                return getDataFromApi();
                return getDataFromApi2();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of Gmail labels attached to the specified account.
         *
         * @return List of Strings labels.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get the labels in the user's account.
            String user = "me";
            List<String> labels = new ArrayList<String>();
            ListLabelsResponse listResponse;
            listResponse = mService.users().labels().list(user).execute();

            for (Label label : listResponse.getLabels()) {
                labels.add(label.getName());
            }

            return labels;
        }

        private List<String> getDataFromApi2() throws IOException {
            // Get the labels in the user's account.
            String user = "me";
            List<String> messages = new ArrayList<String>();
//            ListMessagesResponse listResponse = mService.users().messages().list(user).execute();

            String[] labelArr = new String[]{"INBOX"};
            List<String> labelList = Arrays.asList(labelArr);

            ListMessagesResponse listResponse = mService.users().messages().list(user).setLabelIds(labelList).setQ("Ookla Speedtest Results").execute();

            String msgID = null;

            messageList = listResponse.getMessages();

            if (messageList != null && messageList.size() > 0) {

                // Delete from the table.
                getContentResolver().delete(CSVContract.CSVEntry.CONTENT_URI, null, null);
            }

            for (Message message : messageList) {

                msgID = message.getId();

                messages.add(msgID);

                try {
                    int match = getMimeMessage(msgID);
                    String s = getBody(msgID);

//                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//                    SharedPreferences.Editor editor = sp.edit();
//                    editor.putString(msgID, s);
//                    editor.commit();

                    parseCSV(msgID, s, match);

                    messages.add(s);

                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }

            return messages;
        }

//        private void getMessageForID(String id) {
//
//            String user = "me";
//
//            try {
//                Message msg = mService.users().messages().get(user, id).setFormat("raw").execute();
//                byte[] emailBytes = Base64.decodeBase64(msg.getRaw());
//
//                Properties props = new Properties();
//                Session session = Session.getDefaultInstance(props, null);
//                MimeMessage email = new MimeMessage(session, new ByteArrayInputStream(emailBytes));
//
//                email.toString();
//
//                Log.d(TAG, "-> " + email.getSubject());
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (MessagingException e) {
//                e.printStackTrace();
//            }
//        }

        public int getMimeMessage(String messageId)
                throws IOException, MessagingException {

            String user = "me";

            // Format allowed: full, metadata, minimal, raw
            Message message = mService.users().messages().get(user, messageId).setFormat("raw").execute();

            byte[] emailBytes = Base64.decodeBase64(message.getRaw());

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage email = new MimeMessage(session, new ByteArrayInputStream(emailBytes));

            String subject = email.getSubject().toUpperCase();

            Log.d(TAG, messageId + " -> " + subject);

            int match = subject.indexOf("O2");

            if (match > -1) {
                return 0;
            }

            match = subject.indexOf("GG");

            if (match > -1) {
                return 1;
            }

            return -1;
        }

        private String getBody(String messageId) {

            String user = "me";

            try {
                Message message = mService.users().messages().get(user, messageId).execute();
                List<MessagePart> parts = message.getPayload().getParts();

//                Log.d(TAG, messageId + " -> " + parts.size());

                Properties props = new Properties();
                Session session = Session.getDefaultInstance(props, null);

                int i = 0;

                String s = null;

                for (MessagePart part : parts) {

                    byte[] emailBytes = Base64.decodeBase64(part.getBody().toString());
                    MimeBodyPart bodyPart = new MimeBodyPart(new ByteArrayInputStream(emailBytes));

                    s = s + bodyPart.getContent().toString();
//                    Log.d(TAG, ++i + "**************************" + " -> " + s);
                }

                if (s != null) {

                    String header = "Date,ConnType,Lat,Lon,Download,Upload,Latency,ServerName,InternalIp,ExternalIp";
                    int space = 1;

                    int fromVal = s.indexOf(header) + header.length() + space;
                    s = s.substring(fromVal);
                }


//                Log.d(TAG, "\\r: " + s.indexOf("\r"));
//                Log.d(TAG, "\\n: " + s.indexOf("\n"));
//                Log.d(TAG, "\\r\\n: " + s.indexOf("\r\n"));

                // Replace LF (\n) CR (\r)
                s = s.replace("\r\n", "*$*");
                s = s.replace("\"*$*", "\"\r\n");
                s = s.replace("*$*", " ");

                return s;

            } catch (IOException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                e.printStackTrace();
            }

            return "";
        }

        private void parseCSV(final String key, String val, final int match) {

//            String val = "";
//
//            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//            val = sp.getString(key, null);

//            Date,ConnType,Lat,Lon,Download,Upload,Latency,ServerName,InternalIp,ExternalIp
            CSVReader reader = new CSVReader(new StringReader(val), ',', '\"', 1);

            final ArrayList<ContentValues> valuesArrayList = new ArrayList<>();

            reader.read(new CSVReadProc() {
                @Override
                public void procRow(int rowIndex, String... values) {

                    ContentValues cv = new ContentValues();
                    String dateTime = values[0];

                    try {
                        cv.put(CSVContract.CSVEntry.COLUMN_CSVP_ID, key);
                        cv.put(CSVContract.CSVEntry.COLUMN_CSVP_GG_FLAG, match);

//                        Log.d(TAG, key + "DATETIME: $" + dateTime);
//                        Log.d(TAG, "TIME: $" + dateTime.substring(11) + "$");

                        cv.put(CSVContract.CSVEntry.COLUMN_CSVP_DATE, dateTime.substring(0, 10));
                        cv.put(CSVContract.CSVEntry.COLUMN_CSVP_TIME, dateTime.substring(11).replace(":", "."));
                        cv.put(CSVContract.CSVEntry.COLUMN_CSVP_CONNECTION_TYPE, values[1]);
                        cv.put(CSVContract.CSVEntry.COLUMN_CSVP_DOWNLOAD_SPEED, String.valueOf(values[4]));
                        cv.put(CSVContract.CSVEntry.COLUMN_CSVP_UPLOAD_SPEED, String.valueOf(values[5]));

                        valuesArrayList.add(cv);
                    } catch (Exception e) {

//                        Log.d(TAG, "dateTime: " + dateTime);

                        e.printStackTrace();
                    }
                }
            });

            // Insert into db.
            if (valuesArrayList != null && valuesArrayList.size() > 0) {
                getContentResolver().bulkInsert(CSVContract.CSVEntry.CONTENT_URI, valuesArrayList.toArray(new ContentValues[valuesArrayList.size()]));
            }
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();

            mStatus.setText("No. of reports received: " + (messageList != null ? messageList.size() : 0));

            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Gmail API:");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
}
