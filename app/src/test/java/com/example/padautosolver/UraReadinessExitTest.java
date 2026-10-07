package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraReadinessExitTest {
    @Test public void coolingSkillCanReturnToVerifiedCombatWithoutBackButton(){
        assertEquals(UraReadinessExit.Outcome.CONTINUE_COMBAT,UraReadinessExit.evaluate(false,false,true,true));
        assertEquals(UraReadinessExit.Outcome.CLOSE_MODAL,UraReadinessExit.evaluate(true,true,false,false));
        assertEquals(UraReadinessExit.Outcome.WAIT,UraReadinessExit.evaluate(true,false,true,true));
        assertEquals(UraReadinessExit.Outcome.WAIT,UraReadinessExit.evaluate(false,true,false,true));
        assertEquals(UraReadinessExit.Outcome.WAIT,UraReadinessExit.evaluate(false,false,true,false));
        assertEquals(UraReadinessExit.Outcome.WAIT,UraReadinessExit.evaluate(false,false,false,false));
    }
}
