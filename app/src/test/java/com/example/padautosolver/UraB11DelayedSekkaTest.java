package com.example.padautosolver;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
public class UraB11DelayedSekkaTest {
    @Test public void onlyUnconsumedEntryReadinessCanReorderChargeBeforeSekka() {
        assertTrue(UraProgressPolicy.b11ChargeFirst(11,0,14,10,10,0,1));
        assertFalse(UraProgressPolicy.b11ChargeFirst(11,1,14,10,10,0,1));
        assertFalse(UraProgressPolicy.b11ChargeFirst(11,0,6,10,10,0,1));
        assertFalse(UraProgressPolicy.b11ChargeFirst(11,0,14,11,10,0,1));
        assertFalse(UraProgressPolicy.b11ChargeFirst(11,0,14,10,10,0,0));
        assertFalse(UraProgressPolicy.b11ChargeFirst(11,0,14,10,10,0,2));
        assertFalse(UraProgressPolicy.b11ChargeFirst(10,0,14,10,10,0,1));
        assertArrayEquals(new int[]{100,0,1,2,101},UraProgressPolicy.script(11,true));
        assertArrayEquals(UraProgressPolicy.script(11),UraProgressPolicy.script(11,false));
        assertArrayEquals(UraProgressPolicy.script(12),UraProgressPolicy.script(12,true));
    }
    @Test public void entryChargeCannotAssumeShieldOrRepeatAfterOneVerifiedTurn() {
        byte[] board={2,0,0,3,2,1,2,5,5,4,4,3,5,2,4,0,2,0,3,0,1,1,2,0,4,5,0,5,3,5};
        assertTrue(UraProgressPolicy.safeB11FirstCharge(board,350000,11,0,10,10,10,1));
        assertFalse(UraProgressPolicy.safeB11FirstCharge(board,349999,11,0,10,10,10,1));
        assertFalse(UraProgressPolicy.safeB11FirstCharge(board,612892,11,1,10,10,10,1));
        assertFalse(UraProgressPolicy.safeB11FirstCharge(board,612892,11,0,11,10,10,1));
        assertFalse(UraProgressPolicy.safeB11FirstCharge(board,612892,11,0,10,10,-1,1));
        assertFalse(UraProgressPolicy.safeB11FirstCharge(board,612892,11,0,10,10,10,0));
        assertFalse(UraProgressPolicy.safeB11FirstCharge(board,612892,11,0,10,10,10,null));
        List<Integer> route=UraChargeRoute.findHealing(board);
        byte[] end=board.clone();byte held=end[route.get(0)];
        for(int i=1;i<route.size();i++){int prev=route.get(i-1),next=route.get(i);end[prev]=end[next];end[next]=held;}
        PuzzleSolver.MatchStats stats=PuzzleSolver.firstWave(end,6,5);
        assertTrue(stats.colorCombos[5]>0);
        assertTrue(stats.colorCombos[3]<2);
        assertEquals(0,stats.firstTShapes[3]);
        board[0]=8;assertFalse(UraProgressPolicy.safeB11FirstCharge(board,612892,11,0,10,10,10,1));
    }
}
