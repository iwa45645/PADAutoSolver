package com.example.padautosolver;
/** Releasing a cooling skill can return directly to combat; never invent a back button. */
final class UraReadinessExit {
    enum Outcome { CLOSE_MODAL, CONTINUE_COMBAT, WAIT }
    static Outcome evaluate(boolean modalPresent,boolean backVerified,boolean enemyVerified,boolean boardKnown) {
        if(modalPresent)return backVerified?Outcome.CLOSE_MODAL:Outcome.WAIT;
        return enemyVerified&&boardKnown?Outcome.CONTINUE_COMBAT:Outcome.WAIT;
    }
    private UraReadinessExit(){}
}
