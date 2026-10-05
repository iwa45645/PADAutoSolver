package com.example.padautosolver;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class UraB20Test {
    static final byte[] ENTRY={1,5,5,0,4,3,3,1,1,5,3,0,5,4,0,4,4,0,4,4,5,-1,3,0,5,4,-1,4,3,5,4,1,0,0,4,1,0,5,4,2,4,5};
    @Test public void entryChargeNeverTouchesEitherSpinnerAndMatchesForAllHundredColors(){
        long mask=UraProgressPolicy.b20Mask(34,34);List<Integer> path=UraChargeRoute.find(ENTRY,7,6,mask);
        byte[] moved=ENTRY.clone();int prior=-1;
        for(int p:path){assertEquals(0,mask&(1L<<p));if(prior>=0){assertEquals(1,Math.abs(p%7-prior%7)+Math.abs(p/7-prior/7));byte v=moved[p];moved[p]=moved[prior];moved[prior]=v;}prior=p;}
        for(byte a=0;a<10;a++)for(byte b=0;b<10;b++){moved[21]=a;moved[26]=b;assertTrue(PuzzleSolver.firstWave(moved,7,6).combos>=1);}
    }
    @Test public void expandedEntryRequiresExactMaskGeometryHpCooldownAndUnconsumedTurn(){
        long mask=UraProgressPolicy.b20Mask(34,34);
        assertTrue(UraProgressPolicy.safeB20Charge(ENTRY,mask,350000,1,34,34,1));
        assertFalse(UraProgressPolicy.safeB20Charge(ENTRY,mask,349999,1,34,34,1));
        assertFalse(UraProgressPolicy.safeB20Charge(ENTRY,mask,350000,1,35,34,1));
        assertFalse(UraProgressPolicy.safeB20Charge(ENTRY,mask,350000,1,34,34,0));
        assertFalse(UraProgressPolicy.safeB20Charge(ENTRY,mask,350000,1,34,34,null));
        assertFalse(UraProgressPolicy.safeB20Charge(ENTRY,UraDualRoulettePlan.MASK,350000,1,34,34,1));
        byte[] toxic=ENTRY.clone();toxic[0]=7;assertFalse(UraProgressPolicy.safeB20Charge(toxic,mask,350000,1,34,34,1));
        assertFalse(UraProgressPolicy.safeB20Charge(new byte[30],mask,350000,1,34,34,1));
    }
    @Test public void restoredBoardKeepsTwoSpinnersUntilThirdConsumedTurn(){
        assertEquals(UraDualRoulettePlan.MASK,UraProgressPolicy.b20Mask(35,34));
        assertEquals(UraDualRoulettePlan.MASK,UraProgressPolicy.b20Mask(36,34));
        assertEquals(0,UraProgressPolicy.b20Mask(37,34));
        assertEquals(-1,UraProgressPolicy.b20Mask(33,34));
        byte[] b=new byte[30];b[12]=-1;b[17]=-1;
        assertTrue(UraProgressPolicy.safeB20Charge(b,UraDualRoulettePlan.MASK,350000,5,36,34,1));
        assertFalse(UraProgressPolicy.safeB20Charge(b,UraDualRoulettePlan.MASK,350000,5,37,34,1));
    }
    @Test public void callbacksElapsedTimeAndUnchangedCooldownAreNotTurnEvidence(){
        assertTrue(UraTurnProof.cooldown(1,0,true,20,21));
        assertTrue(UraTurnProof.cooldown(2,1,true,20,21));
        assertFalse(UraTurnProof.cooldown(1,1,true,20,1000));
        assertFalse(UraTurnProof.cooldown(1,0,false,20,1000));
        assertFalse(UraTurnProof.cooldown(1,0,true,20,20));
        assertFalse(UraTurnProof.cooldown(0,0,true,20,21));
        assertFalse(UraTurnProof.cooldown(null,0,true,20,21));
        assertTrue(UraTurnProof.nextFloor(20,21,true));
        assertFalse(UraTurnProof.nextFloor(20,21,false));
        assertFalse(UraTurnProof.nextFloor(20,20,true));
        assertFalse(UraTurnProof.nextFloor(20,22,true));
    }
    @Test public void b20ScheduleUsesOnlyAvailableSkillsAcrossTwoAttacks(){
        int[] cd={3,1,0,0,0};int round=34;
        for(int op:UraProgressPolicy.script(20)){
            if(op<100){assertEquals(0,cd[op]);cd[op]=new int[]{4,2,5,5,5}[op];}
            else{round++;for(int i=0;i<cd.length;i++)cd[i]=Math.max(0,cd[i]-1);}
        }
        assertEquals(38,round);assertEquals(1,cd[1]);assertNull(UraProgressPolicy.script(23));
    }
}
