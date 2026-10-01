package com.example.padautosolver;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraCooldownPostconditionTest {
 @Test public void unassistedOdinConsumptionAcceptsNullWithoutUnboxing(){
  assertTrue(UraCooldownPostcondition.matches(5,5,null,0));
  assertFalse(UraCooldownPostcondition.matches(5,0,null,0));
  assertFalse(UraCooldownPostcondition.matches(5,-1,null,0));
 }
 @Test public void readinessDoesNotProveTransformation(){
  assertFalse(UraCooldownPostcondition.matches(3,0,19,0));
  assertTrue(UraCooldownPostcondition.matches(3,3,21,0));
  assertFalse(UraCooldownPostcondition.matches(3,3,19,0));
 }
 @Test public void friendOnlyDoesNotRequireASpecificAssistButNeedsConsumedBase(){
  assertTrue(UraCooldownPostcondition.matches(4,2,null,0));
  assertTrue(UraCooldownPostcondition.matches(4,2,18,0));
  assertFalse(UraCooldownPostcondition.matches(4,0,18,0));
  assertFalse(UraCooldownPostcondition.matches(4,2,0,0));
 }
}
