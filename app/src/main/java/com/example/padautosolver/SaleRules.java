package com.example.padautosolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Destructive operations require positive identification, never fuzzy name matching. */
final class SaleRules {
    private static final String[] NAMES = {"氷のサムライオーガ", "樹のサムライオーガ",
            "炎のアイスオーガ", "樹のアイスオーガ", "炎のアーマーオーガ", "氷のアーマーオーガ"};
    static boolean allowedIdentity(String name, int number, int level, boolean unlocked, boolean protectedItem) {
        boolean identity = number >= 310 && number <= 315 && NAMES[number - 310].equals(name) && level == 8;
        identity |= number == 318 && "木の機神兵・アースガル".equals(name) && level == 3;
        return identity && unlocked && !protectedItem;
    }
    static int number(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text.replace(",", ""));
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
    static boolean singleConfirmation(String text, int icons, boolean sameIcon) {
        return icons == 1 && batchConfirmation(text, icons, sameIcon);
    }
    static boolean batchConfirmation(String text, int count, boolean sameIcons) {
        return count >= 1 && count <= 10 && sameIcons && text.contains("まとめて売却")
                && number(text, "MPは([0-9]+)PT") == count
                && number(text, "(?:プラス|\\+|ト)[ポボ]イントは([0-9]+)") == 0;
    }
}
