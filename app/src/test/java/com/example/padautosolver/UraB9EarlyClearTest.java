package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraB9EarlyClearTest {
    private boolean retain(int floor,int op,int round,int start,int sekka,int skill,int skillRound,String dispatch,String pending,String proof,Integer cd){
        return UraProgressPolicy.b9RetainShield(floor,op,round,start,sekka,skill,skillRound,dispatch,pending,proof,cd);
    }
    @Test public void onlyVerifiedRecoveryChargeCanRetainOneTurnShield(){
        assertTrue(retain(9,0,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,7,7,3,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,7,7,4,1,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,7,7,4,6,6,"ACKNOWLEDGED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,7,7,4,6,6,"VERIFIED","ATTACK","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:8",1));
        assertFalse(retain(9,1,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(8,0,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        assertFalse(retain(9,0,8,7,4,6,7,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",1));
        for(Integer cd:new Integer[]{null,-1,0,2})assertFalse(retain(9,0,7,7,4,6,6,"VERIFIED","CHARGE","literal-next-floor-two-frames:9",cd));
    }
    @Test public void skipChargeOnlyAfterRetainedShieldAndActualMionReady(){
        assertTrue(UraProgressPolicy.b9SkipCharge(9,1,7,7,7,0));
        for(Integer cd:new Integer[]{null,-1,1,2})assertFalse(UraProgressPolicy.b9SkipCharge(9,1,7,7,7,cd));
        assertFalse(UraProgressPolicy.b9SkipCharge(9,1,7,7,-1,0));
        assertFalse(UraProgressPolicy.b9SkipCharge(9,2,7,7,7,0));
        assertFalse(UraProgressPolicy.b9SkipCharge(9,1,8,7,8,0));
        assertFalse(UraProgressPolicy.b9SkipCharge(10,1,7,7,7,0));
    }
}
