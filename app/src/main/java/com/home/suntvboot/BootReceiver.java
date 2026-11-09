package com.home.suntvboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "null" : intent.getAction();
        Log.i("SunTVBoot", "BootReceiver fired: " + action);
        Intent svc = new Intent(context, BootService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(svc);
            } else {
                context.startService(svc);
            }
        } catch (Throwable t) {
            Log.e("SunTVBoot", "startService failed", t);
        }
    }
}
