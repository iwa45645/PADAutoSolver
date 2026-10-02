package com.example.padautosolver;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class PuzzleAccessibilityService extends AccessibilityService {
    private static volatile PuzzleAccessibilityService instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Drag active;
    private boolean controlRunning;
    private int controlEpoch;
    private volatile String foregroundPackage = "";
    public boolean isGameForeground() {
        android.app.KeyguardManager keyguard=(android.app.KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if(keyguard!=null&&keyguard.isKeyguardLocked())return false;
        android.view.accessibility.AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        boolean game = "jp.gungho.pad".contentEquals(root.getPackageName() == null ? "" : root.getPackageName());
        root.recycle();
        return game;
    }
    public static PuzzleAccessibilityService getInstance() { return instance; }

    @Override protected void onServiceConnected() {
        super.onServiceConnected(); instance = this;
        android.util.Log.i("PADAccessibility", "connected");
        Toast.makeText(this, "PAD Auto Solver: 自動スワイプ有効", Toast.LENGTH_SHORT).show();
    }
    @Override public boolean onUnbind(android.content.Intent intent) {
        cancelDrag();
        if (instance == this) instance = null;
        android.util.Log.i("PADAccessibility", "disconnected");
        super.onUnbind(intent);
        return true; // Ask Android to deliver onRebind when this service instance is reused.
    }
    @Override public void onRebind(android.content.Intent intent) {
        super.onRebind(intent);
        instance=this;
        android.util.Log.i("PADAccessibility","rebound");
        // Gesture dispatch still requires a current game root and an unlocked device.
    }
    @Override public void onDestroy() {
        cancelDrag();
        if (instance == this) instance = null;
        super.onDestroy();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(instance==null) {
            // An event plus an accessible current root proves a live framework connection.
            // This covers OEM rebinds which omit the usual connected callback.
            android.view.accessibility.AccessibilityNodeInfo live=getRootInActiveWindow();
            if(live!=null){live.recycle();instance=this;android.util.Log.i("PADAccessibility","connectionRecoveredFromLiveEvent");}
        }
        if (event.getPackageName() != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String pkg = event.getPackageName().toString();
            if (!getPackageName().equals(pkg)) foregroundPackage = pkg;
        }
    }
    @Override public void onInterrupt() { cancelDrag(); }

    public void cancelDrag() {
        controlEpoch++;
        mainHandler.post(() -> { if (active != null) active.cancelled = true; });
    }

    public void performTap(float x, float y, Runnable onDone) {
        performPress(x, y, 80, onDone);
    }
    public void performPress(float x, float y, long holdMs, Runnable onDone) {
        performControl(x, y, y, holdMs, onDone, () -> { });
    }
    public void performControl(float x, float y, float endY, long holdMs, Runnable onDone, Runnable onFailure) {
        if(controlRunning || active!=null || !isGameForeground()){onFailure.run();return;}
        controlRunning=true; final int epoch=++controlEpoch;
        java.util.concurrent.atomic.AtomicBoolean ended=new java.util.concurrent.atomic.AtomicBoolean();
        Runnable failure=()->{if(!ended.compareAndSet(false,true))return;controlRunning=false;onFailure.run();};
        Runnable success=()->{if(!ended.compareAndSet(false,true))return;controlRunning=false;if(epoch==controlEpoch&&isGameForeground())onDone.run();else onFailure.run();};
        mainHandler.postDelayed(failure,holdMs+2000);
        Path path = new Path(); path.moveTo(x, y);
        if (endY != y) path.lineTo(x, endY);
        boolean accepted = dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, holdMs)).build(),
                new GestureResultCallback() {
                    @Override public void onCompleted(GestureDescription gesture) { mainHandler.removeCallbacks(failure);success.run(); }
                    @Override public void onCancelled(GestureDescription gesture) { mainHandler.removeCallbacks(failure);failure.run(); }
                }, mainHandler);
        if(!accepted){mainHandler.removeCallbacks(failure);failure.run();}
    }

    // Select one visible 5-column page; check cancellation before every tap.
    public void performSelectionPage(float x, float y, int count,
            java.util.function.BooleanSupplier canContinue, Runnable onDone, Runnable onFailure) {
        performSelectionTap(x, y, count, 0, canContinue, onDone, onFailure);
    }
    private void performSelectionTap(float x, float y, int count, int index,
            java.util.function.BooleanSupplier canContinue, Runnable onDone, Runnable onFailure) {
        if (!canContinue.getAsBoolean() || !isGameForeground()) { onFailure.run(); return; }
        if (index == count) { onDone.run(); return; }
        float tapX = x + (index % 5) * 223;
        float tapY = y + (index / 5) * 237;
        performControl(tapX, tapY, tapY, 60, () ->
                mainHandler.postDelayed(() -> performSelectionTap(x, y, count, index + 1,
                        canContinue, onDone, onFailure), 100), onFailure);
    }

    public void performDrag(List<Integer> route, RectF rect, int cols, int rows,
                            long durationMs, Runnable onDone) {
        performDrag(route,rect,cols,rows,durationMs,onDone,onDone);
    }
    public void performDrag(List<Integer> route, RectF rect, int cols, int rows,
                            long durationMs, Runnable onDone,Runnable onFailure) {
        performDrag(route,rect,cols,rows,durationMs,false,onDone,onFailure);
    }
    public void performDrag(List<Integer> route, RectF rect, int cols, int rows,
                            long durationMs, boolean preciseStart, Runnable onDone,Runnable onFailure) {
        mainHandler.post(() -> {
            if (!isGameForeground() || controlRunning || active != null || route == null || route.isEmpty() || rect == null
                    || cols < 1 || rows < 1 || rect.width() <= 0 || rect.height() <= 0) {
                if (onFailure != null) onFailure.run(); return;
            }
            for (int i = 0; i < route.size(); i++) {
                int p = route.get(i);
                if (p < 0 || p >= cols * rows || (i > 0 &&
                        Math.abs(p % cols - route.get(i - 1) % cols)
                                + Math.abs(p / cols - route.get(i - 1) / cols) != 1)) {
                    Toast.makeText(this, "不連続なルートは実行しません", Toast.LENGTH_SHORT).show();
                    if (onFailure != null) onFailure.run(); return;
                }
            }
            active = new Drag(new ArrayList<>(route), new RectF(rect), cols, rows, durationMs, preciseStart,onDone,onFailure);
            Drag started=active;
            mainHandler.postDelayed(()->{if(active==started){started.cancelled=true;started.finish(false);}},durationMs+2500);
            active.hold();
        });
    }

    private final class Drag {
        final List<Integer> route;
        final RectF rect;
        final int cols, rows;
        final long perCellMs;
        final boolean preciseStart;
        final Runnable onDone;
        final Runnable onFailure;
        int index;
        boolean cancelled, done;
        GestureDescription.StrokeDescription previous;
        Drag(List<Integer> route, RectF rect, int cols, int rows, long limit, boolean preciseStart, Runnable done,Runnable failure) {
            this.route = route; this.rect = rect; this.cols = cols; this.rows = rows; this.onDone = done;this.onFailure=failure;
            this.preciseStart=preciseStart;
            perCellMs = Math.max(25, Math.min(preciseStart||route.size() <= 5 ? 100 : 60, (Math.max(500, limit) - (preciseStart?400:150)) / Math.max(1, route.size() - 1)));
        }
        float x(int at) { return BoardGeometry.centerX(rect, cols, route.get(at)); }
        float y(int at) { return BoardGeometry.centerY(rect, cols, rows, route.get(at)); }
        void hold() {
            Path path = new Path(); path.moveTo(x(0), y(0));
            android.util.Log.i("PADSolver","dragStart cell="+route.get(0)+" x="+x(0)+" y="+y(0)+" precise="+preciseStart+" perCellMs="+perCellMs);
            previous = new GestureDescription.StrokeDescription(path, 0, preciseStart?300:100, true);
            boolean accepted = dispatchGesture(new GestureDescription.Builder().addStroke(previous).build(), new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription gesture) { next(); }
                @Override public void onCancelled(GestureDescription gesture) { android.util.Log.i("PADSolver", "dragCancelled=hold"); finish(false); }
            }, mainHandler);
            if (!accepted) { android.util.Log.i("PADSolver", "dragRejected=hold"); finish(false); }
        }
        void next() {
            if (done) return;
            if(cancelled || !isGameForeground()){finish(false);return;}
            Path path = new Path(); path.moveTo(x(index), y(index));
            // The failed ALL board exactly matched a route missing its first swap.
            // Keep the first movement separate so the game has acquired the starting orb.
            int end = Math.min(route.size() - 1, index + (preciseStart&&index==0?1:5));
            for (int i = index + 1; i <= end; i++) path.lineTo(x(i), y(i));
            boolean more = preciseStart || end < route.size() - 1;
            long duration = end == index ? 1 : (end - index) * perCellMs;
            GestureDescription.StrokeDescription stroke = previous == null
                    ? new GestureDescription.StrokeDescription(path, 0, duration, more)
                    : previous.continueStroke(path, 0, duration, more);
            previous = stroke; index = end;
            GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
            boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription description) {
                    android.util.Log.i("PADSolver","dragReached cell="+route.get(index)+" index="+index);
                    if(preciseStart&&index==route.size()-1)release();
                    else if (more) next(); else finish(true);
                }
                @Override public void onCancelled(GestureDescription description) { android.util.Log.i("PADSolver", "dragCancelled=move"); finish(false); }
            }, mainHandler);
            if (!accepted) finish(false);
        }
        void release() {
            if(done)return;
            Path path=new Path();path.moveTo(x(index),y(index));
            GestureDescription.StrokeDescription stroke=previous.continueStroke(path,0,100,false);
            if(!dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(),new GestureResultCallback(){
                @Override public void onCompleted(GestureDescription description){finish(true);}
                @Override public void onCancelled(GestureDescription description){finish(false);}
            },mainHandler))finish(false);
        }
        void finish(boolean success) {
            if (done) return;
            done = true;
            if (active == this) active = null;
            if (success&&!cancelled&&isGameForeground()){if(onDone!=null)onDone.run();}
            else if(onFailure!=null)onFailure.run();
        }
    }
}
