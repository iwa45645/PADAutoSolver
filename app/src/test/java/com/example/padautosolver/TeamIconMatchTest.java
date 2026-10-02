package com.example.padautosolver;
import org.junit.Test;
import java.io.DataInputStream;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class TeamIconMatchTest {
    @Test public void allocationFreeDistancePreservesRgbMetric() {
        java.util.Random random=new java.util.Random(73);
        int[] a=new int[48*48],b=new int[a.length];long expected=0;
        for(int i=0;i<a.length;i++) {
            a[i]=random.nextInt();b[i]=random.nextInt();
            for(int shift=0;shift<=16;shift+=8)
                expected+=Math.abs(((a[i]>>>shift)&255)-((b[i]>>>shift)&255));
        }
        assertEquals(expected/(a.length*3.0*255),TeamIconMatch.distance(a,b),0);
    }
    private int[][] references()throws Exception {
        int[][] all=new int[6][48*32];
        for(int i=0;i<6;i++)try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/preentry-slot-"+i+".argb.gz")))){
            for(int j=0;j<all[i].length;j++)all[i][j]=in.readInt();
        }
        return all;
    }
    @Test public void phoneFaceCropsMatchTheirOwnSlotsAndRejectEverySwap()throws Exception {
        int[][] all=references();
        for(int slot=0;slot<6;slot++)for(int observed=0;observed<6;observed++)
            assertEquals("slot "+slot+" observed "+observed,slot==observed,TeamIconMatch.matches(all[observed],all,slot));
    }
    @Test public void emptyOrWrongSizePixelsAreNeverIdentityEvidence()throws Exception {
        assertFalse(TeamIconMatch.matches(new int[48*32],references(),0));
        assertFalse(TeamIconMatch.matches(new int[1],references(),0));
        assertEquals(1,TeamIconMatch.distance(null,new int[1]),0);
    }
}
