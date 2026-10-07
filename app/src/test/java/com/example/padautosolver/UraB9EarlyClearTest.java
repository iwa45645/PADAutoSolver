package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraB9EarlyClearTest {
    private boolean retain(int floor,int op,int round,int start,int sekka,int skill,int skillRound,String dispatch,String pending,String proof,Integer cd){
        return UraProgressPolicy.b9RetainShield(floor,op,round,start,sekka,skill,skillRound,dispatch,pending,proof,cd);
    }
    @Test public void onlyVerifiedRecoveryChargeCanRetainOneTurnShield(){
        assertTrue(retain(9,0,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        assertTrue(retain(9,0,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",4));
        assertFalse(retain(9,0,7,7,3,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,7,7,4,1,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,7,7,4,6,6,"ACKNOWLEDGED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,7,7,4,6,6,"VERIFIED","ATTACK","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:8",1));
        assertFalse(retain(9,1,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(8,0,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,8,7,4,6,7,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        for(Integer cd:new Integer[]{null,-1,0,2,3,5})assertFalse(retain(9,0,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",cd));
    }
    @Test public void skipChargeOnlyAfterRetainedShieldAndActualMionReady(){
        assertTrue(UraProgressPolicy.b9SkipCharge(9,1,7,7,7,0));
        for(Integer cd:new Integer[]{null,-1,1,2})assertFalse(UraProgressPolicy.b9SkipCharge(9,1,7,7,7,cd));
        assertFalse(UraProgressPolicy.b9SkipCharge(9,1,7,7,-1,0));
        assertFalse(UraProgressPolicy.b9SkipCharge(9,2,7,7,7,0));
        assertTrue(UraProgressPolicy.b9SkipCharge(9,1,10,7,7,0));
        assertFalse(UraProgressPolicy.b9SkipCharge(9,1,11,7,7,0));
        assertFalse(UraProgressPolicy.b9SkipCharge(9,1,8,7,8,0));
        assertFalse(UraProgressPolicy.b9SkipCharge(10,1,7,7,7,0));
    }
    @Test public void delayedChargeIsBoundedByActualStartingCdAndFreshHp(){
        for(int turn=0;turn<3;turn++)assertTrue(UraProgressPolicy.safeB9DelayedCharge(new byte[30],350000,9,1,7+turn,7,7,3,3-turn,true));
        assertFalse(UraProgressPolicy.safeB9DelayedCharge(new byte[30],349999,9,1,7,7,7,3,3,true));
        assertFalse(UraProgressPolicy.safeB9DelayedCharge(new byte[30],350000,9,1,10,7,7,3,0,true));
        assertFalse(UraProgressPolicy.safeB9DelayedCharge(new byte[30],350000,9,1,7,7,7,4,4,true));
        assertFalse(UraProgressPolicy.safeB9DelayedCharge(new byte[30],350000,9,1,8,7,7,3,3,true));
        assertFalse(UraProgressPolicy.safeB9DelayedCharge(new byte[30],350000,9,2,7,7,7,3,3,true));
        assertFalse(UraProgressPolicy.safeB9DelayedCharge(new byte[30],350000,9,1,7,7,7,3,3,false));
        byte[] poison=new byte[30];poison[4]=8;assertFalse(UraProgressPolicy.safeB9DelayedCharge(poison,350000,9,1,7,7,7,3,3,true));
        assertFalse(UraProgressPolicy.b9CooldownCourse(7,7,3,null));
        assertFalse(UraProgressPolicy.b9CooldownCourse(6,7,3,3));
    }
    @Test public void actualB9ChargeRouteConsumesHealingWithoutLeaderAttack(){
        byte[] board={0,0,5,2,1,3,1,5,0,5,1,5,0,3,3,5,0,5,4,3,5,2,0,5,3,2,2,5,5,3};
        java.util.List<Integer> path=UraChargeRoute.findHealing(board);
        for(int i=1;i<path.size();i++){int a=path.get(i-1),b=path.get(i);assertEquals(1,Math.abs(a%6-b%6)+Math.abs(a/6-b/6));byte temp=board[a];board[a]=board[b];board[b]=temp;}
        PuzzleSolver.MatchStats stats=PuzzleSolver.firstWave(board,6,5);
        assertTrue(stats.colorCombos[5]>0);assertEquals(0,stats.firstTShapes[3]);assertTrue(stats.colorCombos[3]<2);
    }
}
