package com.example.padautosolver;

import java.util.List;

/** Only known progression controls for the requested dungeon are actionable. */
final class StagePolicy {
    static final class Item {
        final String text;
        final float x, y;
        Item(String text, float x, float y) { this.text = normalize(text); this.x = x; this.y = y; }
    }
    static final class Decision {
        final Item target;
        final String status;
        final boolean stop;
        long holdMs = 80;
        int selectionTaps = 0;
        float endY = -1;
        Runnable completed;
        Decision(Item target, String status, boolean stop) { this.target = target; this.status = status; this.stop = stop; }
    }
    private boolean battled, results, selectedTarget, selectedDifficulty;
    private int clears;
    private boolean returningFromSale;
    void reset() { battled = results = selectedTarget = selectedDifficulty = false; clears = 0; }
    void onBoard() { battled = true; results = false; selectedTarget = selectedDifficulty = false; }
    int clears() { return clears; }
    void afterSale() { results = true; selectedTarget = selectedDifficulty = true; returningFromSale = true; }
    static String normalize(String s) { return s.replaceAll("[\\s　!！]", "").toUpperCase(java.util.Locale.ROOT).replace("ランケアップ", "ランクアップ").replace("地獄統", "地獄級"); }
    Decision inspect(List<Item> items) {
        StringBuilder all = new StringBuilder(); for (Item item : items) all.append(item.text).append('\n');
        String text = all.toString();
        if (returningFromSale && text.contains("チーム編成") && text.contains("進化")) {
            Item dungeonTab = exact(items, "売却後ダンジョンへ");
            if (dungeonTab != null) {
                Decision d = tap(dungeonTab, "売却後にダンジョンへ戻る");
                d.completed = () -> returningFromSale = false;
                return d;
            }
        }
        boolean tips = exact(items, "TIPS") != null;
        for (String blocked : new String[]{"コンティニュー", "スタミナを回復", "魔法石を使用", "GAMEOVER", "ゲームオーバー"}) {
            if (text.contains(blocked)) return new Decision(null, "回復・購入・ゲームオーバー画面のため停止", true);
        }
        if (text.contains("購入") && (!tips || text.contains("購入しますか") || text.contains("購入する")))
            return new Decision(null, "購入確認画面のため停止", true);
        if (tips) {
            Item ok = exact(items, "OK", "ＯＫ");
            return ok == null ? new Decision(null, "TIPSのOK表示を待機", false) : tap(ok, "TIPSを閉じる");
        }
        if (battled && (text.contains("獲得経験値") || text.contains("獲得コイン") || text.contains("CLEAR") || text.contains("クリア報酬"))) {
            if (!results) clears++;
            results = true; battled = false;
        }
        Item rewardTitle = exact(items, "クリア報酬");
        if (rewardTitle != null && text.contains("獲得コイン")) results = true;
        if (text.contains("潜入確認") && text.contains("ランクアップ応援ダンジョン") && text.contains("地獄級") && !text.contains("超地獄級")) {
            selectedTarget = selectedDifficulty = true;
        }
        if (results) {
            Item button = exact(items, "再挑戦", "もう一度挑戦", "再挑戦する");
            if (button != null) { selectedTarget = selectedDifficulty = true; return tap(button, "再挑戦"); }
            button = exact(items, "次へ", "NEXT", "OK", "ＯＫ");
            if (button != null) return tap(button, "クリア結果を進める");
            if (rewardTitle != null && text.contains("獲得コイン")) {
                Item rankUp = exact(items, "ランクアップ");
                if (rankUp != null && text.contains("スタミナが全回復"))
                    return tap(rankUp, "ランクアップ結果を進める");
                return tap(rewardTitle, "報酬画面をタップして進める");
            }
        }
        Item dungeon = exact(items, "ランクアップ応援ダンジョン");
        if (dungeon != null && results) { selectedTarget = true; return tap(dungeon, "ランクアップ応援ダンジョンを選択"); }
        if (selectedTarget) {
            Item level = exact(items, "地獄級");
            if (level != null) { selectedDifficulty = true; return tap(level, "地獄級を選択"); }
        }
        if (selectedTarget && selectedDifficulty) {
            Item enter = exact(items, "挑戦する", "潜入する", "潜入", "挑戦");
            if (enter != null) return tap(enter, "同じダンジョンへ再入場");
        }
        return new Decision(null, results ? "結果・再挑戦画面を確認中" : "演出が終わるまで待機", false);
    }
    private static Decision tap(Item item, String status) { return new Decision(item, status, false); }
    private static Item exact(List<Item> items, String... options) {
        for (String option : options) for (Item item : items) if (item.text.equals(normalize(option))) return item;
        return null;
    }
}
