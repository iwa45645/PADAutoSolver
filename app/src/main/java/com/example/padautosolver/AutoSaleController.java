package com.example.padautosolver;

import android.graphics.Bitmap;
import java.util.List;

/** Sale is gated by a positively recognized total MP of exactly 30. */
final class AutoSaleController {
    private enum State { IDLE, SELECTED, CONFIRM, WAIT }
    private State state = State.IDLE;
    private int confirmed, completed;
    private long waitSince;
    boolean active() { return state != State.IDLE; }
    boolean takeFinished() { return false; }
    int sold() { return completed; }
    void reset() { state=State.IDLE; confirmed=completed=0; }
    StagePolicy.Decision inspect(Bitmap bitmap, List<StagePolicy.Item> items) {
        StringBuilder all=new StringBuilder();
        for(StagePolicy.Item item:items) all.append(item.text).append('\n');
        String text=all.toString();
        boolean list=text.contains("合計コイン") && text.contains("合計MP");
        boolean dialog=text.contains("まとめて売却") && text.contains("本当によろしい");
        if(bitmap.getWidth()!=1220 || bitmap.getHeight()!=2712) return stop("画面サイズが未対応のため停止");
        if(state==State.WAIT) {
            if(confirmed==3 && list && !dialog && SaleVisuals.selectedMp(bitmap)==0) {
                completed+=30; state=State.IDLE;
            } else if(confirmed<3 && dialog && SaleVisuals.dialogRemainingMp(bitmap)==30-confirmed*10) {
                state=State.CONFIRM;
            } else {
                if(android.os.SystemClock.elapsedRealtime()-waitSince>8000) return stop("売却後の画面切替を確認できないため停止");
                return new StagePolicy.Decision(null,"売却後の画面切替を確認中",false);
            }
        }
        if(state==State.CONFIRM) {
            if(!dialog) return stop("売却確認を読み取れないため停止");
            if(!SaleRules.batchConfirmation(text,10,true)) return stop("確認画面のMP10・プラス0を確認できないため停止");
            StagePolicy.Item yes=exact(items,"はい");
            if(yes==null) {
                boolean title=false;
                for(StagePolicy.Item item:items) if(item.text.equals("モンスター売却") && item.y>990 && item.y<1140) title=true;
                if(title && SaleVisuals.dialogRemainingMp(bitmap)==30-confirmed*10)
                    yes=new StagePolicy.Item("はい",443,1980);
            }
            if(yes==null) return stop("はいボタンを確認できないため停止");
            return action(yes,"合計MP30確認済み：10体を売却（"+(confirmed+1)+"/3）",()->{
                confirmed++; waitSince=android.os.SystemClock.elapsedRealtime(); state=State.WAIT;
            });
        }
        if(dialog) return stop("合計MP30を確認していないため停止");
        if(!list) return state==State.IDLE ? null : stop("売却一覧を確認できないため停止");
        int mp=SaleVisuals.selectedMp(bitmap);
        if(state==State.SELECTED || (state==State.IDLE && mp==30)) {
            if(mp!=30) return stop("合計MPが30ではない、または読み取れないため停止（"+mp+"）");
            StagePolicy.Item ok=exact(items,"OK");
            if(ok==null) for(StagePolicy.Item item:items) {
                if(item.text.endsWith("OK") && item.y>2250 && item.y<2420 && item.x>800) {
                    ok=new StagePolicy.Item("OK",1070,2340); break;
                }
            }
            if(ok==null) return stop("OKボタンを確認できないため停止");
            return action(ok,"合計MP30：売却確認へ",()->{confirmed=0;state=State.CONFIRM;});
        }
        if(mp!=0) return stop("既存の選択があるため停止。解除してから開始してください");
        StagePolicy.Decision d=action(new StagePolicy.Item("30枠連続選択",115,930),"30枠を連続選択（累計売却"+completed+"体）",()->state=State.SELECTED);
        d.selectionTaps=30; return d;
    }
    private static StagePolicy.Item exact(List<StagePolicy.Item> items,String name) {
        for(StagePolicy.Item item:items) if(item.text.equals(name)) return item;
        return null;
    }
    private static StagePolicy.Decision action(StagePolicy.Item target,String status,Runnable done) {
        StagePolicy.Decision d=new StagePolicy.Decision(target,status,false);d.holdMs=100;d.completed=done;return d;
    }
    private static StagePolicy.Decision stop(String text) { return new StagePolicy.Decision(null,text,true); }
}
