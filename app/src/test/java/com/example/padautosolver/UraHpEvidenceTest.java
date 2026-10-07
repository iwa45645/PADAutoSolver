package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraHpEvidenceTest {
    @Test public void newHelperMaximumNeedsTwoIndependentFrames() {
        UraHpEvidence e=new UraHpEvidence();e.observe(new int[]{244660,612278},100,1);assertEquals(0,e.maximum(100));
        e.observe(new int[]{489571,612278},101,2);assertEquals(612278,e.maximum(101));
        assertTrue(e.matches(new int[]{489571,612278},101));assertFalse(e.matches(new int[]{489571,611045},101));
    }
    @Test public void sameFrameAndSameTimeCannotConfirmAndEvidenceExpires() {
        UraHpEvidence e=new UraHpEvidence();e.observe(new int[]{244660,612278},100,1);
        e.observe(new int[]{244660,612278},101,1);assertEquals(0,e.maximum(101));
        e.observe(new int[]{244660,612278},100,2);assertEquals(0,e.maximum(100));
        e.observe(new int[]{244660,612278},102,3);assertEquals(612278,e.maximum(102));
        assertEquals(0,e.maximum(15103));assertEquals(0,e.maximum(99));
    }
    @Test public void nullConflictAndAwakeningLossInvalidatePriorMaximum() {
        UraHpEvidence e=new UraHpEvidence();e.observe(new int[]{611045,611045},100,1);e.observe(new int[]{611045,611045},101,2);
        e.observe(new int[]{244416,244416},102,3);assertEquals(0,e.maximum(102));
        e.observe(new int[]{244416,244416},103,4);assertEquals(244416,e.maximum(103));
        e.observe(new int[]{244416,612278},104,5);assertEquals(0,e.maximum(104));
        e.observe(null,105,6);assertEquals(0,e.maximum(105));
        e.observe(new int[]{244416,612278},106,7);assertEquals(0,e.maximum(106));
        e.observe(new int[]{700000,612278},107,8);assertEquals(0,e.maximum(107));
    }
    @Test public void barScalingDoesNotOverflowOrInventAnUnknownMaximum() {
        int[] scan=new int[3270];java.util.Arrays.fill(scan,0xffff40aa);
        assertEquals(0,UraB3Policy.hpFillLowerBound(scan,0));
        assertTrue(UraB3Policy.hpFillLowerBound(scan,612278)<=612278);
        assertTrue(UraB3Policy.hpFillLowerBound(scan,99999999)>99000000);
        assertTrue(UraB3Policy.recoveryVerified(new int[]{489571,612278},612278,4,5));
        assertFalse(UraB3Policy.recoveryVerified(new int[]{489571,612278},611045,4,5));
    }
}
