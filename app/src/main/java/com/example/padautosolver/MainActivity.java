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
import android.widget.CheckBox;
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
    private CheckBox autoLocate;
    private TextView accessibilityStatus;
    private Button captureButton;
    private final android.os.Handler connectionHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean capturePending;
    private long connectionDeadline;
    private final Runnable connectionPoll = new Runnable() {
        @Override public void run() {
            accessibilityStatus.setText(AccessibilityConnection.status(MainActivity.this));
            if (!capturePending) return;
            if (PuzzleAccessibilityService.getInstance() != null) {
                capturePending = false;
                captureButton.setEnabled(true);
                launchScreenCapture();
            } else if (android.os.SystemClock.elapsedRealtime() < connectionDeadline
                    && AccessibilityConnection.isEnabled(MainActivity.this)) {
                connectionHandler.postDelayed(this, 250);
            } else {
                capturePending = false;
                captureButton.setEnabled(true);
                showAccessibilityRecovery();
            }
        }
    };

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
        help.setText("初回だけ①オーバーレイ権限、②ユーザー補助サービス、③画面キャプチャを許可してください。\n\n周回モードはキャプチャ開始後に自動進行します。売却モードは専用パネルの開始ボタンで実行します。赤い停止ボタンで終了します。◎は手動実行・長押しは盤面枠の確認です。");
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
        accessibility.setOnClickListener(v -> {
            if (PuzzleAccessibilityService.getInstance() != null) {
                Toast.makeText(this, "接続済みです。再設定は不要です", Toast.LENGTH_SHORT).show();
            } else openAccessibilitySettings();
        });
        root.addView(accessibility, lp());
        accessibilityStatus = new TextView(this);
        accessibilityStatus.setText(AccessibilityConnection.status(this));
        root.addView(accessibilityStatus, lp());

        TextView modeTitle = new TextView(this); modeTitle.setText("動作モード"); modeTitle.setTextSize(20); root.addView(modeTitle, lp());
        android.widget.RadioGroup modes = new android.widget.RadioGroup(this);
        android.widget.RadioButton farming = new android.widget.RadioButton(this); farming.setId(View.generateViewId()); farming.setText("周回モード：パズル・クリア後の進行");
        android.widget.RadioButton selling = new android.widget.RadioButton(this); selling.setId(View.generateViewId()); selling.setText("売却モード：30枠選択・合計MP30のみ連続売却");
        android.widget.RadioButton scanning = new android.widget.RadioButton(this); scanning.setId(View.generateViewId()); scanning.setText("BOX_SCAN：所持BOXを読み取り（変更操作なし）");
        android.widget.RadioButton ura = new android.widget.RadioButton(this); ura.setId(View.generateViewId()); ura.setText("裏魔門：スキルのDry Run（読み取り専用）");
        android.widget.RadioButton uraAuto = new android.widget.RadioButton(this); uraAuto.setId(View.generateViewId()); uraAuto.setText("裏魔門：自動攻略の実機試験（未確認条件で停止）");
        modes.addView(farming); modes.addView(selling); modes.addView(scanning);modes.addView(ura);modes.addView(uraAuto);
        String savedMode=prefs.getString("operationMode", "farm");
        modes.check(savedMode.equals("URA_SHURA_AUTO")?uraAuto.getId():savedMode.startsWith("URA_SHURA")?ura.getId():savedMode.equals("BOX_SCAN")?scanning.getId():savedMode.equals("sale")?selling.getId():farming.getId());
        modes.setOnCheckedChangeListener((group,id) -> prefs.edit().putString("operationMode", id==uraAuto.getId()?"URA_SHURA_AUTO":id==ura.getId()?"URA_SHURA_DRY_RUN":id==scanning.getId()?"BOX_SCAN":id==selling.getId()?"sale":"farm").apply());
        root.addView(modes, lp());
        TextView modeHelp = new TextView(this); modeHelp.setText("モードを切り替えると実行を停止します。画面共有中なら、ゲームに戻って各モードの開始ボタンを押してください。"); root.addView(modeHelp, lp());

        Button inventory = button("所持一覧・BOXキャリブレーション・詳細確認");
        inventory.setOnClickListener(v -> startActivity(new Intent(this, InventoryActivity.class)));
        root.addView(inventory, lp());

        Button fixedTeam=button("裏魔門の確定編成と確認状況");
        fixedTeam.setOnClickListener(v -> {
            String message="ユーザー確定編成（変更しません）\nエスペル14094／セッカ7333／オーディン3391／ユキネ10042／ルカ2955／助っ人ミオン9411\n"
                    +"装備8766／5418／なし／8110／10862。挑戦前はフレンドがミオンであることだけ確認します。\n\n"
                    +"パーティー・装備・潜在・スキブの事前照合は省略します。実戦の使用可否と使用後の状態を確認します。\n"
                    +"開幕：ルカ装備→エスペル→セッカ→ユキネ→ミオン→オーディン。\n"
                    +"水2コンボで両リーダーの条件を満たし、回復を優先します。\n\n"
                    +"開発段階：B1のスキル・パズルの実機試験。全階層の自動クリアは未検証です。";
            new android.app.AlertDialog.Builder(this).setTitle("裏魔門・固定TeamProfile").setMessage(message).setPositiveButton("閉じる",null).show();
        });
        root.addView(fixedTeam,lp());

        Button capture = button("③ 画面キャプチャを開始");
        captureButton = capture;
        capture.setOnClickListener(v -> requestScreenCapture());
        root.addView(capture, lp());

        Button stop = button("■ 自動操作・画面キャプチャを停止");
        stop.setTextColor(android.graphics.Color.RED);
        stop.setOnClickListener(v -> {
            PuzzleAccessibilityService service = PuzzleAccessibilityService.getInstance();
            if (service != null) service.cancelDrag();
            stopService(new Intent(this, AutoPuzzleService.class));
            Toast.makeText(this, "自動操作を停止しました", Toast.LENGTH_SHORT).show();
        });
        root.addView(stop, lp());

        Button update = button("アプリの更新を確認");
        update.setOnClickListener(v -> updateManager.checkForUpdates(true));
        root.addView(update, lp());

        TextView settingsTitle = new TextView(this);
        settingsTitle.setText("\n調整項目");
        settingsTitle.setTextSize(20f);
        root.addView(settingsTitle, lp());

        columns = numberField(root, "盤面の列数（通常 6 / 7×6盤面は 7）", prefs.getInt("columns", 6));
        autoLocate = new CheckBox(this);
        autoLocate.setText("盤面の縦位置を自動検出（画面下の余白に対応）");
        autoLocate.setChecked(prefs.getBoolean("autoLocate", true));
        root.addView(autoLocate, lp());
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

    @Override protected void onResume() {
        super.onResume();
        accessibilityStatus.setText(AccessibilityConnection.status(this));
    }

    @Override protected void onPause() {
        capturePending = false;
        connectionHandler.removeCallbacks(connectionPoll);
        captureButton.setEnabled(true);
        super.onPause();
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS");
        intent.putExtra("android.intent.extra.COMPONENT_NAME",
                new android.content.ComponentName(this, PuzzleAccessibilityService.class).flattenToString());
        try { startActivity(intent); }
        catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    private void showAccessibilityRecovery() {
        boolean enabled = AccessibilityConnection.isEnabled(this);
        new android.app.AlertDialog.Builder(this)
                .setTitle(enabled ? "ユーザー補助の接続が切れています" : "ユーザー補助の許可が必要です")
                .setMessage(enabled
                        ? "許可は保存されていますが、Androidとの再接続を確認できませんでした。設定でPAD Auto Solverを一度オフ→オンにすると復旧できます。接続済みになった後は、毎回の設定は不要です。"
                        : "設定でPAD Auto Solverのユーザー補助を一度だけ許可してください。")
                .setPositiveButton("設定を開く", (dialog, which) -> openAccessibilitySettings())
                .setNegativeButton("閉じる", null).show();
    }

    private void requestScreenCapture() {
        if (capturePending) return;
        if (PuzzleAccessibilityService.getInstance() != null) {
            launchScreenCapture();
        } else if (AccessibilityConnection.isEnabled(this)) {
            accessibilityStatus.setText("ユーザー補助：許可済み・再接続を待っています…");
            capturePending = true;
            captureButton.setEnabled(false);
            connectionDeadline = android.os.SystemClock.elapsedRealtime() + 8000;
            connectionHandler.postDelayed(connectionPoll, 250);
        } else showAccessibilityRecovery();
    }

    private void launchScreenCapture() {
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
                String mode=prefs.getString("operationMode","farm");
                if(!mode.startsWith("URA_SHURA"))Toast.makeText(this,
                        mode.equals("BOX_SCAN")?"BOX_SCAN準備完了。BOXパネルから開始してください":
                        mode.equals("sale")?"売却準備完了。専用パネルから開始してください":
                        "準備完了。パズドラを開くと自動で進行します", Toast.LENGTH_LONG).show();
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
                .putBoolean("autoLocate", autoLocate.isChecked())
                .putInt("columns", cols)
                .putInt("horizontalMarginDp", margin)
                .putInt("bottomInsetDp", inset)
                .putInt("maxSteps", steps)
                .putInt("beamWidth", beam)
                .putInt("durationMs", duration)
                .apply();
        if(!prefs.getString("operationMode","").startsWith("URA_SHURA"))Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show();
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
