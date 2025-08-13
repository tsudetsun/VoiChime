package com.tsudetsun.voichime;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private TextView timeText;
    private Handler handler = new Handler();
    private Runnable timeUpdater;
    private boolean hasPlayed = false; // 同じ時刻で何度も鳴らさないようにする
    private Spinner voiceSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeText = findViewById(R.id.timeText);
        voiceSelector = findViewById(R.id.voiceSelector);

        // SharedPreferencesの準備
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String currentVoice = prefs.getString("voiceType", "zundamon");


        // Spinnerに選択肢を設定
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.voice_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceSelector.setAdapter(adapter);

        // 現在の選択を反映
        int position = adapter.getPosition(currentVoice);
        voiceSelector.setSelection(position);

        // 選択されたら保存
        voiceSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedVoice = parent.getItemAtPosition(position).toString();
                prefs.edit().putString("voiceType", selectedVoice).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 時刻更新処理を定義
        timeUpdater = new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);

                String message = hour + " : " + minute + " : " + second;
                timeText.setText(message);

                // 00分または30分のみ対象
                if ((minute == 0 || minute == 30) && second == 0 && !hasPlayed) {
                    String selectedVoice = prefs.getString("voiceType", "zundamon");

                    int introResId = getResources().getIdentifier(selectedVoice + "_intro", "raw", getPackageName());
                    int hourResId = getResources().getIdentifier(selectedVoice + "_hour" + hour, "raw", getPackageName());
                    int minuteResId = getResources().getIdentifier(selectedVoice + "_minute" + minute, "raw", getPackageName());
                    int outroResId = getResources().getIdentifier(selectedVoice + "_outro", "raw", getPackageName());

                    MediaPlayer introPlayer = MediaPlayer.create(MainActivity.this, introResId);
                    MediaPlayer hourPlayer = MediaPlayer.create(MainActivity.this, hourResId);
                    MediaPlayer minutePlayer = MediaPlayer.create(MainActivity.this, minuteResId);
                    MediaPlayer outroPlayer = MediaPlayer.create(MainActivity.this, outroResId);

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

                // 分が変わったらリセット
                if (minute != 0 && minute != 30) {
                    hasPlayed = false;
                }

                handler.postDelayed(this, 1000);
            }
        };

        handler.post(timeUpdater);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // アプリ終了時に更新を止める
        handler.removeCallbacks(timeUpdater);
    }
}