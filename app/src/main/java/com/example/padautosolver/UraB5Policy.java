package com.example.padautosolver;
/** One charge turn, with no assumed shield or recovery. Luka's inherited Yukine
 * spinner must disappear before the ordinary attack solver can be used. */
final class UraB5Policy {
    // Historical guide values are not a verified bound for the current game revision.
    // Keep a conservative HP floor above both that value and the real-device HP loss.
    static final int MIN_CHARGE_HP=230000;
    static boolean safeCharge(byte[] board,int hp,Integer cooldown,boolean alreadyCharged) {
        return safeCharge(board,0,false,false,hp,cooldown,alreadyCharged);
    }
    static boolean acceptedMask(long mask,boolean luka,boolean inheritedYukine,boolean charged) {
        if(mask==0)return true;
        return luka&&inheritedYukine&&!charged&&mask>0&&(mask>>>30)==0&&Long.bitCount(mask)==1;
    }
    static boolean safeCharge(byte[] board,long mask,boolean luka,boolean inheritedYukine,int hp,Integer cooldown,boolean alreadyCharged) {
        if(board==null||board.length!=30||hp<MIN_CHARGE_HP||cooldown==null||cooldown!=1||alreadyCharged)return false;
        if(!acceptedMask(mask,luka,inheritedYukine,false))return false;
        for(int i=0;i<30;i++)if((mask&(1L<<i))==0&&(board[i]<0||board[i]>6))return false;
        return true;
    }
    static boolean enoughRecovery(byte[] board) {
        if(board==null)return false;int count=0;for(byte c:board)if(c==5)count++;return count>=3;
    }
    static boolean rukaCanRecover(byte[] board) {
        if(board==null)return false;int count=0;for(byte c:board)if(c==5||c==0||c==4||c==7||c==8)count++;return count>=3;
    }
}
