package com.example.padautosolver;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Actual floor -> named skill -> fresh cooldown/HP -> one puzzle -> actual floor. */
final class UraB3Controller {
    private static final int[] SLOTS={1,0,4,5};
    private static final String[] SKILLS={"フィーリングガーデン","デッドリースペードエッジ","ダブル防御態勢水","ブリリアントコンチェルト"};
    private static final int[] COOLDOWNS={4,5,5,2};
    private final Context context;
    private final UraBattleVision vision;
    private final JSONObject record=new JSONObject();
    private volatile int phase;
    private int round,step,misses;
    private long lastSequence=-1,actionAt,actionSequence,menuAt;
    private boolean saved,prepared;
    private boolean postSaved;
    private volatile UraHeldSkillInfo held;
    private volatile boolean heldSekkaHeading;
    private volatile long heldAt,heldSequence;
    private Integer sekkaCooldown,esperCooldown;
    private UraPuzzlePlan plan;
    private UraB3Controller(Context context){this.context=context;vision=new UraBattleVision(context);}
    static UraB3Controller resume(Context context,Bitmap live)throws Exception {
        if(live.getWidth()!=1220||live.getHeight()!=2712)return null;
        UraB3Controller c=new UraB3Controller(context);
        File b2=new File(context.getFilesDir(),"ura-lucifer-state.json"),b2image=new File(context.getFilesDir(),"ura-lucifer-state.png");
        JSONObject b2Record=b2.isFile()?new JSONObject(new String(Files.readAllBytes(b2.toPath()),StandardCharsets.UTF_8)):null;
        File state=new File(context.getFilesDir(),"ura-b3-state.json"),picture=new File(context.getFilesDir(),"ura-b3-state.png");
        if(state.isFile()&&picture.isFile()) {
            JSONObject data=new JSONObject(new String(Files.readAllBytes(state.toPath()),StandardCharsets.UTF_8));
            boolean sameSource=b2Record!=null&&data.optString("sourceB2RunId").equals(b2Record.optString("runId"));
            Bitmap old=BitmapFactory.decodeFile(picture.getPath());boolean same=false;
            try{
                byte[] liveBoard=c.vision.luciferBoard(live),oldBoard=old==null?null:c.vision.luciferBoard(old);
                if(oldBoard==null&&data.optInt("round")==0&&data.optInt("phase")>=5&&data.optInt("phase")<=6)
                    oldBoard=bytes(data.optJSONArray("checkpointBoard"));
                same=old!=null&&UraBattleVision.sameRunPortraits(old,live)&&Arrays.equals(oldBoard,liveBoard)&&liveBoard!=null;
            }finally{if(old!=null)old.recycle();}
            if(sameSource&&same&&data.optInt("phase")!=11&&data.optInt("phase")!=12) {
                c.restore(data);int p=c.phase;
                // Consumed actions recover observation only. Never replay skill/puzzle dispatch.
                if(p==5||p==6)c.phase=5;
                else if(p==9&&data.optBoolean("puzzleConsumed"))c.phase=10;
                else if(p==9)c.phase=8;
                else if(p==0||p==1)c.phase=0;
                else if(p==4)c.phase=3;
                c.actionAt=now()-18000;c.record.remove("failureReason");return c;
            }
            // Once this native run consumed an action, the older B2 checkpoint cannot
            // authorize a fresh B3 opening (and duplicate the recovery skill).
            if(sameSource&&"B3_LEONIS".equals(data.optString("mode")))return null;
        }
        if(!c.vision.leonis(live)||!UraB3Policy.allDark(c.vision.luciferBoard(live)))return null;
        if(!b2.isFile()||!b2image.isFile())return null;
        JSONObject data=b2Record;
        if(data.optInt("phase")!=7||data.optInt("observedFloor")!=3||data.optInt("trial")!=2||!"ALL".equals(data.optString("instruction")))return null;
        Bitmap old=BitmapFactory.decodeFile(b2image.getPath());boolean same=false;
        try{same=old!=null&&UraBattleVision.sameRunPortraits(old,live)&&Arrays.equals(c.vision.luciferBoard(old),c.vision.luciferBoard(live));}finally{if(old!=null)old.recycle();}
        if(!same)return null;
        c.record.put("runId",UUID.randomUUID().toString()).put("sourceB2RunId",data.getString("runId")).put("mode","B3_LEONIS");return c;
    }
    private void restore(JSONObject data)throws Exception {
        for(Iterator<String> it=data.keys();it.hasNext();){String key=it.next();record.put(key,data.get(key));}
        phase=data.getInt("phase");round=data.optInt("round");step=data.optInt("step");
        if(!data.isNull("sekkaCooldown"))sekkaCooldown=data.getInt("sekkaCooldown");
        if(!data.isNull("esperCooldown"))esperCooldown=data.getInt("esperCooldown");
    }
    StagePolicy.Decision inspect(Bitmap frame,StageNavigator nav,long time,long seq)throws Exception {
        if(frame.getWidth()!=1220||frame.getHeight()!=2712)return stop(frame,List.of(),"B3_CALIBRATION_MISMATCH",time,seq);
        if(seq<=lastSequence)return waitFor("B3：新しい画面を待機");lastSequence=seq;
        if(phase==0||phase==1) {
            List<StagePolicy.Item> text=nav.readUraCombat(frame);
            if(UraCombatText.blocked(text))return stop(frame,text,"B3_GAME_OVER_OR_PURCHASE",time,seq);
            if(phase==0) {
                StagePolicy.Item menu=UraCombatText.control(text,"MENU",500,620);
                if(menu==null)return retry(frame,text,"B3_MENU_REQUIRED",time,seq);
                return action(menu,"B3：実際の階層を確認",()->{phase=1;menuAt=now();misses=0;saved=false;});
            }
            int floor=UraCombatText.floor(text);
            if(floor<0||!UraCombatText.joined(text).contains("裏魔門の守護者")) {
                if(now()-menuAt<3000)return waitFor("B3：メニュー表示を待機");
                StagePolicy.Item menu=UraCombatText.control(text,"MENU",500,620);
                if(menu!=null&&misses++<3)return action(menu,"B3：遷移後のメニューを再確認",()->menuAt=now());
                return retry(frame,text,"B3_FLOOR_REQUIRED",time,seq);
            }
            if(floor!=3&&floor!=4)return stop(frame,text,"B3_UNEXPECTED_FLOOR:"+floor,time,seq);
            if(floor==4&&round==0)return stop(frame,text,"B3_UNEXPECTED_EARLY_FLOOR4",time,seq);
            record.put("observedFloor",floor);
            if(!saved){save(frame,text,"b3-floor",time,seq);saved=true;return waitFor("B3：階層記録後の新しい画面を待機");}
            StagePolicy.Item back=UraCombatText.control(text,"戻る",2080,2220);
            if(back==null)return retry(frame,text,"B3_MENU_BACK_REQUIRED",time,seq);
            return action(back,"B3：戦闘画面へ戻る",()->{phase=floor==4?12:2;misses=0;saved=false;});
        }
        if(phase==12)return stop(frame,List.of(),"B3_CLEAR_VERIFIED_B4_CAPTURE_REQUIRED",time,seq);
        if(phase==2) {
            if(!vision.leonis(frame))return retry(frame,List.of(),"B3_LEONIS_REQUIRED",time,seq);
            byte[] board=vision.luciferBoard(frame);if(board==null)return retry(frame,List.of(),"B3_BOARD_UNKNOWN",time,seq);
            record.put("checkpointBoard",array(board));
            if(round==0) {
                if(!UraB3Policy.allDark(board)||!vision.leonisFullHealth(frame))return stop(frame,List.of(),"B3_OPENING_ALL_DARK_AND_FULL_ENEMY_HP_REQUIRED",time,seq);
                step=0;
            } else {
                if(round>1)return stop(frame,List.of(),"B3_ATTACK_NOT_CLEARED",time,seq);
                step=UraB3Policy.needsRuka(board)?2:3;
            }
            phase=3;misses=0;return waitFor("B3：現在盤面に合わせてスキルを確認");
        }
        if(phase==3) return action(new StagePolicy.Item("B3_SKILL_"+step,100+203*SLOTS[step],1425),"B3："+SKILLS[step]+"の本体ボタンを確認",()->{phase=4;misses=0;prepared=false;});
        if(phase==4) {
            List<StagePolicy.Item> text=nav.readUraDialog(frame);UraDialogPolicy.Read read=UraDialogPolicy.read(text,2712);
            if(read==null||read.layer!=1||!read.named(SKILLS[step])||!vision.backControl(frame,read)||!vision.active(frame,read.activate))
                return retry(frame,text,"B3_SKILL_NOT_READY_OR_IDENTITY:"+step,time,seq);
            // Persist consumed observation state before dispatch, so an interrupted call cannot replay.
            if(!prepared){phase=5;save(frame,text,"b3-skill-consumed",time,seq);phase=4;prepared=true;return waitFor("B3：スキル証拠保存後に再照合");}
            return action(read.activate,"B3："+SKILLS[step]+"を発動",()->{phase=5;actionAt=now();actionSequence=seq;misses=0;held=null;});
        }
        if(phase==5) {
            if(now()-actionAt<1500)return waitFor("B3：スキル演出を待機");
            StagePolicy.Decision d=action(new StagePolicy.Item("B3_POST_"+step,100+203*SLOTS[step],1425),"B3：使用後のスキル残りターンを取得",()->{phase=6;misses=0;postSaved=false;});
            d.holdMs=4000;d.heldStampedFrame=(image,current,t,s)->{
                List<StagePolicy.Item> text=nav.readUraHeldSkill(image);UraHeldSkillInfo info=UraHeldSkillInfo.read(text);
                if(!current.getAsBoolean())return;
                held=info;heldAt=t;heldSequence=s;
                heldSekkaHeading=step==0&&vision.sekkaPostHeading(image);
                record.put("postSkill",info==null?JSONObject.NULL:new JSONObject().put("name",info.baseName).put("remaining",info.baseRemaining));
                record.put("sekkaHeadingPixelsVerified",heldSekkaHeading);
                save(image,text,"b3-held",t,s);
            };return d;
        }
        if(phase==6) {
            if(held==null)return retry(frame,List.of(),"B3_HELD_POST_REQUIRED",time,seq);
            if(heldAt<=actionAt||heldSequence<=actionSequence||!(held.baseNamed(SKILLS[step])||step==0&&heldSekkaHeading)||!Integer.valueOf(COOLDOWNS[step]).equals(held.baseRemaining))
                return stop(frame,List.of(),"B3_SKILL_POSTCONDITION:"+step,time,seq);
            if(step==0)sekkaCooldown=held.baseRemaining;if(step==1)esperCooldown=held.baseRemaining;
            if(!postSaved){save(frame,List.of(),"b3-skill-verified",time,seq);postSaved=true;return waitFor("B3：使用後証拠保存後の新しい画面を待機");}
            UraDialogPolicy.Read read=UraDialogPolicy.read(nav.readUraDialog(frame),2712);
            if(read!=null&&vision.backControl(frame,read))return action(read.back,"B3：使用後のスキルを確認済み",()->advanceSkill());
            if(vision.luciferBoard(frame)==null)return retry(frame,List.of(),"B3_POST_DIALOG_OR_BOARD_REQUIRED",time,seq);
            advanceSkill();return waitFor("B3：スキル使用を実機で確認");
        }
        if(phase==7) {
            List<StagePolicy.Item> hpText=nav.readUraHp(frame);int[] hp=UraB3Policy.hp(hpText);
            int lowerBound=vision.b3HpLowerBound(frame);
            boolean recoveredPixels=round==0&&vision.recoveredB3Hp(frame);
            if(hp==null&&recoveredPixels)hp=new int[]{246863,611045};
            // After the recovery checkpoint, a conservative fill measurement may
            // authorize only the minimum-HP gate; it is not recorded as an exact HP read.
            if(hp==null&&round==1&&record.optInt("maxHp")==611045&&lowerBound>=60000)hp=new int[]{lowerBound,611045};
            record.put("hpLowerBound",lowerBound).put("recoveredHpPixelsVerified",recoveredPixels).put("hpEvidence",recoveredPixels?"REVIEWED_246863_PIXELS":UraB3Policy.hp(hpText)==null?"CONTIGUOUS_BAR_LOWER_BOUND":"OCR");
            if(round==0&&(!UraB3Policy.recoveryVerified(hp,sekkaCooldown,esperCooldown)||lowerBound<230000))return retry(frame,hpText,"B3_RECOVERY_HP_REQUIRED",time,seq);
            if(round==1&&(hp==null||hp[0]<60000))return retry(frame,hpText,"B3_ATTACK_HP_REQUIRED",time,seq);
            record.put("hp",hp[0]).put("maxHp",hp[1]);phase=8;misses=0;save(frame,hpText,"b3-hp-verified",time,seq);return waitFor("B3：HPを確認して盤面を探索");
        }
        if(phase==8) {
            if(!vision.leonis(frame))return stop(frame,List.of(),"B3_PUZZLE_ENEMY_CHANGED",time,seq);
            byte[] board=vision.luciferBoard(frame);if(board==null)return retry(frame,List.of(),"B3_BOARD_UNKNOWN",time,seq);
            PuzzleGoal goal=round==0?new PuzzleGoal(PuzzleGoal.Type.FULL_CLEAR,4,0):PuzzleGoal.waterAndHeal();
            if(round==0&&!UraB3Policy.allDark(board))return stop(frame,List.of(),"B3_REFRESH_BOARD_CHANGED",time,seq);
            PuzzleSolver.Result result=round==0?new PuzzleSolver.Result(List.of(0,1,0),1,30,2,0,0,true):PuzzleSolver.solve(board,6,5,44,2500,1800,goal);
            try{plan=new UraPuzzlePlan(board,result,goal,now());}catch(IllegalArgumentException e){return stop(frame,List.of(),"B3_NO_VALID_ROUTE:"+e.getMessage(),time,seq);}
            record.put("sourceBoard",array(board)).put("route",new JSONArray(plan.path)).put("goal",goal.type.name()).put("predictedCombos",plan.stats.combos)
                .put("waterCombos",plan.stats.colorCombos[3]).put("healCombos",plan.stats.colorCombos[5]).put("puzzleConsumed",false);
            phase=9;prepared=false;save(frame,List.of(),"b3-plan",time,seq);
            // The panel's expanded route preview covers Leonis' face on this device.
            // Keep the immutable dry-run evidence in the native log and leave that ROI visible.
            return waitFor(round==0?"B3：回復・軽減後に闇30個を消して盤面更新":"B3：水2セット＋回復の経路を再照合");
        }
        if(phase==9) {
            byte[] board=vision.luciferBoard(frame);
            if(board==null&&plan!=null&&now()-plan.plannedAt<15000)return waitFor("B3：盤面の発光が収まるまで待機");
            if(plan==null||!plan.current(board,now())||!vision.leonis(frame))return stop(frame,List.of(),"B3_STALE_PLAN",time,seq);
            if(!prepared){phase=10;record.put("puzzleConsumed",true);save(frame,List.of(),"b3-puzzle-consumed",time,seq);phase=9;prepared=true;return waitFor("B3：送信前記録後の盤面を再照合");}
            StagePolicy.Decision d=action(new StagePolicy.Item("B3_PUZZLE",610,1700),"B3："+(round==0?"全闇盤面を更新":"水2セット＋回復を実行"),()->{phase=10;actionAt=now();misses=0;});
            d.puzzlePath=plan.path;d.puzzleCols=6;d.puzzleRows=5;d.puzzleRect=BoardGeometry.calculate(1220,2712,6,5,0,84);d.puzzleDurationMs=3500;d.puzzlePreciseStart=true;return d;
        }
        if(phase==10) {
            List<StagePolicy.Item> text=nav.readUraCombat(frame);
            if(UraCombatText.blocked(text))return stop(frame,text,"B3_GAME_OVER_OR_PURCHASE",time,seq);
            if(now()-actionAt<18000)return waitFor("B3：コンボ・敵行動・次階層を待機");
            round++;phase=0;misses=0;save(frame,text,"b3-result",time,seq);return waitFor("B3：パズル後の実階層を確認");
        }
        return stop(frame,List.of(),"B3_UNKNOWN_STATE",time,seq);
    }
    private void advanceSkill(){phase=(step==0||step==2)?3:7;if(step==0)step=1;else if(step==2)step=3;misses=0;}
    private static JSONArray array(byte[] b){JSONArray a=new JSONArray();for(byte x:b)a.put(x);return a;}
    private static byte[] bytes(JSONArray a)throws Exception{if(a==null)return null;byte[] b=new byte[a.length()];for(int i=0;i<b.length;i++)b[i]=(byte)a.getInt(i);return b;}
    private static long now(){return android.os.SystemClock.elapsedRealtime();}
    private StagePolicy.Decision waitFor(String s){StagePolicy.Decision d=new StagePolicy.Decision(null,s,false);d.nextFrameDelayMs=350;return d;}
    private StagePolicy.Decision action(StagePolicy.Item item,String s,Runnable done){StagePolicy.Decision d=new StagePolicy.Decision(item,s,false);d.holdMs=160;d.completed=done;return d;}
    private StagePolicy.Decision retry(Bitmap f,List<StagePolicy.Item> t,String s,long time,long seq)throws Exception{if(++misses<8)return waitFor(s+"（再読込"+misses+"/8）");return stop(f,t,s,time,seq);}
    private StagePolicy.Decision stop(Bitmap f,List<StagePolicy.Item> t,String s,long time,long seq)throws Exception{record.put("failureReason",s);save(f,t,"b3-stop",time,seq);return new StagePolicy.Decision(null,s,true);}
    private void save(Bitmap f,List<StagePolicy.Item> text,String name,long time,long seq)throws Exception {
        record.put("phase",phase).put("round",round).put("step",step).put("sekkaCooldown",sekkaCooldown==null?JSONObject.NULL:sekkaCooldown).put("esperCooldown",esperCooldown==null?JSONObject.NULL:esperCooldown).put("capturedAt",time).put("sequence",seq);
        JSONArray lines=new JSONArray();for(var i:text)lines.put(new JSONObject().put("text",i.rawText).put("x",i.x).put("y",i.y));record.put("ocr",lines);
        File dir=new File(context.getExternalFilesDir("Download"),"ura-runtime");if(!dir.exists()&&!dir.mkdirs())throw new IOException("B3 evidence unavailable");
        String prefix=name+"-"+System.currentTimeMillis();
        try(OutputStream out=new FileOutputStream(new File(dir,prefix+".png"))){f.compress(Bitmap.CompressFormat.PNG,100,out);}
        Files.write(new File(dir,prefix+".json").toPath(),record.toString(2).getBytes(StandardCharsets.UTF_8));
        try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"ura-b3-state.png"))){f.compress(Bitmap.CompressFormat.PNG,100,out);}
        Files.write(new File(context.getFilesDir(),"ura-b3-state.json").toPath(),record.toString(2).getBytes(StandardCharsets.UTF_8));
        android.util.Log.i("PADB3",record.toString());
    }
}
