package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoundGateTest {
    @Test public void waitsForStableFrameAndChangedBoardAfterMove() {
        RoundGate gate = new RoundGate();
        byte[] a = {1,2,3}, b = {2,1,3}; int[] p = {0x445566};
        assertFalse(gate.ready(a,p,1000)); assertFalse(gate.ready(a,p,1400));
        assertTrue(gate.ready(a,p,1800)); gate.played(a,1800);
        assertFalse(gate.ready(a,p,5000)); assertFalse(gate.ready(a,p,6000));
        assertFalse(gate.ready(b,p,6400)); assertTrue(gate.ready(b,p,7200));
    }
    @Test public void animationResetsSettlingTime() {
        RoundGate gate = new RoundGate(); byte[] b = {1};
        assertFalse(gate.ready(b,new int[]{0},1000));
        assertFalse(gate.ready(b,new int[]{0xffffff},2000));
        assertFalse(gate.ready(b,new int[]{0xffffff},2400));
        assertTrue(gate.ready(b,new int[]{0xffffff},2800));
    }
}
