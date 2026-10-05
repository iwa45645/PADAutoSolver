package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
/** Replays captured B1/B3/B5 boards against both leaders' first-wave conditions. */
public class UraEarlyAttackReplayTest {
    private void verify(byte[] board){
        PuzzleGoal goal=PuzzleGoal.esperMionAndHeal();
        var result=PuzzleSolver.solve(board,6,5,44,2500,1800,goal);
        var plan=new UraPuzzlePlan(board,result,goal,0);
        assertTrue(plan.stats.firstTShapes[3]>=1);
        assertTrue(plan.stats.firstColorCombos[3]>=2);
        assertTrue(plan.stats.firstColorCombos[5]>=1);
    }
    @Test public void actualB1AfterSkills(){verify(new byte[]{3,5,5,3,5,3,3,5,5,5,5,3,3,3,3,3,5,5,3,3,5,3,5,3,3,5,5,3,5,5});}
    @Test public void actualB3AfterMion(){verify(new byte[]{5,3,3,3,3,2,4,3,3,3,3,5,3,5,3,5,2,5,3,3,3,2,3,4,3,4,4,3,4,3});}
    @Test public void actualB5AfterRecovery(){verify(new byte[]{5,4,4,3,3,3,3,3,4,3,5,2,5,3,3,2,3,5,2,5,3,2,3,3,3,3,2,3,2,3});}
}
