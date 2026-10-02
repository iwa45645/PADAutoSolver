package com.example.padautosolver;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class LuciferInstructionTest {
    private List<StagePolicy.Item> line(String text){return List.of(new StagePolicy.Item(text,610,680));}
    @Test public void literalObservedInstructionsAuthorizeDifferentGoals(){
        assertEquals(LuciferInstruction.POISON,LuciferInstruction.read(line("全ての毒を消してみるがいい")));
        assertEquals(LuciferInstruction.ZERO,LuciferInstruction.read(line("次はコンボ禁止だ")));
        assertEquals(LuciferInstruction.ALL,LuciferInstruction.read(line("全てのドロップを消してみるがいい")));
        assertEquals(LuciferInstruction.ALL,LuciferInstruction.read(line("水、光を全て消すがいい")));
        assertNull(LuciferInstruction.read(line("全Tの毒を消しTみるがいい")));
        assertNull(LuciferInstruction.read(line("試練を乗り越えろ")));
        assertNull(LuciferInstruction.read(List.of(new StagePolicy.Item("次はコンボ禁止だ",610,300))));
        assertNull(LuciferInstruction.read(List.of(new StagePolicy.Item("次はコンボ禁止だ",610,680),new StagePolicy.Item("全ての毒を消してみるがいい",610,680))));
    }
    @Test public void poisonCountIncludesEverySourcePoisonAndPlanCannotLowerIt(){
        byte[] b={2,3,5,6,2,2,0,3,7,4,6,6,1,7,2,0,7,6,2,5,5,2,1,5,7,3,3,4,3,2};
        PuzzleGoal goal=LuciferInstruction.POISON.goal(b);assertEquals(4,goal.exactCombos);
        var partial=new PuzzleSolver.MatchStats();partial.colorMatched[7]=3;partial.combos=10;
        assertFalse(goal.satisfied(partial,30));partial.colorMatched[7]=4;assertTrue(goal.satisfied(partial,30));
        var result=PuzzleSolver.solve(b,6,5,40,2000,0,goal);assertTrue(result.goalSatisfied);
        var plan=new UraPuzzlePlan(b,result,goal,100);assertEquals(4,plan.stats.colorMatched[7]);
        assertThrows(IllegalArgumentException.class,()->new UraPuzzlePlan(b,result,PuzzleGoal.clearColor(7,3),100));
    }
    @Test public void zeroComboRoundTripConsumesMovementButLeavesBoardUnchanged(){
        byte[] b={0,2,3,4,0,2,2,3,4,0,2,3,3,4,0,2,3,4,4,0,2,3,4,0,0,2,3,4,0,2};
        var result=new PuzzleSolver.Result(List.of(0,1,0),0,0,2,0,0,true);
        var plan=new UraPuzzlePlan(b,result,LuciferInstruction.ZERO.goal(b),1);
        assertEquals(0,plan.stats.combos);assertArrayEquals(b,plan.previewBoard());
        b[1]=0;b[2]=0;
        assertThrows(IllegalArgumentException.class,()->new UraPuzzlePlan(b,result,LuciferInstruction.ZERO.goal(b),1));
    }
    @Test public void specialOrbsAreCountedSeparatelyAndAllClearRequiresEveryCell(){
        byte[] b={7,7,7,7,0,2,1,2,3,4,5,6,2,3,4,5,6,1,3,4,5,6,1,2,4,5,6,1,2,3};
        var s=PuzzleSolver.analyze(b,6,5);assertEquals(4,s.colorMatched[7]);assertEquals(1,s.colorCombos[7]);
        assertTrue(LuciferInstruction.POISON.goal(b).satisfied(s,30));assertFalse(LuciferInstruction.ALL.goal(b).satisfied(s,30));
    }
    @Test public void observedTwoColorBoardCanBeFullyClearedWithoutAssumingSkyfall(){
        byte[] b={1,1,1,3,3,1,1,3,1,1,1,3,3,1,3,3,3,1,3,3,3,3,1,1,1,1,1,3,3,3};
        var goal=LuciferInstruction.ALL.goal(b);
        var result=PuzzleSolver.solve(b,6,5,48,3000,0,goal);
        assertTrue(result.goalSatisfied);
        var plan=new UraPuzzlePlan(b,result,goal,100);
        assertEquals(30,plan.stats.matched);
        assertEquals(15,plan.stats.colorMatched[1]);assertEquals(15,plan.stats.colorMatched[3]);
    }
    @Test public void newTrialDoesNotAssumeFifteenOfEachColor() {
        byte[] b={1,3,3,3,1,1,3,3,3,3,1,3,3,1,3,3,3,1,1,3,3,1,3,3,3,3,3,1,1,1};
        var goal=LuciferInstruction.ALL.goal(b);
        var result=PuzzleSolver.solve(b,6,5,48,3000,0,goal);
        assertTrue(result.goalSatisfied);
        var plan=new UraPuzzlePlan(b,result,goal,100);
        assertEquals(30,plan.stats.matched);
        assertEquals(11,plan.stats.colorMatched[1]);assertEquals(19,plan.stats.colorMatched[3]);
    }
}
