package com.tsudetsun.voichime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // ビープ音の設定
        CheckBox beepCheckbox = findViewById(R.id.checkbox_beep);
        beepCheckbox.setChecked(prefs.getBoolean("beepEnabled", true));
        beepCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("beepEnabled", isChecked).apply();
        });

        Button btnAddPreset = findViewById(R.id.btnAddPreset);
        btnAddPreset.setOnClickListener(v -> {
            EditText inputDisplay = new EditText(SettingsActivity.this);
            inputDisplay.setHint("表示名（例: わたしの声）");

            EditText inputId = new EditText(SettingsActivity.this);
            inputId.setHint("識別子（半角英数のみ）");

            LinearLayout layout = new LinearLayout(SettingsActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(inputDisplay);
            layout.addView(inputId);

            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("新しいプリセットを追加")
                    .setView(layout)
                    .setPositiveButton("追加", (dialog, which) -> {
                        String displayName = inputDisplay.getText().toString().trim();
                        String voiceId = inputId.getText().toString().trim();

                        if (displayName.isEmpty() || voiceId.isEmpty()) {
                            Toast.makeText(this, "両方の項目を入力してください", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 英数字チェック
                        if (!voiceId.matches("^[a-zA-Z0-9_]+$")) {
                            Toast.makeText(this, "識別子は半角英数字のみ使用できます", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // フォルダ作成
                        File presetFolder = new File(getExternalFilesDir("voice_presets").getAbsolutePath(), voiceId);
                        if (presetFolder.exists()) {
                            Toast.makeText(this, "同じ識別子のプリセットがすでに存在します", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean created = presetFolder.mkdirs();
                        if (created) {
                            // JSONに追加
                            try {
                                File presetJson = new File(getExternalFilesDir(null), "voice_presets/presets.json");
                                JSONArray array = new JSONArray();

                                if (presetJson.exists()) {
                                    BufferedReader reader = new BufferedReader(new FileReader(presetJson));
                                    StringBuilder builder = new StringBuilder();
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        builder.append(line);
                                    }
                                    reader.close();
                                    array = new JSONArray(builder.toString());
                                }

                                JSONObject newPreset = new JSONObject();
                                newPreset.put("displayName", displayName);
                                newPreset.put("voiceId", voiceId);
                                array.put(newPreset);

                                FileWriter writer = new FileWriter(presetJson);
                                writer.write(array.toString());
                                writer.close();

                                Toast.makeText(this, "プリセットを追加しました", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(this, "保存に失敗しました", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "フォルダ作成に失敗しました", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("キャンセル", null)
                    .show();
        });

        TextView hintText = findViewById(R.id.preset_add_hint);
        SpannableString spannable = new SpannableString("プリセットの追加方法はここ");

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tsudetsun/VoiChime/blob/master/how_to_make_preset.md"));
                widget.getContext().startActivity(browserIntent);
            }
        };

        spannable.setSpan(clickableSpan, 0, 13, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        hintText.setText(spannable);
        hintText.setMovementMethod(LinkMovementMethod.getInstance());

        // クレジットボタン
        Button btnCredits = findViewById(R.id.btnCredits);
        btnCredits.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, CreditActivity.class);
            startActivity(intent);
        });

        // 戻るボタン
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish(); // アクティビティを閉じる
        });
    }
}
