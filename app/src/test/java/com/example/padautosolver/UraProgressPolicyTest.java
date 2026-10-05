package com.example.padautosolver;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class UraProgressPolicyTest {
    @Test public void b19HasteCannotUseTheWrongLayerOrGuessOverchargedBaseReadiness(){
        assertTrue(UraProgressPolicy.yukineAssistReady(new UraHeldSkillInfo("雪花の氷乱","10連ガチャパワー",-1,0)));
        assertFalse(UraProgressPolicy.yukineAssistReady(new UraHeldSkillInfo("雪花の氷乱","10連ガチャパワー",0,0)));
        assertFalse(UraProgressPolicy.yukineAssistReady(new UraHeldSkillInfo("雪花の氷乱","10連ガチャパワー",-1,1)));
        assertFalse(UraProgressPolicy.yukineAssistReady(new UraHeldSkillInfo("雪花の氷乱","別アシスト",-1,0)));
        assertFalse(UraProgressPolicy.yukineAssistReady(null));
        assertFalse(UraProgressPolicy.yukineAssistReady(new UraHeldSkillInfo("雪花の氷乱","10連ガチャバワー",-1,0)));
        assertTrue(UraProgressPolicy.yukineAssistReady(new UraHeldSkillInfo("雪花の氷乱","10連ガチャバワー",-1,0),true));
        assertFalse(UraProgressPolicy.yukineAssistReady(new UraHeldSkillInfo("雪花の氷乱","10連ガチャバワー",-1,1),true));
        assertTrue(UraProgressPolicy.b19HasteAllowed(19,3,33,31,true));
        assertFalse(UraProgressPolicy.b19HasteAllowed(19,3,33,31,false));
        assertFalse(UraProgressPolicy.b19HasteAllowed(19,0,31,31,true));
        assertFalse(UraProgressPolicy.b19HasteAllowed(19,3,34,31,true));
        assertTrue(UraProgressPolicy.b19SecondAttackAllowed(6,33,31,33,33,33,1));
        assertFalse(UraProgressPolicy.b19SecondAttackAllowed(6,33,31,32,33,33,1));
        assertFalse(UraProgressPolicy.b19SecondAttackAllowed(6,33,31,33,31,33,1));
        assertFalse(UraProgressPolicy.b19SecondAttackAllowed(6,33,31,33,33,33,5));
        assertNull(UraProgressPolicy.script(22));
    }
    @Test public void b19OnlyAllowsItsUnconsumedEntryTurnAndBothFullEnemyBars(){
        byte[] b=new byte[30];b[12]=-1;b[17]=-1;long mask=UraDualRoulettePlan.MASK;
        assertTrue(UraProgressPolicy.safeB19FirstCharge(b,mask,350000,0,31,31,1,true));
        assertFalse(UraProgressPolicy.safeB19FirstCharge(b,mask,349999,0,31,31,1,true));
        assertFalse(UraProgressPolicy.safeB19FirstCharge(b,mask,350000,0,32,31,1,true));
        assertFalse(UraProgressPolicy.safeB19FirstCharge(b,mask,350000,3,31,31,1,true));
        assertFalse(UraProgressPolicy.safeB19FirstCharge(b,mask,350000,0,31,31,1,false));
        assertFalse(UraProgressPolicy.safeB19FirstCharge(b,mask,350000,0,31,31,0,true));
        b[0]=7;assertFalse(UraProgressPolicy.safeB19FirstCharge(b,mask,350000,0,31,31,1,true));
    }
    @Test public void b16SecondChargeRequiresReviewedHalfHpAndDoesNotAssumeCanceledShield(){
        byte[] b=new byte[30];
        assertTrue(UraProgressPolicy.safeB16SecondCharge(b,350000,16,4,25,23,1,true,true));
        assertFalse(UraProgressPolicy.safeB16SecondCharge(b,349999,16,4,25,23,1,true,true));
        assertFalse(UraProgressPolicy.safeB16SecondCharge(b,350000,16,4,25,23,1,false,true));
        assertFalse(UraProgressPolicy.safeB16SecondCharge(b,350000,16,4,25,23,1,true,false));
        assertFalse(UraProgressPolicy.safeB16SecondCharge(b,350000,16,4,25,23,0,true,true));
        assertFalse(UraProgressPolicy.safeB16SecondCharge(b,350000,16,3,25,23,1,true,true));
        assertFalse(UraProgressPolicy.safeB16SecondCharge(b,350000,16,4,26,23,1,true,true));
        assertFalse(UraProgressPolicy.safeB16SecondCharge(b,350000,15,4,25,23,1,true,true));
        b[2]=7;assertFalse(UraProgressPolicy.safeB16SecondCharge(b,350000,16,4,25,23,1,true,true));
    }
    @Test public void b15RecoveryRequiresAllUnchangedCountersAndNoSkillConsumedOnThisFloor(){
        assertTrue(UraProgressPolicy.recoverB15Charge(15,1,10,21,21,18,20,true));
        assertTrue(UraProgressPolicy.recoverB15Charge(15,1,13,22,21,18,20,true));
        assertFalse(UraProgressPolicy.recoverB15Charge(15,1,10,21,21,18,20,false));
        assertFalse(UraProgressPolicy.recoverB15Charge(15,1,13,22,21,22,22,true));
        assertFalse(UraProgressPolicy.recoverB15Charge(15,2,10,21,21,18,20,true));
        assertFalse(UraProgressPolicy.recoverB15Charge(15,1,13,23,21,18,20,true));
        assertFalse(UraProgressPolicy.recoverB15Charge(15,1,5,21,21,18,20,true));
        assertFalse(UraProgressPolicy.recoverB15Charge(16,1,10,21,21,18,20,true));
    }
    @Test public void b6ScriptHasRealChargeTurnsAndLateAttributeVoid() {
        int[] cooldown={0,1,0,0,0};int round=0,sekkaRound=-1,skillRound=-1,odinRound=-1;
        byte[] board=new byte[30];
        for(int op:UraProgressPolicy.script(6)) {
            if(op<100){assertEquals("Unready skill "+op,0,cooldown[op]);cooldown[op]=new int[]{4,2,5,5,5}[op];skillRound=round;if(op==0)sekkaRound=round;if(op==3)odinRound=round;}
            else {
                if(op==100)assertTrue(UraProgressPolicy.safeCharge(board,230000,round,sekkaRound,skillRound,cooldown[1]));
                else {assertEquals(round,skillRound);if(round==3)assertEquals(round,odinRound);}
                round++;for(int i=0;i<cooldown.length;i++)cooldown[i]=Math.max(0,cooldown[i]-1);
            }
        }
        assertEquals(4,round);assertEquals(4,cooldown[3]);assertEquals(1,cooldown[1]);
    }
    @Test public void chargeRejectsExpiredShieldStaleSkillUnsafeBoardAndWrongCooldown(){
        byte[] board=new byte[30];assertTrue(UraProgressPolicy.safeCharge(board,230000,2,0,2,1));
        assertFalse(UraProgressPolicy.safeCharge(board,230000,4,0,4,1));
        assertFalse(UraProgressPolicy.safeCharge(board,230000,2,0,1,1));
        assertFalse(UraProgressPolicy.safeCharge(board,229999,2,0,2,1));
        assertFalse(UraProgressPolicy.safeCharge(board,230000,2,0,2,0));
        assertFalse(UraProgressPolicy.safeCharge(board,230000,2,0,2,null));
        board[0]=8;assertFalse(UraProgressPolicy.safeCharge(board,230000,2,0,2,1));
    }
    @Test public void unsupportedFloorCannotCreateActions(){assertNull(UraProgressPolicy.script(22));}
    @Test public void b15CannotChargeWithoutAllFourActualCountersOrRepeatAfterOneTurn(){
        byte[] b=new byte[30];assertTrue(UraProgressPolicy.safeB15Charge(b,350000,15,0,21,21,1,true));
        assertFalse(UraProgressPolicy.safeB15Charge(b,350000,15,0,21,21,1,false));assertFalse(UraProgressPolicy.safeB15Charge(b,350000,15,0,22,21,1,true));
        assertFalse(UraProgressPolicy.safeB15Charge(b,350000,15,1,21,21,1,true));assertFalse(UraProgressPolicy.safeB15Charge(b,350000,14,0,21,21,1,true));
        assertArrayEquals(new int[]{100,0,1,101},UraProgressPolicy.script(15));
    }
    @Test public void b13UnshieldedChargeRequiresFirstTurnKnownBoardAndHigherHp(){
        byte[] b=new byte[30];assertTrue(UraProgressPolicy.safeB13Charge(b,350000,13,0,17,17,1));
        assertFalse(UraProgressPolicy.safeB13Charge(b,349999,13,0,17,17,1));assertFalse(UraProgressPolicy.safeB13Charge(b,350000,14,0,17,17,1));
        assertFalse(UraProgressPolicy.safeB13Charge(b,350000,13,1,17,17,1));assertFalse(UraProgressPolicy.safeB13Charge(b,350000,13,0,18,17,1));
        b[0]=7;assertFalse(UraProgressPolicy.safeB13Charge(b,350000,13,0,17,17,1));
        int[] cd={1,1,1,3,1};for(int op:UraProgressPolicy.script(13)){
            if(op<100){assertEquals(0,cd[op]);cd[op]=new int[]{4,2,5,5,5}[op];}
            else for(int i=0;i<5;i++)cd[i]=Math.max(0,cd[i]-1);
        }assertEquals(1,cd[1]);
    }
    @Test public void odinAbsorbLastsTwoConsumedTurnsAndRejectsUnknownOrFutureProof(){
        assertTrue(UraProgressPolicy.odinAbsorbActive(15,15));assertTrue(UraProgressPolicy.odinAbsorbActive(16,15));
        assertFalse(UraProgressPolicy.odinAbsorbActive(17,15));assertFalse(UraProgressPolicy.odinAbsorbActive(14,15));assertFalse(UraProgressPolicy.odinAbsorbActive(15,-1));
    }
    @Test public void b12ActualDelayNeedsTwoChargesAndKeepsAbsorbForFinalAttack(){
        int[] cd={2,2,4,1,4};int round=14,odin=-1;int operation=0;
        for(int op:UraProgressPolicy.script(12)){
            if(op<100){assertEquals(0,cd[op]);cd[op]=new int[]{4,2,5,5,5}[op];if(op==3)odin=round;}
            else {if(op==100){assertEquals(UraProgressPolicy.chargeCooldown(12,operation),cd[1]);assertTrue(UraProgressPolicy.safeB12Charge(new byte[30],350000,12,operation,round,14,12,cd[1]));}
                else assertTrue(UraProgressPolicy.odinAbsorbActive(round,odin));
                round++;for(int i=0;i<5;i++)cd[i]=Math.max(0,cd[i]-1);
            }operation++;
        }
        assertEquals(17,round);assertEquals(0,cd[0]);assertEquals(1,cd[1]);
    }
    @Test public void b12ShieldOnlyChargeRequiresReviewedOperationCooldownAndHp(){
        byte[] board=new byte[30];
        assertTrue(UraProgressPolicy.safeB12Charge(board,350000,12,0,14,14,12,2));
        assertFalse(UraProgressPolicy.safeB12Charge(board,349999,12,0,14,14,12,2));
        assertFalse(UraProgressPolicy.safeB12Charge(board,350000,11,0,14,14,12,2));
        assertFalse(UraProgressPolicy.safeB12Charge(board,350000,12,1,14,14,12,2));
        assertFalse(UraProgressPolicy.safeB12Charge(board,350000,12,0,16,14,12,1));
        assertFalse(UraProgressPolicy.safeB12Charge(board,350000,12,0,16,16,12,1));
        assertFalse(UraProgressPolicy.safeB12Charge(board,350000,12,0,14,14,12,0));
        board[0]=7;assertFalse(UraProgressPolicy.safeB12Charge(board,350000,12,0,14,14,12,2));
    }
    @Test public void shieldAndCooldownsContinueAcrossReviewedFloorsWithoutResettingTurns(){
        int[] cooldown={0,1,0,0,0};int round=0,sekkaRound=-1,skillRound=-1;
        for(int floor=6;floor<=11;floor++)for(int op:UraProgressPolicy.script(floor)){
            if(op<100){assertEquals("B"+floor+" skill "+op,0,cooldown[op]);cooldown[op]=new int[]{4,2,5,5,5}[op];skillRound=round;if(op==0)sekkaRound=round;}
            else{
                if(op==100)assertTrue(UraProgressPolicy.safeCharge(new byte[30],230000,round,sekkaRound,skillRound,cooldown[1]));
                else assertEquals(round,skillRound);
                round++;for(int i=0;i<5;i++)cooldown[i]=Math.max(0,cooldown[i]-1);
            }
        }
        assertEquals(14,round);assertEquals(1,cooldown[1]);
    }
    @Test public void expandedChargeRequiresExplicitGeometryAndCannotUseThirtyCellBoard(){
        assertTrue(UraProgressPolicy.safeCharge(new byte[42],230000,10,8,10,1,7,6));
        assertFalse(UraProgressPolicy.safeCharge(new byte[42],230000,10,8,10,1));
        assertFalse(UraProgressPolicy.safeCharge(new byte[30],230000,10,8,10,1,7,6));
        assertFalse(UraProgressPolicy.safeCharge(new byte[42],230000,12,8,12,1,7,6));
    }
    @Test public void b8ClearsUnmatchBeforeChargingAndDoesNotAssumeFreshSekka(){
        int[] cooldown={2,1,1,2,0};int round=6,sekkaRound=4,skillRound=5;
        assertEquals(UraProgressPolicy.RUKA,UraProgressPolicy.script(8)[0]);
        for(int op:UraProgressPolicy.script(8)){
            if(op<100){assertEquals(0,cooldown[op]);cooldown[op]=new int[]{4,2,5,5,5}[op];skillRound=round;}
            else{
                if(op==100)assertTrue(UraProgressPolicy.safeCharge(new byte[30],230000,round,sekkaRound,skillRound,cooldown[1]));
                else assertEquals(round,skillRound);
                round++;for(int i=0;i<5;i++)cooldown[i]=Math.max(0,cooldown[i]-1);
            }
        }
        assertEquals(8,round);assertEquals(0,cooldown[0]);assertEquals(1,cooldown[1]);assertEquals(0,cooldown[3]);
    }
    @Test public void b7KeepsShieldUntilAttackAndChargesMionWithoutUsingUnreadySkills(){
        int[] cooldown={0,1,3,4,0};int round=4,sekkaRound=-1,skillRound=-1;
        byte[] board=new byte[30];
        for(int op:UraProgressPolicy.script(7)){
            if(op<100){assertEquals(0,cooldown[op]);cooldown[op]=new int[]{4,2,5,5,5}[op];skillRound=round;if(op==0)sekkaRound=round;}
            else{
                if(op==100)assertTrue(UraProgressPolicy.safeCharge(board,230000,round,sekkaRound,skillRound,cooldown[1]));
                else{assertEquals(round,skillRound);assertTrue(round-sekkaRound<4);}
                round++;for(int i=0;i<5;i++)cooldown[i]=Math.max(0,cooldown[i]-1);
            }
        }
        assertEquals(6,round);assertEquals(1,cooldown[1]);assertEquals(2,cooldown[0]);
    }
}
