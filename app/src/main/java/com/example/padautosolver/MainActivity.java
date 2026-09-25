package com.example.padautosolver;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjectionConfig;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_CAPTURE = 1001;
    private static final int REQ_NOTIFICATIONS = 1002;

    private MediaProjectionManager projectionManager;
    private SharedPreferences prefs;
    private EditText bottomInsetDp;
    private EditText horizontalMarginDp;
    private EditText maxSteps;
    private EditText beamWidth;
    private EditText durationMs;
    private EditText columns;
    private UpdateManager updateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("pad_solver", MODE_PRIVATE);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        updateManager = new UpdateManager(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("PAD Auto Solver");
        title.setTextSize(26f);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, lp());

        TextView help = new TextView(this);
        help.setText("初回だけ①オーバーレイ権限、②ユーザー補助サービス、③画面キャプチャを許可してください。\n\nパズドラを開いたら右上の丸い『◎』をタップすると、盤面を読み取って自動スワイプします。長押しすると認識中の盤面枠を表示します。");
        help.setTextSize(15f);
        help.setPadding(0, dp(8), 0, dp(14));
        root.addView(help, lp());

        Button overlay = button("① オーバーレイ権限を開く");
        overlay.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        root.addView(overlay, lp());

        Button accessibility = button("② ユーザー補助サービスを開く");
        accessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibility, lp());

        Button capture = button("③ 画面キャプチャを開始");
        capture.setOnClickListener(v -> requestScreenCapture());
        root.addView(capture, lp());

        Button update = button("アプリの更新を確認");
        update.setOnClickListener(v -> updateManager.checkForUpdates(true));
        root.addView(update, lp());

        TextView settingsTitle = new TextView(this);
        settingsTitle.setText("\n調整項目");
        settingsTitle.setTextSize(20f);
        root.addView(settingsTitle, lp());

        columns = numberField(root, "盤面の列数（通常 6 / 7×6盤面は 7）", prefs.getInt("columns", 6));
        horizontalMarginDp = numberField(root, "盤面の左右余白 dp（通常 0）", prefs.getInt("horizontalMarginDp", 0));
        bottomInsetDp = numberField(root, "盤面下端の余白 dp（通常 0。ナビバー分だけ上げたい時に調整）", prefs.getInt("bottomInsetDp", 0));
        maxSteps = numberField(root, "探索手数（推奨 28〜36）", prefs.getInt("maxSteps", 30));
        beamWidth = numberField(root, "探索幅（推奨 800〜2000。大きいほど高精度・重い）", prefs.getInt("beamWidth", 1200));
        durationMs = numberField(root, "自動スワイプ時間 ms（例 4000）", prefs.getInt("durationMs", 4000));

        Button save = button("設定を保存");
        save.setOnClickListener(v -> saveSettings());
        root.addView(save, lp());

        Button showBubble = button("フローティングボタンを表示");
        showBubble.setOnClickListener(v -> {
            saveSettings();
            Intent i = new Intent(this, AutoPuzzleService.class);
            i.setAction(AutoPuzzleService.ACTION_SHOW_BUBBLE);
            startService(i);
            Toast.makeText(this, "画面キャプチャ開始後に ◎ が使えます", Toast.LENGTH_LONG).show();
        });
        root.addView(showBubble, lp());

        TextView note = new TextView(this);
        note.setPadding(0, dp(14), 0, 0);
        note.setText("初版の色認識は通常の火・水・木・光・闇・回復を前提にしています。毒・猛毒・お邪魔・爆弾・ルーレット・特殊スキンは誤認識する場合があります。");
        note.setTextSize(13f);
        root.addView(note, lp());

        setContentView(scroll);
        if (Build.VERSION.SDK_INT >= 35) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        long now = System.currentTimeMillis();
        long lastUpdateCheck = prefs.getLong("lastUpdateCheck", 0L);
        if (now - lastUpdateCheck >= 24L * 60L * 60L * 1000L) {
            prefs.edit().putLong("lastUpdateCheck", now).apply();
            updateManager.checkForUpdates(false);
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    private void requestScreenCapture() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "先にオーバーレイ権限を許可してください", Toast.LENGTH_LONG).show();
        }
        saveSettings();
        Intent captureIntent = Build.VERSION.SDK_INT >= 34
                ? projectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                : projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQ_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UpdateManager.REQ_UNKNOWN_APPS) {
            updateManager.resumePendingInstall();
            return;
        }
        if (requestCode == REQ_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                Intent service = new Intent(this, AutoPuzzleService.class);
                service.setAction(AutoPuzzleService.ACTION_START_CAPTURE);
                service.putExtra(AutoPuzzleService.EXTRA_RESULT_CODE, resultCode);
                service.putExtra(AutoPuzzleService.EXTRA_RESULT_DATA, data);
                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(service);
                } else {
                    startService(service);
                }
                Toast.makeText(this, "準備完了。パズドラを開いて ◎ をタップしてください", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "画面キャプチャが許可されませんでした", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveSettings() {
        int cols = clamp(parse(columns, 6), 6, 7);
        int margin = clamp(parse(horizontalMarginDp, 0), 0, 200);
        int inset = clamp(parse(bottomInsetDp, 0), 0, 500);
        int steps = clamp(parse(maxSteps, 30), 8, 50);
        int beam = clamp(parse(beamWidth, 1200), 100, 5000);
        int duration = clamp(parse(durationMs, 4000), 800, 12000);
        prefs.edit()
                .putInt("columns", cols)
                .putInt("horizontalMarginDp", margin)
                .putInt("bottomInsetDp", inset)
                .putInt("maxSteps", steps)
                .putInt("beamWidth", beam)
                .putInt("durationMs", duration)
                .apply();
        Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show();
    }

    private EditText numberField(LinearLayout root, String labelText, int value) {
        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(14f);
        label.setPadding(0, dp(10), 0, dp(4));
        root.addView(label, lp());

        EditText field = new EditText(this);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setText(String.valueOf(value));
        field.setSelectAllOnFocus(true);
        root.addView(field, lp());
        return field;
    }

    private int parse(EditText field, int fallback) {
        try {
            return Integer.parseInt(field.getText().toString().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout.LayoutParams lp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(6);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
