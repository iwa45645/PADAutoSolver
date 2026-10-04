package com.example.padautosolver;
/** Reviewed native floor scripts; unsupported floors have no actions. */
final class UraProgressPolicy {
    static final int SEKKA=0,MION=1,ESPER=2,ODIN=3,RUKA=4,CHARGE=100,ATTACK=101;
    static int[] script(int floor) {
        if(floor==6)return new int[]{SEKKA,CHARGE,MION,ATTACK,ESPER,CHARGE,MION,ODIN,ATTACK};
        if(floor==7)return new int[]{SEKKA,CHARGE,MION,ATTACK};
        if(floor==8)return new int[]{RUKA,CHARGE,MION,ATTACK};
        if(floor==9)return new int[]{SEKKA,CHARGE,MION,ATTACK};
        if(floor==10)return new int[]{ODIN,CHARGE,MION,ATTACK};
        if(floor==11)return new int[]{SEKKA,CHARGE,MION,ESPER,ATTACK};
        if(floor==12)return new int[]{CHARGE,ODIN,CHARGE,MION,ATTACK};
        if(floor==13)return new int[]{CHARGE,SEKKA,MION,ATTACK};
        if(floor==14)return new int[]{ESPER,CHARGE,MION,ATTACK};
        if(floor==15)return new int[]{CHARGE,SEKKA,MION,ATTACK};
        if(floor==16)return new int[]{ODIN,CHARGE,MION,ATTACK,CHARGE,MION,ATTACK};
        if(floor==17)return new int[]{SEKKA,CHARGE,MION,ATTACK};
        return null;
    }
    static boolean safeCharge(byte[] board,int hp,int round,int sekkaRound,int skillRound,Integer mionRemaining) {
        return safeCharge(board,hp,round,sekkaRound,skillRound,mionRemaining,6,5);
    }
    static boolean safeCharge(byte[] board,int hp,int round,int sekkaRound,int skillRound,Integer mionRemaining,int cols,int rows) {
        return skillRound==round&&Integer.valueOf(1).equals(mionRemaining)&&safeShieldCharge(board,hp,round,sekkaRound,cols,rows);
    }
    private static boolean safeShieldCharge(byte[] board,int hp,int round,int sekkaRound,int cols,int rows) {
        if(!((cols==6&&rows==5)||(cols==7&&rows==6))||board==null||board.length!=cols*rows||hp<230000||sekkaRound<0||round<sekkaRound||round-sekkaRound>=4)return false;
        for(byte c:board)if(c<0||c>6)return false;
        return true;
    }
    /** Reviewed two charge turns against Toto/Sopdet; no leader shield is assumed. */
    static boolean safeB12Charge(byte[] board,int hp,int floor,int operation,int round,int floorStartRound,int sekkaRound,Integer mionRemaining) {
        if(floor!=12||hp<350000)return false;
        if(operation==0&&round==floorStartRound&&Integer.valueOf(2).equals(mionRemaining))return safeShieldCharge(board,hp,round,sekkaRound,6,5);
        if(operation==2&&round==floorStartRound+1&&Integer.valueOf(1).equals(mionRemaining))return safeShieldCharge(board,hp,round,sekkaRound,6,5);
        return false;
    }
    static boolean odinAbsorbActive(int round,int odinRound){return odinRound>=0&&round>=odinRound&&round-odinRound<2;}
    static int chargeCooldown(int floor,int operation){return floor==12&&operation==0?2:1;}
    static boolean safeB13Charge(byte[] board,int hp,int floor,int operation,int round,int floorStartRound,Integer mionRemaining){
        return floor==13&&safeFirstCharge(board,hp,operation,round,floorStartRound,mionRemaining);
    }
    private static boolean safeFirstCharge(byte[] board,int hp,int operation,int round,int floorStartRound,Integer mionRemaining){
        if(operation!=0||round!=floorStartRound||hp<350000||!Integer.valueOf(1).equals(mionRemaining)||board==null||board.length!=30)return false;
        for(byte c:board)if(c<0||c>6)return false;
        return true;
    }
    static boolean safeB15Charge(byte[] board,int hp,int floor,int operation,int round,int floorStartRound,Integer mionRemaining,boolean allCountersVerified){
        return allCountersVerified&&floor==15&&safeFirstCharge(board,hp,operation,round,floorStartRound,mionRemaining);
    }
    static boolean recoverB15Charge(int floor,int operation,int phase,int round,int start,int sekkaRound,int skillRound,boolean entryCounters){
        return floor==15&&operation==1&&(phase==10||phase==13||phase==14)&&round>=start&&round<=start+1
            &&sekkaRound<start&&skillRound<start&&entryCounters;
    }
    static boolean safeB16SecondCharge(byte[] board,int hp,int floor,int operation,int round,int start,Integer mionRemaining,boolean buffsInvalidated,boolean halfHpVerified){
        if(floor!=16||operation!=4||round!=start+2||hp<350000||!buffsInvalidated||!halfHpVerified||!Integer.valueOf(1).equals(mionRemaining)||board==null||board.length!=30)return false;
        for(byte c:board)if(c<0||c>6)return false;
        return true;
    }
    static boolean safeB17Charge(byte[] board,long mask,int hp,int floor,int round,int sekkaRound,int skillRound,Integer mionRemaining){
        if(floor!=17||mask!=UraDualRoulettePlan.MASK||board==null||board.length!=30)return false;
        byte[] stable=board.clone();
        for(int i=0;i<30;i++)if((mask&(1L<<i))!=0)stable[i]=0;
        return safeCharge(stable,hp,round,sekkaRound,skillRound,mionRemaining);
    }
}
