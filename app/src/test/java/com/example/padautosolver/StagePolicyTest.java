package com.example.padautosolver;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
public class StagePolicyTest {
    private List<StagePolicy.Item> words(String... words) {
        java.util.ArrayList<StagePolicy.Item> items = new java.util.ArrayList<>();
        for (String w : words) items.add(new StagePolicy.Item(w,1,1)); return items;
    }
    @Test public void unrelatedOkIsNeverClicked() { assertNull(new StagePolicy().inspect(words("OK")).target); }
    @Test public void purchaseExplanationInMpShopTipDoesNotStopProgress() {
        StagePolicy.Decision d = new StagePolicy().inspect(words("T I P S", "MPショップ", "MP（モンスターポイント）で", "一部のモンスターを", "購入できるようになるよ！", "OK"));
        assertFalse(d.stop);
        assertNotNull(d.target);
        assertEquals("OK", d.target.text);
        StagePolicy.Decision covered = new StagePolicy().inspect(words("TIPS", "MPショップ", "購入できるようになるよ！"));
        assertFalse(covered.stop);
        assertNull(covered.target);
        assertTrue(new StagePolicy().inspect(words("購入しますか？", "OK")).stop);
        assertTrue(new StagePolicy().inspect(words("TIPS", "購入しますか？", "OK")).stop);
    }
    @Test public void advancesRankUpWithoutAnOkButtonIncludingAfterRestart() {
        StagePolicy p = new StagePolicy();
        StagePolicy.Decision d = p.inspect(words("クリア報酬", "獲得コイン", "獲得EHP", "ランケアップ", "スタミナが全回復しました！"));
        assertNotNull(d.target);
        assertEquals("ランクアップ", d.target.text);
        assertNotNull(p.inspect(words("クリア報酬", "獲得コイン", "獲得EXP")).target);
        assertTrue(p.inspect(words("クリア報酬", "獲得コイン", "魔法石を使用", "OK")).stop);
    }
    @Test public void recognizesActualDeviceConfirmationAndTips() {
        StagePolicy p = new StagePolicy();
        assertNotNull(p.inspect(words("戻る 潜入確認", "～ランケアップ応援ダンジョン！", "月曜ダンジョン 地獄統", "挑戦する")).target);
        assertNotNull(new StagePolicy().inspect(words("T I P S", "目を大切にね", "OK")).target);
        assertNull(new StagePolicy().inspect(words("潜入確認", "別のダンジョン", "地獄級", "挑戦する")).target);
    }
    @Test public void proceedsFromResultsToRequestedDungeonOnly() {
        StagePolicy p = new StagePolicy(); p.onBoard();
        assertNotNull(p.inspect(words("獲得経験値", "次へ")).target);
        assertEquals(1, p.clears());
        assertNotNull(p.inspect(words("ランクアップ応援ダンジョン")).target);
        assertNull(p.inspect(words("超地獄級")).target);
        assertNotNull(p.inspect(words("地獄級")).target);
        assertNotNull(p.inspect(words("挑戦する")).target);
        p.onBoard(); assertNotNull(p.inspect(words("CLEAR!", "OK")).target);
        assertEquals(2, p.clears());
    }
    @Test public void stopsAtRecoveryAndDefeat() {
        StagePolicy p = new StagePolicy(); p.onBoard(); p.inspect(words("獲得コイン"));
        assertTrue(p.inspect(words("魔法石を使用", "OK")).stop);
        assertTrue(p.inspect(words("コンティニュー", "はい")).stop);
    }
}
