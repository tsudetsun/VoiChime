package com.tsudetsun.voichime;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CreditActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit);

        TextView creditsText = findViewById(R.id.creditsText);
        creditsText.setText(Html.fromHtml(
                "【使用音声】<br>" +
                        "・つくよみちゃん(COEIROINK) <a href='https://tyc.rei-yumesaki.net/'>つくよみちゃん公式サイト</a><br>" +
                        "・ずんだもん(VOICEVOX) <a href='https://zunko.jp/'ずんずんPJ公式サイト</a><br>" +
                        "・四国めたん(VOICEVOX) <a href='https://zunko.jp/'>ずんずんPJ公式サイト</a><br><br>" +
                        "【使用ソフト】<br>" +
                        "・COEIROINK <a href='https://coeiroink.com/'>COEIROINK公式サイト</a><br>" +
                        "・VOICEVOX <a href='https://voicevox.hiroshiba.jp/'>VOICEVOX公式サイト</a><br><br>" +
                        "【使用ライブラリ】<br>" +
                        "・ExoPlayer - Apache License 2.0<br>" +
                        "・AndroidX AppCompat - Apache License 2.0<br>" +
                        "・AndroidX Core - Apache License 2.0<br>" +
                        "・Material Components - Apache License 2.0<br>" +
                        "・Kotlin Standard Library - Apache License 2.0",
                Html.FROM_HTML_MODE_LEGACY
        ));
        creditsText.setMovementMethod(LinkMovementMethod.getInstance());
        creditsText.setLinkTextColor(Color.BLUE);
    }
}
