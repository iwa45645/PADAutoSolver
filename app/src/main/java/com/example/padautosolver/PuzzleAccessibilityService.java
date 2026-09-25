package com.example.padautosolver;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.List;

public class PuzzleAccessibilityService extends AccessibilityService {
    private static volatile PuzzleAccessibilityService instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static PuzzleAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Toast.makeText(this, "PAD Auto Solver: 自動スワイプ有効", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 画面内容は取得しない。ジェスチャー送信だけに使う。
    }

    @Override
    public void onInterrupt() {
    }

    public void performDrag(List<Integer> route, RectF boardRect, int cols, int rows,
                            long durationMs, Runnable onDone) {
        mainHandler.post(() -> {
            if (route == null || route.isEmpty() || cols < 1 || rows < 1
                    || boardRect == null || boardRect.width() <= 0 || boardRect.height() <= 0) {
                Toast.makeText(this, "有効な移動ルートが見つかりませんでした", Toast.LENGTH_SHORT).show();
                if (onDone != null) onDone.run();
                return;
            }

            for (int i = 0; i < route.size(); i++) {
                int p = route.get(i);
                if (p < 0 || p >= cols * rows || (i > 0 &&
                        Math.abs(p % cols - route.get(i - 1) % cols)
                                + Math.abs(p / cols - route.get(i - 1) / cols) != 1)) {
                    Toast.makeText(this, "連続していないルートは実行できません", Toast.LENGTH_SHORT).show();
                    if (onDone != null) onDone.run();
                    return;
                }
            }
            Path path = new Path();
            int first = route.get(0);
            path.moveTo(
                    BoardGeometry.centerX(boardRect, cols, first),
                    BoardGeometry.centerY(boardRect, cols, rows, first));

            for (int i = 1; i < route.size(); i++) {
                int p = route.get(i);
                path.lineTo(
                        BoardGeometry.centerX(boardRect, cols, p),
                        BoardGeometry.centerY(boardRect, cols, rows, p));
            }

            long duration = route.size() == 1 ? 100 : Math.max(800, Math.min(12_000, durationMs));
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, duration, false);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(stroke)
                    .build();

            boolean accepted = dispatchGesture(gesture,
                    new GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            super.onCompleted(gestureDescription);
                            if (onDone != null) onDone.run();
                        }

                        @Override
                        public void onCancelled(GestureDescription gestureDescription) {
                            super.onCancelled(gestureDescription);
                            Toast.makeText(PuzzleAccessibilityService.this,
                                    "自動スワイプがキャンセルされました", Toast.LENGTH_SHORT).show();
                            if (onDone != null) onDone.run();
                        }
                    }, mainHandler);

            if (!accepted) {
                Toast.makeText(this, "ジェスチャー送信に失敗しました", Toast.LENGTH_SHORT).show();
                if (onDone != null) onDone.run();
            }
        });
    }
}
