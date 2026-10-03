package com.example.padautosolver;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoulettePlanTest {
    @Test public void irrelevantAssistOcrConflictCannotEraseIndependentBaseRead() {
        List<StagePolicy.Item> text=List.of(new StagePolicy.Item("スキル1:ブリリアントコンチェルトあと1ターン",600,945),new StagePolicy.Item("スキル2:氷刀あと7ターン",600,994),new StagePolicy.Item("UCD1_あと1ターン",1118,253),new StagePolicy.Item("UCD2_あと17ターン",1118,399));
        assertNull(UraHeldSkillInfo.read(text));
        UraHeldSkillInfo info=UraHeldSkillInfo.readBase(text);assertNotNull(info);assertTrue(info.baseNamed("ブリリアントコンチェルト"));assertEquals(Integer.valueOf(1),info.baseRemaining);
        List<StagePolicy.Item> conflicting=new ArrayList<>(text);conflicting.add(new StagePolicy.Item("UCD1_使用可能",1118,253));assertNull(UraHeldSkillInfo.readBase(conflicting));
    }
    @Test public void chargingCreatesActualStableComboDespiteFiveRouletteCells() {
        byte[] b={6,5,0,3,-1,1,0,-1,4,2,3,4,0,6,6,2,0,-1,2,4,1,3,-1,1,1,3,2,-1,2,5};
        long mask=(1L<<4)|(1L<<7)|(1L<<17)|(1L<<22)|(1L<<27);
        assertFalse(RoulettePlan.stableTriple(b,mask));
        List<Integer> route=RoulettePlan.chargeRoute(b,mask);
        assertTrue(route.size()>1);assertTrue(RoulettePlan.stableTriple(RoulettePlan.replay(b,mask,route),mask));
        // The old out-and-back path was zero combo and did not charge Mion.
        assertFalse(RoulettePlan.stableTriple(RoulettePlan.replay(b,mask,List.of(0,6,0)),mask));
    }
    @Test public void chargeRejectsPoisonAndUnknownStableCells() {
        byte[] b=separated();b[0]=7;
        try{RoulettePlan.chargeRoute(b,1L<<29);fail();}catch(IllegalArgumentException expected){}
        b[0]=-1;try{RoulettePlan.chargeRoute(b,1L<<29);fail();}catch(IllegalArgumentException expected){}
    }
    private byte[] separated() {
        return new byte[]{3,3,3,1,3,2, 1,2,4,5,3,4, 3,3,3,1,3,2, 1,2,4,5,1,4, 5,5,5,1,2,4};
    }
    @Test public void robustPlanIgnoresEveryRoulettePhaseButRejectsChangedStableOrbs() {
        byte[] b=separated();long mask=1L<<29;b[29]=-1;
        RoulettePlan p=new RoulettePlan(b,mask,List.of(6,7,6),100);
        assertTrue(p.water>=2);assertTrue(p.heal>=1);
        for(byte color=0;color<10;color++){b[29]=color;assertTrue(p.current(b,mask,101));}
        b[0]=1;assertFalse(p.current(b,mask,101));assertFalse(p.current(p.source,1L<<28,101));assertFalse(p.current(p.source,mask,15101));
    }
    @Test public void waterRouletteCanMergeTwoSetsAndMustRejectSnapshotOnlyProof() {
        byte[] b=separated();b[6]=3;b[7]=3;b[8]=3;
        b[6]=1;b[7]=2;b[8]=4; // row 1 only the roulette can bridge water rows 0 and 2
        b[4]=2;b[10]=4;b[16]=1;b[6]=-1;long mask=1L<<6;
        int[] worst=RoulettePlan.worst(b,mask);assertEquals(1,worst[0]);
        try{new RoulettePlan(b,mask,List.of(9,10,9),0);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void pathMustNeverVisitRouletteAndUnknownStableCellsAreRejected() {
        byte[] b=separated();b[29]=-1;long mask=1L<<29;
        try{new RoulettePlan(b,mask,List.of(28,29),0);fail();}catch(IllegalArgumentException expected){}
        b[3]=-1;try{RoulettePlan.solve(b,mask,5,10,50,0);fail();}catch(IllegalArgumentException expected){}
        try{RoulettePlan.solve(separated(),3,5,10,50,0);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void solverProducesPhaseIndependentAdjacentRoute() {
        byte[] b=separated();b[29]=-1;RoulettePlan p=RoulettePlan.solve(b,1L<<29,15,80,1000,0);
        assertTrue(p.water>=2&&p.heal>=1);assertFalse(p.path.contains(29));
    }
    @Test public void firstWaveDoesNotIncludeGravityMatches() {
        byte[] b={0,0,0, 3,1,3, 3,3,3};
        PuzzleSolver.MatchStats first=PuzzleSolver.firstWave(b,3,3);
        assertEquals(2,first.combos);assertEquals(1,first.colorCombos[3]);
    }
}
