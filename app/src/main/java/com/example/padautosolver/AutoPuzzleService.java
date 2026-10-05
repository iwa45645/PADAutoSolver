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
    private volatile boolean uraMode;
    private android.widget.LinearLayout uraPanel;
    private TextView uraStatus;
    private int uraResumeAttempts;
    private View uraPreview;
    private UraShuraInspector uraInspector;
    private UraPreflightController uraPreflight;
    private volatile UraBattleController uraBattle;
    private volatile UraLuciferController uraLucifer;
    private volatile UraB3Controller uraB3;
    private volatile UraB4Controller uraB4;
    private volatile UraB5Controller uraB5;
    private volatile UraProgressController uraProgress;
    private boolean uraResumeRequested;
    private volatile boolean recordDetail;
    private BoxScanController boxScanner;
    private BoxDetailController detailScanner;
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
    private long latestFrameSequence;

    private final SharedPreferences.OnSharedPreferenceChangeListener modeListener = (prefs,key) -> {
        if (!"operationMode".equals(key)) return;
        mainHandler.post(() -> {
            pauseLoop("モードを切り替えました。開始ボタンで実行してください");
            captureGeneration++;
            PuzzleAccessibilityService accessibility=PuzzleAccessibilityService.getInstance();
            if(accessibility!=null) accessibility.cancelDrag();
            saleMode=prefs.getString("operationMode","farm").equals("sale");
            scanMode=prefs.getString("operationMode","farm").equals("BOX_SCAN");
            uraMode=isUraMode(prefs.getString("operationMode","farm"));
            uraInspector=null;
            uraPreflight=null;
            clearUraBattle();
            clearLucifer();
            boxScanner=null; detailScanner=null; recordDetail=false;
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
        uraMode=isUraMode(getSharedPreferences("pad_solver",MODE_PRIVATE).getString("operationMode","farm"));
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
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                // Mode switches invalidate plans, not the still-live projection callback.
                if (image == null || destroyed || mediaProjection != session || imageReader != reader) return;
                long now = android.os.SystemClock.elapsedRealtime();
                if (now - lastFrameTime < 150) return;
                lastFrameTime = now;
                Bitmap next = imageToBitmap(image, captureWidth, captureHeight);
                if (latestFrame != null) latestFrame.recycle();
                latestFrame = next;
                latestFrameSequence++;
                UraBattleController battle=uraBattle;
                UraLuciferController lucifer=uraLucifer;
                PuzzleAccessibilityService liveAccessibility=PuzzleAccessibilityService.getInstance();
                if(uraMode&&loopEnabled&&lucifer!=null&&liveAccessibility!=null&&liveAccessibility.isGameForeground())
                    lucifer.captureInstruction(next,now,latestFrameSequence);
                if(uraMode&&loopEnabled&&battle!=null&&liveAccessibility!=null&&liveAccessibility.isGameForeground())
                    battle.captureInstruction(next,now,latestFrameSequence);
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
        uraMode=isUraMode(getSharedPreferences("pad_solver",MODE_PRIVATE).getString("operationMode","farm"));
        loopEnabled = !saleMode && !scanMode && !uraMode;
        showBubble();
        roundGate.reset();
        lastLoopProgress = android.os.SystemClock.elapsedRealtime();
        scheduleLoop(1000);
        toast(uraMode?"裏魔門：読み取り専用パネルから照合してください":scanMode ? "BOX_SCAN：キャリブレーション後、BOXパネルから開始" : saleMode ? "売却モード：専用パネルから開始してください" : "周回モードを開始しました");
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
            final long capturedAt=lastFrameTime,sequence=latestFrameSequence;
            try { solverExecutor.execute(() -> processFrame(copy, generation,capturedAt,sequence)); }
            catch (java.util.concurrent.RejectedExecutionException stopped) { copy.recycle(); }
        });
    }

    private void processFrame(Bitmap bitmap, int generation,long capturedAt,long sequence) {
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
            // Every Ura mode is intercepted, including manual ◎ requests. Never fall into FARM.
            if(uraMode) {
                if(navigator==null)navigator=new StageNavigator();
                if(loopEnabled&&uraResumeRequested) {
                    if(prefs.getString("operationMode","").equals("URA_SHURA_AUTO")) {
                        StagePolicy.Decision back=UraBattleController.pausedModalBack(this,bitmap,navigator);
                        if(back!=null){back.capturedAt=capturedAt;bitmap.recycle();handleStageDecision(back,generation);return;}
                    }
                    uraResumeRequested=false;
                    if(prefs.getString("operationMode","").equals("URA_SHURA_AUTO")&& !UraScenePolicy.preentry(navigator.readUraPreentry(bitmap),2712)) {
                        uraProgress=UraProgressController.resume(this,bitmap);
                        uraB5=uraProgress==null?UraB5Controller.resume(this,bitmap):null;
                        uraB4=uraProgress==null&&uraB5==null?UraB4Controller.resume(this,bitmap):null;
                        uraB3=uraProgress==null&&uraB5==null&&uraB4==null?UraB3Controller.resume(this,bitmap):null;
                        uraLucifer=uraProgress==null&&uraB5==null&&uraB4==null&&uraB3==null?UraLuciferController.resume(this,bitmap):null;
                        if(uraProgress!=null||uraB5!=null||uraB3!=null||uraB4!=null){clearUraBattle();uraPreflight=null;}
                        if(uraLucifer!=null) {
                            clearUraBattle();uraPreflight=null;
                        } else if(UraLuciferController.isScene(this,bitmap)) {
                                bitmap.recycle();
                                if(++uraResumeAttempts<4){uraResumeRequested=true;scheduleLoop(500);return;}
                                pauseLoop("B2_RESUME_EVIDENCE_REQUIRED：実指示と現在盤面を再確認できないため停止");return;
                        }
                        UraBattleController resumed=uraProgress==null&&uraB5==null&&uraB4==null&&uraB3==null&&uraLucifer==null?UraBattleController.resumePausedB1(this,bitmap):null;
                        if(resumed==null&&uraProgress==null&&uraB5==null&&uraB4==null&&uraB3==null&&uraLucifer==null)resumed=UraBattleController.currentB1(this,bitmap);
                        if(resumed!=null){uraBattle=resumed;uraPreflight=null;}
                        else if(UraBattleController.isB1(this,bitmap)) {
                            bitmap.recycle();
                            if(++uraResumeAttempts<4){uraResumeRequested=true;scheduleLoop(500);return;}
                            pauseLoop("B1_RESUME_CAPTURE_REQUIRED：発動済みの状態を読み直せないため停止");return;
                        }
                        else if(uraProgress==null&&uraB5==null&&uraB4==null&&uraB3==null&&uraLucifer==null&&!UraScenePolicy.preentry(navigator.readUraPreentry(bitmap),2712)) {
                            bitmap.recycle();
                            if(++uraResumeAttempts<4){uraResumeRequested=true;scheduleLoop(500+uraResumeAttempts*97);return;}
                            pauseLoop("BATTLE_RESUME_CAPTURE_REQUIRED：実戦画面を読み直せないため停止");return;
                        }
                    }
                }
                if(loopEnabled&&(uraPreflight!=null||uraBattle!=null||uraLucifer!=null||uraB3!=null||uraB4!=null||uraB5!=null||uraProgress!=null)) {
                    long now=android.os.SystemClock.elapsedRealtime();
                    if(now<capturedAt||now-capturedAt>1500){bitmap.recycle();scheduleLoop(250);return;}
                    StagePolicy.Decision decision=uraProgress!=null?uraProgress.inspect(bitmap,navigator,capturedAt,sequence):uraB5!=null?uraB5.inspect(bitmap,navigator,capturedAt,sequence):uraB4!=null?uraB4.inspect(bitmap,navigator,capturedAt,sequence):uraB3!=null?uraB3.inspect(bitmap,navigator,capturedAt,sequence):uraLucifer!=null?uraLucifer.inspect(bitmap,navigator,capturedAt,sequence):uraBattle!=null?uraBattle.inspect(bitmap,navigator,capturedAt,sequence):uraPreflight.inspect(bitmap,navigator,sequence);
                    if(decision.stop&&uraProgress!=null&&decision.status.matches("B[0-9]+_CLEAR_VERIFIED_B[0-9]+_CAPTURE_REQUIRED")) {
                        UraProgressController next=UraProgressController.resume(this,bitmap);
                        if(next!=null){clearLucifer();uraProgress=next;decision=new StagePolicy.Decision(null,"突破確認済み：次の階へ続行",false);}
                    }
                    if(decision.stop&&decision.status.equals("B5_CLEAR_VERIFIED_B6_CAPTURE_REQUIRED")) {
                        UraProgressController next=UraProgressController.resume(this,bitmap);
                        if(next!=null){clearLucifer();uraProgress=next;decision=new StagePolicy.Decision(null,"B5突破確認済み：B6へ続行",false);}
                    }
                    if(decision.stop&&decision.status.equals("B4_CLEAR_VERIFIED_B5_CAPTURE_REQUIRED")) {
                        UraB5Controller next=UraB5Controller.resume(this,bitmap);
                        if(next!=null){clearLucifer();uraB5=next;decision=new StagePolicy.Decision(null,"B4突破確認済み：B5のナポレオンへ続行",false);}
                    }
                    if(decision.stop&&decision.status.equals("B3_CLEAR_VERIFIED_B4_CAPTURE_REQUIRED")) {
                        UraB4Controller next=UraB4Controller.resume(this,bitmap);
                        if(next!=null){clearLucifer();uraB4=next;decision=new StagePolicy.Decision(null,"B3突破確認済み：B4のルーレットへ続行",false);}
                    }
                    if(decision.stop&&decision.status.equals("B2_CLEAR_VERIFIED_B3_CAPTURE_REQUIRED")) {
                        UraB3Controller next=UraB3Controller.resume(this,bitmap);
                        if(next!=null){clearLucifer();uraB3=next;decision=new StagePolicy.Decision(null,"B2突破確認済み：B3の現在盤面から続行",false);}
                    }
                    if(decision.stop&&decision.status.equals("B1_CLEAR_VERIFIED_B2_CURRENT_INSTRUCTION_REQUIRED")) {
                        UraLuciferController next=UraLuciferController.resume(this,bitmap);
                        if(next!=null){uraLucifer=next;clearUraBattle();decision=new StagePolicy.Decision(null,"B1突破確認済み：B2の実指示から続行",false);}
                    }
                    decision.capturedAt=capturedAt;
                    bitmap.recycle();
                    String uraDecisionStatus=decision.status;
                    mainHandler.post(()->{if(uraStatus!=null)uraStatus.setText(uraDecisionStatus);});
                    if(uraPreflight!=null&&uraPreflight.validated&&uraBattle==null){uraBattle=new UraBattleController(this,uraPreflight.helperAssistTotal,prefs.getString("operationMode","").equals("URA_SHURA_DRY_RUN"));scheduleLoop(350);return;}
                    handleStageDecision(decision,generation);return;
                }
                if(uraInspector==null)uraInspector=new UraShuraInspector(this);
                String status=uraInspector.inspect(bitmap,navigator,capturedAt,android.os.SystemClock.elapsedRealtime(),
                        generation,foreground!=null&&foreground.isGameForeground());
                bitmap.recycle();busy.set(false);
                mainHandler.post(()->{if(!destroyed&&generation==captureGeneration&&uraStatus!=null)uraStatus.setText(status);});
                return;
            }
            if (scanMode) {
                if(navigator==null) navigator=new StageNavigator();
                StagePolicy.Decision decision;
                if(detailScanner!=null) {
                    decision=detailScanner.inspect(bitmap,navigator);
                } else if(recordDetail) {
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
                showUraPanel();
                if (saleMode || scanMode || uraMode) bubble.setVisibility(View.GONE);
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
        scanStatus=new TextView(this);scanStatus.setText("BOX_SCAN：読み取り専用・待機中");scanStatus.setTextColor(Color.WHITE);scanStatus.setTextSize(11);scanStatus.setSingleLine(true);scanStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);scanPanel.addView(scanStatus);
        android.widget.LinearLayout controls=new android.widget.LinearLayout(this);scanPanel.addView(controls);
        android.widget.Button start=new android.widget.Button(this);start.setText("新規");start.setTextSize(11);controls.addView(start);
        android.widget.Button resume=new android.widget.Button(this);resume.setText("再開");resume.setTextSize(11);controls.addView(resume);
        android.widget.Button detail=new android.widget.Button(this);detail.setText("詳細");detail.setTextSize(11);controls.addView(detail);
        android.widget.Button stop=new android.widget.Button(this);stop.setText("停止");stop.setTextSize(11);controls.addView(stop);
        for(android.widget.Button b:new android.widget.Button[]{start,resume,detail,stop}){b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(0,0,0,0);b.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,dp(40),1));}
        start.setOnClickListener(v->{if(busy.get())return;try{boxScanner=new BoxScanController(this);detailScanner=null;recordDetail=false;loopEnabled=true;roundGate.reset();lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(200);}catch(Exception e){pauseLoop(e.getMessage());}});
        resume.setOnClickListener(v->{if(busy.get())return;try{boxScanner=new BoxScanController(this,true);detailScanner=null;recordDetail=false;loopEnabled=true;roundGate.reset();lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(200);}catch(Exception e){pauseLoop(e.getMessage());}});
        detail.setOnClickListener(v->{if(busy.get())return;detailScanner=null;recordDetail=true;loopEnabled=true;lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(200);});
        stop.setOnClickListener(v->pauseLoop("BOX_SCAN停止。途中結果を保持しています"));
        android.widget.Button batch=new android.widget.Button(this);batch.setText("連続詳細（記録済みはスキップ）");batch.setTextSize(11);scanPanel.addView(batch,new android.widget.LinearLayout.LayoutParams(-1,dp(38)));
        batch.setOnClickListener(v->{if(busy.get())return;try{detailScanner=new BoxDetailController(this);boxScanner=null;recordDetail=false;pendingControl="";loopEnabled=true;lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(200);}catch(Exception e){pauseLoop(e.getMessage());}});
        android.widget.Button candidates=new android.widget.Button(this);candidates.setText("候補詳細（基本＋長押しスキル）");candidates.setTextSize(11);scanPanel.addView(candidates,new android.widget.LinearLayout.LayoutParams(-1,dp(38)));
        candidates.setOnClickListener(v->{if(busy.get())return;try{detailScanner=new BoxDetailController(this,true);boxScanner=null;recordDetail=false;pendingControl="";loopEnabled=true;lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(200);}catch(Exception e){pauseLoop(e.getMessage());}});
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(dp(300),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.x=dp(4);p.y=dp(8);
        try{windowManager.addView(scanPanel,p);}catch(RuntimeException e){scanPanel=null;scanStatus=null;}
    }

    private static boolean isUraMode(String value) {
        return value!=null&&(value.startsWith("URA_SHURA")||value.equals("TEAM_ANALYSIS"));
    }

    private void showUraPanel() {
        if(!uraMode||uraPanel!=null)return;
        uraPanel=new android.widget.LinearLayout(this);uraPanel.setOrientation(android.widget.LinearLayout.VERTICAL);
        uraPanel.setPadding(dp(6),dp(4),dp(6),dp(4));uraPanel.setBackgroundColor(0xEE182847);
        uraStatus=new TextView(this);uraStatus.setText("裏魔門：フレンドのミオンだけ確認\n潜入確認または保存済みの戦闘から再開できます。\n停止ボタンで中断できます。");
        uraStatus.setTextColor(Color.WHITE);uraStatus.setTextSize(11);uraStatus.setMaxLines(3);
        uraStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        uraPanel.addView(uraStatus,new android.widget.LinearLayout.LayoutParams(-1,dp(55)));
        android.widget.Button inspect=new android.widget.Button(this);inspect.setText("現在のB1でDry Run（発動なし）");inspect.setTextSize(11);
        inspect.setMinHeight(0);inspect.setMinimumHeight(0);uraPanel.addView(inspect,new android.widget.LinearLayout.LayoutParams(-1,dp(36)));
        inspect.setOnClickListener(v->{
            if(mediaProjection==null||busy.get())return;
            clearUraPreview();
            try{clearUraBattle();uraBattle=UraBattleController.readOnlyB1(this);clearLucifer();uraPreflight=null;pendingControl="";loopEnabled=true;
                lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(350);
            }catch(Exception e){pauseLoop("Dry Runを開始できません："+e.getMessage());}
        });
        android.widget.Button preflight=new android.widget.Button(this);preflight.setText("自動攻略：ミオン確認／実戦を再開");preflight.setTextSize(11);
        preflight.setMinHeight(0);preflight.setMinimumHeight(0);uraPanel.addView(preflight,new android.widget.LinearLayout.LayoutParams(-1,dp(36)));
        preflight.setOnClickListener(v->{
            android.util.Log.i("PADSolver","uraStart projection="+(mediaProjection!=null)+" busy="+busy.get()+" loop="+loopEnabled+" frames="+latestFrameSequence);
            if(mediaProjection==null){uraStatus.setText("③から画面全体の共有を開始してください");return;}
            if(loopEnabled||busy.get()){uraStatus.setText("処理中です。停止してから再開してください");return;}
            clearUraPreview();
            uraResumeAttempts=0;
            try{uraPreflight=new UraPreflightController(this);clearUraBattle();clearLucifer();uraResumeRequested=true;pendingControl="";loopEnabled=true;
                lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(350);
            }catch(Exception e){pauseLoop("編成確認を開始できません："+e.getMessage());}
        });
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(dp(270),WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                |WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.START;p.x=dp(4);p.y=dp(4);
        try{windowManager.addView(uraPanel,p);}catch(RuntimeException e){uraPanel=null;uraStatus=null;}
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
        if (loopBubble != null || saleMode || scanMode || uraMode) return;
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

    private void clearUraPreview(){if(uraPreview!=null&&uraPanel!=null)uraPanel.removeView(uraPreview);uraPreview=null;}
    private void handleStageDecision(StagePolicy.Decision decision, int generation) {
        if(decision.previewPlan!=null)mainHandler.post(()->{
            if(destroyed||generation!=captureGeneration||uraPanel==null)return;
            clearUraPreview();uraPreview=new UraRoutePreview(this,decision.previewPlan);
            uraPanel.addView(uraPreview,new android.widget.LinearLayout.LayoutParams(dp(180),dp(150)));
        });
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
                if(uraMode&&(decision.capturedAt<=0||android.os.SystemClock.elapsedRealtime()-decision.capturedAt>1500)){pendingControl="";scheduleLoop(250);return;}
                try{if(decision.beforeDispatch!=null)decision.beforeDispatch.prepare();}
                catch(Exception e){pauseLoop("操作前の状態保存に失敗したため停止："+e.getMessage());return;}
                android.util.Log.i("PADSolver", "stageAction=" + decision.status);
                if(decision.puzzlePath!=null) {
                    clearUraPreview(); // Do not cover enemy messages during result/next-floor capture.
                    final int puzzleEpoch=selectionEpoch;
                    service.performDrag(decision.puzzlePath,decision.puzzleRect,decision.puzzleCols,decision.puzzleRows,decision.puzzleDurationMs,decision.puzzlePreciseStart,()->{
                        if(destroyed||generation!=captureGeneration||!loopEnabled||puzzleEpoch!=selectionEpoch)return;
                        if(!completeStageDecision(decision))return;pendingControl="";
                        lastLoopProgress=android.os.SystemClock.elapsedRealtime();scheduleLoop(250);
                    },()->{if(loopEnabled&&puzzleEpoch==selectionEpoch)pauseLoop("DRAG_CANCELLED：パズル結果を確認するまで再実行しません");});
                    return;
                }
                if(decision.heldFrame!=null||decision.heldStampedFrame!=null) {
                    performHeldCapture(service,decision,generation);
                    return;
                }
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
                final int controlEpoch=selectionEpoch;
                service.performControl(target.x, target.y, decision.endY < 0 ? target.y : decision.endY, decision.holdMs, () -> {
                    if(destroyed||generation!=captureGeneration||!loopEnabled||mediaProjection==null||controlEpoch!=selectionEpoch)return;
                    if (!completeStageDecision(decision)) return;
                    pendingControl = ""; roundGate.reset();
                    lastLoopProgress = android.os.SystemClock.elapsedRealtime();
                    scheduleLoop(navigator != null && navigator.sales.active() ? 900 : 1800);
                }, () -> {if(controlEpoch==selectionEpoch&&loopEnabled)pauseLoop("操作が中断されたため停止しました。画面を確認してください");});
            });
        } else {
            pendingControl = "";
            if (now - lastLoopProgress > 60000) pauseLoop("自動で進められない画面のため一時停止しました");
            else scheduleLoop(decision.nextFrameDelayMs);
        }
    }

    private boolean completeStageDecision(StagePolicy.Decision decision) {
        try{if(decision.completed!=null)decision.completed.run();return true;}
        catch(Exception e){pauseLoop("操作後の状態保存に失敗したため停止："+e.getMessage());return false;}
    }

    private void performHeldCapture(PuzzleAccessibilityService service, StagePolicy.Decision decision, int generation) {
        // The controls overlap the blue skill tooltip. Keep Stop visible, but remove
        // the panel from the captured image until the held evidence has been read.
        if(uraPanel!=null)uraPanel.setVisibility(android.view.View.INVISIBLE);
        final int epoch=selectionEpoch;
        final long started=android.os.SystemClock.elapsedRealtime();
        java.util.function.BooleanSupplier current=()->!destroyed && generation==captureGeneration
                && loopEnabled && epoch==selectionEpoch && mediaProjection!=null;
        final boolean[] done={false,false,false}; // gesture released, evidence saved, terminal callback
        Runnable finish=()->{
            if(!current.getAsBoolean()||done[2]||!done[0]||!done[1])return;
            done[2]=true;
            if(uraPanel!=null)uraPanel.setVisibility(android.view.View.VISIBLE);
            if(decision.completed!=null)decision.completed.run();
            pendingControl="";lastLoopProgress=android.os.SystemClock.elapsedRealtime();
            scheduleLoop(900);
        };
        Runnable fail=()->{if(done[2]||!current.getAsBoolean())return;done[2]=true;if(uraPanel!=null)uraPanel.setVisibility(android.view.View.VISIBLE);service.cancelDrag();pauseLoop("長押し中の画像を保存できないため停止しました");};
        service.performControl(decision.target.x,decision.target.y,decision.target.y,decision.holdMs,
                ()->{if(!current.getAsBoolean())return;done[0]=true;finish.run();},fail);
        mainHandler.postDelayed(()->{
            if(!current.getAsBoolean()||done[2]||done[0]||!service.isGameForeground())return;
            captureHandler.post(()->{
                long now=android.os.SystemClock.elapsedRealtime();
                if(!current.getAsBoolean()||latestFrame==null
                        ||!HeldCaptureWindow.contains(started,lastFrameTime,now,decision.holdMs)) {mainHandler.post(fail);return;}
                Bitmap copy=latestFrame.copy(Bitmap.Config.ARGB_8888,false);
                final long heldAt=lastFrameTime,heldSequence=latestFrameSequence;
                try {solverExecutor.execute(()->{
                    try {
                        if(!current.getAsBoolean()||!service.isGameForeground())return;
                        if(decision.heldStampedFrame!=null)decision.heldStampedFrame.accept(copy,current,heldAt,heldSequence);
                        else decision.heldFrame.accept(copy,current);
                        mainHandler.post(()->{if(!current.getAsBoolean())return;done[1]=true;finish.run();});
                    } catch(Exception e) {
                        android.util.Log.w("PADSolver","heldCaptureFailure",e);
                        mainHandler.post(fail);
                    } finally {copy.recycle();}
                });} catch(java.util.concurrent.RejectedExecutionException e) {copy.recycle();mainHandler.post(fail);}
            });
        },1200);
        mainHandler.postDelayed(fail,18000);
    }

    private void pauseLoop(String message) {
        if(uraPanel!=null)mainHandler.post(()->{if(uraPanel!=null)uraPanel.setVisibility(android.view.View.VISIBLE);});
        selectionEpoch++;
        PuzzleAccessibilityService accessibility=PuzzleAccessibilityService.getInstance();
        if(accessibility!=null)accessibility.cancelDrag();
        android.util.Log.i("PADSolver", "paused=" + message);
        getSharedPreferences("pad_solver", MODE_PRIVATE).edit()
                .putString("lastStopReason", message)
                .putLong("lastStopTime", System.currentTimeMillis()).apply();
        loopEnabled = false; busy.set(false);
        mainHandler.post(() -> { if (loopBubble != null) loopBubble.setText("▶ 周回");
            if (saleMode && saleStatus != null) saleStatus.setText(message);
            if(uraMode&&uraStatus!=null)uraStatus.setText(message);
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
        if(uraPanel!=null){try{windowManager.removeView(uraPanel);}catch(Exception ignored){}uraPanel=null;uraStatus=null;}
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
        if(uraMode)return; // Toasts cover skill/board evidence in MediaProjection; the dedicated panel reports status.
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

    private void clearUraBattle() {
        UraBattleController previous=uraBattle;
        uraBattle=null;
        if(previous!=null)previous.close();
    }

    private void clearLucifer() {
        uraProgress=null;
        uraB5=null;
        uraB4=null;
        uraB3=null;
        UraLuciferController previous=uraLucifer;
        uraLucifer=null;
        if(previous!=null)previous.close();
    }

    @Override
    public void onDestroy() {
        clearUraBattle();
        clearLucifer();
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
