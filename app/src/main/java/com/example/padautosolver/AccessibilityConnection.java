package com.example.padautosolver;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

/** Permission and binding are separate: an enabled service can be reconnecting. */
final class AccessibilityConnection {
    private AccessibilityConnection() {}

    static boolean isEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName expected = new ComponentName(context, PuzzleAccessibilityService.class);
        for (String entry : enabled.split(":")) {
            if (expected.equals(ComponentName.unflattenFromString(entry))) return true;
        }
        return false;
    }

    static String status(Context context) {
        if (PuzzleAccessibilityService.getInstance() != null) return "ユーザー補助：接続済み";
        return isEnabled(context) ? "ユーザー補助：許可済み・未接続" : "ユーザー補助：未許可";
    }

    static String unavailableMessage(Context context) {
        return isEnabled(context)
                ? "ユーザー補助は許可済みですが接続が切れています。アプリの②で接続状態を確認してください"
                : "②でPAD Auto Solverのユーザー補助を有効にしてください";
    }
}
