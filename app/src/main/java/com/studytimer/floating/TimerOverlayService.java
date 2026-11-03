package com.studytimer.floating;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TimerOverlayService extends Service {
    private static final String CHANNEL_ID = "study_timer_running";
    private static final int NOTIFICATION_ID = 7101;
    private static final long PERIOD_MS = 10_000L;
    private static final String ACTION_STOP = "com.studytimer.floating.STOP";
    private static final String ACTION_TOGGLE = "com.studytimer.floating.TOGGLE";

    private final Handler handler = new Handler();
    private WindowManager windowManager;
    private WindowManager.LayoutParams overlayParams;
    private View overlay;
    private TextView counter;
    private TextView state;
    private Button pauseButton;
    private long cycleStart;
    private long pausedRemaining = PERIOD_MS;
    private boolean paused;
    private boolean overlayAdded;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            updateCounter();
            handler.postDelayed(this, 100L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startAsForeground();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        cycleStart = SystemClock.elapsedRealtime();
        paused = false;
        addOverlay();
        handler.post(ticker);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (ACTION_STOP.equals(intent.getAction())) {
                stopSelf();
                return START_NOT_STICKY;
            }
            if (ACTION_TOGGLE.equals(intent.getAction())) togglePause();
        }
        return START_STICKY;
    }

    private void startAsForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Floating study timer", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the repeating timer running.");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private PendingIntent servicePendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, TimerOverlayService.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(this, requestCode, intent, flags);
    }

    private Notification buildNotification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Study Timer Floating")
                .setContentText(paused ? "Timer paused" : "Repeating every 10 seconds")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,
                        paused ? "Resume" : "Pause",
                        servicePendingIntent(ACTION_TOGGLE, 2)).build())
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Stop",
                        servicePendingIntent(ACTION_STOP, 3)).build());

        Intent open = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        builder.setContentIntent(PendingIntent.getActivity(this, 1, open, flags));
        return builder.build();
    }

    private void refreshNotification() {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, buildNotification());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void addOverlay() {
        if (overlayAdded || windowManager == null) return;

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.setGravity(Gravity.CENTER_VERTICAL);
        shell.setPadding(dp(9), dp(5), dp(7), dp(5));

        GradientDrawable background = new GradientDrawable();
        background.setColor(0xF0192638);
        background.setCornerRadius(dp(19));
        background.setStroke(dp(1), 0xFF31D18A);
        shell.setBackground(background);
        shell.setElevation(dp(10));

        counter = new TextView(this);
        counter.setText("10");
        counter.setTextColor(0xFFFFFFFF);
        counter.setTextSize(32);
        counter.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        counter.setGravity(Gravity.CENTER);
        shell.addView(counter, new LinearLayout.LayoutParams(dp(70), dp(58)));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER);

        pauseButton = new Button(this);
        pauseButton.setText("Ⅱ");
        pauseButton.setTextSize(12);
        pauseButton.setMinWidth(0);
        pauseButton.setMinimumWidth(0);
        pauseButton.setPadding(0, 0, 0, 0);
        pauseButton.setOnClickListener(v -> togglePause());

        Button stopButton = new Button(this);
        stopButton.setText("×");
        stopButton.setTextSize(15);
        stopButton.setMinWidth(0);
        stopButton.setMinimumWidth(0);
        stopButton.setPadding(0, 0, 0, 0);
        stopButton.setOnClickListener(v -> stopSelf());

        controls.addView(pauseButton, new LinearLayout.LayoutParams(dp(48), dp(30)));
        controls.addView(stopButton, new LinearLayout.LayoutParams(dp(48), dp(30)));
        shell.addView(controls);

        state = new TextView(this);
        state.setText("10s");
        state.setTextColor(0xFF31D18A);
        state.setTextSize(9);
        state.setGravity(Gravity.CENTER);
        shell.addView(state, new LinearLayout.LayoutParams(dp(28), dp(58)));

        overlay = shell;
        overlayParams = new WindowManager.LayoutParams(
                dp(166), dp(72),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        overlayParams.gravity = Gravity.TOP | Gravity.RIGHT;
        overlayParams.x = dp(6);
        overlayParams.y = dp(105);

        View.OnTouchListener drag = new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            private boolean dragging;

            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = overlayParams.x;
                        startY = overlayParams.y;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (Math.abs(dx) > dp(4) || Math.abs(dy) > dp(4)) dragging = true;
                        if (dragging) {
                            overlayParams.x = Math.max(0, startX - (int) dx);
                            overlayParams.y = Math.max(0, startY + (int) dy);
                            try { windowManager.updateViewLayout(overlay, overlayParams); }
                            catch (Exception ignored) {}
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                }
                return false;
            }
        };
        shell.setOnTouchListener(drag);
        counter.setOnTouchListener(drag);
        state.setOnTouchListener(drag);

        try {
            windowManager.addView(overlay, overlayParams);
            overlayAdded = true;
            updateCounter();
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void togglePause() {
        if (paused) {
            paused = false;
            cycleStart = SystemClock.elapsedRealtime() - (PERIOD_MS - pausedRemaining);
        } else {
            long elapsed = SystemClock.elapsedRealtime() - cycleStart;
            long inCycle = elapsed % PERIOD_MS;
            pausedRemaining = PERIOD_MS - inCycle;
            if (pausedRemaining <= 0) pausedRemaining = PERIOD_MS;
            paused = true;
        }
        if (pauseButton != null) pauseButton.setText(paused ? "▶" : "Ⅱ");
        if (state != null) state.setText(paused ? "PAUSE" : "10s");
        refreshNotification();
        updateCounter();
    }

    private void updateCounter() {
        long remaining;
        if (paused) {
            remaining = pausedRemaining;
        } else {
            long elapsed = SystemClock.elapsedRealtime() - cycleStart;
            remaining = PERIOD_MS - (elapsed % PERIOD_MS);
        }
        int seconds = (int) Math.max(1, (remaining + 999) / 1000);
        if (counter != null) counter.setText(String.valueOf(seconds));
        if (state != null) state.setText(paused ? "PAUSE" : "10s");
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(ticker);
        if (overlayAdded && windowManager != null && overlay != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) {}
        }
        overlayAdded = false;
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
