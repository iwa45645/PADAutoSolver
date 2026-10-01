package com.example.padautosolver;
import org.junit.Test;
import java.io.DataInputStream;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class TeamIconMatchTest {
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
