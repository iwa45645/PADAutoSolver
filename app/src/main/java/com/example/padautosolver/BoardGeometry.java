package com.example.padautosolver;

import android.graphics.RectF;

public final class BoardGeometry {
    private BoardGeometry() {}

    public static RectF calculate(int screenWidth, int screenHeight, int cols, int rows,
                                  int horizontalMarginPx, int bottomInsetPx) {
        float left = Math.max(0, horizontalMarginPx);
        float right = Math.min(screenWidth, screenWidth - horizontalMarginPx);
        float width = Math.max(1f, right - left);
        float cell = width / cols;
        float bottom = Math.min(screenHeight, Math.max(1, screenHeight - bottomInsetPx));
        float top = bottom - cell * rows;
        if (top < 0) {
            top = 0;
            float h = bottom - top;
            cell = h / rows;
            width = cell * cols;
            left = (screenWidth - width) / 2f;
            right = left + width;
        }
        return new RectF(left, top, right, bottom);
    }

    public static float centerX(RectF rect, int cols, int index) {
        int x = index % cols;
        return rect.left + (x + 0.5f) * rect.width() / cols;
    }

    public static float centerY(RectF rect, int cols, int rows, int index) {
        int y = index / cols;
        return rect.top + (y + 0.5f) * rect.height() / rows;
    }
}
