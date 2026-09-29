package com.example.padautosolver;
import java.util.*;
/** Explicit turn simulation. Unknown cooldowns or transformation metadata are rejected. */
public final class SkillScheduleSimulator {
 public static final class Skill {public final String instance;public final int base,assist;public Skill(String id,int base,int assist){if(base<1||assist<0)throw new IllegalArgumentException("unknown cooldown");this.instance=id;this.base=base;this.assist=assist;}}
 public static final class Event {public final int elapsed,slot,delay,haste,transformCooldown;public Event(int elapsed,int slot,int delay,int haste,int transformCooldown){this.elapsed=elapsed;this.slot=slot;this.delay=delay;this.haste=haste;this.transformCooldown=transformCooldown;}}
 public static List<String> validate(List<Skill> skills,int boosts,List<Event> schedule){List<String> errors=new ArrayList<>();Set<String> ids=new HashSet<>();int[] charged=new int[skills.size()],cooldown=new int[skills.size()];for(int i=0;i<skills.size();i++){if(!ids.add(skills.get(i).instance))errors.add("duplicate instance");charged[i]=boosts;cooldown[i]=skills.get(i).base;}
  for(int n=0;n<schedule.size();n++){Event e=schedule.get(n);if(e.elapsed<0||e.delay<0||e.haste<0||e.slot<0||e.slot>=skills.size()){errors.add("invalid event "+n);continue;}for(int i=0;i<charged.length;i++)charged[i]=Math.max(0,charged[i]+e.elapsed-e.delay);int slot=e.slot;Skill skill=skills.get(slot);if(charged[slot]<cooldown[slot])errors.add("not ready "+n);if(skill.assist>0&&charged[slot]>=cooldown[slot]+skill.assist)errors.add("assist overrides base "+n);charged[slot]=0;if(e.transformCooldown>0)cooldown[slot]=e.transformCooldown;for(int i=0;i<charged.length;i++)if(i!=slot)charged[i]+=e.haste;}
  return errors;
 }
}
