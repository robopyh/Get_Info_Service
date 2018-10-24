package com.mobilki.getinfoservice;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.ByteArrayInputStream;

public class UploadInfo extends AsyncTask {

    private DropboxAPI mDBApi;
    private String UploadingData;

    public UploadInfo(DropboxAPI mDBApi, String UploadingData){
        this.mDBApi = mDBApi;
        this.UploadingData = UploadingData;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(UploadingData.getBytes());
        try {
            DropboxAPI.Entry newEntry = mDBApi.putFile("/phone_info.txt", inputStream, UploadingData.length(), null, null);
            return true;
        } catch (DropboxUnlinkedException e) {
            Log.e("DbExampleLog", "User has unlinked.");
        } catch (DropboxException e) {
            Log.e("DbExampleLog", "Something went wrong while uploading.");
        }
        return false;
    }
}
