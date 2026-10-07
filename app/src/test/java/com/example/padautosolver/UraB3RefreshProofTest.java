package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraB3RefreshProofTest {
    @Test public void coolingSkillReleasedToCombatDoesNotRequireBackButton(){
        assertEquals(UraB3RefreshProof.Outcome.ADVANCE_FROM_COMBAT,read(4,3,true,11,21,false,false,true,true));
        assertEquals(UraB3RefreshProof.Outcome.CLOSE_NAMED_MODAL,read(4,3,true,11,21,true,true,false,false));
    }
    @Test public void wrongOrStaleTurnReceiptCannotAdvance(){
        assertEquals(UraB3RefreshProof.Outcome.UNVERIFIED,read(4,4,true,11,21,false,false,true,true));
        assertEquals(UraB3RefreshProof.Outcome.UNVERIFIED,read(4,3,false,11,21,false,false,true,true));
        assertEquals(UraB3RefreshProof.Outcome.UNVERIFIED,read(4,3,true,10,21,false,false,true,true));
        assertEquals(UraB3RefreshProof.Outcome.UNVERIFIED,read(4,3,true,11,20,false,false,true,true));
        assertEquals(UraB3RefreshProof.Outcome.UNVERIFIED,read(null,3,true,11,21,false,false,true,true));
        assertEquals(UraB3RefreshProof.Outcome.UNVERIFIED,read(3,2,true,11,21,false,false,true,true));
    }
    @Test public void ambiguousModalEnemyOrBoardOnlyAllowsObservation(){
        assertEquals(UraB3RefreshProof.Outcome.WAIT_FOR_COMBAT,read(4,3,true,11,21,true,false,true,true));
        assertEquals(UraB3RefreshProof.Outcome.WAIT_FOR_COMBAT,read(4,3,true,11,21,false,false,false,true));
        assertEquals(UraB3RefreshProof.Outcome.WAIT_FOR_COMBAT,read(4,3,true,11,21,false,false,true,false));
    }
    private UraB3RefreshProof.Outcome read(Integer before,Integer after,boolean named,long at,long seq,
            boolean modal,boolean back,boolean enemy,boolean board){
        return UraB3RefreshProof.evaluate(before,after,named,10,20,at,seq,modal,back,enemy,board);
    }
}
