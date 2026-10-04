package com.example.padautosolver;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Native reviewed floor scripts, consumed checkpoints, actual next-floor proof. */
final class UraProgressController {
    private static final String[] SKILLS={"フィーリングガーデン","ブリリアントコンチェルト","デッドリースペードエッジ","神泉槍グングニール","ダブル防御態勢水"};
    private static final int[] SLOTS={1,5,0,2,4},CDS={4,2,5,5,5};
    private final Context context;private final UraBattleVision vision;
    private final JSONObject record=new JSONObject();
    private int floor=6,operation,phase,step,round,floorStartRound,misses;
    private int[] script=UraProgressPolicy.script(6);
    private long lastSequence=-1,actionAt,actionSequence,menuAt;
    private boolean charged,saved,prepared,postSaved;
    private volatile UraHeldSkillInfo held;
    private volatile boolean heading;
    private volatile long heldAt,heldSequence;
    private UraPuzzlePlan plan;
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
                    if(same&&(data.optInt("phase")==7||data.optInt("phase")==9)) {
                        byte[] before=c.vision.luciferBoard(old),current=c.vision.luciferBoard(live);
                        same=before!=null&&current!=null&&Arrays.equals(before,current);
                    }
                }finally{if(old!=null)old.recycle();}
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
                // An unchanged entry board and all four unchanged counters prove that the
                // B15 free charge was never applied. Its cooldown is independently reread.
                boolean b15EntryCounters=c.floor==15&&c.vision.b15ChargeCounters(live);
                if(UraProgressPolicy.recoverB15Charge(c.floor,c.operation,c.phase,c.round,c.floorStartRound,
                        c.record.optInt("sekkaRound",-1),c.record.optInt("lastSkillRound",-1),b15EntryCounters)){
                    c.operation=0;c.phase=16;c.round=c.floorStartRound;c.record.put("b15ChargeNotApplied",true);
                }
                // The prior B13 build only inspected Sekka at operation zero; no skill was consumed.
                if(c.floor==13&&c.operation==0&&c.step==UraProgressPolicy.SEKKA&&(c.phase==13||c.phase==14))c.phase=2;
                if(c.phase==12){c.phase=0;c.operation=0;c.floorStartRound=c.round;}
                c.record.remove("failureReason");c.actionAt=now()-18000;return c;
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
        if(phase==0||phase==1) {
            List<StagePolicy.Item> text=nav.readUraCombat(frame);
            if(UraCombatText.blocked(text))return stop(frame,text,"PROGRESS_GAME_OVER_OR_PURCHASE",time,seq);
            if(phase==0) {
                StagePolicy.Item menu=UraCombatText.control(text,"MENU",500,620);if(menu==null)return retry(frame,text,"PROGRESS_MENU_REQUIRED",time,seq);
                return action(menu,"B"+floor+"：実階層を確認",()->{phase=1;menuAt=now();misses=0;saved=false;});
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
            if(observed==floor+1&&round<=floorStartRound)return stop(frame,text,"PROGRESS_UNEXPECTED_EARLY_NEXT_FLOOR",time,seq);
            record.put("observedFloor",observed);
            if(!saved){save(frame,text,"progress-floor",time,seq);saved=true;return waitFor("B"+floor+"：階層保存後の新しい画面を待機");}
            StagePolicy.Item back=UraCombatText.control(text,"戻る",2080,2220);if(back==null)return retry(frame,text,"PROGRESS_MENU_BACK_REQUIRED",time,seq);
            final boolean cleared=observed==floor+1;
            return action(back,"B"+floor+"：戦闘へ戻る",()->{phase=cleared?12:2;misses=0;saved=false;});
        }
        if(phase==12)return stop(frame,List.of(),"B"+floor+"_CLEAR_VERIFIED_B"+(floor+1)+"_CAPTURE_REQUIRED",time,seq);
        if(phase==2) {
            boolean matched=enemy(frame);long mask=mask(frame);byte[] current=board(frame);
            record.put("enemyMatched",matched).put("rouletteMask",mask).put("boardKnown",current!=null).put("boardCols",cols()).put("boardRows",rows()).put("boardDistances",vision.boardDistances==null?new JSONArray():new JSONArray(vision.boardDistances));
            // Only open the reviewed recovery skill while water's unmatchable marker obscures its artwork.
            // No orb substitution and no puzzle are permitted under this exception.
            boolean unmatchRecovery=floor==8&&operation==0&&waterUnmatch(frame);
            if(!matched||mask!=0||(current==null&&!unmatchRecovery))return retry(frame,List.of(),"PROGRESS_ENEMY_BOARD_REQUIRED",time,seq);
            if(floor==8&&!unmatchRecovery&&waterUnmatch(frame))return retry(frame,List.of(),"PROGRESS_WATER_UNMATCH_NOT_CLEARED",time,seq);
            if(operation>=script.length)return stop(frame,List.of(),"PROGRESS_SCRIPT_NOT_CLEARED",time,seq);
            int op=script[operation];prepared=false;
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
            if(held==null||held.baseRemaining==null||!(held.baseNamed(SKILLS[step])||heading))return retry(frame,List.of(),"PROGRESS_SKILL_READINESS_UNKNOWN:"+step,time,seq);
            int next=3;
            if(held.baseRemaining!=0)return stop(frame,List.of(),"PROGRESS_SKILL_COOLDOWN:"+step+":"+held.baseRemaining,time,seq);
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
            int hp=vision.b3HpLowerBound(frame);int sekkaRound=record.optInt("sekkaRound",-1),mionRemaining=record.optInt("chargeMionRemaining",-1);
            boolean safe=UraProgressPolicy.safeCharge(board,hp,round,sekkaRound,record.optInt("lastSkillRound",-1),mionRemaining,cols(),rows())
                ||UraProgressPolicy.safeB12Charge(board,hp,floor,operation,round,floorStartRound,sekkaRound,mionRemaining)
                ||UraProgressPolicy.safeB13Charge(board,hp,floor,operation,round,floorStartRound,mionRemaining)
                ||UraProgressPolicy.safeB15Charge(board,hp,floor,operation,round,floorStartRound,mionRemaining,floor==15&&vision.b15ChargeCounters(frame));
            if(floor==15)safe=UraProgressPolicy.safeB15Charge(board,hp,floor,operation,round,floorStartRound,mionRemaining,vision.b15ChargeCounters(frame));
            if(!enemy(frame)||mask(frame)!=0||!safe)return retry(frame,List.of(),"PROGRESS_CHARGE_SHIELD_HP_REQUIRED",time,seq);
            List<Integer> route=UraChargeRoute.find(board,cols(),rows());misses=0;
            if(!prepared){operation++;phase=10;record.put("chargeRoute",new JSONArray(route)).put("chargeHpLowerBound",vision.b3HpLowerBound(frame)).put("chargeSourceBoard",array(board)).put("chargeCols",cols()).put("chargeRows",rows());save(frame,List.of(),"progress-charge-consumed",time,seq);operation--;phase=15;prepared=true;return waitFor("残存軽減・HP下限・実コンボを再照合");}
            StagePolicy.Decision d=action(new StagePolicy.Item("PROGRESS_CHARGE",610,1700),"B"+floor+"：現在HPを確認し実コンボで充填",()->{operation++;phase=10;actionAt=now();misses=0;});puzzle(d,route);return d;
        }
        if(phase==3) {
            return action(new StagePolicy.Item("PROGRESS_SKILL_"+step,100+203*SLOTS[step],1425),"B"+floor+"："+SKILLS[step]+"の本体ボタンを確認",()->{phase=4;misses=0;prepared=false;});
        }
        if(phase==4) {
            List<StagePolicy.Item> text=nav.readUraDialog(frame);UraDialogPolicy.Read read=UraDialogPolicy.read(text,2712);
            if(read==null||read.layer!=1||!read.named(SKILLS[step])||!vision.backControl(frame,read)||!vision.active(frame,read.activate))return retry(frame,text,"PROGRESS_SKILL_NOT_READY_OR_IDENTITY:"+step,time,seq);
            if(!prepared){phase=5;save(frame,text,"progress-skill-consumed",time,seq);phase=4;prepared=true;return waitFor("B"+floor+"：保存後に本体スキルを再照合");}
            return action(read.activate,"B"+floor+"："+SKILLS[step]+"を発動",()->{phase=5;actionAt=now();actionSequence=seq;misses=0;held=null;});
        }
        if(phase==5) {
            if(now()-actionAt<1800)return waitFor("B"+floor+"：スキル演出を待機");
            StagePolicy.Decision d=action(new StagePolicy.Item("PROGRESS_POST_"+step,100+203*SLOTS[step],1425),"B"+floor+"：使用後の残りターンを取得",()->{phase=6;misses=0;postSaved=false;});captureHeld(d,nav,"progress-held");return d;
        }
        if(phase==6) {
            if(held==null)return retry(frame,List.of(),"PROGRESS_POST_SKILL_REQUIRED",time,seq);
            if(heldAt<=actionAt||heldSequence<=actionSequence||!(held.baseNamed(SKILLS[step])||heading)||!Integer.valueOf(CDS[step]).equals(held.baseRemaining))return stop(frame,List.of(),"PROGRESS_SKILL_POSTCONDITION:"+step,time,seq);
            if(!postSaved){record.put("lastSkillRound",round).put("lastSkill",step);if(step==UraProgressPolicy.SEKKA)record.put("sekkaRound",round);if(step==UraProgressPolicy.ODIN)record.put("odinRound",round);if(step==UraProgressPolicy.ESPER)record.put("esperRound",round);save(frame,List.of(),"progress-skill-verified",time,seq);postSaved=true;return waitFor("B"+floor+"：使用後証拠を保存");}
            UraDialogPolicy.Read read=UraDialogPolicy.read(nav.readUraDialog(frame),2712);
            if(read!=null&&vision.backControl(frame,read))return action(read.back,"B"+floor+"：スキル確認を閉じる",this::finishSkill);
            if(board(frame)==null)return retry(frame,List.of(),"PROGRESS_POST_DIALOG_OR_BOARD_REQUIRED",time,seq);
            finishSkill();return waitFor("本体スキルの使用を確認済み");
        }
        if(phase==7) {
            if(cols()!=6||rows()!=5)return stop(frame,List.of(),"PROGRESS_ATTACK_LAYOUT_NOT_RESTORED",time,seq);
            byte[] board=board(frame);
            if(!enemy(frame)||mask(frame)!=0||board==null)return retry(frame,List.of(),"PROGRESS_ATTACK_BOARD_REQUIRED",time,seq);
            if(record.optInt("lastSkillRound",-1)!=round)return stop(frame,List.of(),"PROGRESS_THIS_TURN_SKILL_REQUIRED",time,seq);
            if(floor==6&&operation==8&&record.optInt("odinRound",-1)!=round)return stop(frame,List.of(),"PROGRESS_THIS_TURN_ATTRIBUTE_VOID_REQUIRED",time,seq);
            if(floor==11&&record.optInt("esperRound",-1)!=round)return stop(frame,List.of(),"PROGRESS_THIS_TURN_DAMAGE_VOID_REQUIRED",time,seq);
            if(floor==12&&!UraProgressPolicy.odinAbsorbActive(round,record.optInt("odinRound",-1)))return stop(frame,List.of(),"PROGRESS_DAMAGE_ABSORB_EXPIRED",time,seq);
            if(!UraB5Policy.enoughRecovery(board)) {
                if(!UraB5Policy.rukaCanRecover(board)||record.optInt("recoveryUsedRound",-1)==round)return stop(frame,List.of(),"PROGRESS_RECOVERY_REQUIRED",time,seq);
                step=UraProgressPolicy.RUKA;phase=13;record.put("recoveryPending",true);save(frame,List.of(),"progress-recovery-required",time,seq);
                return waitFor("B"+floor+"：ルカで回復を補充するため使用可能を確認");
            }
            PuzzleGoal goal=floor>=8?PuzzleGoal.esperMionAndHeal():PuzzleGoal.waterAndHeal();PuzzleSolver.Result result=PuzzleSolver.solve(board,6,5,44,2500,2400,goal);
            try{plan=new UraPuzzlePlan(board,result,goal,now());}catch(IllegalArgumentException e){return stop(frame,List.of(),"PROGRESS_NO_VALID_ROUTE:"+e.getMessage(),time,seq);}
            record.put("sourceBoard",array(board)).put("route",new JSONArray(plan.path)).put("predictedCombos",plan.stats.combos).put("waterCombos",plan.stats.colorCombos[3]).put("healCombos",plan.stats.colorCombos[5]);
            record.put("firstWaterT",plan.stats.firstTShapes[3]);
            phase=9;prepared=false;save(frame,List.of(),"progress-plan",time,seq);return waitFor("B"+floor+"：水2セット＋回復の経路を再照合");
        }
        if(phase==9) {
            if(plan==null)return stop(frame,List.of(),"PROGRESS_PLAN_REQUIRED",time,seq);
            if(now()-plan.plannedAt>=15000){phase=7;return waitFor("B"+floor+"：現在の盤面から作り直す");}
            byte[] board=board(frame);if(board==null)return waitFor("B"+floor+"：ドロップの発光が収まるまで待機");
            if(!plan.current(board,now())||mask(frame)!=0)return stop(frame,List.of(),"PROGRESS_STALE_PLAN",time,seq);
            if(!enemy(frame))return waitFor("B"+floor+"：敵の発光が収まった画面を待機");
            if(!prepared){phase=10;operation++;record.put("attackConsumed",true);save(frame,List.of(),"progress-puzzle-consumed",time,seq);operation--;phase=9;prepared=true;return waitFor("B"+floor+"：送信直前の盤面を再照合");}
            StagePolicy.Decision d=action(new StagePolicy.Item("PROGRESS_PUZZLE",610,1700),"B"+floor+"：水2セット＋回復で攻撃",()->{operation++;phase=10;actionAt=now();misses=0;});puzzle(d,plan.path);return d;
        }
        if(phase==10) {
            List<StagePolicy.Item> text=nav.readUraCombat(frame);if(UraCombatText.blocked(text))return stop(frame,text,"PROGRESS_GAME_OVER_OR_PURCHASE",time,seq);
            if(now()-actionAt<18000)return waitFor("B"+floor+"：コンボ・敵行動・階層遷移を待機");
            if(floor==15&&operation==1&&vision.b15ChargeCounters(frame))return stop(frame,text,"PROGRESS_B15_CHARGE_NOT_APPLIED",time,seq);
            round++;phase=0;misses=0;saved=false;save(frame,text,"progress-result",time,seq);return waitFor("B"+floor+"：パズル後の実階層を確認");
        }
        return stop(frame,List.of(),"PROGRESS_UNKNOWN_STATE",time,seq);
    }
    private void finishSkill(){
        if(record.optBoolean("recoveryPending")){record.remove("recoveryPending");try{record.put("recoveryUsedRound",round);}catch(JSONException e){throw new IllegalStateException(e);}phase=7;}
        else {operation++;phase=2;}
        misses=0;
    }
    private int cols(){return floor==10&&round==floorStartRound?7:6;}
    private int rows(){return cols()==7?6:5;}
    private byte[] board(Bitmap frame)throws Exception {
        if(cols()==7&&vision.distance(frame,"progress-b10-expanded.png",1040,1240,120,80)>=.035)return null;
        return vision.progressBoard(frame,cols(),rows());
    }
    private long mask(Bitmap frame)throws Exception {return vision.rouletteMask(frame,cols(),rows());}
    private boolean enemy(Bitmap frame)throws Exception {
        double best=1;for(int dx=-12;dx<=12;dx+=6)for(int dy=-12;dy<=12;dy+=6)best=Math.min(best,vision.distance(frame,"progress-b"+floor+"-enemy.png",260+dx,650+dy,550,500));return best<.055;
    }
    private boolean waterUnmatch(Bitmap frame)throws Exception {
        return floor==8&&vision.distance(frame,"progress-b8-water-unmatch.png",1040,1240,120,80)<.035;
    }
    private void captureHeld(StagePolicy.Decision d,StageNavigator nav,String name) {
        d.holdMs=4000;d.heldStampedFrame=(image,current,t,s)->{
            List<StagePolicy.Item> text=nav.readUraHeldSkill(image);UraHeldSkillInfo info=UraHeldSkillInfo.readBase(text);if(!current.getAsBoolean())return;
            held=info;heldAt=t;heldSequence=s;
            heading=step==0&&vision.sekkaPostHeading(image)||step==1&&vision.distance(image,"post-mion-skill-header.png",130,230,770,50)<.025||step==4&&vision.distance(image,"post-ruka-skill-header.png",130,230,490,50)<.025;
            record.put("reviewedHeading",heading).put("heldSkill",info==null?JSONObject.NULL:new JSONObject().put("name",info.baseName).put("remaining",info.baseRemaining));save(image,text,name,t,s);
        };
    }
    private void puzzle(StagePolicy.Decision d,List<Integer> route){d.puzzlePath=route;d.puzzleCols=cols();d.puzzleRows=rows();d.puzzleRect=BoardGeometry.calculate(1220,2712,cols(),rows(),0,84);d.puzzleDurationMs=3500;d.puzzlePreciseStart=true;}
    private static JSONObject read(File f)throws Exception{return new JSONObject(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));}
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
        Files.write(new File(context.getFilesDir(),"ura-progress-state.json").toPath(),record.toString(2).getBytes(StandardCharsets.UTF_8));android.util.Log.i("PADProgress",record.toString());
    }
}
