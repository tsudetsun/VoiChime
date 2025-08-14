package com.tsudetsun.voichime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    private Spinner voiceSelector;
    private Switch signalSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        voiceSelector = findViewById(R.id.voiceSelector);

        // 表示名と識別子の対応表
        Map<String, String> voiceMap = new HashMap<>();
        voiceMap.put("COEIROINK: つくよみちゃん", "tsukuyomichan");
        voiceMap.put("VOICEVOX: ずんだもん", "zundamon");
        voiceMap.put("VOICEVOX: 四国めたん", "shikokumetan");

        // SharedPreferencesの準備
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String currentVoiceId = prefs.getString("voiceType", "tsukuyomichan");

        // 識別子から表示名を逆引き
        String currentDisplayName = "COEIROINK: つくよみちゃん";
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
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String displayName = parent.getItemAtPosition(position).toString();
                String voiceId = voiceMap.get(displayName);
                prefs.edit().putString("voiceType", voiceId).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Foreground Serviceの起動
        Intent serviceIntent = new Intent(this, TimeSignalService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        TextView timeText = findViewById(R.id.timeText);
        Handler timeHandler = new Handler();

        Runnable updateTime = new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);

                String timeString = String.format("%02d:%02d:%02d", hour, minute, second);
                timeText.setText(timeString);

                timeHandler.postDelayed(this, 1000); // 1秒ごとに更新
            }
        };

        signalSwitch = findViewById(R.id.signalSwitch);
        boolean isSignalEnabled = prefs.getBoolean("signalEnabled", true);
        signalSwitch.setChecked(isSignalEnabled);

        signalSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("signalEnabled", isChecked).apply();

            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                stopService(serviceIntent);
            }
        });

        // 起動時にサービスを開始するかどうか
        if (isSignalEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }

        timeHandler.post(updateTime);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isSignalEnabled = prefs.getBoolean("signalEnabled", true);

        signalSwitch.setChecked(isSignalEnabled);
    }
}
