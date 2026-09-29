package com.example.padautosolver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoPuzzleService extends Service {
    public static final String ACTION_START_CAPTURE = "com.example.padautosolver.START_CAPTURE";
    public static final String ACTION_SHOW_BUBBLE = "com.example.padautosolver.SHOW_BUBBLE";
    public static final String ACTION_STOP = "com.example.padautosolver.STOP";
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_DATA = "resultData";

    private static final int NOTIFICATION_ID = 77;
    private static final String CHANNEL_ID = "pad_solver_capture";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService solverExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private WindowManager windowManager;
    private TextView bubble;
    private TextView stopBubble;
    private TextView loopBubble;
    private android.widget.LinearLayout salePanel;
    private TextView saleStatus;
    private volatile boolean saleMode;
    private volatile boolean scanMode;
    private volatile boolean recordDetail;
    private BoxScanController boxScanner;
    private android.widget.LinearLayout scanPanel;
    private TextView scanStatus;
    private volatile int selectionEpoch;
    private volatile boolean loopEnabled;
    private final RoundGate roundGate = new RoundGate();
    private long lastLoopProgress;
    private int unchangedRetries;
    private StageNavigator navigator;
    private String pendingControl = "";
    private long pendingControlSince;
    private View guideView;
    private MediaProjection mediaProjection;
    private MediaProjection.Callback projectionCallback;
    private volatile boolean destroyed;
    private volatile int captureGeneration;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread captureThread;
    private Handler captureHandler;
    private int captureWidth;
    private int captureHeight;
    private int densityDpi;
    private volatile boolean guideRequested;
    private RectF lastBoardRect;
    private Bitmap latestFrame;
    private long lastFrameTime;

    private final SharedPreferences.OnSharedPreferenceChangeListener modeListener = (prefs,key) -> {
        if (!"operationMode".equals(key)) return;
        mainHandler.post(() -> {
            pauseLoop("モードを切り替えました。開始ボタンで実行してください");
            captureGeneration++;
            PuzzleAccessibilityService accessibility=PuzzleAccessibilityService.getInstance();
            if(accessibility!=null) accessibility.cancelDrag();
            saleMode=prefs.getString("operationMode","farm").equals("sale");
            scanMode=prefs.getString("operationMode","farm").equals("BOX_SCAN");
            boxScanner=null; recordDetail=false;
            pendingControl="";
            removeBubble();
            if(mediaProjection!=null) showBubble();
        });
    };

    @Override
    public void onCreate() {
        super.onCreate();
        saleMode=getSharedPreferences("pad_solver",MODE_PRIVATE).getString("operationMode","farm").equals("sale");
        scanMode=getSharedPreferences("pad_solver",MODE_PRIVATE).getString("operationMode","farm").equals("BOX_SCAN");
        getSharedPreferences("pad_solver",MODE_PRIVATE).registerOnSharedPreferenceChangeListener(modeListener);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        captureThread = new HandlerThread("pad-capture");
        captureThread.start();
        captureHandler = new Handler(captureThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopNow();
            return START_NOT_STICKY;
        }
        if (ACTION_START_CAPTURE.equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData;
            if (Build.VERSION.SDK_INT >= 33) {
                resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
            } else {
                //noinspection deprecation
                resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            }
            try {
                startCapture(resultCode, resultData);
            } catch (RuntimeException e) {
                toast("画面キャプチャを開始できません。もう一度許可してください");
                stopSelf();
            }
        } else if (ACTION_SHOW_BUBBLE.equals(action)) {
            showBubble();
        }
        return START_NOT_STICKY;
    }

    private void startCapture(int resultCode, Intent resultData) {
        if (resultData == null) {
            toast("画面キャプチャ情報がありません");
            stopSelf();
            return;
        }

        createNotificationChannel();
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        stopProjectionOnly();

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        //noinspection deprecation
        wm.getDefaultDisplay().getRealMetrics(dm);
        captureWidth = dm.widthPixels;
        captureHeight = dm.heightPixels;
        densityDpi = dm.densityDpi;

        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = manager.getMediaProjection(resultCode, resultData);
        if (mediaProjection == null) {
            toast("MediaProjection の開始に失敗しました");
            stopSelf();
            return;
        }

        final MediaProjection session = mediaProjection;
        projectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                if (mediaProjection != session || destroyed) return;
                stopProjectionOnly();
                removeBubble();
                toast("画面キャプチャが停止しました");
                stopSelf();
            }
            @Override
            public void onCapturedContentResize(int width, int height) {
                if (mediaProjection == session && (width != captureWidth || height != captureHeight)) {
                    // 画面座標とキャプチャの不一致で誤操作しないよう、新しい同意から再開する。
                    stopProjectionOnly();
                    removeBubble();
                    toast("画面サイズが変わりました。画面全体のキャプチャを再開してください");
                    stopSelf();
                }
            }
        };
        mediaProjection.registerCallback(projectionCallback, mainHandler);

        imageReader = ImageReader.newInstance(captureWidth, captureHeight,
                PixelFormat.RGBA_8888, 3);
        final int frameGeneration = captureGeneration;
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image == null || destroyed || frameGeneration != captureGeneration) return;
                long now = android.os.SystemClock.elapsedRealtime();
                if (now - lastFrameTime < 150) return;
                lastFrameTime = now;
                Bitmap next = imageToBitmap(image, captureWidth, captureHeight);
                if (latestFrame != null) latestFrame.recycle();
                latestFrame = next;
            } catch (IllegalStateException ignored) {
                // Capture was released while the callback was queued.
            } finally { if (image != null) image.close(); }
        }, captureHandler);
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "PADAutoSolverCapture",
                captureWidth,
                captureHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                captureHandler);

        saleMode=getSharedPreferences("pad_solver",MODE_PRIVATE).getString("operationMode","farm").equals("sale");
        scanMode=getSharedPreferences("pad_solver",MODE_PRIVATE).getString("operationMode","farm").equals("BOX_SCAN");
        loopEnabled = !saleMode && !scanMode;
        showBubble();
        roundGate.reset();
        lastLoopProgress = android.os.SystemClock.elapsedRealtime();
        scheduleLoop(1000);
        toast(scanMode ? "BOX_SCAN：キャリブレーション後、BOXパネルから開始" : saleMode ? "売却モード：専用パネルから開始してください" : "周回モードを開始しました");
    }

    private void requestFreshFrame() {
        final int generation = captureGeneration;
        captureHandler.post(() -> {
            if (destroyed || generation != captureGeneration) return;
            if (latestFrame == null) {
                mainHandler.postDelayed(() -> {
                    if (!destroyed && generation == captureGeneration) requestFreshFrame();
                }, 200);
                return;
            }
            Bitmap copy = latestFrame.copy(Bitmap.Config.ARGB_8888, false);
            try { solverExecutor.execute(() -> processFrame(copy, generation)); }
            catch (java.util.concurrent.RejectedExecutionException stopped) { copy.recycle(); }
        });
    }

    private void processFrame(Bitmap bitmap, int generation) {
        try {
            if (destroyed || generation != captureGeneration) { bitmap.recycle(); return; }
            PuzzleAccessibilityService foreground = PuzzleAccessibilityService.getInstance();
            if (loopEnabled && foreground == null) {
                bitmap.recycle(); pauseLoop(AccessibilityConnection.unavailableMessage(this)); return;
            }
            if (loopEnabled && !foreground.isGameForeground()) {
                bitmap.recycle();
                lastLoopProgress = android.os.SystemClock.elapsedRealtime();
                scheduleLoop(1000); return;
            }
            SharedPreferences prefs = getSharedPreferences("pad_solver", MODE_PRIVATE);
            if (scanMode) {
                if(navigator==null) navigator=new StageNavigator();
                StagePolicy.Decision decision;
                if(recordDetail) {
                    String result=CandidateDetailInspector.record(this,bitmap,navigator.readItems(bitmap));recordDetail=false;
                    decision=new StagePolicy.Decision(null,result,true);
                } else if(boxScanner==null) decision=new StagePolicy.Decision(null,"BOXパネルの開始を押してください",true);
                else decision=boxScanner.inspect(bitmap,navigator.readItems(bitmap));
                bitmap.recycle(); mainHandler.post(()->{if(scanStatus!=null)scanStatus.setText(decision.status);});
                handleStageDecision(decision,generation); return;
            }
            if (loopEnabled && saleMode) {
                if (navigator == null) navigator = new StageNavigator();
                StagePolicy.Decision sale = navigator.inspectSale(bitmap);
                if (sale != null) {
                    if (sale.status.contains("BOX所持数")) {
                        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(getExternalFilesDir("Download"), "sale-last-frame.png"))) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        }
                    }
                    bitmap.recycle(); handleStageDecision(sale, generation); return;
                }
                bitmap.recycle(); pauseLoop("売却一覧を表示してから「連続売却を開始」を押してください"); return;
            }
            int cols = prefs.getInt("columns", 6);
            int rows = cols == 7 ? 6 : 5;
            float density = getResources().getDisplayMetrics().density;
            int marginPx = Math.round(prefs.getInt("horizontalMarginDp", 0) * density);
            int insetPx = Math.round(prefs.getInt("bottomInsetDp", 0) * density);
            int steps = prefs.getInt("maxSteps", 30);
            int beam = prefs.getInt("beamWidth", 1200);
            int duration = prefs.getInt("durationMs", 4000);
            // 各マス通過に時間を確保し、曲がり角を飛ばしてコンボを崩さない。
            steps = Math.min(steps, Math.max(1, (duration - 150) / 60));

            BoardDetector.DetectionResult detection =
                    BoardDetector.detect(bitmap, cols, marginPx, insetPx, prefs.getBoolean("autoLocate", true));
            if (lastBoardRect != null && prefs.getBoolean("autoLocate", true)
                    && Math.abs(lastBoardRect.bottom - detection.rect.bottom) < detection.rect.width() / cols * .12f) {
                detection = BoardDetector.detect(bitmap, cols, marginPx,
                        bitmap.getHeight() - Math.round(lastBoardRect.bottom), false);
            }
            final BoardDetector.DetectionResult stableDetection = detection;
            android.util.Log.i("PADSolver", "detect confidence=" + detection.confidence + " rect=" + detection.rect);
            int[] frame = loopEnabled ? sampleFrame(bitmap, detection.rect) : null;

            if (detection.averageSaturation < 0.18f || detection.confidence < .16f) {
                guideRequested = false;
                if (loopEnabled) {
                    inspectStage(bitmap, generation);
                    bitmap.recycle();
                    return;
                }
                bitmap.recycle();
                fail("盤面を認識できません。◎長押しで枠位置を確認してください");
                return;
            }
            bitmap.recycle();
            lastBoardRect = detection.rect;
            if (guideRequested) {
                guideRequested = false;
                mainHandler.post(this::showBoardGuide);
                busy.set(false); setBubbleText("◎"); return;
            }
            if (loopEnabled && !roundGate.ready(detection.board, frame, android.os.SystemClock.elapsedRealtime())) {
                if (android.os.SystemClock.elapsedRealtime() - lastLoopProgress > 12000 && roundGate.isUnchanged(detection.board)) {
                    if (++unchangedRetries > 2) pauseLoop("操作が盤面に反映されないため停止しました");
                    else {
                        android.util.Log.i("PADSolver", "retryUnchanged=" + unchangedRetries);
                        roundGate.reset(); scheduleLoop(400);
                    }
                } else if (android.os.SystemClock.elapsedRealtime() - lastLoopProgress > 45000) pauseLoop("盤面の安定を確認できないため停止しました");
                else scheduleLoop(350);
                return;
            }

            if (!roundGate.isUnchanged(detection.board) && roundGate.hasPlayed()) unchangedRetries = 0;
            long searchStart = android.os.SystemClock.elapsedRealtime();
            if (loopEnabled) {
                if (navigator == null) navigator = new StageNavigator();
                navigator.policy.onBoard();
                pendingControl = "";
            }
            PuzzleSolver.Result result = PuzzleSolver.solve(
                    detection.board, cols, rows, steps, beam, 800);
            long searchMs = android.os.SystemClock.elapsedRealtime() - searchStart;
            if (result.combos == 0) {
                fail("0コンボのため操作しません。盤面枠と認識位置を確認してください");
                return;
            }

            PuzzleAccessibilityService accessibility = PuzzleAccessibilityService.getInstance();
            if (accessibility == null) {
                fail(AccessibilityConnection.unavailableMessage(this));
                return;
            }

            mainHandler.post(() -> {
                if (destroyed || generation != captureGeneration || mediaProjection == null) return;
                if (loopEnabled && !accessibility.isGameForeground()) { scheduleLoop(1000); return; }
                removeGuide();
                if (bubble != null) bubble.setVisibility(View.INVISIBLE);
                toast("推定 " + result.combos + " コンボ / 探索 " + searchMs + "ms");
                android.util.Log.i("PADSolver", "combos=" + result.combos + " searchMs=" + searchMs + " steps=" + (result.path.size() - 1) + " board=" + stableDetection.rect + " route=" + result.path + " colors=" + stableDetection.debugText.replace('\n', '/'));
                accessibility.performDrag(result.path, stableDetection.rect, cols, rows, duration, () -> {
                    busy.set(false);
                    if (bubble != null) bubble.setVisibility(View.VISIBLE);
                    setBubbleText("◎");
                    if (loopEnabled) {
                        long now = android.os.SystemClock.elapsedRealtime();
                        roundGate.played(stableDetection.board, now); lastLoopProgress = now;
                        scheduleLoop(2400);
                    }
                });
            });
        } catch (Throwable t) {
            if (!bitmap.isRecycled()) bitmap.recycle();
            fail("解析エラー: " + t.getClass().getSimpleName() + " / " + t.getMessage());
        }
    }

    private Bitmap imageToBitmap(Image image, int width, int height) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        int paddedWidth = width + rowPadding / pixelStride;

        Bitmap padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888);
        buffer.rewind();
        padded.copyPixelsFromBuffer(buffer);
        if (paddedWidth == width) return padded;
        Bitmap cropped = Bitmap.createBitmap(padded, 0, 0, width, height);
        padded.recycle();
        return cropped;
    }

    private void drainImages() {
        if (imageReader == null) return;
        Image image;
        while ((image = imageReader.acquireLatestImage()) != null) {
            image.close();
        }
    }

    private void showBubble() {
        mainHandler.post(() -> {
            if (destroyed) return;
            if (!Settings.canDrawOverlays(this)) {
                toast("オーバーレイ権限を許可してください");
                return;
            }
            if (bubble != null) return;

            bubble = new TextView(this);
            bubble.setText("◎");
            bubble.setTextColor(Color.WHITE);
            bubble.setTextSize(24f);
            bubble.setGravity(Gravity.CENTER);
            bubble.setElevation(dp(8));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(0xDD263238);
            bg.setStroke(dp(2), 0xFFFFFFFF);
            bubble.setBackground(bg);

            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    dp(42), dp(38),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.TOP | Gravity.END;
            p.x = dp(12);
            p.y = dp(40);

            bubble.setOnClickListener(v -> {
                if (!busy.compareAndSet(false, true)) {
                    toast("解析中です");
                    return;
                }
                setBubbleText("…");
                requestFreshFrame();
            });
            bubble.setOnLongClickListener(v -> {
                if (busy.compareAndSet(false, true)) {
                    guideRequested = true;
                    requestFreshFrame();
                }
                return true;
            });

            try {
                makeMovable(bubble, p);
                windowManager.addView(bubble, p);
                showStopBubble();
                showLoopBubble();
                showSalePanel();
                showScanPanel();
                if (saleMode || scanMode) bubble.setVisibility(View.GONE);
            } catch (Exception e) {
                bubble = null;
                toast("フローティングボタン表示に失敗しました");
            }
        });
    }

    private void showStopBubble() {
        if (stopBubble != null) return;
        stopBubble = new TextView(this);
        stopBubble.setText("■ 停止");
        stopBubble.setTextColor(Color.WHITE);
        stopBubble.setTextSize(16f);
        stopBubble.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xF0B71C1C);
        background.setCornerRadius(dp(12));
        stopBubble.setBackground(background);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(dp(66), dp(38),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END;
        p.x = dp(60); p.y = dp(40);
        stopBubble.setOnClickListener(view -> stopNow());
        makeMovable(stopBubble, p);
        try { windowManager.addView(stopBubble, p); }
        catch (RuntimeException failure) { stopBubble = null; }
    }

    private void makeMovable(View view, WindowManager.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            float startX, startY; int initialX, initialY; boolean moving;
            @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = e.getRawX(); startY = e.getRawY(); initialX = params.x; initialY = params.y; moving = false; break;
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (Math.hypot(e.getRawX() - startX, e.getRawY() - startY) > dp(8)) moving = true;
                        if (moving) {
                            params.x = Math.max(0, Math.min(getResources().getDisplayMetrics().widthPixels - params.width, initialX - Math.round(e.getRawX() - startX)));
                            params.y = Math.max(0, Math.min(getResources().getDisplayMetrics().heightPixels - params.height, initialY + Math.round(e.getRawY() - startY)));
                            windowManager.updateViewLayout(v, params); return true;
                        }
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (moving) { v.setPressed(false); return true; }
                }
                return false;
            }
        });
    }

    private void stopNow() {
        loopEnabled = false;
        captureGeneration++;
        busy.set(false);
        PuzzleAccessibilityService accessibility = PuzzleAccessibilityService.getInstance();
        if (accessibility != null) accessibility.cancelDrag();
        solverExecutor.shutdownNow();
        stopProjectionOnly();
        removeBubble();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        toast("自動操作を停止しました");
    }

    private void showScanPanel() {
        if(!scanMode||scanPanel!=null)return;
        scanPanel=new android.widget.LinearLayout(this);scanPanel.setOrientation(android.widget.LinearLayout.VERTICAL);scanPanel.setPadding(dp(6),dp(2),dp(6),dp(2));scanPanel.setBackgroundColor(0xEE183747);
        scanStatus=new TextView(this);scanStatus.setText("BOX_SCAN：読み取り専用・待機中");scanStatus.setTextColor(Color.WHITE);scanStatus.setTextSize(11);scanPanel.addView(scanStatus);
        android.widget.LinearLayout controls=new android.widget.LinearLayout(this);scanPanel.addView(controls);
        android.widget.Button start=new android.widget.Button(this);start.setText("新規");start.setTextSize(11);controls.addView(start);
        android.widget.Button resume=new android.widget.Button(this);resume.setText("再開");resume.setTextSize(11);controls.addView(resume);
        android.widget.Button detail=new android.widget.Button(this);detail.setText("詳細");detail.setTextSize(11);controls.addView(detail);
        android.widget.Button stop=new android.widget.Button(this);stop.setText("停止");stop.setTextSize(11);controls.addView(stop);
        for(android.widget.Button b:new android.widget.Button[]{start,resume,detail,stop}){b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(0,0,0,0);b.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,dp(40),1));}
        start.setOnClickListener(v->{if(busy.get())return;try{boxScanner=new BoxScanController(this);recordDetail=false;loopEnabled=true;roundGate.reset();lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(200);}catch(Exception e){pauseLoop(e.getMessage());}});
        resume.setOnClickListener(v->{if(busy.get())return;try{boxScanner=new BoxScanController(this,true);recordDetail=false;loopEnabled=true;roundGate.reset();lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(200);}catch(Exception e){pauseLoop(e.getMessage());}});
        detail.setOnClickListener(v->{if(busy.get())return;recordDetail=true;loopEnabled=true;lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(200);});
        stop.setOnClickListener(v->pauseLoop("BOX_SCAN停止。途中結果を保持しています"));
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(dp(300),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.x=dp(4);p.y=dp(80);
        try{windowManager.addView(scanPanel,p);}catch(RuntimeException e){scanPanel=null;scanStatus=null;}
    }

    private void showSalePanel() {
        if (salePanel != null || !saleMode) return;
        salePanel = new android.widget.LinearLayout(this);
        salePanel.setOrientation(android.widget.LinearLayout.VERTICAL);
        salePanel.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(0xF0222933); bg.setCornerRadius(dp(10));
        salePanel.setBackground(bg);
        TextView title = new TextView(this); title.setText("売却モード  ↔ 移動"); title.setTextColor(Color.WHITE);
        salePanel.addView(title);
        saleStatus = new TextView(this); saleStatus.setText("合計MP30のみ売却／それ以外は停止"); saleStatus.setTextColor(Color.WHITE); saleStatus.setTextSize(11);
        salePanel.addView(saleStatus);
        android.widget.LinearLayout actions = new android.widget.LinearLayout(this);
        android.widget.Button select = new android.widget.Button(this); select.setText("連続売却を開始"); select.setTextSize(12);
        android.widget.Button stop = new android.widget.Button(this); stop.setText("売却を停止"); stop.setTextSize(12);
        actions.addView(select); actions.addView(stop); salePanel.addView(actions);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(dp(280), WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.LEFT; p.x = dp(8); p.y = dp(85);
        title.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy;
            public boolean onTouch(View v, android.view.MotionEvent e) {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) { sx=e.getRawX(); sy=e.getRawY(); ox=p.x; oy=p.y; return true; }
                if (e.getAction() == android.view.MotionEvent.ACTION_MOVE) { p.x=ox+(int)(e.getRawX()-sx); p.y=oy+(int)(e.getRawY()-sy); windowManager.updateViewLayout(salePanel,p); return true; }
                return true;
            }
        });
        select.setOnClickListener(v -> {
            if (busy.get()) { toast("実行中です。停止してから選択を開始してください"); return; }
            if (mediaProjection == null) { toast("③から画面共有を開始してください"); return; }
            if (navigator == null) navigator = new StageNavigator();
            navigator.sales.reset(); saleMode=true; loopEnabled=true; pendingControl="";
            roundGate.reset(); lastLoopProgress=android.os.SystemClock.elapsedRealtime();
            saleStatus.setText("売却一覧を確認中…");
            if (loopBubble != null) loopBubble.setText("Ⅱ 選択中");
            scheduleLoop(100);
        });
        stop.setOnClickListener(v -> pauseLoop("売却を停止しました。選択状態を確認してください"));
        try { windowManager.addView(salePanel,p); } catch(RuntimeException e) { salePanel=null; saleStatus=null; }
    }

    private void showLoopBubble() {
        if (loopBubble != null || saleMode || scanMode) return;
        loopBubble = new TextView(this);
        loopBubble.setText(loopEnabled ? "Ⅱ 周回" : "▶ 周回"); loopBubble.setTextColor(Color.WHITE);
        loopBubble.setTextSize(16f); loopBubble.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(0xF000695C); bg.setCornerRadius(dp(12));
        loopBubble.setBackground(bg);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(dp(66), dp(38),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END; p.x = dp(130); p.y = dp(40);
        loopBubble.setOnClickListener(view -> {
            if (loopEnabled) { pauseLoop("連続実行を一時停止しました"); return; }
            if (busy.get()) return;
            saleMode = false;
            if (navigator != null) navigator.sales.reset();
            loopEnabled = true; roundGate.reset(); lastLoopProgress = android.os.SystemClock.elapsedRealtime();
            loopBubble.setText("Ⅱ 周回"); scheduleLoop(250);
        });
        makeMovable(loopBubble, p);
        try { windowManager.addView(loopBubble, p); } catch (RuntimeException e) { loopBubble = null; }
    }

    private void scheduleLoop(long delay) {
        busy.set(false);
        mainHandler.postDelayed(() -> {
            if (!destroyed && loopEnabled && mediaProjection != null && busy.compareAndSet(false, true)) requestFreshFrame();
        }, delay);
    }

    private void inspectStage(Bitmap bitmap, int generation) throws Exception {
        if (navigator == null) navigator = new StageNavigator();
        StagePolicy.Decision decision = navigator.inspect(bitmap);
        handleStageDecision(decision, generation);
    }

    private void handleStageDecision(StagePolicy.Decision decision, int generation) {
        if (destroyed || generation != captureGeneration || !loopEnabled) return;
        if (decision.stop) { pauseLoop(decision.status); return; }
        long now = android.os.SystemClock.elapsedRealtime();
        if (decision.target != null) {
            StagePolicy.Item target = decision.target;
            String control = target.text + ":" + Math.round(target.x / 30) + ":" + Math.round(target.y / 30);
            if (!control.equals(pendingControl)) {
                pendingControl = control; pendingControlSince = now;
                scheduleLoop(700); return;
            }
            if (now - pendingControlSince < 600) { scheduleLoop(700); return; }
            mainHandler.post(() -> {
                PuzzleAccessibilityService service = PuzzleAccessibilityService.getInstance();
                if (destroyed || generation != captureGeneration || !loopEnabled) return;
                if (service == null || !service.isGameForeground()) { scheduleLoop(1000); return; }
                android.util.Log.i("PADSolver", "stageAction=" + decision.status);
                if (decision.selectionTaps > 0) {
                    if (saleStatus != null) saleStatus.setText("30枠を連続選択中…（停止できます）");
                    final int epoch = ++selectionEpoch;
                    service.performSelectionPage(target.x, target.y, decision.selectionTaps,
                            () -> !destroyed && generation == captureGeneration && loopEnabled && epoch == selectionEpoch,
                            () -> {
                                if (epoch != selectionEpoch) return;
                                if (decision.completed != null) decision.completed.run();
                                pendingControl = ""; roundGate.reset();
                                lastLoopProgress = android.os.SystemClock.elapsedRealtime();
                                scheduleLoop(150);
                            }, () -> { if (epoch == selectionEpoch) pauseLoop("選択を中断しました。選択状態を確認してください"); });
                    return;
                }
                service.performControl(target.x, target.y, decision.endY < 0 ? target.y : decision.endY, decision.holdMs, () -> {
                    if (decision.completed != null) decision.completed.run();
                    pendingControl = ""; roundGate.reset();
                    lastLoopProgress = android.os.SystemClock.elapsedRealtime();
                    scheduleLoop(navigator != null && navigator.sales.active() ? 900 : 1800);
                }, () -> pauseLoop("操作が中断されたため停止しました。画面を確認してください"));
            });
        } else {
            pendingControl = "";
            if (now - lastLoopProgress > 60000) pauseLoop("自動で進められない画面のため一時停止しました");
            else scheduleLoop(1000);
        }
    }

    private void pauseLoop(String message) {
        selectionEpoch++;
        android.util.Log.i("PADSolver", "paused=" + message);
        getSharedPreferences("pad_solver", MODE_PRIVATE).edit()
                .putString("lastStopReason", message)
                .putLong("lastStopTime", System.currentTimeMillis()).apply();
        loopEnabled = false; busy.set(false);
        mainHandler.post(() -> { if (loopBubble != null) loopBubble.setText("▶ 周回");
            if (saleMode && saleStatus != null) saleStatus.setText(message);
            if(scanMode && scanStatus!=null)scanStatus.setText(message); });
        toast(message);
    }

    private int[] sampleFrame(Bitmap bitmap, RectF rect) {
        int[] out = new int[16 * 16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            out[y * 16 + x] = bitmap.getPixel(
                    Math.min(bitmap.getWidth() - 1, Math.round(rect.left + rect.width() * (.04f + .92f * x / 15))),
                    Math.min(bitmap.getHeight() - 1, Math.round(rect.top + rect.height() * (.04f + .92f * y / 15))));
        }
        return out;
    }

    private void showBoardGuide() {
        if (guideView != null) return;
        SharedPreferences prefs = getSharedPreferences("pad_solver", MODE_PRIVATE);
        int cols = prefs.getInt("columns", 6);
        int rows = cols == 7 ? 6 : 5;
        float density = getResources().getDisplayMetrics().density;
        int marginPx = Math.round(prefs.getInt("horizontalMarginDp", 0) * density);
        int insetPx = Math.round(prefs.getInt("bottomInsetDp", 0) * density);
        RectF rect = lastBoardRect != null ? lastBoardRect : BoardGeometry.calculate(captureWidth > 0 ? captureWidth : getResources().getDisplayMetrics().widthPixels,
                captureHeight > 0 ? captureHeight : getResources().getDisplayMetrics().heightPixels,
                cols, rows, marginPx, insetPx);

        guideView = new BoardGuideView(this, rect, cols, rows);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        try {
            windowManager.addView(guideView, p);
            mainHandler.postDelayed(this::removeGuide, 1600);
        } catch (Exception e) {
            guideView = null;
        }
    }

    private void removeGuide() {
        if (guideView != null) {
            try { windowManager.removeView(guideView); } catch (Exception ignored) {}
            guideView = null;
        }
    }

    private void removeBubble() {
        if(scanPanel!=null){try{windowManager.removeView(scanPanel);}catch(Exception ignored){}scanPanel=null;scanStatus=null;}
        if (salePanel != null) {
            try { windowManager.removeView(salePanel); } catch (Exception ignored) {}
            salePanel=null; saleStatus=null;
        }
        removeGuide();
        if (loopBubble != null) {
            try { windowManager.removeView(loopBubble); } catch (Exception ignored) {}
            loopBubble = null;
        }
        if (stopBubble != null) {
            try { windowManager.removeView(stopBubble); } catch (Exception ignored) {}
            stopBubble = null;
        }
        if (bubble != null) {
            try { windowManager.removeView(bubble); } catch (Exception ignored) {}
            bubble = null;
        }
    }

    private void setBubbleText(String text) {
        mainHandler.post(() -> {
            if (bubble != null) bubble.setText(text);
        });
    }

    private void fail(String message) {
        if (loopEnabled) pauseLoop(message);
        busy.set(false);
        setBubbleText("◎");
        toast(message);
    }

    private void toast(String message) {
        mainHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stop = new Intent(this, AutoPuzzleService.class).setAction(ACTION_STOP);
        PendingIntent stopIntent = PendingIntent.getService(this, 1, stop,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setContentTitle("PAD Auto Solver")
                .setContentText("盤面とステージを自動確認中。停止ボタンで終了します")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pi)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause, "停止", stopIntent).build())
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "PAD Auto Solver", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }
    }

    private void stopProjectionOnly() {
        captureGeneration++;
        busy.set(false);
        releaseCaptureObjects();
        captureHandler.post(() -> {
            if (latestFrame != null) { latestFrame.recycle(); latestFrame = null; }
        });
        if (mediaProjection != null) {
            if (projectionCallback != null) mediaProjection.unregisterCallback(projectionCallback);
            try { mediaProjection.stop(); } catch (Exception ignored) {}
            mediaProjection = null;
        }
        projectionCallback = null;
    }

    private void releaseCaptureObjects() {
        if (virtualDisplay != null) {
            try { virtualDisplay.release(); } catch (Exception ignored) {}
            virtualDisplay = null;
        }
        if (imageReader != null) {
            try { imageReader.close(); } catch (Exception ignored) {}
            imageReader = null;
        }
    }

    @Override
    public void onDestroy() {
        getSharedPreferences("pad_solver",MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(modeListener);
        destroyed = true;
        loopEnabled = false;
        PuzzleAccessibilityService accessibility = PuzzleAccessibilityService.getInstance();
        if (accessibility != null) accessibility.cancelDrag();
        removeBubble();
        stopProjectionOnly();
        solverExecutor.shutdownNow();
        if (captureThread != null) captureThread.quitSafely();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static final class BoardGuideView extends View {
        private final RectF rect;
        private final int cols;
        private final int rows;
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);

        BoardGuideView(Context context, RectF rect, int cols, int rows) {
            super(context);
            this.rect = new RectF(rect);
            this.cols = cols;
            this.rows = rows;
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(5f);
            border.setColor(0xFFFF4081);
            grid.setStyle(Paint.Style.STROKE);
            grid.setStrokeWidth(2f);
            grid.setColor(0xCCFFFFFF);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawRect(rect, border);
            float cw = rect.width() / cols;
            float ch = rect.height() / rows;
            for (int x = 1; x < cols; x++) {
                float px = rect.left + x * cw;
                canvas.drawLine(px, rect.top, px, rect.bottom, grid);
            }
            for (int y = 1; y < rows; y++) {
                float py = rect.top + y * ch;
                canvas.drawLine(rect.left, py, rect.right, py, grid);
            }
        }
    }
}
