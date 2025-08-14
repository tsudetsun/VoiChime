package com.tsudetsun.voichime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class StopServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("StopServiceReceiver", "停止ボタンが押されました");

        // SharedPreferences を更新して signalEnabled を false にする
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("signalEnabled", false);
        editor.apply();

        // サービスを停止
        context.stopService(new Intent(context, TimeSignalService.class));
    }
}