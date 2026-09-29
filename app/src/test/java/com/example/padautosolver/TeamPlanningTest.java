package com.example.padautosolver;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class TeamPlanningTest {
 @Test public void delayAndAssistInvalidateSchedule(){List<SkillScheduleSimulator.Skill>s=Arrays.asList(new SkillScheduleSimulator.Skill("a",5,3));assertFalse(SkillScheduleSimulator.validate(s,5,Arrays.asList(new SkillScheduleSimulator.Event(0,0,1,0,0))).isEmpty());assertFalse(SkillScheduleSimulator.validate(s,8,Arrays.asList(new SkillScheduleSimulator.Event(0,0,0,0,0))).isEmpty());assertTrue(SkillScheduleSimulator.validate(s,5,Arrays.asList(new SkillScheduleSimulator.Event(0,0,0,0,0))).isEmpty());}
 @Test public void unverifiedDuplicateMembersRejected(){List<TeamOptimizer.Member> t=new ArrayList<>();for(int i=0;i<6;i++)t.add(new TeamOptimizer.Member("same",true,true,false,false,new HashSet<>()));List<String> errors=TeamOptimizer.validate(t,Set.of("absorb"));assertTrue(errors.contains("duplicate instance"));assertTrue(errors.contains("unverified member"));assertTrue(errors.contains("helper unverified"));assertTrue(errors.contains("missing absorb"));}
}
