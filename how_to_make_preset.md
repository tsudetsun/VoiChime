# プリセットの追加方法
1. 「プリセットを追加する」を押す
   
<img width="200" alt="設定画面" src="https://github.com/user-attachments/assets/50da5d02-92b4-4877-91a7-086210f62838" />

---

2. 名前と識別子※を入力する
   
※識別子は追加する音声ファイルにつける名前です。**あとから変更はできません。**

<img height="200" alt="プリセット追加画面" src="https://github.com/user-attachments/assets/258e7182-ea8c-495d-afab-99dd27fc4207" />

---

3. 「追加」を押す

<img height="200" alt="プリセット追加画面(入力済み)" src="https://github.com/user-attachments/assets/95d9a491-bb46-4cfa-9152-7ae3c8a02385" />

---

4. 音声ファイルを追加する

   音声ファイルはPC等とスマートフォンを接続し、`内部共有ストレージ\Android\data\com.tsudetsun.voichime\files\voice_presets\(識別子)`内に保存してください。

   [プリセット追加用台本](https://github.com/tsudetsun/VoiChime/blob/master/%E3%83%97%E3%83%AA%E3%82%BB%E3%83%83%E3%83%88%E8%BF%BD%E5%8A%A0%E7%94%A8%E5%8F%B0%E6%9C%AC.txt)にそれぞれの音声の台本を書いています。合成音声等で読ませる場合にご活用ください。

ファイル命名規則
|内容|名称|
----|---- 
|午前0時|識別子_hour0.wav|
|...|...|
|午後11時|識別子_hour23.wav|
|ちょうど|識別子_minute0.wav|
|10分|識別子_minute10.wav|
|...|...|
|50分|識別子_minute50.wav|
|(名前)が|識別子_intro.wav|
|を、お伝えします|識別子_outro.wav|

<img height="200" alt="ファイルの名前" src="https://github.com/user-attachments/assets/48729315-0609-4f64-99d5-54dc1d7e56de" />

# プリセットの設定

<img height="300" alt="設定画面(例)" src="https://github.com/user-attachments/assets/867e6b55-20a5-44be-bec7-a539116d160f" />

1. **ドロップダウンメニュー**
   
  ここでプリセットの選択します

2. **表示名**

   現在選択しているプリセットの表示名を表示します

3. **ボリュームバー**

   ボリュームを調整できます(0~100%)

4. **音声プレビュー**

   現在選択しているプリセットの音声を聞くことができます

   再生内容:「(名前)が0時ちょうどをお知らせします」

5. **名前変更**

   現在選択しているプリセットの表示名を変更できます

   (識別子は変更不可)

   <img height="200" alt="表示名変更" src="https://github.com/user-attachments/assets/8a895463-4554-45b0-a2a1-5c89780af445" />


7. **削除**

   現在選択しているプリセットを削除します

   **一度削除するともとに戻せません**

   <img height="200" alt="削除確認" src="https://github.com/user-attachments/assets/60896a31-f9b7-41cd-ba0a-396bad776778" />
