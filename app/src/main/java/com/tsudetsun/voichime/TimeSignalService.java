package com.tsudetsun.voichime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.util.Calendar;
import android.util.Log;

public class TimeSignalService extends Service {
    private Handler handler = new Handler();
    private Runnable timeUpdater;
    private boolean hasPlayed = false;
    private boolean hasPlayedBeep = false;
    private static final String TAG = "ChimeService";
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {};
    private RandomVoicePreset randomVoicePreset;
    private static final String CHANNEL_ID = "timesignal_channel";
    private static final int NOTIFICATION_ID = 1;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showForegroundNotification();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (intent != null && intent.getBooleanExtra("isPreview", false)) {
            int result = audioManager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            );

            String voiceId = intent.getStringExtra("selectedVoice");

            if ("random".equals(voiceId)) {
                if (randomVoicePreset == null) {
                    randomVoicePreset = new RandomVoicePreset(this);
                }
                voiceId = randomVoicePreset.getRandomVoiceId();
            }

            float volume = intent.getFloatExtra("volume", 1.0f); // デフォルト最大音量
            playChime(voiceId, 0, 0, volume);
            return START_NOT_STICKY;
        }


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
                    float volume = prefs.getInt("volume", 100) / 100f;
                    Calendar calendar = Calendar.getInstance();
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);
                    int second = calendar.get(Calendar.SECOND);
                    long lastChimeTime = 0;

                    boolean isBeepEnabled = prefs.getBoolean("beepEnabled", true);

                    int intervalMinutes = prefs.getInt("intervalMinutes", 30); // デフォルト30分

                    String selectedVoice = getVoiceType();

                    if (isBeepEnabled && (minute % intervalMinutes == 0) && second == 1 && !hasPlayed) {
                        playChime(selectedVoice, hour, minute, volume);
                    }

                    if (!isBeepEnabled && (minute % intervalMinutes == 0) && second == 0 && !hasPlayed) {
                        playChime(selectedVoice, hour, minute, volume);
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

                    if ((minute % intervalMinutes == 0) && second == 10 && !hasPlayed){
                        audioManager.abandonAudioFocus(focusChangeListener);
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
                    CHANNEL_ID,
                    "時報通知",
                    NotificationManager.IMPORTANCE_LOW // LOW に変更
            );
            channel.setDescription("時報アプリがバックグラウンドで動作中です");
            channel.setSound(null, null); // 通知音を無効化
            channel.enableVibration(false); // バイブレーションを無効化

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showForegroundNotification() {
        // 通知チャンネルを作成
        createNotificationChannel();

        // メインアクティビティを開くIntent
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        // 通知を構築
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("VoiChime")
                .setContentText("タップしてアプリを開く")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(false)  // タップ可能にする
                .setAutoCancel(false) // サービス継続のため自動削除しない
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setSound(null) // 通知音を無効化
                .setVibrate(null); // バイブレーションを無効化

        // フォアグラウンドサービスとして通知を表示
        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void playBeepChime() {
        MediaPlayer beepPlayer = MediaPlayer.create(this, R.raw.time_signal_beep);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        );

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "Audio focus not granted. Skipping chime.");
            return;
        }

        beepPlayer.setOnCompletionListener(mp -> {
            mp.release();
        });
        beepPlayer.start();
    }

    public void playChime(String selectedVoice, int hour, int minute, float volume) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        MediaPlayer introPlayer;
        MediaPlayer hourPlayer;
        MediaPlayer minutePlayer;
        MediaPlayer outroPlayer;

        boolean useExternalFiles = false;

        File presetFolder = new File(getExternalFilesDir("voice_presets").getAbsolutePath(), selectedVoice);
        File introFile = new File(presetFolder, selectedVoice + "_intro.wav");
        File hourFile = new File(presetFolder, selectedVoice + "_hour" + hour + ".wav");
        File minuteFile = new File(presetFolder, selectedVoice + "_minute" + minute + ".wav");
        File outroFile = new File(presetFolder, selectedVoice + "_outro.wav");

        if (introFile.exists() && hourFile.exists() && minuteFile.exists() && outroFile.exists()) {
            useExternalFiles = true;
        }

        try {
            if (useExternalFiles) {
                introPlayer = new MediaPlayer();
                hourPlayer = new MediaPlayer();
                minutePlayer = new MediaPlayer();
                outroPlayer = new MediaPlayer();

                introPlayer.setDataSource(introFile.getAbsolutePath());
                hourPlayer.setDataSource(hourFile.getAbsolutePath());
                minutePlayer.setDataSource(minuteFile.getAbsolutePath());
                outroPlayer.setDataSource(outroFile.getAbsolutePath());

                introPlayer.prepare();
                hourPlayer.prepare();
                minutePlayer.prepare();
                outroPlayer.prepare();
            } else {
                int introResId = getResources().getIdentifier(selectedVoice + "_intro", "raw", getPackageName());
                int hourResId = getResources().getIdentifier(selectedVoice + "_hour" + hour, "raw", getPackageName());
                int minuteResId = getResources().getIdentifier(selectedVoice + "_minute" + minute, "raw", getPackageName());
                int outroResId = getResources().getIdentifier(selectedVoice + "_outro", "raw", getPackageName());

                if (introResId == 0 || hourResId == 0 || minuteResId == 0 || outroResId == 0) {
                    Log.w(TAG, "音声ファイルが見つかりません: intro=" + introResId + ", hour=" + hourResId + ", minute=" + minuteResId + ", outro=" + outroResId);
                    return;
                }

                introPlayer = MediaPlayer.create(this, introResId);
                hourPlayer = MediaPlayer.create(this, hourResId);
                minutePlayer = MediaPlayer.create(this, minuteResId);
                outroPlayer = MediaPlayer.create(this, outroResId);
            }

            introPlayer.setVolume(volume, volume);
            hourPlayer.setVolume(volume, volume);
            minutePlayer.setVolume(volume, volume);
            outroPlayer.setVolume(volume, volume);
        } catch (Exception e) {
            Log.e(TAG, "音声ファイルの読み込みに失敗", e);
            return;
        }

        if (introPlayer == null || hourPlayer == null || minutePlayer == null || outroPlayer == null) {
            Log.e(TAG, "MediaPlayer の生成に失敗しました");
            return;
        }

        introPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "introPlayer 完了 → hourPlayer 再生");
            hourPlayer.start();
            introPlayer.release();
        });

        hourPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "hourPlayer 完了 → minutePlayer 再生");
            minutePlayer.start();
            hourPlayer.release();
        });

        minutePlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "minutePlayer 完了 → outroPlayer 再生");
            outroPlayer.start();
            minutePlayer.release();
        });

        outroPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "outroPlayer 完了 → AudioFocus 解放");
            outroPlayer.release();
            audioManager.abandonAudioFocus(focusChangeListener);
        });

        try {
            Log.d(TAG, "introPlayer 再生開始");
            introPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "introPlayer の再生中に例外", e);
        }

        hasPlayed = true;
    }

    private String getVoiceType() {
        // 初回呼び出し時にRandomVoicePresetを初期化
        if (randomVoicePreset == null) {
            randomVoicePreset = new RandomVoicePreset(this);
        }

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String selectedVoiceId = prefs.getString("voiceType", "tsukuyomichan");

        if ("random".equals(selectedVoiceId)) {
            // ランダムが選択されている場合
            return randomVoicePreset.getRandomVoiceId();
        } else {
            // 固定プリセットが選択されている場合
            return selectedVoiceId;
        }
    }
}
