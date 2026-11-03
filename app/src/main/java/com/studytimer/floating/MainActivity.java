package com.studytimer.floating;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQ_OVERLAY = 501;
    private static final int REQ_NOTIFICATIONS = 502;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout.LayoutParams params() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(26), dp(30), dp(26), dp(30));
        root.setBackgroundColor(0xFF101827);

        TextView title = new TextView(this);
        title.setText("STUDY TIMER");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);

        TextView subtitle = new TextView(this);
        subtitle.setText("Repeating 10-second floating countdown");
        subtitle.setTextColor(0xFFB6C4D6);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(10), 0, dp(28));

        TextView preview = new TextView(this);
        preview.setText("00:10");
        preview.setTextColor(0xFF31D18A);
        preview.setTextSize(64);
        preview.setGravity(Gravity.CENTER);
        preview.setTypeface(null, 1);
        preview.setPadding(0, dp(12), 0, dp(24));

        Button start = new Button(this);
        start.setText("Start floating timer");
        start.setAllCaps(false);
        start.setTextSize(16);
        start.setOnClickListener(v -> requestPermissionsAndStart());

        Button stop = new Button(this);
        stop.setText("Stop timer");
        stop.setAllCaps(false);
        stop.setOnClickListener(v -> stopService(new Intent(this, TimerOverlayService.class)));

        status = new TextView(this);
        status.setText("Press Start once. The timer will keep repeating.");
        status.setTextColor(0xFFB6C4D6);
        status.setGravity(Gravity.CENTER);
        status.setTextSize(13);
        status.setPadding(0, dp(20), 0, 0);

        root.addView(title, params());
        root.addView(subtitle, params());
        root.addView(preview, params());
        root.addView(start, params());
        root.addView(stop, params());
        root.addView(status, params());
        setContentView(root);
    }

    private void requestPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            status.setText("Allow Display over other apps, then return here.");
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, REQ_OVERLAY);
            return;
        }
        requestNotificationThenStart();
    }

    private void requestNotificationThenStart() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
            return;
        }
        startTimerService();
    }

    private void startTimerService() {
        Intent service = new Intent(this, TimerOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
        status.setText("Running. You can switch to ChatGPT now.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                requestNotificationThenStart();
            } else {
                status.setText("Overlay permission is required.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS) {
            startTimerService();
        }
    }
}
