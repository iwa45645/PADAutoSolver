package com.example.padautosolver;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class UraPuzzlePlanTest {
 private byte[] board(){return new byte[]{3,3,3,0,0,0,1,2,4,5,1,2,3,3,3,4,4,4,5,5,5,0,1,2,1,2,4,0,1,2};}
 private PuzzleSolver.Result result(List<Integer> path){return new PuzzleSolver.Result(path,0,0,0,0,0,true);}
 @Test public void staleOrChangedBoardCannotUsePlanAndSourceCannotMutateIt(){
  byte[] source=board(),same=source.clone();List<Integer> path=new ArrayList<>(List.of(0));
  var plan=new UraPuzzlePlan(source,result(path),PuzzleGoal.waterAndHeal(),1000);
  source[0]=0;path.clear();assertTrue(plan.current(same,1500));assertEquals(1,plan.path.size());
  assertFalse(plan.current(source,1500));assertFalse(plan.current(same,999));assertFalse(plan.current(same,16001));
 }
 @Test(expected=IllegalArgumentException.class) public void claimedGoalCannotOverrideReplay(){
  new UraPuzzlePlan(new byte[30],result(List.of(0)),PuzzleGoal.water(),0);
 }
 @Test public void recognitionCrossingExpiryDoesNotMakeAnUnchangedBoardCurrent(){
  byte[] same=board();var plan=new UraPuzzlePlan(same,result(List.of(0)),PuzzleGoal.waterAndHeal(),1000);
  assertTrue(plan.current(same,15999));assertFalse(plan.current(same,16001));
  byte[] changed=same.clone();changed[0]=0;assertFalse(plan.current(changed,15999));
 }
 @Test(expected=IllegalArgumentException.class) public void wrappingBetweenRowsIsNotAdjacent(){
  new UraPuzzlePlan(board(),result(List.of(5,6)),PuzzleGoal.water(),0);
 }
 @Test(expected=IllegalArgumentException.class) public void unknownOrbCannotEnterPlan(){
  byte[] source=board();source[29]=-1;new UraPuzzlePlan(source,result(List.of(0)),PuzzleGoal.water(),0);
 }
}
