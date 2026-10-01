package com.example.padautosolver;
import java.util.*;
/** Explicit turn simulation. Unknown cooldowns or transformation metadata are rejected. */
public final class SkillScheduleSimulator {
 public enum Layer { BASE, ASSIST }
 /** Stateful, per-member delay/charge model. Assist cooldown is additional to the base. */
 public static final class Timeline {
  private final int[] base, assist, charged, resistance;
  public Timeline(int boosts,int[] base,int[] assist,int[] resistance){
   if(boosts<0||base==null||assist==null||resistance==null||base.length!=assist.length||base.length!=resistance.length)throw new IllegalArgumentException("unknown timeline");
   this.base=base.clone();this.assist=assist.clone();this.resistance=resistance.clone();charged=new int[base.length];
   for(int i=0;i<base.length;i++){if(base[i]<1||assist[i]<0||resistance[i]<0)throw new IllegalArgumentException("unknown member "+i);charged[i]=Math.min(boosts,cap(i));}
  }
  private int cap(int i){return base[i]+assist[i];}
  public int charge(int i){return charged[i];}
  public int remaining(int i,Layer layer){if(layer==Layer.ASSIST&&assist[i]==0)throw new IllegalArgumentException("no assist");return Math.max(0,(layer==Layer.BASE?base[i]:cap(i))-charged[i]);}
  public Layer activeLayer(int i){return assist[i]>0&&charged[i]>=cap(i)?Layer.ASSIST:Layer.BASE;}
  public boolean ready(int i,Layer layer){return remaining(i,layer)==0&&activeLayer(i)==layer;}
  public void delay(int turns){if(turns<0)throw new IllegalArgumentException("delay");for(int i=0;i<base.length;i++)charged[i]=Math.max(0,charged[i]-Math.max(0,turns-resistance[i]));}
  public void advance(int turns){if(turns<0)throw new IllegalArgumentException("turns");for(int i=0;i<base.length;i++)charged[i]=Math.min(cap(i),charged[i]+turns);}
  public void use(int slot,Layer layer,int nextBaseCooldown,int haste){
   if(haste<0||nextBaseCooldown<0)throw new IllegalArgumentException("unknown effect");
   if(!ready(slot,layer))throw new IllegalStateException(activeLayer(slot)!=layer?"ASSIST_OVERCHARGE":"SKILL_NOT_READY");
   charged[slot]=0;
   if(nextBaseCooldown>0)base[slot]=nextBaseCooldown;
   for(int i=0;i<base.length;i++)if(i!=slot)charged[i]=Math.min(cap(i),charged[i]+haste);
  }
 }
 public static final class Skill {public final String instance;public final int base,assist;public Skill(String id,int base,int assist){if(base<1||assist<0)throw new IllegalArgumentException("unknown cooldown");this.instance=id;this.base=base;this.assist=assist;}}
 public static final class Event {public final int elapsed,slot,delay,haste,transformCooldown;public Event(int elapsed,int slot,int delay,int haste,int transformCooldown){this.elapsed=elapsed;this.slot=slot;this.delay=delay;this.haste=haste;this.transformCooldown=transformCooldown;}}
 public static List<String> validate(List<Skill> skills,int boosts,List<Event> schedule){List<String> errors=new ArrayList<>();Set<String> ids=new HashSet<>();int[] charged=new int[skills.size()],cooldown=new int[skills.size()];for(int i=0;i<skills.size();i++){if(!ids.add(skills.get(i).instance))errors.add("duplicate instance");charged[i]=boosts;cooldown[i]=skills.get(i).base;}
  for(int n=0;n<schedule.size();n++){Event e=schedule.get(n);if(e.elapsed<0||e.delay<0||e.haste<0||e.slot<0||e.slot>=skills.size()){errors.add("invalid event "+n);continue;}for(int i=0;i<charged.length;i++)charged[i]=Math.max(0,charged[i]+e.elapsed-e.delay);int slot=e.slot;Skill skill=skills.get(slot);if(charged[slot]<cooldown[slot])errors.add("not ready "+n);if(skill.assist>0&&charged[slot]>=cooldown[slot]+skill.assist)errors.add("assist overrides base "+n);charged[slot]=0;if(e.transformCooldown>0)cooldown[slot]=e.transformCooldown;for(int i=0;i<charged.length;i++)if(i!=slot)charged[i]+=e.haste;}
  return errors;
 }
}
