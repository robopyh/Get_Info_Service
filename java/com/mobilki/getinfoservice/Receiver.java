package com.mobilki.getinfoservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent arg1){
        Intent intent = new Intent(context, MyService.class);
        context.startService(intent);
        Log.d("receiver", "trying to start service");
    }
}