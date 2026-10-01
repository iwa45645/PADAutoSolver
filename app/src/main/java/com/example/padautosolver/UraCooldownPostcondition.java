package com.example.padautosolver;
import java.util.Objects;
/** Actual cooldowns after each opening skill; nullable unassisted cooldown is intentional. */
final class UraCooldownPostcondition {
 static boolean matches(int step,Integer base,Integer assist,int helperTotal){
  if(step<0||step>5)return false;
  int[] expectedBase={5,5,4,3,2,5};
  Integer[] expectedAssist={16,33,26,21,helperTotal==0?null:Integer.valueOf(helperTotal-20),null};
  boolean assistMatches=step==4&&helperTotal==0?assist==null||assist>2:Objects.equals(expectedAssist[step],assist);
  return Integer.valueOf(expectedBase[step]).equals(base)&&assistMatches;
 }
}
