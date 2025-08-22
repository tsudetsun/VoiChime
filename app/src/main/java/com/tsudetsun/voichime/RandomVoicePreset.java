package com.tsudetsun.voichime;

import android.content.Context;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class RandomVoicePreset {
    private List<String> availableVoiceIds;
    private Random random;
    private String lastUsedVoice;
    private Context context;

    public RandomVoicePreset(Context context) {
        this.context = context;
        this.availableVoiceIds = new ArrayList<>();
        this.random = new Random();
        this.lastUsedVoice = null;
        loadAvailableVoices();
    }

    private void loadAvailableVoices() {
        // presets.jsonからランダム以外の音声IDを取得
        availableVoiceIds.clear();

        File presetJson = new File(context.getExternalFilesDir(null), "voice_presets/presets.json");
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
                    String voiceId = obj.getString("voiceId");

                    // "random"以外の音声IDを追加
                    if (!"random".equals(voiceId)) {
                        availableVoiceIds.add(voiceId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // エラー時のフォールバック
                availableVoiceIds.add("tsukuyomichan");
                availableVoiceIds.add("zundamon");
                availableVoiceIds.add("metan");
            }
        } else {
            // ファイルが存在しない場合のフォールバック
            availableVoiceIds.add("tsukuyomichan");
            availableVoiceIds.add("zundamon");
            availableVoiceIds.add("metan");
        }
    }

    public String getRandomVoiceId() {
        // 設定を再読み込み（プリセットが変更された場合に対応）
        loadAvailableVoices();

        if (availableVoiceIds.isEmpty()) {
            return "tsukuyomichan"; // フォールバック
        }

        if (availableVoiceIds.size() == 1) {
            return availableVoiceIds.get(0);
        }

        // 前回と同じ音声は避ける
        String newVoice;
        do {
            newVoice = availableVoiceIds.get(random.nextInt(availableVoiceIds.size()));
        } while (newVoice.equals(lastUsedVoice) && availableVoiceIds.size() > 1);

        lastUsedVoice = newVoice;
        return newVoice;
    }

    public List<String> getAvailableVoiceIds() {
        return new ArrayList<>(availableVoiceIds);
    }
}