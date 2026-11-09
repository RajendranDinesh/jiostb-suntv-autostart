package com.home.suntvboot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

public class BootService extends Service {
    static final String TAG = "SunTVBoot";
    static final String DEEPLINK = "jiotvplus://304189?resume=true&source=watchnext";
    static final String TARGET_PKG = "com.jio.media.jiotvplus";

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAsForeground();
        new Thread(new Runnable() {
            @Override public void run() { work(); }
        }, "suntv-boot").start();
        return START_NOT_STICKY;
    }

    private void work() {
        // 1) Wait for network (poll up to ~120s)
        int waited = 0;
        while (!isConnected() && waited < 120000) { sleep(3000); waited += 3000; }
        Log.i(TAG, "network connected=" + isConnected() + " after " + waited + "ms");
        // 2) Let DNS/Rethink/registration settle
        sleep(10000);
        // 3) Fire ONCE, then only re-fire if JioTV+ isn't already on top.
        //    This brings Sun TV up without reloading it over and over.
        int fires = 0;
        for (int i = 1; i <= 2; i++) {          // at most 2 fires
            if (isTargetForeground()) {
                Log.i(TAG, "JioTV+ already foreground after " + fires + " fire(s); stopping.");
                break;
            }
            fireDeepLink(i);
            fires++;
            sleep(18000);  // let it fully come up + usage-stats to catch up before re-checking
        }
        Log.i(TAG, "done; stopping service");
        stopSelf();
    }

    private void fireDeepLink(int attempt) {
        try {
            Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(DEEPLINK));
            view.setPackage(TARGET_PKG);
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_FROM_BACKGROUND);
            startActivity(view);
            Log.i(TAG, "fired deep link attempt " + attempt);
        } catch (Throwable t) {
            Log.e(TAG, "fire attempt " + attempt + " failed", t);
            try {
                Intent launch = getPackageManager().getLaunchIntentForPackage(TARGET_PKG);
                if (launch != null) { launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(launch); }
            } catch (Throwable t2) { Log.e(TAG, "fallback launch failed", t2); }
        }
    }

    // True if JioTV+ is the current foreground app (needs PACKAGE_USAGE_STATS, granted via appops).
    private boolean isTargetForeground() {
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return false;
            long end = System.currentTimeMillis();
            long begin = end - 120000;
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begin, end);
            if (stats == null || stats.isEmpty()) return false;
            String top = null; long topTime = 0;
            for (UsageStats u : stats) {
                if (u.getLastTimeUsed() > topTime) { topTime = u.getLastTimeUsed(); top = u.getPackageName(); }
            }
            return TARGET_PKG.equals(top);
        } catch (Throwable t) { return false; }
    }

    private boolean isConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Throwable t) { return false; }
    }

    private void startAsForeground() {
        try {
            String chId = "suntvboot";
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel ch = new NotificationChannel(chId, "Sun TV Autostart", NotificationManager.IMPORTANCE_MIN);
                if (nm != null) nm.createNotificationChannel(ch);
                Notification n = new Notification.Builder(this, chId)
                        .setContentTitle("Starting Sun TV")
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .build();
                startForeground(1, n);
            }
        } catch (Throwable t) { Log.e(TAG, "foreground failed", t); }
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) {} }
}
