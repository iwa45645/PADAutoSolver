package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraSkillPostReadPolicyTest {
    @Test public void misreadMionNameWithTwoTurnsOnlyPermitsBoundedObservation(){
        for(int retries=0;retries<2;retries++)assertEquals(UraSkillPostReadPolicy.Outcome.RECAPTURE_ONLY,UraSkillPostReadPolicy.assess(2,2,false,retries));
        assertEquals(UraSkillPostReadPolicy.Outcome.STOP,UraSkillPostReadPolicy.assess(2,2,false,2));
    }
    @Test public void namedPostconditionCanVerifyAfterObservation(){
        assertEquals(UraSkillPostReadPolicy.Outcome.VERIFIED,UraSkillPostReadPolicy.assess(2,2,true,1));
    }
    @Test public void wrongMissingOrReadyCooldownIsNeverANameRetry(){
        for(Integer value:new Integer[]{null,0,1,3,-1})assertEquals(UraSkillPostReadPolicy.Outcome.STOP,UraSkillPostReadPolicy.assess(2,value,false,0));
    }
}
