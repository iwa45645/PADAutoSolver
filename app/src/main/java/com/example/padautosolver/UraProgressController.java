package com.example.padautosolver;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Native floor scripts with dispatch receipts and independently verified game turns. */
final class UraProgressController {
    private static final String[] SKILLS={"フィーリングガーデン","ブリリアントコンチェルト","デッドリースペードエッジ","神泉槍グングニール","ダブル防御態勢水","10連ガチャパワー","かつての水柱"};
    private static final int[] SLOTS={1,5,0,2,4,3,4},CDS={4,2,5,5,5,3,5};
    private final Context context;private final UraBattleVision vision;
    private final JSONObject record=new JSONObject();
    private final UraHpEvidence hpEvidence=new UraHpEvidence();
    private int floor=6,operation,phase,step,round,floorStartRound,misses;
    private int[] script=UraProgressPolicy.script(6);
    private long lastSequence=-1,actionAt,actionSequence,menuAt;
    private boolean charged,saved,prepared,postSaved,terminalOnly;
    private volatile UraHeldSkillInfo held;
    private volatile boolean heading,assistHeading;
    private volatile long heldAt,heldSequence;
    private UraPuzzlePlan plan;
    private UraDualRoulettePlan dualPlan;
    private final UraClearProof clearResult=new UraClearProof();
    private final UraTurnProof.FloorConfirmation resultFloor=new UraTurnProof.FloorConfirmation();
    private UraProgressController(Context c){context=c;vision=new UraBattleVision(c);}
    static UraProgressController resume(Context ctx,Bitmap live)throws Exception {
        if(live.getWidth()!=1220||live.getHeight()!=2712)return null;
        File source=new File(ctx.getFilesDir(),"ura-b5-state.json"),picture=new File(ctx.getFilesDir(),"ura-b5-state.png");
        if(!source.isFile()||!picture.isFile())return null;
        JSONObject b5=read(source);if(b5.optInt("phase")!=12||b5.optInt("observedFloor")!=6)return null;
        UraProgressController c=new UraProgressController(ctx);
        File own=new File(ctx.getFilesDir(),"ura-progress-state.json"),ownImage=new File(ctx.getFilesDir(),"ura-progress-state.png");
        if(own.isFile()&&ownImage.isFile()) {
            JSONObject data=read(own);
            if(data.optString("sourceB5RunId").equals(b5.optString("runId"))) {
                Bitmap old=BitmapFactory.decodeFile(ownImage.getPath());boolean same;
                try{
                    same=old!=null&&UraBattleVision.sameStablePortraits(old,live);
                    // Before an unconsumed attack, a changed board cannot inherit a skill-use claim.
                    if(data.optInt("floor")==22&&data.optBoolean("completed")){
                        same=UraClearProof.completedResume(data.optInt("phase"),data.optString("dispatchState"),c.vision.finalClearLogo(live)||c.vision.finalRewardTitle(live));
                    }
                    if(same&&(data.optInt("phase")==7||data.optInt("phase")==9)) {
                        byte[] before=c.vision.luciferBoard(old),current=c.vision.luciferBoard(live);
                        if(data.optInt("floor")>=17&&data.optInt("floor")<=19){
                            before=c.vision.rouletteBoard(old,UraDualRoulettePlan.MASK);current=c.vision.rouletteBoard(live,UraDualRoulettePlan.MASK);
                            same=c.vision.rouletteMask(old)==UraDualRoulettePlan.MASK&&c.vision.rouletteMask(live)==UraDualRoulettePlan.MASK;
                        }
                        same=same&&before!=null&&current!=null&&Arrays.equals(before,current);
                    }
                }finally{if(old!=null)old.recycle();}
                if(UraClearProof.terminalResume(data.optInt("floor"),data.optInt("operation"),data.optBoolean("awaitingTurn"),data.optString("pendingAction"),data.optString("dispatchState"),c.vision.finalClearLogo(live))){
                    same=true;c.terminalOnly=true; // CLEAR allows result observation only, never resending combat.
                }
                if(!same)return null;
                int target=data.optInt("phase")==12?data.optInt("observedFloor"):data.optInt("floor",6);
                if(UraProgressPolicy.script(target)==null)return null;
                c.floor=target;c.script=UraProgressPolicy.script(target);
                for(Iterator<String> it=data.keys();it.hasNext();){String k=it.next();c.record.put(k,data.get(k));}
                c.phase=data.getInt("phase");c.operation=data.optInt("operation");c.round=data.optInt("round");c.floorStartRound=data.optInt("floorStartRound");c.step=data.optInt("step");c.charged=data.optBoolean("charged");
                if(c.phase==5||c.phase==6)c.phase=5;
                else if(c.phase==4)c.phase=3;
                else if(c.phase==9)c.phase=7;
                else if(c.phase==1)c.phase=0;
                else if(c.phase==14)c.phase=13;
                else if(c.phase==17)c.phase=16;
                else if(c.phase==19)c.phase=18;
                else if(c.phase==15)c.phase=16;
                // All four unchanged counters prove that the B15 free charge was not
                // applied. The current board and cooldown are independently reread.
                boolean b15EntryCounters=c.floor==15&&c.vision.b15ChargeCounters(live);
                if(UraProgressPolicy.recoverB15Charge(c.floor,c.operation,c.phase,c.round,c.floorStartRound,
                        c.record.optInt("sekkaRound",-1),c.record.optInt("lastSkillRound",-1),b15EntryCounters)){
                    c.operation=0;c.phase=16;c.round=c.floorStartRound;c.record.put("b15ChargeNotApplied",true);
                }
                // The prior B13 build only inspected Sekka at operation zero; no skill was consumed.
                if(c.floor==13&&c.operation==0&&c.step==UraProgressPolicy.SEKKA&&(c.phase==13||c.phase==14))c.phase=2;
                // The initial B19 build only read delayed Sekka; no action was consumed.
                if(c.floor==19&&c.operation==0&&c.round==c.floorStartRound&&c.step==UraProgressPolicy.SEKKA&&(c.phase==13||c.phase==14))c.phase=2;
                if(c.phase==12){c.phase=0;c.operation=0;c.floorStartRound=c.round;}
                c.terminalOnly|=c.record.optBoolean("terminalOnly");c.record.put("terminalOnly",c.terminalOnly);
                c.record.remove("failureReason");
                if(c.record.optBoolean("awaitingTurn"))c.record.put("dispatchSequence",-1); // New capture session has its own sequence counter.
                c.actionAt=now()-18000;return c;
            }
        }
        if(!c.enemy(live)||c.vision.rouletteMask(live)!=0||c.vision.luciferBoard(live)==null)return null;
        Bitmap old=BitmapFactory.decodeFile(picture.getPath());boolean same;
        try{same=old!=null&&UraBattleVision.sameStablePortraits(old,live)&&Arrays.equals(c.vision.luciferBoard(old),c.vision.luciferBoard(live));}finally{if(old!=null)old.recycle();}
        if(!same)return null;
        c.record.put("runId",UUID.randomUUID().toString()).put("sourceB5RunId",b5.getString("runId")).put("mode","PROGRESS_DENEBOLA");return c;
    }
    StagePolicy.Decision inspect(Bitmap frame,StageNavigator nav,long time,long seq)throws Exception {
        if(seq<=lastSequence)return waitFor("B"+floor+"：新しい画面を待機");lastSequence=seq;
        if(frame.getWidth()!=1220||frame.getHeight()!=2712)return stop(frame,List.of(),"PROGRESS_CALIBRATION_MISMATCH",time,seq);
        if(UraClearProof.finalOutcomePhase(phase)&&floor==22&&record.optBoolean("awaitingTurn")&&record.optString("pendingAction").equals("ATTACK")){
            List<StagePolicy.Item> outcome=nav.readUraResult(frame);
            if(UraCombatText.blocked(outcome))return stop(frame,outcome,"PROGRESS_GAME_OVER_OR_PURCHASE",time,seq);
            boolean evidence=UraClearProof.evidence(floor,true,"ATTACK",outcome)||vision.finalClearLogo(frame);
            if(evidence)save(frame,outcome,"progress-clear-candidate",time,seq);
            if(clearResult.observe(evidence,seq)){
                phase=23;record.put("completed",true).put("completedAt",System.currentTimeMillis());verifyTurn("final-clear-two-fresh-frames");
                save(frame,outcome,"progress-clear-verified",time,seq);return waitFor("裏魔門CLEAR確認済み：クリア結果を取得");
            }
        }
        if(phase==23||phase==24){
            List<StagePolicy.Item> outcome=nav.readUraResult(frame);
            if(UraCombatText.blocked(outcome))return stop(frame,outcome,"PROGRESS_GAME_OVER_OR_PURCHASE",time,seq);
            if(UraClearProof.reward(outcome)){
                phase=25;record.put("resultVerified",true).put("rewardCoins",UraClearProof.amount(outcome,"獲得コイン"))
                    .put("rewardExperienceStock",UraClearProof.amount(outcome,"経験値ストック"));
                long exp=UraClearProof.amount(outcome,"獲得EXP");if(exp<0)exp=UraClearProof.amount(outcome,"獲得経験値");if(exp<0)exp=UraClearProof.amount(outcome,"獲得EHP");
                record.put("rewardExp",exp);return stop(frame,outcome,"URA_SHURA_CLEAR_AND_RESULT_VERIFIED",time,seq);
            }
            boolean tips=false;StagePolicy.Item ok=null;
            for(var item:outcome){tips|=item.text.equals("TIPS");if(item.text.equals("OK")&&item.y>1500&&item.y<2500)ok=item;}
            if(phase==23&&tips&&ok!=null)return action(ok,"CLEAR確認済み：TIPSを閉じて報酬を確認",()->{phase=24;misses=0;actionAt=now();});
            if(UraCombatText.joined(outcome).contains("通信中")&&now()-actionAt<60000)return waitFor("CLEAR確認済み：報酬の通信を待機");
            return retry(frame,outcome,"URA_CLEAR_CONFIRMED_RESULT_CAPTURE_REQUIRED",time,seq);
        }
        if(phase==25)return stop(frame,List.of(),"URA_SHURA_CLEAR_AND_RESULT_VERIFIED",time,seq);
        if(terminalOnly)return retry(frame,List.of(),"URA_TERMINAL_CLEAR_EVIDENCE_REQUIRED",time,seq);
        int hpLowerBound=0;
        if(phase==15) {
            int[] hp=UraB3Policy.hp(nav.readUraHp(frame));hpEvidence.observe(hp,time,seq);
            int maximum=hpEvidence.maximum(time);hpLowerBound=vision.b3HpLowerBound(frame,maximum);
            record.put("observedMaxHp",maximum).put("hpLowerBound",hpLowerBound);
        }
        if(phase==0||phase==1) {
            List<StagePolicy.Item> text=nav.readUraCombat(frame);
            if(UraCombatText.blocked(text))return stop(frame,text,"PROGRESS_GAME_OVER_OR_PURCHASE",time,seq);
            if(phase==0) {
                if(UraCombatText.joined(text).contains("裏魔門の守護者")&&UraCombatText.floor(text)>=0&&UraCombatText.control(text,"戻る",2080,2220)!=null){
                    phase=1;menuAt=now();misses=0;saved=false;resultFloor.reset();return waitFor("開いているメニューから実階層を再確認");
                }
                StagePolicy.Item menu=UraCombatText.control(text,"MENU",500,620);if(menu==null)return retry(frame,text,"PROGRESS_MENU_REQUIRED",time,seq);
                return action(menu,"B"+floor+"：実階層を確認",()->{phase=1;menuAt=now();misses=0;saved=false;resultFloor.reset();});
            }
            int observed=UraCombatText.floor(text);
            if(observed>=0)record.put("floorProof","literal-ocr:"+observed);
            if(observed<0&&UraCombatText.joined(text).contains("裏魔門の守護者")&&UraCombatText.control(text,"戻る",2080,2220)!=null&&vision.menuFloor13(frame)) {
                observed=13;record.put("floorProof","reviewed-menu-glyphs-13/22");
            }
            if(observed<0||!UraCombatText.joined(text).contains("裏魔門の守護者")) {
                if(now()-menuAt<3000)return waitFor("B"+floor+"：メニュー表示を待機");
                StagePolicy.Item menu=UraCombatText.control(text,"MENU",500,620);
                if(menu!=null&&misses++<3)return action(menu,"B"+floor+"：メニューを再確認",()->menuAt=now());
                return retry(frame,text,"PROGRESS_FLOOR_REQUIRED",time,seq);
            }
            if(observed!=floor&&observed!=floor+1)return stop(frame,text,"PROGRESS_UNEXPECTED_FLOOR:"+observed,time,seq);
            if(record.optBoolean("awaitingTurn")) {
                boolean confirmed=resultFloor.observe(floor+1,observed);
                if(UraTurnProof.nextFloor(floor,observed,true)) {
                    if(!confirmed)return waitFor("B"+floor+"：次階層を新しい2画面で確認");
                    verifyTurn("literal-next-floor-two-frames:"+observed);
                }
            }
            if(observed==floor+1&&round<=floorStartRound)return stop(frame,text,"PROGRESS_UNEXPECTED_EARLY_NEXT_FLOOR",time,seq);
            record.put("observedFloor",observed);
            if(!saved){save(frame,text,"progress-floor",time,seq);saved=true;return waitFor("B"+floor+"：階層保存後の新しい画面を待機");}
            StagePolicy.Item back=UraCombatText.control(text,"戻る",2080,2220);if(back==null)return retry(frame,text,"PROGRESS_MENU_BACK_REQUIRED",time,seq);
            final boolean cleared=observed==floor+1;
            return action(back,"B"+floor+"：戦闘へ戻る",()->{phase=cleared?12:record.optBoolean("awaitingTurn")?18:2;misses=0;saved=false;});
        }
        if(phase==12)return stop(frame,List.of(),"B"+floor+"_CLEAR_VERIFIED_B"+(floor+1)+"_CAPTURE_REQUIRED",time,seq);
        if(phase==2) {
            boolean matched=enemy(frame);long mask=mask(frame);byte[] current=board(frame);
            record.put("enemyMatched",matched).put("rouletteMask",mask).put("boardKnown",current!=null).put("boardCols",cols()).put("boardRows",rows()).put("boardDistances",vision.boardDistances==null?new JSONArray():new JSONArray(vision.boardDistances));
            // Only open the reviewed recovery skill while water's unmatchable marker obscures its artwork.
            // No orb substitution and no puzzle are permitted under this exception.
            boolean unmatchRecovery=floor==8&&operation==0&&waterUnmatch(frame);
            if(!matched||mask!=expectedMask()||(current==null&&!unmatchRecovery))return retry(frame,List.of(),"PROGRESS_ENEMY_BOARD_REQUIRED",time,seq);
            if(floor==8&&!unmatchRecovery&&waterUnmatch(frame))return retry(frame,List.of(),"PROGRESS_WATER_UNMATCH_NOT_CLEARED",time,seq);
            if(operation>=script.length)return stop(frame,List.of(),"PROGRESS_SCRIPT_NOT_CLEARED",time,seq);
            if(floor==22&&operation>=2&&!verifyB22Recovery(frame,nav,time,seq))return retry(frame,List.of(),"PROGRESS_B22_AWAKENINGS_HP_RESTORE_REQUIRED",time,seq);
            int op=script[operation];prepared=false;
            if(op==UraProgressPolicy.YUKINE_ASSIST&&!UraProgressPolicy.b19HasteAllowed(floor,operation,round,floorStartRound,vision.distance(frame,"progress-b19-half-hp.png",130,1205,940,50)<.025))return stop(frame,List.of(),"PROGRESS_B19_HALF_HP_REQUIRED",time,seq);
            if(op==UraProgressPolicy.CHARGE){step=UraProgressPolicy.MION;phase=16;}
            else if(op==UraProgressPolicy.ATTACK)phase=7;
            else {step=op;phase=13;}
            return waitFor("B"+floor+"：次の手順を確認（"+(operation+1)+"/"+script.length+"）");
        }
        if(phase==13) {
            held=null;
            StagePolicy.Decision d=action(new StagePolicy.Item("PROGRESS_READY_"+step,100+203*SLOTS[step],1425),"B"+floor+"："+SKILLS[step]+"の残りターンを取得",()->{phase=14;misses=0;});
            captureHeld(d,nav,"progress-readiness");return d;
        }
        if(phase==14) {
            if(step==UraProgressPolicy.RUKA_RECOVERY){
                int layer=UraProgressPolicy.rukaRecoveryLayer(held,heading);
                if(layer==0)return retry(frame,List.of(),"PROGRESS_RUKA_RECOVERY_READINESS_REQUIRED",time,seq);
                if(layer==1)step=UraProgressPolicy.RUKA;
            }
            boolean assist=step==UraProgressPolicy.YUKINE_ASSIST||step==UraProgressPolicy.RUKA_RECOVERY;
            if(step==UraProgressPolicy.YUKINE_ASSIST&&!UraProgressPolicy.yukineAssistReady(held,assistHeading))return retry(frame,List.of(),"PROGRESS_YUKINE_ASSIST_READINESS_REQUIRED",time,seq);
            if(!assist&&(held==null||held.baseRemaining==null||!(held.baseNamed(SKILLS[step])||heading)))return retry(frame,List.of(),"PROGRESS_SKILL_READINESS_UNKNOWN:"+step,time,seq);
            int next=3;
            if(!assist&&held.baseRemaining!=0)return stop(frame,List.of(),"PROGRESS_SKILL_COOLDOWN:"+step+":"+held.baseRemaining,time,seq);
            UraDialogPolicy.Read read=UraDialogPolicy.read(nav.readUraDialog(frame),2712);
            if(read!=null&&vision.backControl(frame,read))return action(read.back,"B"+floor+"：確認を閉じる",()->{phase=next;prepared=false;misses=0;});
            if(board(frame)==null)return retry(frame,List.of(),"PROGRESS_READINESS_DIALOG_OR_BOARD_REQUIRED",time,seq);
            phase=next;prepared=false;misses=0;return waitFor(next==15?"B"+floor+"：1コンボ消してあと1ターン溜める":"B"+floor+"：本体スキル使用可能を確認");
        }
        if(phase==16) {
            held=null;step=UraProgressPolicy.MION;
            StagePolicy.Decision d=action(new StagePolicy.Item("PROGRESS_CHARGE_READY",1115,1425),"B"+floor+"：ミオンの残りターンを確認",()->{phase=17;misses=0;});captureHeld(d,nav,"progress-charge-ready");return d;
        }
        if(phase==17) {
            int expected=UraProgressPolicy.chargeCooldown(floor,operation);
            if(held==null||!(held.baseNamed(SKILLS[step])||heading)||!Integer.valueOf(expected).equals(held.baseRemaining))return retry(frame,List.of(),"PROGRESS_CHARGE_MION_COOLDOWN_REQUIRED:"+expected,time,seq);
            record.put("chargeMionRemaining",expected);
            UraDialogPolicy.Read read=UraDialogPolicy.read(nav.readUraDialog(frame),2712);
            if(read!=null&&vision.backControl(frame,read))return action(read.back,"充填確認を閉じる",()->{phase=15;prepared=false;misses=0;});
            if(board(frame)==null)return retry(frame,List.of(),"PROGRESS_CHARGE_DIALOG_OR_BOARD_REQUIRED",time,seq);
            phase=15;prepared=false;misses=0;return waitFor("軽減と現在HPを再確認");
        }
        if(phase==15) {
            byte[] board=board(frame);
            int hp=hpLowerBound;int sekkaRound=record.optInt("sekkaRound",-1),mionRemaining=record.optInt("chargeMionRemaining",-1);
            boolean safe=UraProgressPolicy.safeCharge(board,hp,round,sekkaRound,record.optInt("lastSkillRound",-1),mionRemaining,cols(),rows())
                ||UraProgressPolicy.safeB12Charge(board,hp,floor,operation,round,floorStartRound,sekkaRound,mionRemaining)
                ||UraProgressPolicy.safeB13Charge(board,hp,floor,operation,round,floorStartRound,mionRemaining)
                ||UraProgressPolicy.safeB15Charge(board,hp,floor,operation,round,floorStartRound,mionRemaining,floor==15&&vision.b15ChargeCounters(frame));
            if(floor==15)safe=UraProgressPolicy.safeB15Charge(board,hp,floor,operation,round,floorStartRound,mionRemaining,vision.b15ChargeCounters(frame));
            if(floor==16&&operation==4)safe=UraProgressPolicy.safeB16SecondCharge(board,hp,floor,operation,round,floorStartRound,mionRemaining,
                record.optBoolean("b16BuffsInvalidated"),vision.distance(frame,"progress-b16-half-hp.png",130,1205,940,50)<.025);
            if(dual()&&floor!=20)safe=UraProgressPolicy.safeB17Charge(board,mask(frame),hp,floor,round,sekkaRound,record.optInt("lastSkillRound",-1),mionRemaining);
            if(floor==19)safe=UraProgressPolicy.safeB19FirstCharge(board,mask(frame),hp,operation,round,floorStartRound,mionRemaining,vision.distance(frame,"progress-b19-entry-hp.png",130,1205,940,50)<.025);
            if(floor==20)safe=UraProgressPolicy.safeB20Charge(board,mask(frame),hp,operation,round,floorStartRound,mionRemaining);
            if(floor==21)safe=UraProgressPolicy.safeB21Charge(board,mask(frame),hp,operation,round,floorStartRound,mionRemaining,vision.distance(frame,"progress-b21-entry-hp.png",130,1205,940,50)<.025);
            if(floor==22)safe=UraProgressPolicy.safeB22Charge(board,hp,operation,round,floorStartRound,mionRemaining,vision.distance(frame,"progress-b22-entry-hp.png",130,1205,940,50)<.025);
            if(!enemy(frame)||mask(frame)!=expectedMask()||!safe)return retry(frame,List.of(),"PROGRESS_CHARGE_SHIELD_HP_REQUIRED",time,seq);
            if(floor==20)safe=UraProgressPolicy.safeB20Charge(board,mask(frame),hp,operation,round,floorStartRound,mionRemaining);
            if(!safe)return retry(frame,List.of(),"PROGRESS_B20_CHARGE_HP_OR_COOLDOWN_REQUIRED",time,seq);
            List<Integer> route=UraChargeRoute.find(board,cols(),rows(),expectedMask(),floor==21||floor==22);misses=0;
            if(!prepared){prepareReceipt("CHARGE",mionRemaining);if(floor==22)record.put("b22ExpectedRestoredMaxHp",hpEvidence.maximum(time));record.put("chargeRoute",new JSONArray(route)).put("chargeHpLowerBound",hpLowerBound).put("chargeSourceBoard",array(board)).put("chargeCols",cols()).put("chargeRows",rows());save(frame,List.of(),"progress-charge-prepared",time,seq);prepared=true;return waitFor("残存軽減・HP下限・実コンボを再照合");}
            StagePolicy.Decision d=consumingAction(new StagePolicy.Item("PROGRESS_CHARGE",610,1700),"B"+floor+"：現在HPを確認し実コンボで充填",10,true,seq);puzzle(d,route);return d;
        }
        if(phase==3) {
            return action(new StagePolicy.Item("PROGRESS_SKILL_"+step,100+203*SLOTS[step],1425),"B"+floor+"："+SKILLS[step]+"の本体ボタンを確認",()->{phase=4;misses=0;prepared=false;});
        }
        if(phase==4) {
            List<StagePolicy.Item> text=nav.readUraDialog(frame);UraDialogPolicy.Read read=UraDialogPolicy.read(text,2712);
            if(read==null||read.layer!=(step==UraProgressPolicy.YUKINE_ASSIST||step==UraProgressPolicy.RUKA_RECOVERY?2:1)||!read.named(SKILLS[step])||!vision.backControl(frame,read)||!vision.active(frame,read.activate))return retry(frame,text,"PROGRESS_SKILL_NOT_READY_OR_IDENTITY:"+step,time,seq);
            if(!prepared){prepareReceipt("SKILL",null);save(frame,text,"progress-skill-prepared",time,seq);prepared=true;return waitFor("B"+floor+"：保存後に本体スキルを再照合");}
            return consumingAction(read.activate,"B"+floor+"："+SKILLS[step]+"を発動",5,false,seq);
        }
        if(phase==5) {
            if(now()-actionAt<1800)return waitFor("B"+floor+"：スキル演出を待機");
            StagePolicy.Decision d=action(new StagePolicy.Item("PROGRESS_POST_"+step,100+203*SLOTS[step],1425),"B"+floor+"：使用後の残りターンを取得",()->{phase=6;misses=0;postSaved=false;});captureHeld(d,nav,"progress-held");return d;
        }
        if(phase==6) {
            if(held==null)return retry(frame,List.of(),"PROGRESS_POST_SKILL_REQUIRED",time,seq);
            String postName=step==UraProgressPolicy.YUKINE_ASSIST?"雪花の氷乱":step==UraProgressPolicy.RUKA_RECOVERY?SKILLS[4]:SKILLS[step];
            if(heldAt<=actionAt||heldSequence<=actionSequence||!(held.baseNamed(postName)||heading)||!Integer.valueOf(CDS[step]).equals(held.baseRemaining))return stop(frame,List.of(),"PROGRESS_SKILL_POSTCONDITION:"+step,time,seq);
            if(!postSaved){record.put("dispatchState","VERIFIED");record.put("lastSkillRound",round).put("lastSkill",step);if(step==UraProgressPolicy.SEKKA)record.put("sekkaRound",round);if(step==UraProgressPolicy.ODIN)record.put("odinRound",round);if(step==UraProgressPolicy.ESPER)record.put("esperRound",round);if(step==UraProgressPolicy.YUKINE_ASSIST)record.put("yukineAssistRound",round);if(floor==22&&(step==UraProgressPolicy.RUKA||step==UraProgressPolicy.RUKA_RECOVERY))record.put("b22RecoverySkillRound",round);save(frame,List.of(),"progress-skill-verified",time,seq);postSaved=true;return waitFor("B"+floor+"：使用後証拠を保存");}
            UraDialogPolicy.Read read=UraDialogPolicy.read(nav.readUraDialog(frame),2712);
            if(read!=null&&vision.backControl(frame,read))return action(read.back,"B"+floor+"：スキル確認を閉じる",this::finishSkill);
            if(board(frame)==null)return retry(frame,List.of(),"PROGRESS_POST_DIALOG_OR_BOARD_REQUIRED",time,seq);
            finishSkill();return waitFor("本体スキルの使用を確認済み");
        }
        if(phase==7) {
            if(cols()!=6||rows()!=5)return stop(frame,List.of(),"PROGRESS_ATTACK_LAYOUT_NOT_RESTORED",time,seq);
            byte[] board=board(frame);
            if(!enemy(frame)||mask(frame)!=expectedMask()||board==null)return retry(frame,List.of(),"PROGRESS_ATTACK_BOARD_REQUIRED",time,seq);
            if(record.optInt("lastSkillRound",-1)!=round)return stop(frame,List.of(),"PROGRESS_THIS_TURN_SKILL_REQUIRED",time,seq);
            if(floor==6&&operation==8&&record.optInt("odinRound",-1)!=round)return stop(frame,List.of(),"PROGRESS_THIS_TURN_ATTRIBUTE_VOID_REQUIRED",time,seq);
            if(floor==11&&record.optInt("esperRound",-1)!=round)return stop(frame,List.of(),"PROGRESS_THIS_TURN_DAMAGE_VOID_REQUIRED",time,seq);
            if(floor==12&&!UraProgressPolicy.odinAbsorbActive(round,record.optInt("odinRound",-1)))return stop(frame,List.of(),"PROGRESS_DAMAGE_ABSORB_EXPIRED",time,seq);
            if(floor==18&&!UraProgressPolicy.odinAbsorbActive(round,record.optInt("odinRound",-1)))return stop(frame,List.of(),"PROGRESS_B18_DAMAGE_ABSORB_EXPIRED",time,seq);
            if(floor==19&&operation==6&&!UraProgressPolicy.b19SecondAttackAllowed(operation,round,floorStartRound,record.optInt("yukineAssistRound",-1),record.optInt("sekkaRound",-1),record.optInt("lastSkillRound",-1),record.optInt("lastSkill",-1)))return stop(frame,List.of(),"PROGRESS_B19_HASTE_SHIELD_MION_REQUIRED",time,seq);
            if(floor==22){
                if(!verifyB22Recovery(frame,nav,time,seq)||!UraProgressPolicy.b22AttackAllowed(operation,round,floorStartRound,record.optInt("b22RecoverySkillRound",-1),record.optInt("odinRound",-1),record.optInt("lastSkillRound",-1),record.optInt("lastSkill",-1),b22Hp(frame,nav),record.optInt("b22ExpectedRestoredMaxHp"),b22AwokenNull(frame)))
                    return retry(frame,List.of(),"PROGRESS_B22_RECOVERY_ABSORB_MION_REQUIRED",time,seq);
            }
            if(!UraB5Policy.enoughRecovery(board)) {
                if(!UraB5Policy.rukaCanRecover(board)||record.optInt("recoveryUsedRound",-1)==round)return stop(frame,List.of(),"PROGRESS_RECOVERY_REQUIRED",time,seq);
                step=UraProgressPolicy.RUKA;phase=13;record.put("recoveryPending",true);save(frame,List.of(),"progress-recovery-required",time,seq);
                return waitFor("B"+floor+"：ルカで回復を補充するため使用可能を確認");
            }
            if(dual()){
                try{dualPlan=UraDualRoulettePlan.solve(board,expectedMask(),44,floor==19?2000:350,12000,now());}catch(IllegalArgumentException e){return stop(frame,List.of(),"PROGRESS_NO_DUAL_ROUTE:"+e.getMessage(),time,seq);}
                record.put("sourceBoard",array(board)).put("route",new JSONArray(dualPlan.path)).put("dualRouletteProofPairs",100);
                phase=9;prepared=false;save(frame,List.of(),"progress-dual-plan",time,seq);return waitFor("B"+floor+"：全100色組合せで水T字・水2セット・回復を検算済み");
            }
            PuzzleGoal goal=PuzzleGoal.esperMionAndHeal();PuzzleSolver.Result result=PuzzleSolver.solve(board,6,5,44,2500,2400,goal);
            try{plan=new UraPuzzlePlan(board,result,goal,now());}catch(IllegalArgumentException e){return stop(frame,List.of(),"PROGRESS_NO_VALID_ROUTE:"+e.getMessage(),time,seq);}
            record.put("sourceBoard",array(board)).put("route",new JSONArray(plan.path)).put("predictedCombos",plan.stats.combos).put("waterCombos",plan.stats.colorCombos[3]).put("healCombos",plan.stats.colorCombos[5]);
            record.put("firstWaterT",plan.stats.firstTShapes[3]);
            phase=9;prepared=false;save(frame,List.of(),"progress-plan",time,seq);return waitFor("B"+floor+"：水2セット＋回復の経路を再照合");
        }
        if(phase==9) {
            if(dual()){
                if(dualPlan==null)return stop(frame,List.of(),"PROGRESS_DUAL_PLAN_REQUIRED",time,seq);
                if(now()-dualPlan.plannedAt>=15000){phase=7;return waitFor("B17：経路の期限切れを再探索");}
                byte[] current=board(frame);if(current==null)return waitFor("B17：固定マスの発光を待機");
                if(!dualPlan.current(current,mask(frame),now()))return stop(frame,List.of(),"PROGRESS_STALE_DUAL_PLAN",time,seq);
                if(!enemy(frame))return waitFor("B17：敵の発光を待機");
                if(!prepared){prepareReceipt("ATTACK",2);save(frame,List.of(),"progress-dual-prepared",time,seq);prepared=true;return waitFor("B"+floor+"：送信前に固定マスと2か所を再照合");}
                StagePolicy.Decision d=consumingAction(new StagePolicy.Item("PROGRESS_DUAL_PUZZLE",610,1700),"B"+floor+"：2か所を避けて水T字・水2セット・回復",10,true,seq);puzzle(d,dualPlan.path);return d;
            }
            if(plan==null)return stop(frame,List.of(),"PROGRESS_PLAN_REQUIRED",time,seq);
            if(now()-plan.plannedAt>=15000){phase=7;return waitFor("B"+floor+"：現在の盤面から作り直す");}
            byte[] board=board(frame);if(board==null)return waitFor("B"+floor+"：ドロップの発光が収まるまで待機");
            if(!plan.current(board,now())||mask(frame)!=0)return stop(frame,List.of(),"PROGRESS_STALE_PLAN",time,seq);
            if(!enemy(frame))return waitFor("B"+floor+"：敵の発光が収まった画面を待機");
            if(!prepared){prepareReceipt("ATTACK",2);save(frame,List.of(),"progress-puzzle-prepared",time,seq);prepared=true;return waitFor("B"+floor+"：送信直前の盤面を再照合");}
            StagePolicy.Decision d=consumingAction(new StagePolicy.Item("PROGRESS_PUZZLE",610,1700),"B"+floor+"：水T字・水2セット＋回復で攻撃",10,true,seq);puzzle(d,plan.path);return d;
        }
        if(phase==10) {
            List<StagePolicy.Item> text=nav.readUraCombat(frame);if(UraCombatText.blocked(text))return stop(frame,text,"PROGRESS_GAME_OVER_OR_PURCHASE",time,seq);
            if(now()-actionAt<18000)return waitFor("B"+floor+"：コンボ・敵行動・階層遷移を待機");
            if(floor==15&&operation==1&&vision.b15ChargeCounters(frame))return stop(frame,text,"PROGRESS_B15_CHARGE_NOT_APPLIED",time,seq);
            if(floor==16&&operation>=4){
                record.put("sekkaRound",-1).put("odinRound",-1).put("esperRound",-1).put("b16BuffsInvalidated",true);
            }
            if(!record.optBoolean("awaitingTurn"))return stop(frame,text,"PROGRESS_LEGACY_TURN_RESULT_REQUIRES_REVIEW",time,seq);
            phase=0;misses=0;saved=false;save(frame,text,"progress-result-awaiting-proof",time,seq);return waitFor("B"+floor+"：ターンを加算せず実階層・CDを確認");
        }
        if(phase==18) {
            held=null;step=UraProgressPolicy.MION;
            StagePolicy.Decision d=action(new StagePolicy.Item("PROGRESS_TURN_PROOF",1115,1425),"B"+floor+"：実ターン進行をミオンCDで確認",()->{phase=19;misses=0;});captureHeld(d,nav,"progress-turn-proof");return d;
        }
        if(phase==19) {
            if(held==null)return retry(frame,List.of(),"PROGRESS_TURN_CD_REQUIRED",time,seq);
            if(!UraTurnProof.cooldown(record.optInt("pendingMionBefore",-1),held.baseRemaining,held.baseNamed(SKILLS[1])||heading,record.optLong("dispatchSequence",Long.MAX_VALUE),heldSequence))
                return stop(frame,List.of(),"PROGRESS_TURN_NOT_VERIFIED:"+held.baseRemaining,time,seq);
            UraDialogPolicy.Read read=UraDialogPolicy.read(nav.readUraDialog(frame),2712);
            Runnable finish=()->{try{verifyTurn("named-mion-cd:"+held.baseRemaining);phase=2;misses=0;persistState();}catch(Exception e){throw new IllegalStateException(e);}};
            if(read!=null&&vision.backControl(frame,read))return action(read.back,"B"+floor+"：ターン確認を閉じる",finish);
            if(board(frame)==null)return retry(frame,List.of(),"PROGRESS_TURN_DIALOG_OR_BOARD_REQUIRED",time,seq);
            finish.run();return waitFor("実CDの減少を確認して次の手順へ");
        }
        return stop(frame,List.of(),"PROGRESS_UNKNOWN_STATE",time,seq);
    }
    private boolean b22AwokenNull(Bitmap frame)throws Exception {
        return vision.distance(frame,"progress-b22-awoken-null.png",1040,1240,120,80)<.06;
    }
    private int[] b22Hp(Bitmap frame,StageNavigator nav)throws Exception {
        int[] hp=UraB3Policy.hp(nav.readUraHp(frame));
        return hpEvidence.matches(hp,now())?hp:null;
    }
    private boolean verifyB22Recovery(Bitmap frame,StageNavigator nav,long time,long seq)throws Exception {
        if(record.optInt("b22RecoverySkillRound",-1)!=round||b22AwokenNull(frame))return false;
        List<StagePolicy.Item> text=nav.readUraHp(frame);int[] hp=UraB3Policy.hp(text);
        hpEvidence.observe(hp,time,seq);
        boolean exact=hp!=null;
        record.put("b22HpEvidence",exact?"two-fresh-literal-maximum-reads":"unreadable");
        if(!hpEvidence.matches(hp,time)||hp[0]<=0||hp[1]!=record.optInt("b22ExpectedRestoredMaxHp"))return false;
        record.put("b22HpOrLowerBound",hp[0]).put("b22ActualMaxHp",hp[1]).put("b22RecoveryVerifiedRound",round);
        if(!record.optBoolean("b22RecoveryEvidenceSaved")){save(frame,text,"progress-b22-recovery-verified",time,seq);record.put("b22RecoveryEvidenceSaved",true);}
        return true;
    }
    private void prepareReceipt(String kind,Integer before)throws Exception {
        if(record.has("gameTurnProof"))record.put("lastVerifiedTurnProof",record.optString("gameTurnProof"));
        record.remove("gameTurnProof");record.remove("verifiedDispatchId");
        record.put("dispatchState","PREPARED").put("dispatchId",UUID.randomUUID().toString()).put("pendingAction",kind).put("awaitingTurn",false);
        record.put("pendingMionBefore",before==null?JSONObject.NULL:before);
    }
    private StagePolicy.Decision consumingAction(StagePolicy.Item target,String status,int next,boolean turn,long seq) {
        final String dispatchId=record.optString("dispatchId");final int preparedPhase=phase,preparedOperation=operation;
        StagePolicy.Decision d=action(target,status,()->{
            actionAt=now();actionSequence=seq;misses=0;held=null;
            try{record.put("dispatchState","ACKNOWLEDGED");persistState();}catch(Exception e){throw new IllegalStateException(e);}
        });
        d.beforeDispatch=()->{
            if(dispatchId.isEmpty()||!dispatchId.equals(record.optString("dispatchId"))||!record.optString("dispatchState").equals("PREPARED")
                    ||phase!=preparedPhase||operation!=preparedOperation)throw new IllegalStateException("STALE_OR_ALREADY_DISPATCHED_RECEIPT");
            // Conditions have been checked on the main thread. This receipt means input MAY be sent,
            // never that its game effect has succeeded. Resume only examines the postcondition.
            phase=next;if(turn)operation++;
            actionAt=now();actionSequence=seq;
            record.put("dispatchState","DISPATCH_INTENT").put("dispatchSequence",seq).put("awaitingTurn",turn);
            persistState();
        };return d;
    }
    private void verifyTurn(String proof)throws Exception {
        if(!record.optBoolean("awaitingTurn"))return;
        round++;record.put("awaitingTurn",false).put("dispatchState","VERIFIED").put("gameTurnProof",proof).put("verifiedDispatchId",record.optString("dispatchId"));
        persistState();
    }
    private synchronized void persistState()throws Exception {
        record.put("checkpointVersion",2).put("floor",floor).put("floorStartRound",floorStartRound).put("operation",operation).put("phase",phase).put("round",round).put("step",step);
        android.util.AtomicFile file=new android.util.AtomicFile(new File(context.getFilesDir(),"ura-progress-state.json"));
        FileOutputStream out=null;
        try{out=file.startWrite();out.write(record.toString(2).getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}
        catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }
    private void finishSkill(){
        if(record.optBoolean("recoveryPending")){record.remove("recoveryPending");try{record.put("recoveryUsedRound",round);}catch(JSONException e){throw new IllegalStateException(e);}phase=7;}
        else {operation++;phase=2;}
        misses=0;
    }
    private int cols(){return (floor==10||floor==20)&&round==floorStartRound?7:6;}
    private int rows(){return cols()==7?6:5;}
    private byte[] board(Bitmap frame)throws Exception {
        if(expectedMask()!=0)return vision.rouletteBoard(frame,expectedMask(),cols(),rows());
        if(cols()==7&&vision.distance(frame,"progress-b"+floor+"-expanded.png",1040,1240,120,80)>=.035)return null;
        return vision.progressBoard(frame,cols(),rows());
    }
    private long mask(Bitmap frame)throws Exception {return vision.rouletteMask(frame,cols(),rows());}
    private boolean dual(){return floor>=17&&floor<=19||(floor==20||floor==21)&&expectedMask()==UraDualRoulettePlan.MASK;}
    private long expectedMask(){return floor==21?round==floorStartRound?UraDualRoulettePlan.MASK:0:floor==20?UraProgressPolicy.b20Mask(round,floorStartRound):floor>=17&&floor<=19?UraDualRoulettePlan.MASK:0;}
    private boolean enemy(Bitmap frame)throws Exception {
        double best=1;for(int dx=-12;dx<=12;dx+=6)for(int dy=-12;dy<=12;dy+=6)best=Math.min(best,vision.distance(frame,"progress-b"+floor+"-enemy.png",260+dx,650+dy,550,500));return best<.055;
    }
    private boolean waterUnmatch(Bitmap frame)throws Exception {
        return floor==8&&vision.distance(frame,"progress-b8-water-unmatch.png",1040,1240,120,80)<.035;
    }
    private void captureHeld(StagePolicy.Decision d,StageNavigator nav,String name) {
        boolean assistRead=(step==UraProgressPolicy.YUKINE_ASSIST||step==UraProgressPolicy.RUKA_RECOVERY)&&phase==13;
        d.holdMs=4000;d.heldStampedFrame=(image,current,t,s)->{
            List<StagePolicy.Item> text=nav.readUraHeldSkill(image);UraHeldSkillInfo info=assistRead?UraHeldSkillInfo.read(text):UraHeldSkillInfo.readBase(text);if(!current.getAsBoolean())return;
            held=info;heldAt=t;heldSequence=s;
            heading=step==0&&vision.sekkaPostHeading(image)||step==1&&vision.distance(image,"post-mion-skill-header.png",130,230,770,50)<.025||step==3&&vision.distance(image,"post-odin-skill-header.png",130,230,490,50)<.025||(step==4||step==6)&&vision.distance(image,"post-ruka-skill-header.png",130,230,490,50)<.025;
            assistHeading=assistRead&&vision.distance(image,"post-yukine-assist-header.png",130,380,550,50)<.025;
            record.put("reviewedAssistHeading",assistHeading);
            record.put("reviewedHeading",heading).put("heldSkill",info==null?JSONObject.NULL:new JSONObject().put("name",info.baseName).put("remaining",info.baseRemaining).put("assistName",info.assistName).put("assistRemaining",info.assistRemaining));save(image,text,name,t,s);
        };
    }
    private void puzzle(StagePolicy.Decision d,List<Integer> route){d.puzzlePath=route;d.puzzleCols=cols();d.puzzleRows=rows();d.puzzleRect=BoardGeometry.calculate(1220,2712,cols(),rows(),0,84);d.puzzleDurationMs=3500;d.puzzlePreciseStart=true;}
    private static JSONObject read(File f)throws Exception{return new JSONObject(new String(new android.util.AtomicFile(f).readFully(),StandardCharsets.UTF_8));}
    private static JSONArray array(byte[] b){JSONArray a=new JSONArray();for(byte v:b)a.put(v);return a;}
    private static long now(){return android.os.SystemClock.elapsedRealtime();}
    private StagePolicy.Decision waitFor(String s){StagePolicy.Decision d=new StagePolicy.Decision(null,s,false);d.nextFrameDelayMs=350;return d;}
    private StagePolicy.Decision action(StagePolicy.Item target,String s,Runnable done){StagePolicy.Decision d=new StagePolicy.Decision(target,s,false);d.holdMs=160;d.completed=done;return d;}
    private StagePolicy.Decision retry(Bitmap f,List<StagePolicy.Item> text,String s,long t,long q)throws Exception{if(++misses<12)return waitFor(s+"（再読込"+misses+"/12）");return stop(f,text,s,t,q);}
    private StagePolicy.Decision stop(Bitmap f,List<StagePolicy.Item> text,String s,long t,long q)throws Exception{record.put("failureReason",s);save(f,text,"progress-stop",t,q);return new StagePolicy.Decision(null,s,true);}
    private void save(Bitmap f,List<StagePolicy.Item> text,String name,long time,long seq)throws Exception {
        record.put("mode","URA_SHURA_PROGRESS").put("floor",floor).put("floorStartRound",floorStartRound).put("operation",operation).put("phase",phase).put("round",round).put("step",step).put("charged",charged).put("capturedAt",time).put("sequence",seq);
        JSONArray lines=new JSONArray();for(var i:text)lines.put(new JSONObject().put("text",i.rawText).put("x",i.x).put("y",i.y));record.put("ocr",lines);
        File dir=new File(context.getExternalFilesDir("Download"),"ura-runtime");if(!dir.exists()&&!dir.mkdirs())throw new IOException("B6 evidence unavailable");String prefix=name+"-"+System.currentTimeMillis();
        try(OutputStream out=new FileOutputStream(new File(dir,prefix+".png"))){f.compress(Bitmap.CompressFormat.PNG,100,out);}
        Files.write(new File(dir,prefix+".json").toPath(),record.toString(2).getBytes(StandardCharsets.UTF_8));
        try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"ura-progress-state.png"))){f.compress(Bitmap.CompressFormat.PNG,100,out);}
        persistState();android.util.Log.i("PADProgress",record.toString());
    }
}
