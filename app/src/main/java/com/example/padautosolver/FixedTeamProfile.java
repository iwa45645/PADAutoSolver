package com.example.padautosolver;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** User-selected composition is immutable; runtime observations never replace it. */
public final class FixedTeamProfile {
    public final String id;
    public final Slot[] slots = new Slot[6];
    public final int expectedSkillBoost, expectedSealResist;
    public static final class Slot {
        public final int monsterNo, assistNo, level, plus, cooldown;
        public final Integer delayResistance;
        public final String name, superAwakening;
        Slot(JSONObject o) throws org.json.JSONException {
            monsterNo=o.getInt("monsterNo"); assistNo=o.isNull("assistNo")?0:o.getInt("assistNo");
            level=o.isNull("level")?0:o.getInt("level");plus=o.isNull("plus")?0:o.getInt("plus");
            cooldown=o.isNull("skillCooldown")?0:o.getInt("skillCooldown");name=o.getString("name");
            delayResistance=o.isNull("delayResistTurns")?null:o.getInt("delayResistTurns");
            superAwakening=o.isNull("superAwakening")?null:o.getString("superAwakening");
        }
    }
    public FixedTeamProfile(JSONObject json) throws org.json.JSONException {
        if(!json.getBoolean("userConfirmedComposition")||!json.getBoolean("lockedComposition"))throw new IllegalArgumentException("User selection required");
        id=json.getString("profileId");expectedSkillBoost=json.getInt("expectedSkillBoost");expectedSealResist=json.getInt("expectedSealResist");
        JSONArray values=json.getJSONArray("slots");if(values.length()!=6)throw new IllegalArgumentException("Six slots required");
        for(int i=0;i<6;i++){JSONObject o=values.getJSONObject(i);int pos=o.getInt("position");if(pos<0||pos>=6||slots[pos]!=null)throw new IllegalArgumentException("Duplicate slot");slots[pos]=new Slot(o);}
    }
    public static final class ObservedMember {
        public int monsterNo, assistNo, level, plus, cooldown;
        public Integer delayResistance;
        public String superAwakening;
        public boolean identityVerified, assistVerified, latentsVerified;
    }
    public static final class Snapshot {
        public String profileId, dungeonId;
        public long capturedAt, generation;
        public int width, height, skillBoost, sealResist;
        public double sceneConfidence;
        public boolean gameForeground, systemDialog, calibrationMatches, helperIncluded, superAwakeningsIncluded;
        public ObservedMember[] members;
    }
    public List<String> validate(Snapshot s,long now,long generation) {
        List<String> errors=new ArrayList<>();
        if(s==null){errors.add("TEAM_CAPTURE_REQUIRED");return errors;}
        if(!id.equals(s.profileId)||!"ura_shura".equals(s.dungeonId))errors.add("TeamProfileMismatch");
        if(s.generation!=generation)errors.add("CAPTURE_GENERATION_MISMATCH");
        if(now<s.capturedAt||now-s.capturedAt>1500)errors.add("STALE_SCREENSHOT");
        if(!s.gameForeground||s.systemDialog)errors.add("FOREGROUND_OR_DIALOG");
        if(!Double.isFinite(s.sceneConfidence)||s.sceneConfidence<.95)errors.add("SCENE_UNKNOWN");
        if(s.width<=0||s.height<=0||!s.calibrationMatches)errors.add("CALIBRATION_MISMATCH");
        if(!s.helperIncluded||!s.superAwakeningsIncluded)errors.add("TEAM_INFO_OPTIONS_REQUIRED");
        if(s.skillBoost<expectedSkillBoost||s.sealResist<expectedSealResist)errors.add("TEAM_AWAKENINGS_MISMATCH");
        if(s.members==null||s.members.length!=6){errors.add("TEAM_SLOTS_UNKNOWN");return errors;}
        for(int i=0;i<6;i++){
            ObservedMember m=s.members[i];Slot expected=slots[i];
            if(m==null||!m.identityVerified||!m.assistVerified||!m.latentsVerified){errors.add("MEMBER_CAPTURE_REQUIRED:"+i);continue;}
            if(m.monsterNo!=expected.monsterNo)errors.add(i==5?"HELPER_MISMATCH":"TeamProfileMismatch:"+i);
            // The friend's assist/latents are observed every run, never pinned to today's values.
            if(i<5){
                if(m.assistNo!=expected.assistNo||m.level!=expected.level||m.plus!=expected.plus||m.cooldown!=expected.cooldown)errors.add("MEMBER_STATE_MISMATCH:"+i);
                if(expected.delayResistance!=null&&!expected.delayResistance.equals(m.delayResistance))errors.add("LATENTS_MISMATCH:"+i);
                if(expected.superAwakening!=null&&!expected.superAwakening.equals(m.superAwakening))errors.add("SUPER_AWAKENING_MISMATCH:"+i);
            }else if(m.cooldown<1||m.level<1||m.delayResistance==null)errors.add("HELPER_RUNTIME_STATE_UNKNOWN");
        }
        return errors;
    }
}
