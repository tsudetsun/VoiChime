package com.tsudetsun.voichime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import android.util.Log;

public class TimeSignalService extends Service {
    private Handler handler = new Handler();
    private Runnable timeUpdater;
    private boolean hasPlayed = false;
    private boolean hasPlayedBeep = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Intent stopIntent = new Intent(this, StopServiceReceiver.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, pendingFlags);

        Notification notification = new NotificationCompat.Builder(this, "timesignal_channel")
                .setContentTitle("時報アプリ起動中")
                .setContentText("バックグラウンドで時刻を監視しています")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .addAction(R.drawable.ic_stop, "停止", stopPendingIntent) // ← アクション追加
                .build();

        startForeground(1, notification);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isSignalEnabled = prefs.getBoolean("signalEnabled", true);
        if (!isSignalEnabled) {
            stopSelf(); // サービスを即終了
            return START_NOT_STICKY;
        }


        if (timeUpdater == null) {
            timeUpdater = new Runnable() {
                @Override
                public void run() {
                    Calendar calendar = Calendar.getInstance();
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);
                    int second = calendar.get(Calendar.SECOND);

                    boolean isBeepEnabled = prefs.getBoolean("beepEnabled", true);

                    int intervalMinutes = prefs.getInt("intervalMinutes", 30); // デフォルト30分

                    String selectedVoice = prefs.getString("voiceType", "tsukuyomichan");

                    if (isBeepEnabled && (minute % intervalMinutes == 0) && second == 1 && !hasPlayed) {
                        playChime(selectedVoice, hour, minute);
                    }

                    if (!isBeepEnabled && (minute % intervalMinutes == 0) && second == 0 && !hasPlayed) {
                        playChime(selectedVoice, hour, minute);
                    }

                    if (minute != 0 && minute != 30) {
                        hasPlayed = false;
                    }

                    if (isBeepEnabled && ((minute + 1) % intervalMinutes == 0) && second == 56 && !hasPlayedBeep) {
                        playBeepChime();
                        hasPlayedBeep = true;
                    }

                    if (second == 0) {
                        hasPlayedBeep = false;
                    }

                    handler.postDelayed(this, 1000);
                }
            };
            handler.post(timeUpdater);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("TimeSignalService", "サービスが停止されました");
        handler.removeCallbacks(timeUpdater);
        timeUpdater = null;
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "timesignal_channel",
                    "時報通知",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("時報アプリがバックグラウンドで動作中です");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void playBeepChime() {
        MediaPlayer beepPlayer = MediaPlayer.create(this, R.raw.time_signal_beep);
        beepPlayer.setOnCompletionListener(mp -> {
            mp.release();
        });
        beepPlayer.start();
    }

    public void playChime(String selectedVoice, int hour, int minute) {
        int introResId = getResources().getIdentifier(selectedVoice + "_intro", "raw", getPackageName());
        int hourResId = getResources().getIdentifier(selectedVoice + "_hour" + hour, "raw", getPackageName());
        int minuteResId = getResources().getIdentifier(selectedVoice + "_minute" + minute, "raw", getPackageName());
        int outroResId = getResources().getIdentifier(selectedVoice + "_outro", "raw", getPackageName());

        if (introResId != 0) {
            MediaPlayer introPlayer = MediaPlayer.create(TimeSignalService.this, introResId);
            MediaPlayer hourPlayer = MediaPlayer.create(TimeSignalService.this, hourResId);
            MediaPlayer minutePlayer = MediaPlayer.create(TimeSignalService.this, minuteResId);
            MediaPlayer outroPlayer = MediaPlayer.create(TimeSignalService.this, outroResId);


            introPlayer.setOnCompletionListener(mp -> {
                hourPlayer.start();
                introPlayer.release();
            });

            hourPlayer.setOnCompletionListener(mp -> {
                minutePlayer.start();
                hourPlayer.release();
            });

            minutePlayer.setOnCompletionListener(mp -> {
                outroPlayer.start();
                minutePlayer.release();
            });

            outroPlayer.setOnCompletionListener(mp -> {
                outroPlayer.release();
            });

            introPlayer.start();
            hasPlayed = true;
        }
    }

}
