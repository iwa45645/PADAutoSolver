package com.example.padautosolver;

/** The held cooldown proves a turn; releasing a cooling skill need not open a modal. */
final class UraB3RefreshProof {
    enum Outcome { UNVERIFIED, CLOSE_NAMED_MODAL, ADVANCE_FROM_COMBAT, WAIT_FOR_COMBAT }
    static Outcome evaluate(Integer before,Integer after,boolean named,long actionAt,long actionSequence,
            long heldAt,long heldSequence,boolean modalPresent,boolean namedModalBack,
            boolean expectedEnemy,boolean boardKnown) {
        if(heldAt<=actionAt||!UraTurnProof.cooldown(before,after,named,actionSequence,heldSequence)
                ||!Integer.valueOf(4).equals(before))return Outcome.UNVERIFIED;
        if(modalPresent)return namedModalBack?Outcome.CLOSE_NAMED_MODAL:Outcome.WAIT_FOR_COMBAT;
        return expectedEnemy&&boardKnown?Outcome.ADVANCE_FROM_COMBAT:Outcome.WAIT_FOR_COMBAT;
    }
    private UraB3RefreshProof(){}
}
