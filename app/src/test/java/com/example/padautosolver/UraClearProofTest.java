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
    @Test public void lateClearCanBeVerifiedAfterWaitingOrMenuButNeverBeforeAttack(){
        assertTrue(UraClearProof.finalOutcomePhase(10));assertTrue(UraClearProof.finalOutcomePhase(0));assertTrue(UraClearProof.finalOutcomePhase(1));
        assertFalse(UraClearProof.finalOutcomePhase(7));assertFalse(UraClearProof.finalOutcomePhase(9));
        assertTrue(UraClearProof.reward(text("クリア報酬","獲得コイン","獲得経験値")));
        assertFalse(UraClearProof.reward(text("TIPS","OK")));
    }
    @Test public void terminalResumeRequiresActualClearAndAnUnverifiedDispatchedFinalAttack(){
        assertTrue(UraClearProof.terminalResume(22,5,true,"ATTACK","ACKNOWLEDGED",true));
        assertTrue(UraClearProof.terminalResume(22,5,true,"ATTACK","DISPATCH_INTENT",true));
        assertFalse(UraClearProof.terminalResume(22,5,true,"ATTACK","ACKNOWLEDGED",false));
        assertFalse(UraClearProof.terminalResume(22,4,true,"ATTACK","ACKNOWLEDGED",true));
        assertFalse(UraClearProof.terminalResume(21,5,true,"ATTACK","ACKNOWLEDGED",true));
        assertFalse(UraClearProof.terminalResume(22,5,true,"CHARGE","ACKNOWLEDGED",true));
        assertFalse(UraClearProof.terminalResume(22,5,true,"ATTACK","PREPARED",true));
        assertFalse(UraClearProof.terminalResume(22,5,false,"ATTACK","ACKNOWLEDGED",true));
    }
    @Test public void actualRewardUsesExpAndStillRequiresBothRewardLabels(){
        assertTrue(UraClearProof.reward(text("クリア報酬","獲得コイン 5,739,050","獲得 E X P 1,390,782")));
        assertTrue(UraClearProof.evidence(22,true,"ATTACK",text("クリア報酬","獲得コイン","獲得EXP")));
        assertFalse(UraClearProof.reward(text("クリア報酬","経験値ストック 54,450","獲得コイン")));
        assertFalse(UraClearProof.reward(text("獲得コイン","獲得EXP")));
        assertFalse(UraClearProof.reward(text("クリア報酬","獲得コイン","獲得EXP","ゲームオーバー")));
    }
    private List<StagePolicy.Item> actualReward(){return new ArrayList<>(List.of(
        new StagePolicy.Item("クリア報酬",190,584),new StagePolicy.Item("裏魔門の守護者",835,800),
        new StagePolicy.Item("獲得コイン",344,883),new StagePolicy.Item("5,739,050",895,887),
        new StagePolicy.Item("経験値ストック",378,951),new StagePolicy.Item("54,450",938,954),
        new StagePolicy.Item("獲得EHP",341,1020),new StagePolicy.Item("1,390,782",903,1023)));}
    @Test public void reviewedEhpReadingRequiresThisDungeonAndAllAlignedNumericRows(){
        var rows=actualReward();assertTrue(UraClearProof.reward(rows));
        assertEquals(5739050,UraClearProof.amount(rows,"獲得コイン"));assertEquals(1390782,UraClearProof.amount(rows,"獲得EHP"));
        rows.remove(1);assertFalse(UraClearProof.reward(rows));
        rows=actualReward();rows.remove(7);assertFalse(UraClearProof.reward(rows));
        assertFalse(UraClearProof.reward(text("クリア報酬","獲得コイン","経験値ストック","裏魔門の守護者","獲得EHP")));
    }
    @Test public void numericRewardMustBeOnTheSameRowAndToTheRight(){
        var rows=actualReward();rows.set(7,new StagePolicy.Item("1,390,782",903,1060));assertFalse(UraClearProof.reward(rows));
        rows=actualReward();rows.set(7,new StagePolicy.Item("1,390,782",500,1023));assertFalse(UraClearProof.reward(rows));
    }
}
