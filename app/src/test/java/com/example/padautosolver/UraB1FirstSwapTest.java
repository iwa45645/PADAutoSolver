package com.example.padautosolver;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Real Oct7 B1 source/route, and the first native result image before gravity. */
public class UraB1FirstSwapTest {
    @Test public void missingFirstSwapExactlyReproducesFailedDeviceBoardAndLosesTActivation() {
        byte[] source={5,5,3,5,3,3,3,3,3,3,5,5,5,3,5,5,5,5,3,5,3,5,3,3,5,3,5,3,5,3};
        var route=List.of(28,22,21,20,14,8,2,3,9,15,21,22,23,17,16,15,9,8,7,13,12,6,7,8);
        byte[] observed={5,5,5,3,3,3,3,3,3,3,5,5,3,5,3,5,3,5,3,5,5,5,3,5,5,3,5,3,5,3};
        byte[] full=replay(source,route,1),missed=replay(source,route,2);
        assertArrayEquals(observed,missed);assertFalse(Arrays.equals(full,observed));
        var good=PuzzleSolver.analyze(full,6,5);var bad=PuzzleSolver.analyze(missed,6,5);
        assertEquals(1,good.firstTShapes[3]);assertTrue(PuzzleGoal.esperMionAndHeal().satisfied(good,30));
        assertEquals(0,bad.firstTShapes[3]);assertFalse(PuzzleGoal.esperMionAndHeal().satisfied(bad,30));
    }
    private byte[] replay(byte[] source,List<Integer> route,int firstSwap) {
        byte[] b=source.clone();for(int i=firstSwap;i<route.size();i++){int a=route.get(i-1),c=route.get(i);byte temp=b[a];b[a]=b[c];b[c]=temp;}return b;
    }
}
