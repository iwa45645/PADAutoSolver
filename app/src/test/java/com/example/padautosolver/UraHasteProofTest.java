package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraHasteProofTest {
    private UraDialogPolicy.Read modal(int layer,String title){return new UraDialogPolicy.Read(layer,title,"",null,new StagePolicy.Item("戻る",792,2168));}
    @Test public void actualNineteenToFifteenHasAnIndependentExactYukineModalTitle() {
        assertTrue(UraHasteProof.matches(19,15,4,false,modal(1,"スキル1:雪雲の一変"),"雪雲の一変",true));
    }
    @Test public void exactIdentityCannotRepairWrongOrMissingCooldowns() {
        var read=modal(1,"スキル1:雪雲の一変");
        assertFalse(UraHasteProof.matches(18,15,4,false,read,"雪雲の一変",true));
        assertFalse(UraHasteProof.matches(null,15,4,false,read,"雪雲の一変",true));
        assertFalse(UraHasteProof.matches(19,null,4,false,read,"雪雲の一変",true));
    }
    @Test public void assistTitleWrongSkillOrUnverifiedBackCannotProvideIdentity() {
        assertFalse(UraHasteProof.matches(19,15,4,false,modal(2,"雪雲の一変"),"雪雲の一変",true));
        assertFalse(UraHasteProof.matches(19,15,4,false,modal(1,"雪花の氷乱"),"雪雲の一変",true));
        assertFalse(UraHasteProof.matches(19,15,4,false,modal(1,"雪雲の一変"),"雪雲の一変",false));
        assertFalse(UraHasteProof.matches(19,15,4,false,null,"雪雲の一変",true));
    }
    @Test public void ExistingHeldIdentityStillWorksWithoutAReadySkillModal() {
        assertTrue(UraHasteProof.matches(19,15,4,true,null,"雪雲の一変",false));
    }
}
