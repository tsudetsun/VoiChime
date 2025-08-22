package com.tsudetsun.voichime;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Switch;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SETTINGS = 1001;
    private Spinner voiceSelector;
    private Switch signalSwitch;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private Handler timeHandler;
    private int originalTextColor;
    private Runnable updateTime;
    private long lastFlashTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermissionIfNeeded();

        voiceSelector = findViewById(R.id.voiceSelector);

        File dir = new File(getExternalFilesDir(null), "voice_presets");
        if (!dir.exists()) {
            dir.mkdirs(); // フォルダを作成
        }

        File target = new File(getExternalFilesDir(null), "voice_presets/presets.json");
        if (!target.exists()) {
            InputStream input = getResources().openRawResource(R.raw.presets);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(target);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            byte[] buffer = new byte[1024];
            int length;
            while (true) {
                try {
                    if (!((length = input.read(buffer)) > 0)) break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    output.write(buffer, 0, length);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                input.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                output.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // 外部ストレージのプリセットフォルダを取得
        File presetRoot = new File(getExternalFilesDir("voice_presets").getAbsolutePath());
        if (!presetRoot.exists()) {
            presetRoot.mkdirs(); // 初回起動時にフォルダ作成
        }

        ArrayList<String> presetNames = new ArrayList<>();
        Map<String, String> voiceMap = new HashMap<>();

        File presetJson = new File(getExternalFilesDir(null), "voice_presets/presets.json");
        if (presetJson.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(presetJson));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();

                JSONArray array = new JSONArray(builder.toString());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String displayName = obj.getString("displayName");
                    String voiceId = obj.getString("voiceId");

                    presetNames.add(displayName);
                    voiceMap.put(displayName, voiceId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, presetNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceSelector.setAdapter(adapter);


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
        ArrayAdapter<String> voiceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, presetNames);
        voiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceSelector.setAdapter(voiceAdapter);

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
        timeHandler = new Handler();

        // 元の色を保存
        originalTextColor = timeText.getCurrentTextColor();

        updateTime = new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);

                String timeString = String.format("%02d:%02d:%02d", hour, minute, second);
                timeText.setText(timeString);

                // 時報のタイミングをチェック
                SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
                boolean isSignalEnabled = prefs.getBoolean("signalEnabled", true);
                int intervalMinutes = prefs.getInt("intervalMinutes", 30);

                if (isSignalEnabled) {
                    // 時報が鳴るタイミング（intervalMinutesごと）をチェック

                    if ((minute + 1) % intervalMinutes == 0) {
                        // 0秒に時報が鳴る場合の色変更
                        if (second == 57) { // 3秒前
                            flashTimeColor(500, Color.RED);
                        } else if (second == 58) { // 2秒前
                            flashTimeColor(500, Color.RED);
                        } else if (second == 59) { // 1秒前
                            flashTimeColor(500, Color.RED);
                        }
                    }
                    if (minute % intervalMinutes == 0 && second == 0) {
                        flashTimeColor(1000, Color.RED);
                    }
                }

                timeHandler.postDelayed(this, 100); // 100ms（0.1秒）ごとに更新
            }
        };

        timeHandler.post(updateTime);
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

        Spinner intervalSelector = findViewById(R.id.intervalSelector);
        int currentInterval = prefs.getInt("intervalMinutes", 30); // デフォルト30分

        Map<String, Integer> intervalMap = new HashMap<>();
        intervalMap.put("10分", 10);
        intervalMap.put("15分", 15);
        intervalMap.put("30分", 30);
        intervalMap.put("1時間", 60);

        ArrayAdapter<CharSequence> intervalAdapter = ArrayAdapter.createFromResource(this,
                R.array.interval_options, android.R.layout.simple_spinner_item);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalSelector.setAdapter(intervalAdapter);

        // 現在の選択を反映
        for (Map.Entry<String, Integer> entry : intervalMap.entrySet()) {
            if (entry.getValue() == currentInterval) {
                int pos = intervalAdapter.getPosition(entry.getKey());
                intervalSelector.setSelection(pos);
                break;
            }
        }

        intervalSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String label = parent.getItemAtPosition(position).toString();
                int interval = intervalMap.get(label);
                prefs.edit().putInt("intervalMinutes", interval).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);
        });

        timeHandler.post(updateTime);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isSignalEnabled = prefs.getBoolean("signalEnabled", true);

        signalSwitch.setChecked(isSignalEnabled);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SETTINGS && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra("presetChanged", false)) {
                reloadPresets();
            }
        }
    }

    private void reloadPresets() {
        try {
            File presetJson = new File(getExternalFilesDir(null), "voice_presets/presets.json");
            if (!presetJson.exists()) return;

            BufferedReader reader = new BufferedReader(new FileReader(presetJson));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();

            JSONArray array = new JSONArray(builder.toString());
            List<String> presetNames = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                presetNames.add(obj.getString("displayName"));
            }

            // 例：Spinner に反映する場合
            Spinner spinner = findViewById(R.id.voiceSelector);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, presetNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 色を変更するメソッド
    private void flashTimeColor(int duration, int color) {
        TextView timeText = findViewById(R.id.timeText);

        long currentTime = System.currentTimeMillis();
        // 0.9秒以内の重複実行を防ぐ
        if (currentTime - lastFlashTime < 990) {
            return;
        }
        lastFlashTime = currentTime;

        // 指定色に変更
        timeText.setTextColor(color);

        // 指定時間後に元の色に戻す
        timeHandler.postDelayed(() -> {
            timeText.setTextColor(originalTextColor);
        }, duration);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ハンドラーのコールバックをクリア
        if (timeHandler != null && updateTime != null) {
            timeHandler.removeCallbacks(updateTime);
        }
    }
}
