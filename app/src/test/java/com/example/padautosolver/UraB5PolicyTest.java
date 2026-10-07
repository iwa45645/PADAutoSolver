package com.example.padautosolver;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class UraB5PolicyTest {
    private byte[] entry(){return new byte[]{5,3,4,6,5,3,0,6,6,1,6,2,5,1,1,2,1,5,2,5,0,2,3,1,3,3,2,3,2,3};}
    @Test public void chargeRequiresIndependentSurvivalAndExactlyOneRemainingTurn(){
        assertTrue(UraB5Policy.safeCharge(entry(),230000,1,false));
        assertFalse(UraB5Policy.safeCharge(entry(),229999,1,false));
        assertFalse(UraB5Policy.safeCharge(entry(),105744,1,false));
        assertFalse(UraB5Policy.safeCharge(entry(),230000,null,false));
        assertFalse(UraB5Policy.safeCharge(entry(),230000,0,false));
        assertFalse(UraB5Policy.safeCharge(entry(),230000,2,false));
        assertFalse(UraB5Policy.safeCharge(entry(),230000,1,true));
        byte[] dangerous=entry();dangerous[0]=7;assertFalse(UraB5Policy.safeCharge(dangerous,230000,1,false));
        dangerous[0]=-1;assertFalse(UraB5Policy.safeCharge(dangerous,230000,1,false));
    }
    @Test public void actualEntryChargeRouteReallyClearsACombo(){
        byte[] board=entry();List<Integer> route=RoulettePlan.chargeRoute(board,0);
        assertTrue(PuzzleSolver.analyze(RoulettePlan.replay(board,0,route),6,5).combos>=1);
    }
    @Test public void mionConversionRetainsHealingAndCanFormTwoWaterSets(){
        byte[] board=entry();for(int i=0;i<board.length;i++)if(board[i]==0||board[i]==1||board[i]==6||board[i]==9)board[i]=3;
        assertTrue(UraB5Policy.enoughRecovery(board));
        PuzzleGoal goal=PuzzleGoal.waterAndHeal();
        var result=PuzzleSolver.solve(board,6,5,44,2500,1800,goal);
        var plan=new UraPuzzlePlan(board,result,goal,0);
        assertTrue(plan.stats.colorCombos[3]>=2);assertTrue(plan.stats.colorCombos[5]>=1);
        board[0]=3;assertFalse(plan.current(board,100));
    }
    @Test public void recoveryFallbackCannotPromiseUnavailableOrbs(){
        assertFalse(UraB5Policy.enoughRecovery(new byte[30]));
        assertTrue(UraB5Policy.rukaCanRecover(new byte[30]));
        byte[] allWater=new byte[30];Arrays.fill(allWater,(byte)3);assertFalse(UraB5Policy.rukaCanRecover(allWater));
    }
    private byte[] lukaEntry(){return new byte[]{5,5,2,4,1,1,6,5,3,5,0,5,5,5,1,1,6,-1,5,5,2,5,6,5,4,2,5,4,5,3};}
    @Test public void lukaChargeExcludesInheritedSpinnerAndSurvivesAllItsPhases(){
        byte[] board=lukaEntry();long mask=1L<<17;
        assertTrue(UraB5Policy.safeCharge(board,mask,true,true,381230,1,false));
        List<Integer> route=RoulettePlan.chargeRoute(board,mask);
        assertFalse(route.contains(17));
        byte[] replay=RoulettePlan.replay(board,mask,route);
        assertTrue(RoulettePlan.stableTriple(replay,mask));
        for(byte color=0;color<10;color++){
            replay[17]=color;assertTrue(PuzzleSolver.firstWave(replay,6,5).combos>=1);
        }
    }
    @Test public void spinnerNeedsLukaAndActualYukineReceiptAndCannotSurviveCharge(){
        byte[] board=lukaEntry();long mask=1L<<17;
        assertFalse(UraB5Policy.safeCharge(board,mask,false,true,381230,1,false));
        assertFalse(UraB5Policy.safeCharge(board,mask,true,false,381230,1,false));
        assertFalse(UraB5Policy.safeCharge(board,mask,true,true,381230,1,true));
        assertFalse(UraB5Policy.safeCharge(board,-1,true,true,381230,1,false));
        assertFalse(UraB5Policy.safeCharge(board,mask|(1L<<23),true,true,381230,1,false));
        assertFalse(UraB5Policy.acceptedMask(mask,true,true,true));
        assertTrue(UraB5Policy.acceptedMask(0,true,true,true));
        board[16]=-1;assertFalse(UraB5Policy.safeCharge(board,mask,true,true,381230,1,false));
    }
    @Test public void lukaChargeRequiresLiteralReadyInOneTurnAndIndependentHp(){
        byte[] board=lukaEntry();long mask=1L<<17;
        assertFalse(UraB5Policy.safeCharge(board,mask,true,true,229999,1,false));
        assertFalse(UraB5Policy.safeCharge(board,mask,true,true,381230,null,false));
        assertFalse(UraB5Policy.safeCharge(board,mask,true,true,381230,2,false));
        board[6]=8;assertFalse(UraB5Policy.safeCharge(board,mask,true,true,381230,1,false));
    }
}
