package com.example.padautosolver;
/** A literal matching cooldown with missing identity permits observation, never another activation. */
final class UraSkillPostReadPolicy {
    enum Outcome { VERIFIED, RECAPTURE_ONLY, STOP }
    static Outcome assess(int expected,Integer observed,boolean named,int recaptures){
        if(expected<1||observed==null||observed!=expected||recaptures<0)return Outcome.STOP;
        if(named)return Outcome.VERIFIED;
        return recaptures<2?Outcome.RECAPTURE_ONLY:Outcome.STOP;
    }
    private UraSkillPostReadPolicy(){}
}
