package com.home.suntvboot;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

// Launcher tile: tapping it opens Sun TV directly, then finishes.
// (Opening it also clears the app's "stopped" state, which keeps the boot autostart armed.)
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(BootService.DEEPLINK));
            view.setPackage(BootService.TARGET_PKG);
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(view);
        } catch (Throwable t) {
            // Fallback: just open the Live TV app if the deep link is unavailable.
            try {
                Intent launch = getPackageManager().getLaunchIntentForPackage(BootService.TARGET_PKG);
                if (launch != null) { launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(launch); }
            } catch (Throwable ignored) {}
        }
        finish();
    }
}
