package com.example.padautosolver;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

public final class BoardDetector {
    public static final byte FIRE = 0;
    public static final byte LIGHT = 1;
    public static final byte WOOD = 2;
    public static final byte WATER = 3;
    public static final byte DARK = 4;
    public static final byte HEART = 5;

    public static final class DetectionResult {
        public final byte[] board;
        public final RectF rect;
        public final int cols;
        public final int rows;
        public final String debugText;
        public final float averageSaturation;

        DetectionResult(byte[] board, RectF rect, int cols, int rows,
                        String debugText, float averageSaturation) {
            this.board = board;
            this.rect = rect;
            this.cols = cols;
            this.rows = rows;
            this.debugText = debugText;
            this.averageSaturation = averageSaturation;
        }
    }

    private BoardDetector() {}

    public static DetectionResult detect(Bitmap bitmap, int cols, int horizontalMarginPx, int bottomInsetPx) {
        int rows = cols == 7 ? 6 : 5;
        RectF rect = BoardGeometry.calculate(bitmap.getWidth(), bitmap.getHeight(), cols, rows,
                horizontalMarginPx, bottomInsetPx);
        byte[] board = new byte[cols * rows];
        StringBuilder sb = new StringBuilder();
        float saturationSum = 0f;

        float cellW = rect.width() / cols;
        float cellH = rect.height() / rows;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                float cx = rect.left + (x + 0.5f) * cellW;
                float cy = rect.top + (y + 0.5f) * cellH;
                Hsv hsv = sampleOrb(bitmap, cx, cy, Math.min(cellW, cellH));
                saturationSum += hsv.s;
                byte type = classify(hsv);
                board[y * cols + x] = type;
                sb.append(symbol(type));
                if (x < cols - 1) sb.append(' ');
            }
            if (y < rows - 1) sb.append('\n');
        }
        return new DetectionResult(board, rect, cols, rows, sb.toString(),
                saturationSum / board.length);
    }

    private static Hsv sampleOrb(Bitmap bitmap, float cx, float cy, float cell) {
        // 中央の＋印・鍵などを避け、円周上の色を平均する。
        final int samples = 16;
        double sx = 0;
        double sy = 0;
        double sat = 0;
        double val = 0;
        double weightSum = 0;
        float[] hsv = new float[3];

        for (int i = 0; i < samples; i++) {
            double angle = Math.PI * 2.0 * i / samples;
            double radius = (i % 2 == 0 ? 0.24 : 0.33) * cell;
            int px = clamp((int) Math.round(cx + Math.cos(angle) * radius), 0, bitmap.getWidth() - 1);
            int py = clamp((int) Math.round(cy + Math.sin(angle) * radius), 0, bitmap.getHeight() - 1);
            int color = bitmap.getPixel(px, py);
            Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
            double w = 0.35 + hsv[1] * 0.65;
            double rad = Math.toRadians(hsv[0]);
            sx += Math.cos(rad) * w;
            sy += Math.sin(rad) * w;
            sat += hsv[1] * w;
            val += hsv[2] * w;
            weightSum += w;
        }

        float hue = (float) Math.toDegrees(Math.atan2(sy, sx));
        if (hue < 0) hue += 360f;
        return new Hsv(hue, (float) (sat / weightSum), (float) (val / weightSum));
    }

    private static byte classify(Hsv hsv) {
        float h = hsv.h;
        // パズドラ通常6色の概略色相。ラベル自体はルート計算には不要だが、同色判定に使う。
        if (h < 24f || h >= 347f) return FIRE;
        if (h < 78f) return LIGHT;
        if (h < 170f) return WOOD;
        if (h < 246f) return WATER;
        if (h < 314f) return DARK;
        return HEART;
    }

    private static char symbol(byte b) {
        switch (b) {
            case FIRE: return 'R';
            case WATER: return 'B';
            case WOOD: return 'G';
            case LIGHT: return 'L';
            case DARK: return 'D';
            case HEART: return 'H';
            default: return '?';
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static final class Hsv {
        final float h, s, v;
        Hsv(float h, float s, float v) {
            this.h = h;
            this.s = s;
            this.v = v;
        }
    }
}
