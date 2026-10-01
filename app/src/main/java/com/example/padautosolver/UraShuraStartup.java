package com.example.padautosolver;

/** One action, then a newer frame proving its effect. No blind retries or timer-based skills. */
public final class UraShuraStartup {
    public enum Kind { SKILL, PUZZLE, WAIT, STOP }
    public static final int[] SLOTS = {4,0,1,3,5,2};
    public static final SkillScheduleSimulator.Layer[] LAYERS = {
        SkillScheduleSimulator.Layer.ASSIST,SkillScheduleSimulator.Layer.BASE,SkillScheduleSimulator.Layer.BASE,
        SkillScheduleSimulator.Layer.BASE,SkillScheduleSimulator.Layer.BASE,SkillScheduleSimulator.Layer.BASE};
    public static final int[] BEFORE = {2955,14094,7333,10042,9411,3391};
    public static final int[] AFTER = {2955,14095,7334,10043,9412,3391};
    public static final class SkillState {
        public int monsterNo;
        public boolean readyKnown, ready, identityVerified, skillVerified;
        public SkillScheduleSimulator.Layer layer;
    }
    public static final class Frame {
        public long time, generation, sequence;
        public int floor, waterCount;
        public boolean teamValidated, boardVerified, gameForeground, systemDialog, calibrationMatches,
                gestureRunning, stopRequested, consumedSkillVerified, hasteVerified, absorbsNullified,
                postObservationComplete;
        public double floorConfidence, enemyConfidence;
        public SkillState[] skills;
    }
    public static final class Decision {
        public final Kind kind;
        public final int slot;
        public final SkillScheduleSimulator.Layer layer;
        public final String reason;
        private long sourceTime=-1, sourceSequence=-1, sourceGeneration=-1;
        Decision(Kind kind,int slot,SkillScheduleSimulator.Layer layer,String reason){this.kind=kind;this.slot=slot;this.layer=layer;this.reason=reason;}
        Decision(Frame f,int slot,SkillScheduleSimulator.Layer layer,String reason){
            this(Kind.SKILL,slot,layer,reason);sourceTime=f.time;sourceSequence=f.sequence;sourceGeneration=f.generation;
        }
    }
    private final long generation;
    private int step;
    private long awaitingSequence = -1, awaitingTime;
    private String stopped;
    public UraShuraStartup(long generation){this.generation=generation;}
    public int step(){return step;}
    private Decision stop(String reason){stopped=reason;return new Decision(Kind.STOP,-1,null,reason);}
    public Decision inspect(Frame f,long now) {
        if(stopped!=null)return stop(stopped);
        if(f==null)return stop("FRAME_REQUIRED");
        if(f.stopRequested)return stop("STOP_REQUESTED");
        if(f.generation!=generation)return stop("CAPTURE_GENERATION_MISMATCH");
        if(now<f.time||now-f.time>1500)return stop("STALE_SCREENSHOT");
        if(!f.gameForeground||f.systemDialog)return stop("FOREGROUND_OR_DIALOG");
        if(!f.teamValidated)return stop("TeamProfileMismatch");
        if(!f.calibrationMatches)return stop("CALIBRATION_MISMATCH");
        if(!Double.isFinite(f.floorConfidence)||!Double.isFinite(f.enemyConfidence)||f.floorConfidence<.95||f.enemyConfidence<.95)return stop("FLOOR_OR_ENEMY_UNKNOWN");
        if(f.floor==2)return stop("B2_CAPTURE_REQUIRED");
        if(f.floor!=1)return stop("FLOOR_MISMATCH");
        if(awaitingSequence>=0&&now-awaitingTime>8000)return stop("POSTCONDITION_TIMEOUT:"+step);
        if(f.gestureRunning)return new Decision(Kind.WAIT,-1,null,"GESTURE_RUNNING");
        if(f.skills==null||f.skills.length!=6)return stop("SKILL_CAPTURE_REQUIRED");
        if(awaitingSequence>=0){
            if(f.sequence<=awaitingSequence||f.time<=awaitingTime)return new Decision(Kind.WAIT,-1,null,"NEW_POST_FRAME_REQUIRED");
            if(!f.postObservationComplete)return new Decision(Kind.WAIT,-1,null,"POST_OBSERVATION_PENDING");
            int slot=SLOTS[step];SkillState state=f.skills[slot];
            boolean success=state!=null&&state.identityVerified&&state.monsterNo==AFTER[step]&&f.consumedSkillVerified;
            if(step==0)success&=f.boardVerified&&f.waterCount>=9;
            if(step==1||step==2||step==3)success&=f.hasteVerified;
            if(step==5)success&=f.absorbsNullified;
            if(!success)return stop("POSTCONDITION_FAILED:"+step);
            awaitingSequence=-1;step++;
            // The post-frame is not also permission to send the next action.
            return new Decision(Kind.WAIT,-1,null,"POSTCONDITION_VERIFIED");
        }
        if(!f.boardVerified)return stop("BOARD_UNKNOWN");
        if(step==SLOTS.length){
            return new Decision(Kind.PUZZLE,-1,null,"ESPER_MION_WATER_TWO_COMBOS_COMBO_DROP");
        }
        int slot=SLOTS[step];SkillState state=f.skills[slot];
        if(state==null||!state.identityVerified||!state.skillVerified||state.monsterNo!=BEFORE[step])return stop("SKILL_IDENTITY_MISMATCH:"+slot);
        if(!state.readyKnown)return stop("SKILL_READY_UNKNOWN:"+slot);
        if(!state.ready)return stop("SKILL_NOT_READY:"+slot);
        if(state.layer!=LAYERS[step])return stop("ACTIVE_SKILL_LAYER_MISMATCH:"+slot);
        if(step==1&&(!f.boardVerified||f.waterCount<9))return stop("ESPER_WATER_CONDITION");
        return new Decision(f,slot,LAYERS[step],"B1_STEP_"+step);
    }
    /** Call only when the guarded dispatcher accepts the actual action; Dry Run never calls this. */
    public void markDispatched(Frame frame,Decision decision,long now){
        if(stopped!=null||awaitingSequence>=0||step>=SLOTS.length||frame==null||decision==null||decision.kind!=Kind.SKILL
                ||decision.slot!=SLOTS[step]||decision.layer!=LAYERS[step]||frame.generation!=generation
                ||decision.sourceTime!=frame.time||decision.sourceSequence!=frame.sequence||decision.sourceGeneration!=generation)
            throw new IllegalStateException("Invalid dispatch");
        Decision current=inspect(frame,now);
        if(current.kind!=Kind.SKILL||current.slot!=decision.slot||current.layer!=decision.layer)
            throw new IllegalStateException("Dispatch proof invalid: "+current.reason);
        awaitingSequence=frame.sequence;awaitingTime=frame.time;
    }
}
