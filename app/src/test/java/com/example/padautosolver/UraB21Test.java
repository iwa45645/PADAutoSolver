package com.example.padautosolver;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class UraB21Test {
    static final byte[] ENTRY={3,0,4,3,0,5,4,0,0,3,4,3,-1,1,5,5,2,-1,3,1,1,4,3,3,3,5,4,4,2,4};
    @Test public void chargeDoesNotPlanWaterTOrTwoWaterMatchesForAnySpinnerPair(){
        long mask=UraDualRoulettePlan.MASK;List<Integer> route=UraChargeRoute.find(ENTRY,6,5,mask,true);
        byte[] b=RoulettePlan.replay(ENTRY,mask,route);
        for(byte a=0;a<10;a++)for(byte c=0;c<10;c++){
            b[12]=a;b[17]=c;PuzzleSolver.MatchStats stats=PuzzleSolver.firstWave(b,6,5);
            assertTrue(stats.combos>=1);assertEquals(0,stats.firstTShapes[3]);assertTrue(stats.colorCombos[3]<2);
        }
    }
    @Test public void firstChargeRequiresActualFullBarsAndCannotRepeat(){
        long mask=UraDualRoulettePlan.MASK;
        assertTrue(UraProgressPolicy.safeB21Charge(ENTRY,mask,350000,0,36,36,1,true));
        assertFalse(UraProgressPolicy.safeB21Charge(ENTRY,mask,350000,0,36,36,1,false));
        assertFalse(UraProgressPolicy.safeB21Charge(ENTRY,mask,349999,0,36,36,1,true));
        assertFalse(UraProgressPolicy.safeB21Charge(ENTRY,mask,350000,0,37,36,1,true));
        assertFalse(UraProgressPolicy.safeB21Charge(ENTRY,mask,350000,1,36,36,1,true));
        assertFalse(UraProgressPolicy.safeB21Charge(ENTRY,mask,350000,0,36,36,0,true));
        assertFalse(UraProgressPolicy.safeB21Charge(ENTRY,0,350000,0,36,36,1,true));
    }
    @Test public void nextFloorNeedsTwoConsecutiveFreshObservations(){
        UraTurnProof.FloorConfirmation confirm=new UraTurnProof.FloorConfirmation();
        assertFalse(confirm.observe(21,21));assertFalse(confirm.observe(21,20));
        assertFalse(confirm.observe(21,21));assertTrue(confirm.observe(21,21));
        confirm.reset();assertFalse(confirm.observe(21,21));assertFalse(confirm.observe(21,22));
    }
    @Test public void scheduleChargesDelayedSekkaAndMionBeforeActivating(){
        int[] cd={1,1,0,1,0};int round=36;
        for(int op:UraProgressPolicy.script(21)){
            if(op<100){assertEquals(0,cd[op]);cd[op]=new int[]{4,2,5,5,5}[op];}
            else {round++;for(int i=0;i<cd.length;i++)cd[i]=Math.max(0,cd[i]-1);}
        }
        assertEquals(38,round);assertEquals(1,cd[1]);assertNull(UraProgressPolicy.script(23));
    }
}
