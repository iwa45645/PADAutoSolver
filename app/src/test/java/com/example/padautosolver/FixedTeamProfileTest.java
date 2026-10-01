package com.example.padautosolver;

import org.json.JSONObject;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class FixedTeamProfileTest {
    static FixedTeamProfile profile() throws Exception {
        try (var in=FixedTeamProfileTest.class.getResourceAsStream("/fixed-team-profile.json")) {
            return new FixedTeamProfile(new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8)));
        }
    }
    static FixedTeamProfile.Snapshot snapshot(FixedTeamProfile p) {
        var s=new FixedTeamProfile.Snapshot();s.profileId=p.id;s.dungeonId="ura_shura";
        s.capturedAt=1000;s.generation=8;s.width=1220;s.height=2712;s.sceneConfidence=.99;
        s.gameForeground=s.calibrationMatches=s.helperIncluded=s.superAwakeningsIncluded=true;
        s.skillBoost=17;s.sealResist=6;s.members=new FixedTeamProfile.ObservedMember[6];
        for(int i=0;i<6;i++) {
            var e=p.slots[i];var m=new FixedTeamProfile.ObservedMember();s.members[i]=m;
            m.monsterNo=e.monsterNo;m.assistNo=e.assistNo;m.level=e.level;m.plus=e.plus;m.cooldown=e.cooldown;
            m.delayResistance=e.delayResistance;m.superAwakening=e.superAwakening;
            m.identityVerified=m.assistVerified=m.latentsVerified=true;
        }
        var helper=s.members[5];helper.assistNo=14067;helper.level=99;helper.plus=300;
        helper.cooldown=22;helper.delayResistance=4;
        return s;
    }
    @Test public void fixedUserCompositionAndRuntimeHelper() throws Exception {
        var p=profile();var s=snapshot(p);assertTrue(p.validate(s,1200,8).isEmpty());
        s.members[5].assistNo=8110;s.members[5].delayResistance=5;
        assertTrue(p.validate(s,1200,8).isEmpty()); // Friend's state is not pinned.
        s.members[5].monsterNo=9412;assertTrue(p.validate(s,1200,8).contains("HELPER_MISMATCH"));
    }
    @Test public void rejectsWrongAssistUnknownLatentsAndSwappedTeam() throws Exception {
        var p=profile();var s=snapshot(p);s.members[4].assistNo=8766;
        assertTrue(p.validate(s,1200,8).contains("MEMBER_STATE_MISMATCH:4"));
        s=snapshot(p);s.members[3].latentsVerified=false;
        assertTrue(p.validate(s,1200,8).contains("MEMBER_CAPTURE_REQUIRED:3"));
        s=snapshot(p);s.members[1]=s.members[2];assertTrue(p.validate(s,1200,8).contains("TeamProfileMismatch:1"));
    }
    @Test public void captureAndAwakeningGuards() throws Exception {
        var p=profile();var s=snapshot(p);
        assertTrue(p.validate(s,2501,8).contains("STALE_SCREENSHOT"));
        assertTrue(p.validate(s,1200,9).contains("CAPTURE_GENERATION_MISMATCH"));
        s.sceneConfidence=Double.NaN;assertTrue(p.validate(s,1200,8).contains("SCENE_UNKNOWN"));
        s=snapshot(p);s.skillBoost=16;s.helperIncluded=false;
        assertTrue(p.validate(s,1200,8).contains("TEAM_AWAKENINGS_MISMATCH"));
        assertTrue(p.validate(s,1200,8).contains("TEAM_INFO_OPTIONS_REQUIRED"));
    }
    @Test public void measuredSeventeenFixtureIsNotAnEntryPermit() throws Exception {
        try(var in=getClass().getResourceAsStream("/team-info-17.json")) {
            var j=new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));
            var o=j.getJSONObject("visuallyObserved");
            assertEquals(17,o.getInt("normalSkillBoostCount")+2*o.getInt("plusSkillBoostCount"));
            assertTrue(j.getJSONArray("limitations").length()>0);
        }
        assertTrue(profile().validate(null,1200,8).contains("TEAM_CAPTURE_REQUIRED"));
    }
}
