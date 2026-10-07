package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraPolluxPolicyTest {
 @Test public void branchNeedsLiteralEntryAndKnownEnemyWithNoRoulette(){
  assertTrue(UraProgressPolicy.polluxEntry(17,0,25,25,0,true));
  assertFalse(UraProgressPolicy.polluxEntry(17,0,25,25,0,false));
  assertFalse(UraProgressPolicy.polluxEntry(17,0,25,25,UraDualRoulettePlan.MASK,true));
  assertFalse(UraProgressPolicy.polluxEntry(17,1,25,25,0,true));
  assertFalse(UraProgressPolicy.polluxEntry(17,0,26,25,0,true));
  assertFalse(UraProgressPolicy.polluxEntry(18,0,25,25,0,true));
  for(int f=17;f<=21;f++)assertEquals(0,UraProgressPolicy.expectedMask(f,25,25,true));
  assertEquals(UraDualRoulettePlan.MASK,UraProgressPolicy.expectedMask(17,25,25,false));
  assertEquals((1L<<21)|(1L<<26),UraProgressPolicy.expectedMask(20,25,25,false));
  assertEquals(0,UraProgressPolicy.expectedMask(22,25,25,true));
 }
 @Test public void absentInheritedSpinnersCannotHideUnknownOrHazardousCells(){
  byte[] b=new byte[30];
  assertTrue(UraProgressPolicy.safeB19FirstCharge(b,0,350000,0,25,25,1,true,true));
  assertFalse(UraProgressPolicy.safeB19FirstCharge(b,0,350000,0,25,25,1,true,false));
  assertFalse(UraProgressPolicy.safeB19FirstCharge(b,UraDualRoulettePlan.MASK,350000,0,25,25,1,true,true));
  b[12]=-1;assertFalse(UraProgressPolicy.safeB19FirstCharge(b,0,350000,0,25,25,1,true,true));
  b[12]=8;assertFalse(UraProgressPolicy.safeB19FirstCharge(b,0,350000,0,25,25,1,true,true));
  byte[] expanded=new byte[42];assertTrue(UraProgressPolicy.safeB20Charge(expanded,0,350000,1,25,25,1,true));
  assertFalse(UraProgressPolicy.safeB20Charge(expanded,0,349999,1,25,25,1,true));
  assertFalse(UraProgressPolicy.safeB20Charge(expanded,0,350000,1,25,25,1,false));
  expanded[21]=-1;assertFalse(UraProgressPolicy.safeB20Charge(expanded,0,350000,1,25,25,1,true));
  assertTrue(UraProgressPolicy.safeB21Charge(new byte[30],0,350000,0,25,25,1,true,true));
  assertFalse(UraProgressPolicy.safeB21Charge(new byte[30],0,350000,0,25,25,1,false,true));
 }
 @Test public void sevenFirstWaveMatchesCannotBeReplacedByCascadesOrAssumedLeaderCombos(){
  var s=new PuzzleSolver.MatchStats();s.firstTShapes[3]=1;s.firstColorCombos[3]=2;s.firstColorCombos[5]=1;
  s.combos=12;s.colorCombos[3]=5;s.colorCombos[5]=3;
  var goal=PuzzleGoal.esperMionSevenFirstAndHeal();assertFalse(goal.satisfied(s,30));
  s.firstColorCombos[0]=2;s.firstColorCombos[4]=2;assertTrue(goal.satisfied(s,30));
  s.firstTShapes[3]=0;assertFalse(goal.satisfied(s,30));
 }
}
