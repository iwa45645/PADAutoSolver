package com.example.padautosolver;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.example.padautosolver.SkillScheduleSimulator.Layer.*;

public class FixedSkillScheduleTest {
    private SkillScheduleSimulator.Timeline timeline(int delay,int helperResistance) {
        // Conservative personal resistance. Extra awakening resistance is not assumed.
        var t=new SkillScheduleSimulator.Timeline(17,new int[]{10,15,5,23,5,22},
                new int[]{28,22,0,18,11,16},new int[]{0,0,5,5,5,helperResistance});
        t.delay(delay);return t;
    }
    private void opening(int delay) {
        var t=timeline(delay,4);
        assertTrue(t.ready(4,ASSIST));t.use(4,ASSIST,5,0);
        assertTrue(t.ready(0,BASE));t.use(0,BASE,5,4);
        assertTrue(t.ready(1,BASE));t.use(1,BASE,4,2);
        assertTrue(t.ready(3,BASE));t.use(3,BASE,3,2);
        assertTrue(t.ready(5,BASE));t.use(5,BASE,2,0);
        assertTrue(t.ready(2,BASE));t.use(2,BASE,5,0);
        assertFalse(t.ready(4,ASSIST)); // Haste has not restored the 16-turn assist.
        assertTrue(t.ready(4,BASE));
    }
    @Test public void delay3(){opening(3);}
    @Test public void delay4(){opening(4);}
    @Test public void delay5(){opening(5);}
    @Test public void unknownHelperMustNotBeAssumedReady(){
        var t=timeline(5,0);t.use(4,ASSIST,5,0);t.use(0,BASE,5,4);t.use(1,BASE,2,2);t.use(3,BASE,3,2);
        assertFalse(t.ready(5,BASE));assertEquals(2,t.remaining(5,BASE));
    }
    @Test public void missingYukineDelayProtectionBreaks23TurnPlan(){
        var t=new SkillScheduleSimulator.Timeline(17,new int[]{10,15,23},new int[]{28,22,18},new int[]{0,0,0});
        t.delay(5);t.use(0,BASE,5,4);t.use(1,BASE,2,2);
        assertFalse(t.ready(2,BASE));assertEquals(5,t.remaining(2,BASE));
    }
    @Test public void chargeIsCappedAndDelayedAfterCap(){
        var t=new SkillScheduleSimulator.Timeline(50,new int[]{5},new int[]{11},new int[]{0});
        t.delay(3);assertEquals(13,t.charge(0));assertFalse(t.ready(0,ASSIST));
    }
    @Test public void overchargedAssistCannotBeUsedAsBase(){
        var t=new SkillScheduleSimulator.Timeline(38,new int[]{10},new int[]{28},new int[]{0});
        assertFalse(t.ready(0,BASE));assertTrue(t.ready(0,ASSIST));
        assertThrows(IllegalStateException.class,()->t.use(0,BASE,5,4));
        t.use(0,ASSIST,10,0);t.advance(10);assertTrue(t.ready(0,BASE));
    }
}
