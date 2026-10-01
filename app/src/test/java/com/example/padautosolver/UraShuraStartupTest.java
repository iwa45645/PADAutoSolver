package com.example.padautosolver;

import org.junit.Test;
import static org.junit.Assert.*;

public class UraShuraStartupTest {
    private UraShuraStartup.Frame frame(long time,long seq){
        var f=new UraShuraStartup.Frame();f.time=time;f.sequence=seq;f.generation=8;f.floor=1;f.waterCount=15;
        f.teamValidated=f.boardVerified=f.gameForeground=f.calibrationMatches=true;
        f.floorConfidence=f.enemyConfidence=.99;f.skills=new UraShuraStartup.SkillState[6];
        int[] no={14094,7333,3391,10042,2955,9411};
        for(int i=0;i<6;i++){var s=new UraShuraStartup.SkillState();f.skills[i]=s;s.monsterNo=no[i];
            s.readyKnown=s.ready=s.identityVerified=s.skillVerified=true;
            s.layer=i==4?SkillScheduleSimulator.Layer.ASSIST:SkillScheduleSimulator.Layer.BASE;}
        return f;
    }
    @Test public void offlineReplayRequiresNewPostFrameBetweenEverySkill(){
        var controller=new UraShuraStartup(8);long time=1000,seq=1;
        for(int step=0;step<6;step++){
            var before=frame(time,seq);var d=controller.inspect(before,time);
            assertEquals(UraShuraStartup.Kind.SKILL,d.kind);assertEquals(UraShuraStartup.SLOTS[step],d.slot);
            // Re-reading a Dry Run does not advance or execute the action.
            assertEquals(step,controller.step());assertEquals(d.slot,controller.inspect(before,time).slot);
            controller.markDispatched(before,d,time);
            assertEquals("NEW_POST_FRAME_REQUIRED",controller.inspect(before,time).reason);
            var anim=frame(time+200,seq+1);assertEquals("POST_OBSERVATION_PENDING",controller.inspect(anim,anim.time).reason);
            var post=frame(time+700,seq+2);post.skills[d.slot].monsterNo=UraShuraStartup.AFTER[step];
            post.postObservationComplete=post.consumedSkillVerified=post.hasteVerified=post.absorbsNullified=true;
            assertEquals("POSTCONDITION_VERIFIED",controller.inspect(post,post.time).reason);
            assertEquals(step+1,controller.step());time+=1000;seq+=3;
        }
        assertEquals(UraShuraStartup.Kind.PUZZLE,controller.inspect(frame(time,seq),time).kind);
    }
    @Test public void rukaRequiresAssistAndEsperRequiresNineWater(){
        var f=frame(1000,1);f.skills[4].layer=SkillScheduleSimulator.Layer.BASE;
        assertEquals("ACTIVE_SKILL_LAYER_MISMATCH:4",new UraShuraStartup(8).inspect(f,1000).reason);
        var c=new UraShuraStartup(8);f=frame(1000,1);c.markDispatched(f,c.inspect(f,1000),1000);
        var post=frame(1700,2);post.postObservationComplete=post.consumedSkillVerified=true;post.waterCount=8;
        assertEquals("POSTCONDITION_FAILED:0",c.inspect(post,1700).reason);
    }
    @Test public void staleUnknownB2AndStopAreNonActionable(){
        assertEquals("STALE_SCREENSHOT",new UraShuraStartup(8).inspect(frame(1000,1),2501).reason);
        var f=frame(1000,1);f.floor=2;assertEquals("B2_CAPTURE_REQUIRED",new UraShuraStartup(8).inspect(f,1000).reason);
        f=frame(1000,1);f.enemyConfidence=0;assertEquals(UraShuraStartup.Kind.STOP,new UraShuraStartup(8).inspect(f,1000).kind);
        f=frame(1000,1);f.stopRequested=true;assertEquals("STOP_REQUESTED",new UraShuraStartup(8).inspect(f,1000).reason);
    }
    @Test public void animationTimeoutLatchesAndDoesNotRetry(){
        var c=new UraShuraStartup(8);var f=frame(1000,1);c.markDispatched(f,c.inspect(f,1000),1000);
        f.time=9999;f.sequence=100; // Dispatch snapshot must not be a mutable reference.
        assertEquals("POSTCONDITION_TIMEOUT:0",c.inspect(frame(9100,2),9100).reason);
        assertEquals(UraShuraStartup.Kind.STOP,c.inspect(frame(9300,3),9300).kind);
    }
    @Test public void dispatcherRejectsStalePlanChangedReadinessOrDifferentFrame(){
        var c=new UraShuraStartup(8);var f=frame(1000,1);var d=c.inspect(f,1000);
        assertThrows(IllegalStateException.class,()->c.markDispatched(f,d,2501));
        var c2=new UraShuraStartup(8);var f2=frame(1000,1);var d2=c2.inspect(f2,1000);
        f2.skills[4].ready=false;
        assertThrows(IllegalStateException.class,()->c2.markDispatched(f2,d2,1100));
        var c3=new UraShuraStartup(8);var f3=frame(1000,1);var d3=c3.inspect(f3,1000);
        assertThrows(IllegalStateException.class,()->c3.markDispatched(frame(1200,2),d3,1200));
    }
}
