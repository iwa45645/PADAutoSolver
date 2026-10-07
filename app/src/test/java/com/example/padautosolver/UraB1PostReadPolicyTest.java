package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraB1PostReadPolicyTest {
    @Test public void actualRukaOcrFailureAllowsOnlyTwoObservationsNeverVerification() {
        assertEquals(UraB1PostReadPolicy.Outcome.RECAPTURE_ONLY,UraB1PostReadPolicy.assess(0,false,5,16,0,0));
        assertEquals(UraB1PostReadPolicy.Outcome.RECAPTURE_ONLY,UraB1PostReadPolicy.assess(0,false,5,16,0,1));
        assertEquals(UraB1PostReadPolicy.Outcome.STOP,UraB1PostReadPolicy.assess(0,false,5,16,0,2));
        assertEquals(UraB1PostReadPolicy.Outcome.VERIFIED,UraB1PostReadPolicy.assess(0,true,5,16,0,1));
    }
    @Test public void wrongOrMissingCooldownCannotBeSelectedThroughIdentityRetry() {
        for(boolean named:new boolean[]{false,true}) {
            assertEquals(UraB1PostReadPolicy.Outcome.STOP,UraB1PostReadPolicy.assess(0,named,4,16,0,0));
            assertEquals(UraB1PostReadPolicy.Outcome.STOP,UraB1PostReadPolicy.assess(0,named,5,15,0,0));
            assertEquals(UraB1PostReadPolicy.Outcome.STOP,UraB1PostReadPolicy.assess(0,named,null,16,0,0));
            assertEquals(UraB1PostReadPolicy.Outcome.STOP,UraB1PostReadPolicy.assess(0,named,5,null,0,0));
        }
    }
    @Test public void onlyExactPostconditionsForEachOpeningSkillCanAdvance() {
        int[] base={5,5,4,3,2,5};Integer[] assist={16,33,26,21,null,null};
        for(int step=0;step<6;step++) {
            assertEquals(UraB1PostReadPolicy.Outcome.VERIFIED,UraB1PostReadPolicy.assess(step,true,base[step],assist[step],0,0));
            assertEquals(UraB1PostReadPolicy.Outcome.STOP,UraB1PostReadPolicy.assess(step,true,0,assist[step],0,0));
        }
        assertEquals(UraB1PostReadPolicy.Outcome.STOP,UraB1PostReadPolicy.assess(-1,true,5,16,0,0));
        assertEquals(UraB1PostReadPolicy.Outcome.STOP,UraB1PostReadPolicy.assess(6,true,5,16,0,0));
    }
}
