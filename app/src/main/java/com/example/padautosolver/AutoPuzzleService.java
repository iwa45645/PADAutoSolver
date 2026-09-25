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
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_DATA = "resultData";

    private static final int NOTIFICATION_ID = 77;
    private static final String CHANNEL_ID = "pad_solver_capture";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService solverExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private WindowManager windowManager;
    private TextView bubble;
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

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        captureThread = new HandlerThread("pad-capture");
        captureThread.start();
        captureHandler = new Handler(captureThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
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
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "PADAutoSolverCapture",
                captureWidth,
                captureHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                captureHandler);

        showBubble();
        toast("準備完了：パズドラ上で ◎ をタップ");
    }

    private void requestFreshFrame() {
        if (imageReader == null || mediaProjection == null) {
            busy.set(false);
            setBubbleText("◎");
            toast("先にアプリで『画面キャプチャを開始』してください");
            return;
        }

        final ImageReader requestedReader = imageReader;
        final int generation = captureGeneration;
        captureHandler.post(() -> {
            if (destroyed || generation != captureGeneration) return;
            try {
            Image old;
            while ((old = requestedReader.acquireLatestImage()) != null) old.close();
            requestedReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image == null) return;
                    if (destroyed || generation != captureGeneration) return;
                    Bitmap bitmap = imageToBitmap(image, captureWidth, captureHeight);
                    reader.setOnImageAvailableListener(null, null);
                    solverExecutor.execute(() -> processFrame(bitmap, generation));
                } catch (Throwable t) {
                    reader.setOnImageAvailableListener(null, null);
                    fail("画面取得エラー: " + t.getClass().getSimpleName());
                } finally {
                    if (image != null) image.close();
                }
            }, captureHandler);
            } catch (IllegalStateException e) {
                if (!destroyed && generation == captureGeneration) fail("キャプチャが停止しました。再開してください");
            }
        });
    }

    private void processFrame(Bitmap bitmap, int generation) {
        try {
            if (destroyed || generation != captureGeneration) { bitmap.recycle(); return; }
            SharedPreferences prefs = getSharedPreferences("pad_solver", MODE_PRIVATE);
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
                    BoardDetector.detect(bitmap, cols, marginPx, insetPx);
            bitmap.recycle();

            if (detection.averageSaturation < 0.18f) {
                fail("盤面を認識できません。◎長押しで枠位置を確認してください");
                return;
            }

            PuzzleSolver.Result result = PuzzleSolver.solve(
                    detection.board, cols, rows, steps, beam);

            PuzzleAccessibilityService accessibility = PuzzleAccessibilityService.getInstance();
            if (accessibility == null) {
                fail("ユーザー補助サービスを有効にしてください");
                return;
            }

            mainHandler.post(() -> {
                if (destroyed || generation != captureGeneration || mediaProjection == null) return;
                removeGuide();
                if (bubble != null) bubble.setVisibility(View.INVISIBLE);
                toast("推定 " + result.combos + " コンボ（盤面内の連鎖込み）");
                accessibility.performDrag(result.path, detection.rect, cols, rows, duration, () -> {
                    busy.set(false);
                    if (bubble != null) bubble.setVisibility(View.VISIBLE);
                    setBubbleText("◎");
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
                    dp(58), dp(58),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.TOP | Gravity.END;
            p.x = dp(12);
            p.y = dp(190);

            bubble.setOnClickListener(v -> {
                if (!busy.compareAndSet(false, true)) {
                    toast("解析中です");
                    return;
                }
                setBubbleText("…");
                requestFreshFrame();
            });
            bubble.setOnLongClickListener(v -> {
                showBoardGuide();
                return true;
            });

            try {
                windowManager.addView(bubble, p);
            } catch (Exception e) {
                bubble = null;
                toast("フローティングボタン表示に失敗しました");
            }
        });
    }

    private void showBoardGuide() {
        if (guideView != null) return;
        SharedPreferences prefs = getSharedPreferences("pad_solver", MODE_PRIVATE);
        int cols = prefs.getInt("columns", 6);
        int rows = cols == 7 ? 6 : 5;
        float density = getResources().getDisplayMetrics().density;
        int marginPx = Math.round(prefs.getInt("horizontalMarginDp", 0) * density);
        int insetPx = Math.round(prefs.getInt("bottomInsetDp", 0) * density);
        RectF rect = BoardGeometry.calculate(captureWidth > 0 ? captureWidth : getResources().getDisplayMetrics().widthPixels,
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
        removeGuide();
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
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setContentTitle("PAD Auto Solver")
                .setContentText("画面認識を待機中。◎を押した時だけ盤面を解析します")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pi)
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
        destroyed = true;
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
