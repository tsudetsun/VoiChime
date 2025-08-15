package com.tsudetsun.voichime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;

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
