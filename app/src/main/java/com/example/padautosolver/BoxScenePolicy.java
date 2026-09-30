package com.example.padautosolver;

import java.util.List;

final class BoxScenePolicy {
    static boolean isBox(List<StagePolicy.Item> items, int height) {
        boolean title = false, all = false;
        for (StagePolicy.Item item : items) {
            float y = item.y / height;
            // Exclude our own status panel and match the game's header, not the whole screenshot.
            if (y >= .18f && y <= .27f) {
                if (item.text.contains("売却")) return false;
                title |= item.text.matches(".*モン?スターBO[XHR].*");
            }
            if (y >= .24f && y <= .34f
                    && (item.text.equals("ALL") || item.text.equals("ALL未設定"))) all = true;
            if (y > .18f && item.text.contains("合計MP")) return false;
        }
        return title && all;
    }
}
