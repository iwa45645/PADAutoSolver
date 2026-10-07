package com.example.padautosolver;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
public class UraB3FactTest {
    private static final byte[] ENTRY={2,4,4,4,4,1,2,4,4,4,4,1,1,4,4,4,4,1,2,4,4,4,4,2,2,4,4,4,4,2};
    @Test public void actualLockedSideColumnsCannotUseLeonisAllDarkGuard() {
        assertTrue(UraB3Policy.factRefreshBoard(ENTRY));assertFalse(UraB3Policy.allDark(ENTRY));
        PuzzleGoal goal=PuzzleGoal.clearColor(4,20);
        UraPuzzlePlan plan=new UraPuzzlePlan(ENTRY,new PuzzleSolver.Result(List.of(0,1,0),1,30,2,0,0,true),goal,100);
        assertEquals(20,plan.stats.colorMatched[4]);assertEquals(0,plan.stats.colorCombos[3]);assertTrue(plan.current(ENTRY,101));
        for(byte color:new byte[]{-1,0,3,5,7,9}){byte[] b=ENTRY.clone();b[0]=color;assertFalse(UraB3Policy.factRefreshBoard(b));}
    }
    @Test public void minimumAttackRequiresTWater2HealBeforeLeaderCombosAndFollowUp() {
        byte[] board={3,3,3,2,2,2,0,3,0,1,1,1,0,3,0,4,4,4,0,0,0,3,3,3,5,5,5,2,4,1};
        PuzzleSolver.MatchStats s=PuzzleSolver.analyze(board,6,5);assertTrue(PuzzleGoal.esperMionAndHeal().satisfied(s,30));
        // At least 3 literal first-wave combos plus the independently reviewed +5/+3 leaders exceeds 6 absorb.
        assertTrue(s.firstColorCombos[3]+s.firstColorCombos[5]+8>6);
        board[7]=2;assertFalse(PuzzleGoal.esperMionAndHeal().satisfied(PuzzleSolver.analyze(board,6,5),30));
    }
    @Test public void reviewedFactCropRejectsLeonisLuciferAndBlank()throws Exception {
        StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(new File("src/main/assets/ura-shura/b3-fact-face.png").toPath())))hash.append(String.format("%02x",b&255));
        try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/b3-fact.argb.gz")))) {
            assertEquals(hash.toString(),in.readUTF());int[][] a=new int[4][48*32];
            for(int[] pixels:a)for(int i=0;i<pixels.length;i++)pixels[i]=in.readInt();
            assertTrue(TeamIconMatch.distance(a[0],a[1])<.055);
            assertTrue(TeamIconMatch.distance(a[0],a[2])>.055);assertTrue(TeamIconMatch.distance(a[0],a[3])>.055);
            assertTrue(TeamIconMatch.distance(a[0],new int[48*32])>.055);
        }
    }
    @Test public void actualNewMaximumScalesActualPinkBarConservatively()throws Exception {
        int[] scan=new int[3270];try(var in=new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("/b3-hp-244660-612278.rgb.gz")))){for(int i=0;i<scan.length;i++)scan[i]=in.readInt();}
        int lower=UraB3Policy.hpFillLowerBound(scan,612278);assertTrue(lower>=230000);assertTrue(lower<=244660);
        assertEquals(0,UraB3Policy.hpFillLowerBound(scan,0));
    }
}
