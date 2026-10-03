package com.example.padautosolver;
/** Verified Napoleon branch only. The one charge turn assumes no shield or recovery. */
final class UraB5Policy {
    // Historical guide values are not a verified bound for the current game revision.
    // Keep a conservative HP floor above both that value and the real-device HP loss.
    static final int MIN_CHARGE_HP=230000;
    static boolean safeCharge(byte[] board,int hp,Integer cooldown,boolean alreadyCharged) {
        if(board==null||board.length!=30||hp<MIN_CHARGE_HP||cooldown==null||cooldown!=1||alreadyCharged)return false;
        for(byte color:board)if(color<0||color>6)return false;
        return true;
    }
    static boolean enoughRecovery(byte[] board) {
        if(board==null)return false;int count=0;for(byte c:board)if(c==5)count++;return count>=3;
    }
    static boolean rukaCanRecover(byte[] board) {
        if(board==null)return false;int count=0;for(byte c:board)if(c==5||c==0||c==4||c==7||c==8)count++;return count>=3;
    }
}
