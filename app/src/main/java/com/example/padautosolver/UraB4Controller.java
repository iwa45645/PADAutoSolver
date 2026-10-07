package com.example.padautosolver;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** B4: one safe charge, named roulette overwrite, worst-phase puzzle, actual B5 proof. */
final class UraB4Controller {
    private static final int[] SLOTS={3,4,5,1};
    private static final String[] SKILLS={"雪花の氷乱","ダブル防御態勢水","ブリリアントコンチェルト","フィーリングガーデン"};
    private static final int[] CDS={3,5,2,4};
    private final Context context;private final UraBattleVision vision;
    private final JSONObject record=new JSONObject();
    private final UraHpEvidence hpEvidence=new UraHpEvidence();
    private int phase,step,round,misses;
    private long lastSequence=-1,actionAt,actionSequence,menuAt;
    private boolean charged,saved,prepared,postSaved;
    private boolean waitingMion,extraCharged;
    private int expectedCooldown;
    private volatile UraHeldSkillInfo held;
    private volatile boolean sekkaHeading;
    private volatile boolean rukaHeading;
    private volatile boolean mionHeading;
    private volatile long heldAt,heldSequence;
    private RoulettePlan plan;
    private UraB4Controller(Context c){context=c;vision=new UraBattleVision(c);}
    static UraB4Controller resume(Context ctx,Bitmap live)throws Exception {
        if(live.getWidth()!=1220||live.getHeight()!=2712)return null;
        UraB4Controller c=new UraB4Controller(ctx);
        File source=new File(ctx.getFilesDir(),"ura-b3-state.json"),picture=new File(ctx.getFilesDir(),"ura-b3-state.png");
        if(!source.isFile()||!picture.isFile())return null;
        JSONObject b3=read(source);if(b3.optInt("phase")!=12||b3.optInt("observedFloor")!=4)return null;
        File own=new File(ctx.getFilesDir(),"ura-b4-state.json"),ownImage=new File(ctx.getFilesDir(),"ura-b4-state.png");
        if(own.isFile()&&ownImage.isFile()) {
            JSONObject data=read(own);
            if(data.optString("sourceB3RunId").equals(b3.optString("runId"))) {
                Bitmap old=BitmapFactory.decodeFile(ownImage.getPath());boolean same;
                try{same=old!=null&&UraBattleVision.sameStablePortraits(old,live);}finally{if(old!=null)old.recycle();}
                if(!same||data.optInt("phase")==12)return null;
                for(Iterator<String> it=data.keys();it.hasNext();){String k=it.next();c.record.put(k,data.get(k));}
                c.phase=data.getInt("phase");c.round=data.optInt("round");c.step=data.optInt("step");c.charged=data.optBoolean("charged");
                c.waitingMion=data.optBoolean("waitingMion");c.extraCharged=data.optBoolean("extraCharged");c.expectedCooldown=data.optInt("expectedCooldown",CDS[Math.min(c.step,3)]);
                if(c.phase==5||c.phase==6)c.phase=5;
                else if(c.phase==4)c.phase=3;
                else if(c.phase==9)c.phase=7;
                else if(c.phase==1)c.phase=0;
                else if(c.phase==14)c.phase=13;
                c.record.remove("failureReason");
                c.actionAt=now()-18000;return c;
            }
        }
        if(!c.vision.gears(live))return null;
        long mask=c.vision.rouletteMask(live);if(Long.bitCount(mask)!=5||c.vision.rouletteBoard(live,mask)==null)return null;
        Bitmap old=BitmapFactory.decodeFile(picture.getPath());boolean same;
        try{same=old!=null&&UraBattleVision.sameStablePortraits(old,live)&&c.vision.rouletteMask(old)==mask&&Arrays.equals(c.vision.rouletteBoard(old,mask),c.vision.rouletteBoard(live,mask));}finally{if(old!=null)old.recycle();}
        if(!same)return null;
        c.record.put("runId",UUID.randomUUID().toString()).put("sourceB3RunId",b3.getString("runId")).put("mode","B4_ROULETTE");return c;
    }
    StagePolicy.Decision inspect(Bitmap frame,StageNavigator nav,long time,long seq)throws Exception {
        if(seq<=lastSequence)return waitFor("B4：新しい画面を待機");lastSequence=seq;
        if(frame.getWidth()!=1220||frame.getHeight()!=2712)return stop(frame,List.of(),"B4_CALIBRATION_MISMATCH",time,seq);
        int hpLowerBound=0;
        if(phase==2||phase==7||phase==15) {
            int[] hp=UraB3Policy.hp(nav.readUraHp(frame));hpEvidence.observe(hp,time,seq);
            int maximum=hpEvidence.maximum(time);hpLowerBound=vision.b3HpLowerBound(frame,maximum);
            record.put("observedMaxHp",maximum).put("hpLowerBound",hpLowerBound);
        }
        if(phase==0||phase==1) {
            List<StagePolicy.Item> text=nav.readUraCombat(frame);
            if(UraCombatText.blocked(text))return stop(frame,text,"B4_GAME_OVER_OR_PURCHASE",time,seq);
            if(phase==0) {
                StagePolicy.Item menu=UraCombatText.control(text,"MENU",500,620);if(menu==null)return retry(frame,text,"B4_MENU_REQUIRED",time,seq);
                return action(menu,"B4：実階層を確認",()->{phase=1;menuAt=now();misses=0;saved=false;});
            }
            int floor=UraCombatText.floor(text);
            if(floor<0||!UraCombatText.joined(text).contains("裏魔門の守護者")) {
                if(now()-menuAt<3000)return waitFor("B4：メニュー表示を待機");
                StagePolicy.Item menu=UraCombatText.control(text,"MENU",500,620);
                if(menu!=null&&misses++<3)return action(menu,"B4：メニューを再確認",()->menuAt=now());
                return retry(frame,text,"B4_FLOOR_REQUIRED",time,seq);
            }
            if(floor!=4&&floor!=5)return stop(frame,text,"B4_UNEXPECTED_FLOOR:"+floor,time,seq);
            if(floor==5&&round==0)return stop(frame,text,"B4_UNEXPECTED_EARLY_FLOOR5",time,seq);
            record.put("observedFloor",floor);
            if(!saved){save(frame,text,"b4-floor",time,seq);saved=true;return waitFor("B4：階層保存後の新しい画面を待機");}
            StagePolicy.Item back=UraCombatText.control(text,"戻る",2080,2220);if(back==null)return retry(frame,text,"B4_MENU_BACK_REQUIRED",time,seq);
            return action(back,"B4：戦闘へ戻る",()->{phase=floor==5?12:2;misses=0;saved=false;});
        }
        if(phase==12)return stop(frame,List.of(),"B4_CLEAR_VERIFIED_B5_CAPTURE_REQUIRED",time,seq);
        if(phase==2) {
            if(!vision.gears(frame))return retry(frame,List.of(),"B4_THREE_GEARS_REQUIRED",time,seq);
            long mask=vision.rouletteMask(frame);byte[] board=vision.rouletteBoard(frame,mask);
            if(board==null)return retry(frame,List.of(),"B4_STABLE_BOARD_UNKNOWN",time,seq);
            if(!charged) {
                if(Long.bitCount(mask)!=5||!vision.b4ChargeCounters(frame)||hpLowerBound<230000)return retry(frame,List.of(),"B4_CHARGE_COUNTERS_2_5_2_AND_HP_REQUIRED",time,seq);
                // One turn is safe because ALL actual counters are >=2. No claim of zero combos.
                List<Integer> route=RoulettePlan.chargeRoute(board,mask);
                if(!prepared){charged=true;phase=10;round=0;record.put("chargeRoute",new JSONArray(route));save(frame,List.of(),"b4-charge-consumed",time,seq);charged=false;phase=2;prepared=true;return waitFor("B4：攻撃まで2ターン以上を再照合");}
                StagePolicy.Decision d=action(new StagePolicy.Item("B4_CHARGE",610,1700),"B4：1ターン進めてユキネを溜める",()->{charged=true;phase=10;actionAt=now();misses=0;});
                puzzle(d,route);return d;
            }
            if(round>(extraCharged?2:1))return stop(frame,List.of(),"B4_ATTACK_NOT_CLEARED",time,seq);
            step=extraCharged?2:0;phase=3;misses=0;return waitFor(extraCharged?"B4：ミオンの使用可否を再確認":"B4：ユキネでルーレットを上書き");
        }
        if(phase==3) {
            if(step==1) {
                long mask=vision.rouletteMask(frame);byte[] board=vision.rouletteBoard(frame,mask);
                if(board==null||Long.bitCount(mask)!=1)return retry(frame,List.of(),"B4_OVERWRITTEN_ONE_ROULETTE_REQUIRED",time,seq);
                int heal=0,convert=0;for(byte color:board){if(color==5)heal++;if(color==0||color==4||color==7||color==8)convert++;}
                if(heal>=3){step=2;return waitFor("B4：回復3個を確認、ミオンで水を生成");}
                if(heal+convert<3)return stop(frame,List.of(),"B4_NOT_ENOUGH_RECOVERY",time,seq);
            }
            return action(new StagePolicy.Item("B4_SKILL_"+step,100+203*SLOTS[step],1425),"B4："+SKILLS[step]+"の本体ボタンを確認",()->{phase=4;misses=0;prepared=false;});
        }
        if(phase==4) {
            List<StagePolicy.Item> text=nav.readUraDialog(frame);UraDialogPolicy.Read read=UraDialogPolicy.read(text,2712);
            if(read==null&&step==2&&++misses>=3) {
                long mask=vision.rouletteMask(frame);
                if(Long.bitCount(mask)==1&&vision.rouletteBoard(frame,mask)!=null){phase=13;return waitFor("B4：ミオンの実際の残りターンを確認");}
            }
            if(read==null||read.layer!=1||!read.named(SKILLS[step])||!vision.backControl(frame,read)||!vision.active(frame,read.activate))return retry(frame,text,"B4_SKILL_NOT_READY_OR_IDENTITY:"+step,time,seq);
            if(!prepared){
                expectedCooldown=CDS[step];
                for(var item:text){java.util.regex.Matcher m=java.util.regex.Pattern.compile("次回使用可能まで([1-9])ターン").matcher(item.text);if(m.find())expectedCooldown=Integer.parseInt(m.group(1));}
                phase=5;save(frame,text,"b4-skill-consumed",time,seq);phase=4;prepared=true;return waitFor("B4：保存後に本体スキルを再照合");}
            return action(read.activate,"B4："+SKILLS[step]+"を発動",()->{phase=5;actionAt=now();actionSequence=seq;misses=0;held=null;});
        }
        if(phase==5) {
            if(now()-actionAt<1800)return waitFor("B4：スキル演出を待機");
            StagePolicy.Decision d=action(new StagePolicy.Item("B4_POST_"+step,100+203*SLOTS[step],1425),"B4：使用後の残りターンを取得",()->{phase=6;misses=0;postSaved=false;});
            d.holdMs=4000;d.heldStampedFrame=(image,current,t,s)->{
                List<StagePolicy.Item> text=nav.readUraHeldSkill(image);UraHeldSkillInfo info=UraHeldSkillInfo.readBase(text);if(!current.getAsBoolean())return;
                held=info;heldAt=t;heldSequence=s;sekkaHeading=step==3&&vision.sekkaPostHeading(image);
                rukaHeading=step==1&&vision.distance(image,"post-ruka-skill-header.png",130,230,490,50)<.025;
                mionHeading=step==2&&vision.distance(image,"post-mion-skill-header.png",130,230,770,50)<.025;
                record.put("reviewedRukaHeading",rukaHeading).put("reviewedSekkaHeading",sekkaHeading);
                record.put("postSkill",info==null?JSONObject.NULL:new JSONObject().put("name",info.baseName).put("remaining",info.baseRemaining));save(image,text,"b4-held",t,s);
            };return d;
        }
        if(phase==6) {
            if(held==null)return retry(frame,List.of(),"B4_POST_SKILL_REQUIRED",time,seq);
            if(heldAt<=actionAt||heldSequence<=actionSequence||!(held.baseNamed(SKILLS[step])||step==3&&sekkaHeading||step==1&&rukaHeading||step==2&&mionHeading)||!Integer.valueOf(expectedCooldown).equals(held.baseRemaining))return stop(frame,List.of(),"B4_SKILL_POSTCONDITION:"+step,time,seq);
            if(!postSaved){record.put("skillVerified_"+step,expectedCooldown);save(frame,List.of(),"b4-skill-verified",time,seq);postSaved=true;return waitFor("B4：使用後証拠の保存を完了");}
            UraDialogPolicy.Read read=UraDialogPolicy.read(nav.readUraDialog(frame),2712);
            if(read!=null&&vision.backControl(frame,read))return action(read.back,"B4：スキル確認を閉じる",this::advance);
            long mask=vision.rouletteMask(frame);if(vision.rouletteBoard(frame,mask)==null)return retry(frame,List.of(),"B4_POST_DIALOG_OR_BOARD_REQUIRED",time,seq);
            advance();return waitFor("B4：スキル使用を確認済み");
        }
        if(phase==7) {
            long mask=vision.rouletteMask(frame);byte[] board=vision.rouletteBoard(frame,mask);
            if(Long.bitCount(mask)!=1||board==null)return retry(frame,List.of(),"B4_ROULETTE_MASK_OR_BOARD_UNKNOWN",time,seq);
            if(!vision.gears(frame)||hpLowerBound<230000)return retry(frame,List.of(),"B4_ENEMY_AND_HP_REQUIRED",time,seq);
            try{plan=RoulettePlan.solveEsper(board,mask,44,1200,5000,now());}catch(IllegalArgumentException e){return stop(frame,List.of(),"B4_NO_ROBUST_ROUTE:"+e.getMessage(),time,seq);}
            record.put("rouletteMask",mask).put("sourceBoard",array(board)).put("route",new JSONArray(plan.path)).put("worstWater",plan.water).put("worstHeal",plan.heal).put("worstWaterT",plan.waterT).put("worstFirstWaveCombos",plan.combos).put("proof","ALL_TEN_COLORS_T_WATER2_HEAL_FIRST_WAVE_NO_ROULETTE_VISIT");
            phase=9;prepared=false;save(frame,List.of(),"b4-plan",time,seq);return waitFor("B4：全ルーレット色で水T字・水2セット＋回復を確認");
        }
        if(phase==9) {
            long mask=vision.rouletteMask(frame);byte[] board=vision.rouletteBoard(frame,mask);
            if(plan==null)return stop(frame,List.of(),"B4_PLAN_REQUIRED",time,seq);
            if(now()-plan.plannedAt>=15000){phase=7;return waitFor("B4：現在の盤面から経路を作り直す");}
            if(board==null)return waitFor("B4：ドロップの発光が収まるまで待機");
            record.put("dispatchMask",mask).put("dispatchBoard",array(board));
            if(!plan.current(board,mask,now()))return stop(frame,List.of(),"B4_STALE_ROULETTE_PLAN",time,seq);
            // The reviewed gear artwork is briefly covered by its own lightning animation.
            // A temporary enemy miss is not a changed board and must not discard a valid route.
            if(!vision.gears(frame))return waitFor("B4：敵の発光が収まった画面で再照合");
            if(!prepared){phase=10;record.put("attackConsumed",true);save(frame,List.of(),"b4-puzzle-consumed",time,seq);phase=9;prepared=true;return waitFor("B4：送信直前の固定マスを再照合");}
            StagePolicy.Decision d=action(new StagePolicy.Item("B4_PUZZLE",610,1700),"B4：ルーレットを避けて水2セット＋回復",()->{phase=10;actionAt=now();misses=0;});puzzle(d,plan.path);return d;
        }
        if(phase==10) {
            List<StagePolicy.Item> text=nav.readUraCombat(frame);if(UraCombatText.blocked(text))return stop(frame,text,"B4_GAME_OVER_OR_PURCHASE",time,seq);
            if(now()-actionAt<18000)return waitFor("B4：コンボ・敵行動・階層遷移を待機");
            round++;phase=0;misses=0;saved=false;save(frame,text,"b4-result",time,seq);return waitFor("B4：パズル後の実階層を確認");
        }
        if(phase==13) {
            StagePolicy.Decision d=action(new StagePolicy.Item("B4_MION_READY",1115,1425),"B4：ミオンの残りターンを長押し確認",()->{phase=14;misses=0;});d.holdMs=4000;
            d.heldStampedFrame=(image,current,t,s)->{List<StagePolicy.Item> text=nav.readUraHeldSkill(image);UraHeldSkillInfo info=UraHeldSkillInfo.readBase(text);if(!current.getAsBoolean())return;held=info;heldAt=t;heldSequence=s;mionHeading=vision.distance(image,"post-mion-skill-header.png",130,230,770,50)<.025;record.put("reviewedMionHeading",mionHeading).put("mionObservedRemaining",info==null||info.baseRemaining==null?JSONObject.NULL:info.baseRemaining);save(image,text,"b4-mion-ready",t,s);};return d;
        }
        if(phase==14) {
            if(held==null||!(held.baseNamed(SKILLS[2])||mionHeading)||held.baseRemaining==null)return stop(frame,List.of(),"B4_MION_READINESS_UNKNOWN",time,seq);
            if(held.baseRemaining==0){phase=3;step=2;return waitFor("B4：ミオンの使用可能を確認");}
            if(held.baseRemaining!=1||extraCharged)return stop(frame,List.of(),"B4_MION_COOLDOWN_UNEXPECTED",time,seq);
            waitingMion=true;phase=15;prepared=false;return waitFor("B4：今のHPで敵行動に耐えられることを確認してあと1ターンを溜める");
        }
        if(phase==15) {
            long mask=vision.rouletteMask(frame);byte[] board=vision.rouletteBoard(frame,mask);
            boolean counters=true;for(int i=0;i<3;i++)counters&=vision.distance(frame,"b4-counter-wait-"+i+".png",112+i*380,730,45,55)<.09;
            if(Long.bitCount(mask)!=1||board==null||!vision.gears(frame)||!counters||hpLowerBound<230000)return retry(frame,List.of(),"B4_EXTRA_CHARGE_HP_AND_COUNTERS_1_4_1_REQUIRED",time,seq);
            misses=0;
            List<Integer> route=RoulettePlan.chargeRoute(board,mask);record.put("extraChargeRoute",new JSONArray(route));
            if(!prepared){extraCharged=true;phase=10;record.put("extraChargeUnshieldedMaxDamage",201700);save(frame,List.of(),"b4-extra-charge-consumed",time,seq);extraCharged=false;phase=15;prepared=true;return waitFor("B4：HP下限23万が左右の合計20万1700を超えることを再照合");}
            StagePolicy.Decision d=action(new StagePolicy.Item("B4_EXTRA_CHARGE",610,1700),"B4：必ず1コンボ消してミオンをあと1ターン溜める",()->{extraCharged=true;phase=10;actionAt=now();misses=0;});puzzle(d,route);return d;
        }
        return stop(frame,List.of(),"B4_UNKNOWN_STATE",time,seq);
    }
    private void advance(){
        if(step==3&&waitingMion&&!extraCharged){phase=15;prepared=false;}
        else if(step==2){phase=7;}
        else {step++;phase=step>=4?7:3;}
        misses=0;
    }
    private static void puzzle(StagePolicy.Decision d,List<Integer> route){d.puzzlePath=route;d.puzzleCols=6;d.puzzleRows=5;d.puzzleRect=BoardGeometry.calculate(1220,2712,6,5,0,84);d.puzzleDurationMs=4500;d.puzzlePreciseStart=true;}
    private static JSONObject read(File f)throws Exception{return new JSONObject(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));}
    private static JSONArray array(byte[] b){JSONArray a=new JSONArray();for(byte v:b)a.put(v);return a;}
    private static long now(){return android.os.SystemClock.elapsedRealtime();}
    private StagePolicy.Decision waitFor(String s){StagePolicy.Decision d=new StagePolicy.Decision(null,s,false);d.nextFrameDelayMs=350;return d;}
    private StagePolicy.Decision action(StagePolicy.Item target,String s,Runnable done){StagePolicy.Decision d=new StagePolicy.Decision(target,s,false);d.holdMs=160;d.completed=done;return d;}
    private StagePolicy.Decision retry(Bitmap f,List<StagePolicy.Item> text,String s,long t,long q)throws Exception{if(++misses<12)return waitFor(s+"（再読込"+misses+"/12）");return stop(f,text,s,t,q);}
    private StagePolicy.Decision stop(Bitmap f,List<StagePolicy.Item> text,String s,long t,long q)throws Exception{record.put("failureReason",s);save(f,text,"b4-stop",t,q);return new StagePolicy.Decision(null,s,true);}
    private void save(Bitmap f,List<StagePolicy.Item> text,String name,long time,long seq)throws Exception {
        record.put("phase",phase).put("round",round).put("step",step).put("charged",charged).put("waitingMion",waitingMion).put("extraCharged",extraCharged).put("expectedCooldown",expectedCooldown).put("capturedAt",time).put("sequence",seq);
        JSONArray lines=new JSONArray();for(var i:text)lines.put(new JSONObject().put("text",i.rawText).put("x",i.x).put("y",i.y));record.put("ocr",lines);
        File dir=new File(context.getExternalFilesDir("Download"),"ura-runtime");if(!dir.exists()&&!dir.mkdirs())throw new IOException("B4 evidence unavailable");String prefix=name+"-"+System.currentTimeMillis();
        try(OutputStream out=new FileOutputStream(new File(dir,prefix+".png"))){f.compress(Bitmap.CompressFormat.PNG,100,out);}
        Files.write(new File(dir,prefix+".json").toPath(),record.toString(2).getBytes(StandardCharsets.UTF_8));
        try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"ura-b4-state.png"))){f.compress(Bitmap.CompressFormat.PNG,100,out);}
        Files.write(new File(context.getFilesDir(),"ura-b4-state.json").toPath(),record.toString(2).getBytes(StandardCharsets.UTF_8));android.util.Log.i("PADB4",record.toString());
    }
}
