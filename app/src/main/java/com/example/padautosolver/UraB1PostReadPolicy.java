package com.example.padautosolver;
/** Missing identity may authorize bounded observation only; conflicting cooldowns still stop. */
final class UraB1PostReadPolicy {
    enum Outcome { VERIFIED, RECAPTURE_ONLY, STOP }
    static Outcome assess(int step,boolean identity,Integer base,Integer assist,int helperTotal,int recaptures) {
        if(recaptures<0||!UraCooldownPostcondition.matches(step,base,assist,helperTotal))return Outcome.STOP;
        if(identity)return Outcome.VERIFIED;
        return recaptures<2?Outcome.RECAPTURE_ONLY:Outcome.STOP;
    }
}
