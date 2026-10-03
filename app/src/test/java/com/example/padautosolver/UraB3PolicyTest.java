package com.example.padautosolver;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraB3PolicyTest {
    @Test public void survivalNeedsActualRecoveryAndBothPostCooldowns(){
        assertFalse(UraB3Policy.recoveryVerified(new int[]{2445,611045},4,5));
        assertTrue(UraB3Policy.recoveryVerified(new int[]{246863,611045},4,5));
        assertFalse(UraB3Policy.recoveryVerified(new int[]{246863,611045},null,5));
        assertFalse(UraB3Policy.recoveryVerified(new int[]{246863,611045},4,0));
        assertFalse(UraB3Policy.recoveryVerified(new int[]{246863,611046},4,5));
    }
    @Test public void unknownOrMixedBoardCannotAuthorizeDarkRefresh(){
        byte[] b=new byte[30];Arrays.fill(b,(byte)4);assertTrue(UraB3Policy.allDark(b));
        b[29]=-1;assertFalse(UraB3Policy.allDark(b));b[29]=3;assertFalse(UraB3Policy.allDark(b));
        assertFalse(UraB3Policy.allDark(null));assertFalse(UraB3Policy.allDark(new byte[29]));
    }
    @Test public void floorTextIsNotHpAndConflictingReadsAreRejected(){
        assertNull(UraB3Policy.hp(List.of(new StagePolicy.Item("3/22",0,0))));
        assertArrayEquals(new int[]{2445,611045},UraB3Policy.hp(List.of(new StagePolicy.Item("2,445/611,045",0,0))));
        assertNull(UraB3Policy.hp(List.of(new StagePolicy.Item("2445/611045",0,0),new StagePolicy.Item("2446/611045",0,0))));
    }
}
