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
import java.util.Map;
import java.util.HashMap;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private TextView timeText;
    private Handler handler = new Handler();
    private Runnable timeUpdater;
    private boolean hasPlayed = false;
    private Spinner voiceSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeText = findViewById(R.id.timeText);
        voiceSelector = findViewById(R.id.voiceSelector);

        // 表示名と識別子の対応表
        Map<String, String> voiceMap = new HashMap<>();
        voiceMap.put("ずんだもん", "zundamon");
        voiceMap.put("四国めたん", "shikokumetan");

        // SharedPreferencesの準備
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String currentVoiceId = prefs.getString("voiceType", "zundamon");

        // 識別子から表示名を逆引き
        String currentDisplayName = "ずんだもん";
        for (Map.Entry<String, String> entry : voiceMap.entrySet()) {
            if (entry.getValue().equals(currentVoiceId)) {
                currentDisplayName = entry.getKey();
                break;
            }
        }

        // Spinnerに表示名を設定
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.voice_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceSelector.setAdapter(adapter);

        // 現在の選択を反映
        int spinnerPosition = adapter.getPosition(currentDisplayName);
        voiceSelector.setSelection(spinnerPosition);

        // 選択されたら識別子を保存
        voiceSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String displayName = parent.getItemAtPosition(position).toString();
                String voiceId = voiceMap.get(displayName);
                prefs.edit().putString("voiceType", voiceId).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 時刻更新処理
        timeUpdater = new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);

                String message = hour + " : " + minute + " : " + second;
                timeText.setText(message);

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
        handler.removeCallbacks(timeUpdater);
    }
}
