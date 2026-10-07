package com.example.padautosolver;
/** Reviewed native floor scripts; unsupported floors have no actions. */
final class UraProgressPolicy {
    static final int SEKKA=0,MION=1,ESPER=2,ODIN=3,RUKA=4,YUKINE_ASSIST=5,RUKA_RECOVERY=6,CHARGE=100,ATTACK=101;
    static int[] script(int floor) {
        if(floor==6)return new int[]{SEKKA,CHARGE,MION,ATTACK,ESPER,CHARGE,MION,ODIN,ATTACK};
        if(floor==7)return new int[]{SEKKA,CHARGE,MION,ATTACK};
        if(floor==8)return new int[]{RUKA_RECOVERY,CHARGE,MION,ATTACK};
        if(floor==9)return new int[]{SEKKA,CHARGE,MION,ATTACK};
        if(floor==10)return new int[]{ODIN,CHARGE,MION,ATTACK};
        if(floor==11)return new int[]{SEKKA,CHARGE,MION,ESPER,ATTACK};
        if(floor==12)return new int[]{CHARGE,ODIN,CHARGE,MION,ATTACK};
        if(floor==13)return new int[]{CHARGE,SEKKA,MION,ATTACK};
        if(floor==14)return new int[]{ESPER,CHARGE,MION,ATTACK};
        if(floor==15)return new int[]{CHARGE,SEKKA,MION,ATTACK};
        if(floor==16)return new int[]{ODIN,CHARGE,MION,ATTACK,CHARGE,MION,ATTACK};
        if(floor==17)return new int[]{SEKKA,CHARGE,MION,ATTACK};
        if(floor==18)return new int[]{ODIN,CHARGE,MION,ATTACK};
        if(floor==19)return new int[]{CHARGE,MION,ATTACK,YUKINE_ASSIST,SEKKA,MION,ATTACK};
        if(floor==20)return new int[]{ODIN,CHARGE,MION,ATTACK,ESPER,CHARGE,MION,ATTACK};
        if(floor==21)return new int[]{CHARGE,SEKKA,MION,ATTACK};
        if(floor==22)return new int[]{CHARGE,RUKA_RECOVERY,ODIN,MION,ATTACK}; // Only the reviewed dark-Menoa sprite is actionable.
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
        if((floor<17||floor>19)||mask!=UraDualRoulettePlan.MASK||board==null||board.length!=30)return false;
        byte[] stable=board.clone();
        for(int i=0;i<30;i++)if((mask&(1L<<i))!=0)stable[i]=0;
        return safeCharge(stable,hp,round,sekkaRound,skillRound,mionRemaining);
    }
    static boolean safeB19FirstCharge(byte[] board,long mask,int hp,int operation,int round,int start,Integer mionRemaining,boolean entryBars){
        if(!entryBars||operation!=0||round!=start||hp<350000||!Integer.valueOf(1).equals(mionRemaining)||mask!=UraDualRoulettePlan.MASK||board==null||board.length!=30)return false;
        for(int i=0;i<30;i++)if((mask&(1L<<i))==0&&(board[i]<0||board[i]>6))return false;
        return true;
    }
    static boolean b19HasteAllowed(int floor,int operation,int round,int start,boolean halfBars){
        return floor==19&&operation==3&&round==start+2&&halfBars;
    }
    static boolean yukineAssistReady(UraHeldSkillInfo info){
        return yukineAssistReady(info,false);
    }
    static boolean yukineAssistReady(UraHeldSkillInfo info,boolean reviewedAssistHeading){
        return info!=null&&info.baseNamed("雪花の氷乱")&&(info.assistNamed("10連ガチャパワー")||reviewedAssistHeading)
            &&Integer.valueOf(-1).equals(info.baseRemaining)&&Integer.valueOf(0).equals(info.assistRemaining);
    }
    static boolean b19SecondAttackAllowed(int operation,int round,int start,int hasteRound,int sekkaRound,int skillRound,int lastSkill){
        return operation==6&&round==start+2&&hasteRound==round&&sekkaRound==round&&skillRound==round&&lastSkill==MION;
    }
    static long b20Mask(int round,int start) {
        int turns=round-start;
        if(turns<0)return -1;
        return turns==0?(1L<<21)|(1L<<26):turns<3?UraDualRoulettePlan.MASK:0;
    }
    /** Known Yo You entry: HP bounds exceed 153000*1.4 without assuming leader shields. */
    static boolean safeB20Charge(byte[] board,long mask,int hp,int operation,int round,int start,Integer mionRemaining) {
        int turns=round-start,cols=turns==0?7:6,rows=turns==0?6:5;
        if(!((operation==1&&turns==0)||(operation==5&&turns==2))||hp<350000||!Integer.valueOf(1).equals(mionRemaining)
                ||mask!=b20Mask(round,start)||board==null||board.length!=cols*rows)return false;
        for(int i=0;i<board.length;i++)if((mask&(1L<<i))==0&&(board[i]<0||board[i]>6))return false;
        return true;
    }
    static int rukaRecoveryLayer(UraHeldSkillInfo info,boolean baseHeading) {
        if(info==null||!(info.baseNamed("ダブル防御態勢水")||baseHeading))return 0;
        if(Integer.valueOf(0).equals(info.baseRemaining))return 1;
        return Integer.valueOf(-1).equals(info.baseRemaining)&&Integer.valueOf(0).equals(info.assistRemaining)?2:0;
    }
    /** Migrate only an unconsumed B8 readiness check; consumed skills retain their receipt. */
    static int rukaReadinessStep(int floor,int operation,int phase,int step) {
        return floor==8&&operation==0&&(phase==13||phase==14)&&step==RUKA?RUKA_RECOVERY:step;
    }
    /** B8's recovery board can clear on the charge itself. Require that exact verified receipt. */
    static boolean b9RetainShield(int floor,int operation,int round,int start,int sekkaRound,int lastSkill,int skillRound,
            String dispatch,String pending,String turnProof,Integer sekkaRemaining) {
        return floor==9&&operation==0&&round==start&&sekkaRound>=0&&round-sekkaRound==3
            &&lastSkill==RUKA_RECOVERY&&skillRound==round-1&&"VERIFIED".equals(dispatch)&&"CHARGE".equals(pending)
            &&"literal-next-floor-two-frames:9".equals(turnProof)&&(Integer.valueOf(1).equals(sekkaRemaining)||Integer.valueOf(4).equals(sekkaRemaining));
    }
    static boolean b9SkipCharge(int floor,int operation,int round,int start,int retainedShieldRound,Integer mionRemaining) {
        return floor==9&&operation==1&&round>=start&&round-start<=3&&retainedShieldRound==start&&Integer.valueOf(0).equals(mionRemaining);
    }
    static boolean b9CooldownCourse(int round,int start,int initial,Integer remaining) {
        return initial>=0&&initial<=3&&round>=start&&round-start<=initial
            &&remaining!=null&&remaining==initial-(round-start);
    }
    /** Three reviewed ordinary enemies total 120160 raw damage; each input needs fresh HP evidence. */
    static boolean safeB9DelayedCharge(byte[] board,int hp,int floor,int operation,int round,int start,int retainedShieldRound,int initial,Integer remaining,boolean reviewedTrio) {
        if(!reviewedTrio||floor!=9||operation!=1||retainedShieldRound!=start||hp<350000
            ||!b9CooldownCourse(round,start,initial,remaining)||remaining==null||remaining<=0||board==null||board.length!=30)return false;
        for(byte orb:board)if(orb<0||orb>6)return false;
        return true;
    }
    static boolean b22AttackAllowed(int operation,int round,int start,int recoveryRound,int odinRound,int skillRound,int lastSkill,int[] hp,int verifiedMaximum,boolean awokenNull) {
        return operation==4&&round==start+1&&recoveryRound==round&&odinRound==round&&skillRound==round&&lastSkill==MION
            &&hp!=null&&verifiedMaximum>=100000&&hp[1]==verifiedMaximum&&hp[0]>0&&hp[0]<=hp[1]&&!awokenNull;
    }
    static boolean safeB22Charge(byte[] board,int hp,int operation,int round,int start,Integer mionRemaining,boolean fullEnemyBars) {
        return fullEnemyBars&&safeFirstCharge(board,hp,operation,round,start,mionRemaining);
    }
    static boolean safeB21Charge(byte[] board,long mask,int hp,int operation,int round,int start,Integer mionRemaining,boolean fullEnemyBars) {
        if(!fullEnemyBars||operation!=0||round!=start||hp<350000||!Integer.valueOf(1).equals(mionRemaining)
                ||mask!=UraDualRoulettePlan.MASK||board==null||board.length!=30)return false;
        for(int i=0;i<30;i++)if((mask&(1L<<i))==0&&(board[i]<0||board[i]>6))return false;
        return true;
    }
}
