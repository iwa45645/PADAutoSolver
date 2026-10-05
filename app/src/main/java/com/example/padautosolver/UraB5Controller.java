package com.example.padautosolver;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Native Napoleon charge/attack with literal skill evidence and actual Battle 6 proof. */
final class UraB5Controller {
    private static final String[] SKILLS={"ブリリアントコンチェルト","ダブル防御態勢水"};
    private static final int[] SLOTS={5,4},CDS={2,5};
    private final Context context;private final UraBattleVision vision;
    private final JSONObject record=new JSONObject();
    private int phase,step,round,misses;
    private long lastSequence=-1,actionAt,actionSequence,menuAt;
    private boolean charged,saved,prepared,postSaved;
    private volatile UraHeldSkillInfo held;
    private volatile boolean heading;
    private volatile long heldAt,heldSequence;
    private UraPuzzlePlan plan;
    private UraB5Controller(Context c){context=c;vision=new UraBattleVision(c);}
    static UraB5Controller resume(Context ctx,Bitmap live)throws Exception {
        if(live.getWidth()!=1220||live.getHeight()!=2712)return null;
        File source=new File(ctx.getFilesDir(),"ura-b4-state.json"),picture=new File(ctx.getFilesDir(),"ura-b4-state.png");
        if(!source.isFile()||!picture.isFile())return null;
        JSONObject b4=read(source);if(b4.optInt("phase")!=12||b4.optInt("observedFloor")!=5)return null;
        UraB5Controller c=new UraB5Controller(ctx);
        File own=new File(ctx.getFilesDir(),"ura-b5-state.json"),ownImage=new File(ctx.getFilesDir(),"ura-b5-state.png");
        if(own.isFile()&&ownImage.isFile()) {
            JSONObject data=read(own);
            if(data.optString("sourceB4RunId").equals(b4.optString("runId"))) {
                Bitmap old=BitmapFactory.decodeFile(ownImage.getPath());boolean same;
                try{
                    same=old!=null&&UraBattleVision.sameStablePortraits(old,live);
                    // Before an unconsumed attack, a changed board cannot inherit a skill-use claim.
                    if(same&&(data.optInt("phase")==7||data.optInt("phase")==9)) {
                        byte[] before=c.vision.luciferBoard(old),current=c.vision.luciferBoard(live);
                        same=before!=null&&current!=null&&Arrays.equals(before,current);
                    }
                }finally{if(old!=null)old.recycle();}
                if(!same||data.optInt("phase")==12)return null;
                for(Iterator<String> it=data.keys();it.hasNext();){String k=it.next();c.record.put(k,data.get(k));}
                c.phase=data.getInt("phase");c.round=data.optInt("round");c.step=data.optInt("step");c.charged=data.optBoolean("charged");
                if(c.phase==5||c.phase==6)c.phase=5;
                else if(c.phase==4)c.phase=3;
                else if(c.phase==9)c.phase=7;
                else if(c.phase==1)c.phase=0;
                else if(c.phase==14)c.phase=13;
                c.record.remove("failureReason");c.actionAt=now()-18000;return c;
            }
        }
        if(!c.vision.napoleon(live)||c.vision.rouletteMask(live)!=0||c.vision.luciferBoard(live)==null)return null;
        Bitmap old=BitmapFactory.decodeFile(picture.getPath());boolean same;
        try{same=old!=null&&UraBattleVision.sameStablePortraits(old,live)&&Arrays.equals(c.vision.luciferBoard(old),c.vision.luciferBoard(live));}finally{if(old!=null)old.recycle();}
        if(!same)return null;
        c.record.put("runId",UUID.randomUUID().toString()).put("sourceB4RunId",b4.getString("runId")).put("mode","B5_NAPOLEON");return c;
    }
    StagePolicy.Decision inspect(Bitmap frame,StageNavigator nav,long time,long seq)throws Exception {
        if(seq<=lastSequence)return waitFor("B5：新しい画面を待機");lastSequence=seq;
        if(frame.getWidth()!=1220||frame.getHeight()!=2712)return stop(frame,List.of(),"B5_CALIBRATION_MISMATCH",time,seq);
        if(phase==0||phase==1) {
            List<StagePolicy.Item> text=nav.readUraCombat(frame);
            if(UraCombatText.blocked(text))return stop(frame,text,"B5_GAME_OVER_OR_PURCHASE",time,seq);
            if(phase==0) {
                StagePolicy.Item menu=UraCombatText.control(text,"MENU",500,620);if(menu==null)return retry(frame,text,"B5_MENU_REQUIRED",time,seq);
                return action(menu,"B5：実階層を確認",()->{phase=1;menuAt=now();misses=0;saved=false;});
            }
            int floor=UraCombatText.floor(text);
            if(floor<0||!UraCombatText.joined(text).contains("裏魔門の守護者")) {
                if(now()-menuAt<3000)return waitFor("B5：メニュー表示を待機");
                StagePolicy.Item menu=UraCombatText.control(text,"MENU",500,620);
                if(menu!=null&&misses++<3)return action(menu,"B5：メニューを再確認",()->menuAt=now());
                return retry(frame,text,"B5_FLOOR_REQUIRED",time,seq);
            }
            if(floor!=5&&floor!=6)return stop(frame,text,"B5_UNEXPECTED_FLOOR:"+floor,time,seq);
            if(floor==6&&round==0)return stop(frame,text,"B5_UNEXPECTED_EARLY_FLOOR6",time,seq);
            record.put("observedFloor",floor);
            if(!saved){save(frame,text,"b5-floor",time,seq);saved=true;return waitFor("B5：階層保存後の新しい画面を待機");}
            StagePolicy.Item back=UraCombatText.control(text,"戻る",2080,2220);if(back==null)return retry(frame,text,"B5_MENU_BACK_REQUIRED",time,seq);
            return action(back,"B5：戦闘へ戻る",()->{phase=floor==6?12:2;misses=0;saved=false;});
        }
        if(phase==12)return stop(frame,List.of(),"B5_CLEAR_VERIFIED_B6_CAPTURE_REQUIRED",time,seq);
        if(phase==2) {
            if(!vision.napoleon(frame)||vision.rouletteMask(frame)!=0||vision.luciferBoard(frame)==null)return retry(frame,List.of(),"B5_NAPOLEON_BOARD_REQUIRED",time,seq);
            if(record.optBoolean("attackConsumed"))return stop(frame,List.of(),"B5_ATTACK_NOT_CLEARED",time,seq);
            if(record.optBoolean("mionVerified")){phase=7;return waitFor("B5：発動済みミオンの記録から再開");}
            step=0;phase=13;return waitFor("B5：ミオンの実際の残りターンを確認");
        }
        if(phase==13) {
            held=null;
            StagePolicy.Decision d=action(new StagePolicy.Item("B5_READY_"+step,100+203*SLOTS[step],1425),"B5："+SKILLS[step]+"の残りターンを取得",()->{phase=14;misses=0;});
            captureHeld(d,nav,"b5-readiness");return d;
        }
        if(phase==14) {
            if(held==null||held.baseRemaining==null||!(held.baseNamed(SKILLS[step])||heading))return retry(frame,List.of(),"B5_SKILL_READINESS_UNKNOWN:"+step,time,seq);
            int next;
            if(held.baseRemaining==0)next=3;
            else if(step==0&&held.baseRemaining==1&&!charged)next=15;
            else return stop(frame,List.of(),"B5_SKILL_COOLDOWN_UNEXPECTED:"+held.baseRemaining,time,seq);
            UraDialogPolicy.Read read=UraDialogPolicy.read(nav.readUraDialog(frame),2712);
            if(read!=null&&vision.backControl(frame,read))return action(read.back,"B5：確認を閉じる",()->{phase=next;prepared=false;misses=0;});
            if(vision.luciferBoard(frame)==null)return retry(frame,List.of(),"B5_READINESS_DIALOG_OR_BOARD_REQUIRED",time,seq);
            phase=next;prepared=false;misses=0;return waitFor(next==15?"B5：1コンボ消してあと1ターン溜める":"B5：本体スキル使用可能を確認");
        }
        if(phase==15) {
            byte[] board=vision.luciferBoard(frame);
            if(!vision.napoleon(frame)||vision.rouletteMask(frame)!=0||!UraB5Policy.safeCharge(board,vision.b3HpLowerBound(frame),held==null?null:held.baseRemaining,charged))return retry(frame,List.of(),"B5_CHARGE_HP_AND_COOLDOWN_REQUIRED",time,seq);
            List<Integer> route=RoulettePlan.chargeRoute(board,0);misses=0;
            if(!prepared){charged=true;phase=10;record.put("chargeRoute",new JSONArray(route)).put("minimumChargeHp",UraB5Policy.MIN_CHARGE_HP).put("chargeHpLowerBound",vision.b3HpLowerBound(frame));save(frame,List.of(),"b5-charge-consumed",time,seq);charged=false;phase=15;prepared=true;return waitFor("B5：HP下限23万以上を再照合");}
            StagePolicy.Decision d=action(new StagePolicy.Item("B5_CHARGE",610,1700),"B5：必ず1コンボ消してミオンを溜める",()->{charged=true;phase=10;actionAt=now();misses=0;});puzzle(d,route);return d;
        }
        if(phase==3) {
            return action(new StagePolicy.Item("B5_SKILL_"+step,100+203*SLOTS[step],1425),"B5："+SKILLS[step]+"の本体ボタンを確認",()->{phase=4;misses=0;prepared=false;});
        }
        if(phase==4) {
            List<StagePolicy.Item> text=nav.readUraDialog(frame);UraDialogPolicy.Read read=UraDialogPolicy.read(text,2712);
            if(read==null||read.layer!=1||!read.named(SKILLS[step])||!vision.backControl(frame,read)||!vision.active(frame,read.activate))return retry(frame,text,"B5_SKILL_NOT_READY_OR_IDENTITY:"+step,time,seq);
            if(!prepared){phase=5;save(frame,text,"b5-skill-consumed",time,seq);phase=4;prepared=true;return waitFor("B5：保存後に本体スキルを再照合");}
            return action(read.activate,"B5："+SKILLS[step]+"を発動",()->{phase=5;actionAt=now();actionSequence=seq;misses=0;held=null;});
        }
        if(phase==5) {
            if(now()-actionAt<1800)return waitFor("B5：スキル演出を待機");
            StagePolicy.Decision d=action(new StagePolicy.Item("B5_POST_"+step,100+203*SLOTS[step],1425),"B5：使用後の残りターンを取得",()->{phase=6;misses=0;postSaved=false;});captureHeld(d,nav,"b5-held");return d;
        }
        if(phase==6) {
            if(held==null)return retry(frame,List.of(),"B5_POST_SKILL_REQUIRED",time,seq);
            if(heldAt<=actionAt||heldSequence<=actionSequence||!(held.baseNamed(SKILLS[step])||heading)||!Integer.valueOf(CDS[step]).equals(held.baseRemaining))return stop(frame,List.of(),"B5_SKILL_POSTCONDITION:"+step,time,seq);
            if(!postSaved){record.put(step==0?"mionVerified":"rukaVerified",true);save(frame,List.of(),"b5-skill-verified",time,seq);postSaved=true;return waitFor("B5：使用後証拠を保存");}
            UraDialogPolicy.Read read=UraDialogPolicy.read(nav.readUraDialog(frame),2712);
            if(read!=null&&vision.backControl(frame,read))return action(read.back,"B5：スキル確認を閉じる",()->{phase=7;misses=0;});
            if(vision.luciferBoard(frame)==null)return retry(frame,List.of(),"B5_POST_DIALOG_OR_BOARD_REQUIRED",time,seq);
            phase=7;misses=0;return waitFor("B5：スキル使用を確認済み");
        }
        if(phase==7) {
            byte[] board=vision.luciferBoard(frame);
            if(!vision.napoleon(frame)||vision.rouletteMask(frame)!=0||board==null)return retry(frame,List.of(),"B5_ATTACK_BOARD_REQUIRED",time,seq);
            if(!record.optBoolean("mionVerified"))return stop(frame,List.of(),"B5_THIS_TURN_SKILL_REQUIRED",time,seq);
            if(!UraB5Policy.enoughRecovery(board)) {
                if(record.optBoolean("rukaVerified")||!UraB5Policy.rukaCanRecover(board))return stop(frame,List.of(),"B5_NOT_ENOUGH_RECOVERY",time,seq);
                step=1;phase=13;return waitFor("B5：ルカで回復を補充できることを確認");
            }
            PuzzleGoal goal=PuzzleGoal.esperMionAndHeal();PuzzleSolver.Result result=PuzzleSolver.solve(board,6,5,44,2500,1800,goal);
            try{plan=new UraPuzzlePlan(board,result,goal,now());}catch(IllegalArgumentException e){return stop(frame,List.of(),"B5_NO_VALID_ROUTE:"+e.getMessage(),time,seq);}
            record.put("sourceBoard",array(board)).put("route",new JSONArray(plan.path)).put("predictedCombos",plan.stats.combos).put("waterCombos",plan.stats.colorCombos[3]).put("healCombos",plan.stats.colorCombos[5]);
            phase=9;prepared=false;save(frame,List.of(),"b5-plan",time,seq);return waitFor("B5：水2セット＋回復の経路を再照合");
        }
        if(phase==9) {
            if(plan==null)return stop(frame,List.of(),"B5_PLAN_REQUIRED",time,seq);
            if(now()-plan.plannedAt>=15000){phase=7;return waitFor("B5：現在の盤面から作り直す");}
            byte[] board=vision.luciferBoard(frame);if(board==null)return waitFor("B5：ドロップの発光が収まるまで待機");
            if(!plan.current(board,now())||vision.rouletteMask(frame)!=0)return stop(frame,List.of(),"B5_STALE_PLAN",time,seq);
            if(!vision.napoleon(frame))return waitFor("B5：敵の発光が収まった画面を待機");
            if(!prepared){phase=10;record.put("attackConsumed",true);save(frame,List.of(),"b5-puzzle-consumed",time,seq);phase=9;prepared=true;return waitFor("B5：送信直前の盤面を再照合");}
            StagePolicy.Decision d=action(new StagePolicy.Item("B5_PUZZLE",610,1700),"B5：水2セット＋回復で攻撃",()->{phase=10;actionAt=now();misses=0;});puzzle(d,plan.path);return d;
        }
        if(phase==10) {
            List<StagePolicy.Item> text=nav.readUraCombat(frame);if(UraCombatText.blocked(text))return stop(frame,text,"B5_GAME_OVER_OR_PURCHASE",time,seq);
            if(now()-actionAt<18000)return waitFor("B5：コンボ・敵行動・階層遷移を待機");
            round++;phase=0;misses=0;saved=false;save(frame,text,"b5-result",time,seq);return waitFor("B5：パズル後の実階層を確認");
        }
        return stop(frame,List.of(),"B5_UNKNOWN_STATE",time,seq);
    }
    private void captureHeld(StagePolicy.Decision d,StageNavigator nav,String name) {
        d.holdMs=4000;d.heldStampedFrame=(image,current,t,s)->{
            List<StagePolicy.Item> text=nav.readUraHeldSkill(image);UraHeldSkillInfo info=UraHeldSkillInfo.readBase(text);if(!current.getAsBoolean())return;
            held=info;heldAt=t;heldSequence=s;
            heading=step==0?vision.distance(image,"post-mion-skill-header.png",130,230,770,50)<.025:vision.distance(image,"post-ruka-skill-header.png",130,230,490,50)<.025;
            record.put("reviewedHeading",heading).put("heldSkill",info==null?JSONObject.NULL:new JSONObject().put("name",info.baseName).put("remaining",info.baseRemaining));save(image,text,name,t,s);
        };
    }
    private static void puzzle(StagePolicy.Decision d,List<Integer> route){d.puzzlePath=route;d.puzzleCols=6;d.puzzleRows=5;d.puzzleRect=BoardGeometry.calculate(1220,2712,6,5,0,84);d.puzzleDurationMs=3500;d.puzzlePreciseStart=true;}
    private static JSONObject read(File f)throws Exception{return new JSONObject(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));}
    private static JSONArray array(byte[] b){JSONArray a=new JSONArray();for(byte v:b)a.put(v);return a;}
    private static long now(){return android.os.SystemClock.elapsedRealtime();}
    private StagePolicy.Decision waitFor(String s){StagePolicy.Decision d=new StagePolicy.Decision(null,s,false);d.nextFrameDelayMs=350;return d;}
    private StagePolicy.Decision action(StagePolicy.Item target,String s,Runnable done){StagePolicy.Decision d=new StagePolicy.Decision(target,s,false);d.holdMs=160;d.completed=done;return d;}
    private StagePolicy.Decision retry(Bitmap f,List<StagePolicy.Item> text,String s,long t,long q)throws Exception{if(++misses<12)return waitFor(s+"（再読込"+misses+"/12）");return stop(f,text,s,t,q);}
    private StagePolicy.Decision stop(Bitmap f,List<StagePolicy.Item> text,String s,long t,long q)throws Exception{record.put("failureReason",s);save(f,text,"b5-stop",t,q);return new StagePolicy.Decision(null,s,true);}
    private void save(Bitmap f,List<StagePolicy.Item> text,String name,long time,long seq)throws Exception {
        record.put("phase",phase).put("round",round).put("step",step).put("charged",charged).put("capturedAt",time).put("sequence",seq);
        JSONArray lines=new JSONArray();for(var i:text)lines.put(new JSONObject().put("text",i.rawText).put("x",i.x).put("y",i.y));record.put("ocr",lines);
        File dir=new File(context.getExternalFilesDir("Download"),"ura-runtime");if(!dir.exists()&&!dir.mkdirs())throw new IOException("B5 evidence unavailable");String prefix=name+"-"+System.currentTimeMillis();
        try(OutputStream out=new FileOutputStream(new File(dir,prefix+".png"))){f.compress(Bitmap.CompressFormat.PNG,100,out);}
        Files.write(new File(dir,prefix+".json").toPath(),record.toString(2).getBytes(StandardCharsets.UTF_8));
        try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"ura-b5-state.png"))){f.compress(Bitmap.CompressFormat.PNG,100,out);}
        Files.write(new File(context.getFilesDir(),"ura-b5-state.json").toPath(),record.toString(2).getBytes(StandardCharsets.UTF_8));android.util.Log.i("PADB5",record.toString());
    }
}
