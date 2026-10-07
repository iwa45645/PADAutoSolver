package com.example.padautosolver;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class UraB22Test {
    static final byte[] ENTRY={0,2,2,2,0,4,4,0,5,5,4,3,5,2,3,0,2,0,2,1,4,2,3,0,5,5,3,1,3,2};
    @Test public void darkEntryChargeHasARealMatchWithoutPlanningTheAttackCondition(){
        List<Integer> path=UraChargeRoute.find(ENTRY,6,5,0,true);byte[] b=RoulettePlan.replay(ENTRY,0,path);
        PuzzleSolver.MatchStats stats=PuzzleSolver.firstWave(b,6,5);
        assertTrue(stats.combos>=1);assertEquals(0,stats.firstTShapes[3]);assertTrue(stats.colorCombos[3]<2);
    }
    @Test public void darkChargeRequiresFullBarsFreshEntryCooldownAndHpBound(){
        assertTrue(UraProgressPolicy.safeB22Charge(ENTRY,350000,0,38,38,1,true));
        assertFalse(UraProgressPolicy.safeB22Charge(ENTRY,349999,0,38,38,1,true));
        assertFalse(UraProgressPolicy.safeB22Charge(ENTRY,350000,0,38,38,1,false));
        assertFalse(UraProgressPolicy.safeB22Charge(ENTRY,350000,1,38,38,1,true));
        assertFalse(UraProgressPolicy.safeB22Charge(ENTRY,350000,0,39,38,1,true));
        assertFalse(UraProgressPolicy.safeB22Charge(ENTRY,350000,0,38,38,0,true));
        byte[] poison=ENTRY.clone();poison[3]=7;assertFalse(UraProgressPolicy.safeB22Charge(poison,350000,0,38,38,1,true));
    }
    @Test public void recoveryReadinessSeparatesBaseAndOverchargedAssist(){
        assertEquals(1,UraProgressPolicy.rukaRecoveryLayer(new UraHeldSkillInfo("ダブル防御態勢水","",0,20),false));
        assertEquals(2,UraProgressPolicy.rukaRecoveryLayer(new UraHeldSkillInfo("ダブル防御態勢水","かつての水柱",-1,0),false));
        assertEquals(0,UraProgressPolicy.rukaRecoveryLayer(new UraHeldSkillInfo("ダブル防御態勢水","かつての水柱",-1,1),false));
        assertEquals(0,UraProgressPolicy.rukaRecoveryLayer(new UraHeldSkillInfo("別スキル","かつての水柱",-1,0),false));
        assertEquals(0,UraProgressPolicy.rukaRecoveryLayer(null,true));
    }
    @Test public void attackRequiresRestoredAwakeningsAndSameTurnRecoveryAbsorptionAndMion(){
        assertTrue(UraProgressPolicy.b22AttackAllowed(4,39,38,39,39,39,1,new int[]{244416,611045},611045,false));
        assertTrue(UraProgressPolicy.b22AttackAllowed(4,39,38,39,39,39,1,new int[]{244660,612278},612278,false));
        assertFalse(UraProgressPolicy.b22AttackAllowed(4,39,38,39,39,39,1,new int[]{244660,612278},0,false));
        assertFalse(UraProgressPolicy.b22AttackAllowed(4,39,38,39,39,39,1,new int[]{244416,244416},611045,false));
        assertFalse(UraProgressPolicy.b22AttackAllowed(4,39,38,39,39,39,1,new int[]{244416,611045},611045,true));
        assertFalse(UraProgressPolicy.b22AttackAllowed(4,39,38,38,39,39,1,new int[]{244416,611045},611045,false));
        assertFalse(UraProgressPolicy.b22AttackAllowed(4,39,38,39,38,39,1,new int[]{244416,611045},611045,false));
        assertFalse(UraProgressPolicy.b22AttackAllowed(4,39,38,39,39,38,1,new int[]{244416,611045},611045,false));
    }
}
