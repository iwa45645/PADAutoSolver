package com.example.padautosolver;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class UraClearProofTest {
    private List<StagePolicy.Item> text(String... lines){List<StagePolicy.Item> out=new ArrayList<>();for(String s:lines)out.add(new StagePolicy.Item(s,600,1000));return out;}
    @Test public void resultMustFollowThePendingFinalAttackAndCannotBeTipsOrGameOver(){
        assertTrue(UraClearProof.evidence(22,true,"ATTACK",text("C L E A R")));
        assertTrue(UraClearProof.evidence(22,true,"ATTACK",text("クリア報酬","獲得経験値","獲得コイン")));
        assertFalse(UraClearProof.evidence(21,true,"ATTACK",text("CLEAR")));
        assertFalse(UraClearProof.evidence(22,false,"ATTACK",text("CLEAR")));
        assertFalse(UraClearProof.evidence(22,true,"CHARGE",text("CLEAR")));
        assertFalse(UraClearProof.evidence(22,true,"ATTACK",text("TIPS","OK")));
        assertFalse(UraClearProof.evidence(22,true,"ATTACK",text("CLEAR","ゲームオーバー")));
        assertFalse(UraClearProof.evidence(22,true,"ATTACK",text("クリア報酬","獲得コイン")));
    }
    @Test public void completionNeedsTwoConsecutiveFreshFrames(){
        UraClearProof p=new UraClearProof();assertFalse(p.observe(true,1));assertFalse(p.observe(true,1));
        assertFalse(p.observe(false,2));assertFalse(p.observe(true,3));assertTrue(p.observe(true,4));
    }
}
