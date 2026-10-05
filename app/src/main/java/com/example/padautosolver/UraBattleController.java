package com.example.padautosolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.nio.file.Files;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Per-run B1 protocol: fresh modal evidence, one action, verified postcondition. */
final class UraBattleController {
    private static final int[] SLOTS={4,0,1,3,5,2};
    private static final String[] BEFORE={"かつての水柱","エースオブスペード","月華咲乱ユキノシタ","雪雲の一変","ブリリアントアンサンブル","神泉槍グングニール"};
    private static final String[] AFTER={"ダブル防御態勢水","デッドリースペードエッジ","フィーリングガーデン","雪花の氷乱","ブリリアントコンチェルト","神泉槍グングニール"};
    private static final String[] BEFORE_ACTOR={"聖洋の新婦ルカ","スペードAを宿す者エスペル","ユキノシタの星光華","雪女の妖人ユキネ","奏龍楽士ミオン","賢泉の秘術神オーディン"};
    private static final String[] AFTER_ACTOR={"聖洋の新婦ルカ","スペードAの騎士エスペル","ユキノシタの情星霊セッカ","銀雪の雪女ユキネ","旋律の奏龍楽士ミオン","賢泉の秘術神オーディン"};
    private final Context context;
    private final UraBattleVision vision;
    private final int helperAssistTotal;
    private final boolean dryOnly;
    private volatile int phase;
    private int step,misses,stable;
    private volatile long enteredAt;
    private long lastSequence=-1,actionSequence=-1,actionAt;
    private byte[] boardBefore;
    private Integer monitorBefore;
    private UraHeldSkillInfo heldSkill;
    private boolean heldSekkaPostHeading;
    private boolean hasteEvidenceSaved;
    private UraPuzzlePlan puzzlePlan;
    private long buffsVerifiedAt;
    private long heldAt,heldSequence;
    private final Integer[] lastAssistRemaining=new Integer[6];
    private final JSONObject run=new JSONObject();
    private final UraInstructionCapture instructionCapture=new UraInstructionCapture();
    void captureInstruction(Bitmap image,long time,long sequence) {
        if(phase==17&&time>=enteredAt&&time-enteredAt<=45000)instructionCapture.capture(image,time,sequence);
    }
    void close(){instructionCapture.close();}
    UraBattleController(Context context,int helperAssistTotal,boolean dryOnly)throws Exception {
        this.context=context;this.vision=new UraBattleVision(context);this.helperAssistTotal=helperAssistTotal;this.dryOnly=dryOnly;
        run.put("runId",UUID.randomUUID().toString()).put("startedAt",System.currentTimeMillis()).put("helperAssistTotal",helperAssistTotal).put("dryOnly",dryOnly);
    }
    static UraBattleController readOnlyB1(Context context)throws Exception {
        UraBattleController c=new UraBattleController(context,0,true);
        c.phase=1;c.enteredAt=now();return c;
    }
    static UraBattleController currentB1(Context context,Bitmap frame)throws Exception {
        UraBattleController c=new UraBattleController(context,0,false);
        if(frame.getWidth()!=1220||frame.getHeight()!=2712||!c.vision.initialTeam(frame)||!c.vision.openingNotStarted(frame)||c.vision.enemyDistance(frame)>.055)return null;
        byte[] board=c.vision.board(frame);
        if(board==null||count(board,3)+count(board,5)==30)return null; // Could be an already consumed Ruka assist; require its native post-action recovery.
        c.phase=1;c.enteredAt=now();c.run.put("entryValidationPolicy","USER_REQUESTED_MION_ONLY_CURRENT_B1");return c;
    }
    static boolean isB1(Context context,Bitmap frame)throws Exception {
        if(frame.getWidth()!=1220||frame.getHeight()!=2712)return false;
        UraBattleVision v=new UraBattleVision(context);return v.enemyDistance(frame)<.055;
    }
    static UraBattleController resumePausedB1(Context context,Bitmap live)throws Exception {
        UraBattleController postRecovery=recoverNativePost(context,live);
        if(postRecovery!=null)return postRecovery;
        File stateFile=new File(context.getFilesDir(),"ura-b1-paused.json");
        File picture=new File(context.getFilesDir(),"ura-b1-paused.png");
        if(!stateFile.isFile()||!picture.isFile()||System.currentTimeMillis()-stateFile.lastModified()>3600000)return null;
        JSONObject state=new JSONObject(new String(Files.readAllBytes(stateFile.toPath()),StandardCharsets.UTF_8));
        if(state.getBoolean("dryOnly")||!state.optBoolean("preflightPassedThisRun")||state.getInt("phase")<2)return null;
        Bitmap previous=BitmapFactory.decodeFile(picture.getPath());
        if(previous==null)return null;
        boolean same;
        try {
            if(previous.getWidth()!=live.getWidth()||previous.getHeight()!=live.getHeight())return null;
            Bitmap a=Bitmap.createBitmap(previous,24,760,1172,820),b=Bitmap.createBitmap(live,24,760,1172,820);
            Bitmap sa=Bitmap.createScaledBitmap(a,96,160,true),sb=Bitmap.createScaledBitmap(b,96,160,true);
            UraBattleVision vision=new UraBattleVision(context);byte[] oldBoard=vision.board(previous),liveBoard=vision.board(live);
            boolean boardMatches=oldBoard!=null&&liveBoard!=null&&Arrays.equals(oldBoard,liveBoard);
            // A paused post-puzzle menu covers the special-orb board. Restore only menu
            // navigation; phase 18 reads the actual floor again before returning to B2.
            boolean verifiedB2Menu=state.getInt("phase")==18&&state.optInt("observedFloorAfterPuzzle")==2;
            same=(boardMatches||verifiedB2Menu)
                    &&TeamIconMatch.distance(UraBattleVision.pixels(sa),UraBattleVision.pixels(sb))<.025&&UraBattleVision.sameRunPortraits(previous,live);
            sa.recycle();sb.recycle();a.recycle();b.recycle();
        }finally{previous.recycle();}
        if(!same)return null;
        UraBattleController c=new UraBattleController(context,state.getInt("helperAssistTotal"),false);
        for(Iterator<String> keys=state.keys();keys.hasNext();) {String key=keys.next();c.run.put(key,state.get(key));}
        c.run.remove("failureReason");c.run.remove("heldSkill");c.run.remove("heldAt");c.run.remove("heldSequence");
        int old=state.getInt("phase");c.phase=old==3?2:old==5?4:old==7?6:old==9?8:old;
        if(c.phase>=11&&c.phase<=16)c.phase=11; // Buffs and route must be captured again after pause.
        c.step=state.getInt("step");c.enteredAt=c.actionAt=now();c.actionSequence=-1;
        c.monitorBefore=state.isNull("monitorBefore")?null:state.getInt("monitorBefore");
        JSONArray remaining=state.getJSONArray("lastAssistRemaining");
        for(int i=0;i<6;i++)c.lastAssistRemaining[i]=remaining.isNull(i)?null:remaining.getInt(i);
        c.run.put("resumedAt",System.currentTimeMillis()).put("resumePermission","Explicit start; same paused battle image; skill evidence will be captured anew");
        return c;
    }
    /** Recover only a post-action observation, never an activation, from this app's native log. */
    private static UraBattleController recoverNativePost(Context context,Bitmap live)throws Exception {
        if(live.getWidth()!=1220||live.getHeight()!=2712)return null;
        File dir=new File(context.getExternalFilesDir("Download"),"ura-runtime");
        File[] logs=dir.listFiles((d,n)->(n.startsWith("stop-")||n.startsWith("held-6-"))&&n.endsWith(".json"));if(logs==null)return null;
        Arrays.sort(logs,Comparator.comparingLong(File::lastModified).reversed());
        UraBattleVision vision=new UraBattleVision(context);byte[] liveBoard=vision.board(live);if(liveBoard==null){android.util.Log.i("PADSolver","resume: live board unreadable");return null;}
        for(File file:logs) {
            if(System.currentTimeMillis()-file.lastModified()>3600000)continue;
            JSONObject state=new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
            int phase=state.optInt("phase"),step=state.optInt("step");
            if(state.optBoolean("dryOnly",true)||!(phase==6||phase==7||phase==8||phase==9)||step<0||step>5)continue;
            if(!state.optBoolean("preflightPassedThisRun")&&!state.optString("entryValidationPolicy").equals("USER_REQUESTED_MION_ONLY_CURRENT_B1"))continue;
            Bitmap previous=BitmapFactory.decodeFile(new File(dir,file.getName().replace(".json",".png")).getPath());if(previous==null)continue;
            boolean same=false;
            try {
                byte[] previousBoard=vision.board(previous);if(previousBoard==null||!Arrays.equals(previousBoard,liveBoard)){android.util.Log.i("PADSolver","resume: board mismatch "+file.getName());continue;}
                // The blue held tooltip ends above this region. Compare the enemy
                // lower body, HP bars and current portraits, excluding the tooltip.
                Bitmap a=Bitmap.createBitmap(previous,24,1060,1172,520),b=Bitmap.createBitmap(live,24,1060,1172,520);
                Bitmap sa=Bitmap.createScaledBitmap(a,96,112,true),sb=Bitmap.createScaledBitmap(b,96,112,true);
                double distance=TeamIconMatch.distance(UraBattleVision.pixels(sa),UraBattleVision.pixels(sb));same=distance<.025&&UraBattleVision.sameRunPortraits(previous,live);
                android.util.Log.i("PADSolver","resume: battle distance="+distance+" "+file.getName());
                sa.recycle();sb.recycle();a.recycle();b.recycle();
            }finally{previous.recycle();}
            if(!same)continue;
            Integer baseline=null;
            if(step>=1&&step<=3) {
                File[] before=dir.listFiles((d,n)->n.startsWith("pre-action-"+step+"-")&&n.endsWith(".json"));
                if(before==null)continue;Arrays.sort(before,Comparator.comparingLong(File::lastModified).reversed());
                for(File evidence:before) {
                    JSONObject data=new JSONObject(new String(Files.readAllBytes(evidence.toPath()),StandardCharsets.UTF_8));
                    if(!data.optString("runId").equals(state.getString("runId")))continue;
                    JSONObject held=data.optJSONObject("heldSkill");
                    if(step>1&&held!=null&&UraDialogPolicy.clean(held.optString("actor")).contains(UraDialogPolicy.clean(AFTER_ACTOR[1]))&&!held.isNull("assistRemaining"))baseline=held.getInt("assistRemaining");
                    if(!data.isNull("monitorBefore"))baseline=data.getInt("monitorBefore");
                    break;
                }
                if(baseline==null){android.util.Log.i("PADSolver","resume: baseline missing "+step);continue;}
            }
            UraBattleController c=new UraBattleController(context,state.optInt("helperAssistTotal"),false);
            for(Iterator<String> keys=state.keys();keys.hasNext();){String key=keys.next();c.run.put(key,state.get(key));}
            c.run.remove("failureReason");c.run.remove("heldSkill");c.run.remove("heldAt");c.run.remove("heldSequence");
            c.phase=phase>=8?8:6;c.step=step;c.monitorBefore=baseline;c.actionAt=c.enteredAt=now();c.actionSequence=-1;
            JSONArray remaining=state.optJSONArray("lastAssistRemaining");
            if(remaining!=null)for(int i=0;i<6;i++)c.lastAssistRemaining[i]=remaining.isNull(i)?null:remaining.getInt(i);
            c.run.put("resumedAt",System.currentTimeMillis()).put("resumePermission","Same board and battle portraits; native post-action log; recapture postcondition before any further action");
            return c;
        }
        return null;
    }
    StagePolicy.Decision inspect(Bitmap frame,StageNavigator nav,long capturedAt,long sequence)throws Exception {
        if(frame.getWidth()!=1220||frame.getHeight()!=2712)return stop(frame,List.of(),"CALIBRATION_MISMATCH",capturedAt,sequence);
        if(sequence<=lastSequence)return waitFor("新しい画面を待機");lastSequence=sequence;
        if(phase==0) {
            List<StagePolicy.Item> lines=nav.readUraPreentry(frame);
            if(!UraScenePolicy.preentry(lines,2712))return retry(frame,lines,"PREENTRY_CAPTURE_REQUIRED",capturedAt,sequence);
            StagePolicy.Item entry=null;
            for(var line:lines)if(line.text.equals("挑戦する")&&line.y>2080&&line.y<2280)entry=line;
            if(entry==null)return retry(frame,lines,"ENTRY_CONTROL_REQUIRED",capturedAt,sequence);
            save(frame,lines,"before-entry",capturedAt,sequence);
            return action(entry,"ミオン確認済み：裏魔門へ潜入",()->{phase=1;enteredAt=now();reset();});
        }
        if(phase==1) {
            if(now()-enteredAt>25000)return stop(frame,List.of(),"B1_CAPTURE_REQUIRED: entry timeout",capturedAt,sequence);
            boolean helperMatched=vision.initialTeam(frame);double enemyDistance=vision.enemyDistance(frame);
            run.put("entryHelperMatched",helperMatched).put("entryEnemyDistance",enemyDistance);
            if(!helperMatched||enemyDistance>.055){
                List<StagePolicy.Item> blocked=nav.readUraCombat(frame);
                if(UraCombatText.blocked(blocked))return stop(frame,blocked,"PURCHASE_RECOVERY_OR_GAME_OVER",capturedAt,sequence);
                return waitFor("B1：敵とミオンの表示を確認中");
            }
            byte[] board=vision.board(frame);run.put("entryBoardKnown",board!=null);if(board==null)return waitFor("B1：未認識のドロップを再確認しています");
            if(++stable<2)return waitFor("B1：新しい2枚の画面で照合中");
            save(frame,List.of(),"b1-ready",capturedAt,sequence);phase=2;step=dryOnly?0:3;reset();
            return waitFor(dryOnly?"B1 Dry Run：予定スキル6件を確認（発動・パズルなし）":"B1：ユキネの残りターンを取得して実戦へ");
        }
        if(phase==10)return stop(frame,List.of(),"B1_DRY_RUN_COMPLETE（スキル・パズル未実行）",capturedAt,sequence);
        if(phase>=11)return puzzle(frame,nav,capturedAt,sequence);
        // Even phases open a modal; odd phases re-read that modal before any gesture.
        if(phase==2||phase==4||phase==6||phase==8) {
            if(phase==6&&sequence<=actionSequence)return waitFor("スキル使用後の新しい画面を待機");
            if(phase>=6&&now()-actionAt>30000)return stop(frame,List.of(),"POSTCONDITION_TIMEOUT:"+step,capturedAt,sequence);
            double enemy=vision.enemyDistance(frame);
            if(step<5&&enemy>.055)return retry(frame,List.of(),"B1_ENEMY_CAPTURE_REQUIRED:"+enemy,capturedAt,sequence);
            byte[] board=vision.board(frame);if(board==null)return retry(frame,List.of(),"BOARD_UNKNOWN",capturedAt,sequence);
            if(phase==2&&!vision.initialTeam(frame))return retry(frame,List.of(),"TeamProfileMismatch: B1 dry run",capturedAt,sequence);
            if(phase==4) {
                boardBefore=board;
                if(step==1&&count(board,3)<9)return stop(frame,List.of(),"ESPER_WATER_CONDITION",capturedAt,sequence);
                if(step==1)monitorBefore=lastAssistRemaining[3];
                if(step==2||step==3)monitorBefore=lastAssistRemaining[0];
            }
            if(phase==6&&step==0&&(count(board,3)!=15||count(board,5)!=15))
                return retry(frame,List.of(),"RUKA_BOARD_POSTCONDITION",capturedAt,sequence);
            int slot=phase==8?(step==1?3:0):SLOTS[step],next=phase+1;
            StagePolicy.Decision open=action(new StagePolicy.Item("URA_B1_OPEN_"+phase+"_"+step,100+slot*203,1425),
                (phase==2?"Dry Run：":phase==4?"使用前：":"使用後：")+"枠"+(slot+1)+"のスキルを照合",()->{phase=next;reset();});
            if(phase==4)return open;
            open.holdMs=4000;
            open.heldStampedFrame=(held,current,time,seq)->{
                List<StagePolicy.Item> text=nav.readUraHeldSkill(held);
                UraHeldSkillInfo info=UraHeldSkillInfo.read(text);
                if(!current.getAsBoolean())return;
                heldSkill=info;heldAt=time;heldSequence=seq;
                heldSekkaPostHeading=step==2&&vision.sekkaPostHeading(held);
                run.put("heldSkill",info==null?JSONObject.NULL:new JSONObject().put("actor",info.actorName).put("baseName",info.baseName).put("baseRemaining",info.baseRemaining).put("assistName",info.assistName).put("assistRemaining",info.assistRemaining))
                    .put("heldAt",time).put("heldSequence",seq);
                if(info!=null)run.getJSONObject("heldSkill").put("baseHeaderName",info.headerName);
                save(held,text,"held-"+phase+"-"+step,time,seq);
            };
            return open;
        }
        List<StagePolicy.Item> lines=nav.readUraDialog(frame);
        UraDialogPolicy.Read read=UraDialogPolicy.read(lines,2712);
        if(phase==3||phase==7||phase==9) {
            if(phase!=3||read==null) {
            if(heldSkill==null)return retry(frame,lines,"HELD_SKILL_CAPTURE_REQUIRED",capturedAt,sequence);
            if(phase==7||phase==9) {
                if(heldAt<=actionAt||heldSequence<=actionSequence)return stop(frame,lines,"STALE_POSTCONDITION",capturedAt,sequence);
            }
            if(phase==7)return ownPost(frame,lines,capturedAt,sequence);
            if(phase==9)return hastePost(frame,lines,read,capturedAt,sequence);
            if(!heldSkill.actorNamed(BEFORE_ACTOR[step])&&!heldSkill.baseNamed(step==0?AFTER[0]:BEFORE[step]))
                return stop(frame,lines,"DRY_SKILL_IDENTITY_MISMATCH:"+step,capturedAt,sequence);
            if(step==0&&!Integer.valueOf(0).equals(heldSkill.assistRemaining))return stop(frame,lines,"RUKA_ASSIST_NOT_READY",capturedAt,sequence);
            if(heldSkill.assistRemaining!=null)lastAssistRemaining[SLOTS[step]]=heldSkill.assistRemaining;
            if(read==null) {
                if(heldSkill.baseRemaining==null||heldSkill.baseRemaining<1||vision.enemyDistance(frame)>.055||vision.board(frame)==null)
                    return retry(frame,lines,"DRY_MODAL_OR_UNREADY_REQUIRED",capturedAt,sequence);
                int oldSlot=SLOTS[step];save(frame,lines,"dry-unready-"+step,capturedAt,sequence);advanceDry();return waitFor("Dry Run：枠"+(oldSlot+1)+"は残りターンあり");
            }
            }
        }
        if(read==null||!vision.backControl(frame,read))return retry(frame,lines,"SKILL_DIALOG_REQUIRED:"+step+":"+phase,capturedAt,sequence);
        boolean active=vision.active(frame,read.activate);
        Integer assist=UraDialogPolicy.assistRemaining(read);
        if(phase==3||phase==5) {
            if(read.layer!=(step==0?2:1)||!read.named(BEFORE[step]))return retry(frame,lines,"SKILL_IDENTITY_MISMATCH:"+step,capturedAt,sequence);
            if(phase==3) {
                if(step==0&&!active)return stop(frame,lines,"RUKA_ASSIST_NOT_READY",capturedAt,sequence);
                if(assist!=null)lastAssistRemaining[SLOTS[step]]=assist;
                run.put("dryStep"+step,new JSONObject().put("header",read.header).put("active",active).put("assistRemaining",assist==null?JSONObject.NULL:assist));
                if(stable++==0)save(frame,lines,"dry-step-"+step,capturedAt,sequence);
                return close(read,"Dry Run："+BEFORE[step]+(active?" 使用可能":" 現時点では使用不可"),()->{
                    advanceDry();
                });
            }
            if(!active)return retry(frame,lines,"SKILL_NOT_READY:"+step,capturedAt,sequence);
            if((step==1||step==2||step==3)&&monitorBefore==null)return stop(frame,lines,"HASTE_BASELINE_REQUIRED",capturedAt,sequence);
            if(stable++==0)save(frame,lines,"pre-action-"+step,capturedAt,sequence);
            return action(read.activate,"B1："+BEFORE[step]+"を1回発動",()->{phase=6;actionAt=now();actionSequence=sequence;reset();});
        }
        return stop(frame,lines,"B1_STATE_UNKNOWN",capturedAt,sequence);
    }
    private void advance(){step++;phase=step==6?11:4;}
    private StagePolicy.Decision puzzle(Bitmap frame,StageNavigator nav,long time,long seq)throws Exception {
        List<StagePolicy.Item> lines;
        if(phase==11) {
            if(vision.board(frame)==null||vision.enemyDistance(frame)>.055)return retry(frame,List.of(),"B1_PUZZLE_SCENE_REQUIRED",time,seq);
            lines=nav.readUraMenuControl(frame);
            StagePolicy.Item menu=UraCombatText.control(lines,"MENU",500,620);
            if(menu==null)return retry(frame,lines,"MENU_CONTROL_REQUIRED",time,seq);
            return action(menu,"B1：吸収無効の現在表示を確認",()->{phase=12;reset();});
        }
        if(phase==12||phase==14||phase==18) {
            lines=nav.readUraCombat(frame);int floor=UraCombatText.floor(lines);
            if(phase==18&&floor<0&&!UraCombatText.joined(lines).contains("リーダースキル確認")) {
                StagePolicy.Item menu=UraCombatText.control(lines,"MENU",500,620);
                if(menu!=null&&misses<3){misses++;return action(menu,"演出後に階層メニューを再確認",()->{});}
            }
            if(floor<0&&(phase==12||phase==14)&&vision.menuFloorOne(frame))floor=1;
            if(floor<1||!UraCombatText.joined(lines).contains("裏魔門の守護者"))return retry(frame,lines,"BATTLE_MENU_REQUIRED",time,seq);
            if(phase==12) {
                if(floor!=1)return stop(frame,lines,"UNEXPECTED_FLOOR:"+floor,time,seq);
                StagePolicy.Item status=UraCombatText.control(lines,"状況確認",1300,1450);
                if(status==null)return retry(frame,lines,"STATUS_CONTROL_REQUIRED",time,seq);
                return action(status,"B1：状況確認を開く",()->{phase=13;reset();});
            }
            if(phase==18){
                run.put("observedFloorAfterPuzzle",floor);
                if(!run.optBoolean("postPuzzleFloorSaved")) {
                    run.put("postPuzzleFloorSaved",true);save(frame,lines,"after-puzzle-floor",time,seq);
                    return waitFor("実階層の記録後、新しい画面で戻るを再確認");
                }
            }
            StagePolicy.Item back=UraCombatText.control(lines,"戻る",2080,2220);
            if(back==null)return retry(frame,lines,"MENU_BACK_REQUIRED",time,seq);
            final int next=phase==14?15:19;
            return action(back,"戦闘画面へ戻る",()->{phase=next;reset();});
        }
        if(phase==13) {
            lines=nav.readUraCombat(frame);
            if(!UraCombatText.absorptionsTwoTurns(lines))return retry(frame,lines,"ABSORPTION_VOID_NOT_VERIFIED",time,seq);
            StagePolicy.Item back=UraCombatText.control(lines,"戻る",1580,1720);
            if(back==null)return retry(frame,lines,"STATUS_BACK_REQUIRED",time,seq);
            buffsVerifiedAt=now();run.put("absorptionVoidVerified",true);save(frame,lines,"absorption-void-verified",time,seq);
            return action(back,"属性・ダメージ吸収無効2Tを確認",()->{phase=14;reset();});
        }
        if(phase==15) {
            byte[] board=vision.board(frame);if(board==null)return retry(frame,List.of(),"BOARD_UNKNOWN",time,seq);
            PuzzleGoal goal=PuzzleGoal.esperMionAndHeal();PuzzleSolver.Result result=PuzzleSolver.solve(board,6,5,44,2500,1800,goal);
            try{puzzlePlan=new UraPuzzlePlan(board,result,goal,now());}catch(IllegalArgumentException e){return stop(frame,List.of(),"NO_VALID_ROUTE:"+e.getMessage(),time,seq);}
            run.put("puzzleRoute",new JSONArray(puzzlePlan.path)).put("goal",goal.type.name()).put("firstWaterT",puzzlePlan.stats.firstTShapes[3])
                .put("predictedCombos",puzzlePlan.stats.combos).put("predictedWaterCombos",puzzlePlan.stats.colorCombos[3]).put("predictedHealCombos",puzzlePlan.stats.colorCombos[5]);
            boardBefore=board;save(frame,List.of(),"b1-puzzle-dry-run",time,seq);phase=16;reset();
            StagePolicy.Decision preview=waitFor("B1：水"+puzzlePlan.stats.colorCombos[3]+"セット＋回復"+puzzlePlan.stats.colorCombos[5]+"を予定／再照合後に実行");
            preview.previewPlan=puzzlePlan;return preview;
        }
        if(phase==16) {
            byte[] live=vision.board(frame);
            if(live==null&&puzzlePlan!=null&&now()-puzzlePlan.plannedAt<=15000&&now()-buffsVerifiedAt<=25000)
                return waitFor("B1：発光が収まった新しい盤面で経路を再確認");
            if(puzzlePlan==null||!puzzlePlan.current(live,now())||now()-buffsVerifiedAt>25000)return stop(frame,List.of(),"STALE_PUZZLE_OR_BUFF_EVIDENCE",time,seq);
            if(vision.enemyDistance(frame)>.055)return stop(frame,List.of(),"PUZZLE_SCENE_CHANGED",time,seq);
            StagePolicy.Decision d=action(new StagePolicy.Item("URA_B1_PUZZLE",610,1700),"B1：水T字・水2セット＋回復を連続ドラッグ",()->{enteredAt=now();phase=17;reset();});
            d.puzzlePath=puzzlePlan.path;d.puzzleCols=6;d.puzzleRows=5;d.puzzleRect=BoardGeometry.calculate(1220,2712,6,5,0,84);d.puzzleDurationMs=3000;return d;
        }
        if(phase==17) {
            instructionCapture.read(context,nav,run.getString("runId"),-1,"b1-lucifer-command");
            lines=nav.readUraCombat(frame);
            save(frame,lines,"puzzle-result-observation",time,seq);
            String observed=UraCombatText.joined(lines);
            if(UraCombatText.blocked(lines))return stop(frame,lines,"PURCHASE_RECOVERY_OR_GAME_OVER",time,seq);
            if(now()-enteredAt<30000){StagePolicy.Decision watch=waitFor("B1：コンボ・敵行動・次階層の実画面を記録中");watch.nextFrameDelayMs=250;return watch;}
            StagePolicy.Item menu=UraCombatText.control(lines,"MENU",500,620);
            if(menu==null){if(now()-enteredAt>45000)return stop(frame,lines,"POST_PUZZLE_CAPTURE_REQUIRED",time,seq);return waitFor("敵行動が終わるまで待機");}
            return action(menu,"パズル後の実階層を確認",()->{phase=18;reset();});
        }
        lines=nav.readUraCombat(frame);
        return stop(frame,lines,run.optInt("observedFloorAfterPuzzle")==2?"B1_CLEAR_VERIFIED_B2_CURRENT_INSTRUCTION_REQUIRED":"B1_NOT_CLEARED_CAPTURE_REQUIRED",time,seq);
    }
    private void advanceDry(){
        if(!dryOnly){step=0;phase=4;reset();return;}
        step++;reset();if(step==6){step=0;phase=10;}else phase=2;
    }
    private StagePolicy.Decision ownPost(Bitmap frame,List<StagePolicy.Item> lines,long time,long seq)throws Exception {
        boolean identity=heldSkill.baseNamed(AFTER[step])||heldSkill.actorNamed(AFTER_ACTOR[step]);
        if(!identity&&step==2&&heldSekkaPostHeading) {
            identity=true;run.put("postIdentityEvidence","REVIEWED_SEKKA_SKILL_HEADER_PIXELS");
        }
        if(!identity||!UraCooldownPostcondition.matches(step,heldSkill.baseRemaining,heldSkill.assistRemaining,helperAssistTotal))
            return stop(frame,lines,"POSTCONDITION_FAILED:"+step,time,seq);
        if(vision.board(frame)==null||(step<5&&vision.enemyDistance(frame)>.055))return retry(frame,lines,"POST_BATTLE_SCENE_REQUIRED",time,seq);
        if(heldSkill.assistRemaining!=null)lastAssistRemaining[SLOTS[step]]=heldSkill.assistRemaining;
        save(frame,lines,"post-verified-"+step,time,seq);
        if(step==1||step==2||step==3)phase=8;else advance();reset();
        return waitFor("スキル使用後の状態を実画面で確認しました");
    }
    private StagePolicy.Decision hastePost(Bitmap frame,List<StagePolicy.Item> lines,UraDialogPolicy.Read read,long time,long seq)throws Exception {
        int amount=step==1?4:2;
        if((!heldSkill.baseNamed(step==1?BEFORE[3]:AFTER[1])&&!heldSkill.actorNamed(step==1?BEFORE_ACTOR[3]:AFTER_ACTOR[1]))||monitorBefore==null||heldSkill.assistRemaining==null||heldSkill.assistRemaining!=monitorBefore-amount)
            return stop(frame,lines,"HASTE_NOT_VERIFIED:"+monitorBefore+"->"+heldSkill.assistRemaining,time,seq);
        Runnable verified=()->{lastAssistRemaining[step==1?3:0]=heldSkill.assistRemaining;advance();reset();};
        if(read!=null) {
            if(!vision.backControl(frame,read)||!read.named(step==1?BEFORE[3]:AFTER[1]))return retry(frame,lines,"HASTE_WITNESS_MODAL_REQUIRED",time,seq);
            if(!hasteEvidenceSaved){save(frame,lines,"post-haste-"+step,time,seq);hasteEvidenceSaved=true;return waitFor("ヘイストの記録後に新しい画面で戻るボタンを確認");}
            return close(read,"ヘイスト反映を確認",verified);
        }
        if(heldSkill.baseRemaining==null||heldSkill.baseRemaining<1||vision.enemyDistance(frame)>.055||vision.board(frame)==null)return retry(frame,lines,"HASTE_WITNESS_SCENE_REQUIRED",time,seq);
        save(frame,lines,"post-haste-"+step,time,seq);verified.run();return waitFor("ヘイスト反映を確認");
    }
    private static int count(byte[] b,int color){int n=0;for(byte c:b)if(c==color)n++;return n;}
    private static long now(){return android.os.SystemClock.elapsedRealtime();}
    private void reset(){misses=stable=0;hasteEvidenceSaved=false;}
    private StagePolicy.Decision close(UraDialogPolicy.Read read,String text,Runnable done){return action(read.back,text,done);}
    private StagePolicy.Decision action(StagePolicy.Item item,String text,Runnable done){StagePolicy.Decision d=new StagePolicy.Decision(item,text,false);d.holdMs=160;d.completed=done;return d;}
    private StagePolicy.Decision waitFor(String text){return new StagePolicy.Decision(null,text,false);}
    private StagePolicy.Decision retry(Bitmap frame,List<StagePolicy.Item> lines,String reason,long time,long seq)throws Exception {
        if(++misses<5){StagePolicy.Decision d=waitFor(reason+"（再読込"+misses+"/5）");d.nextFrameDelayMs=250+misses*97;return d;}return stop(frame,lines,reason,time,seq);
    }
    private StagePolicy.Decision stop(Bitmap frame,List<StagePolicy.Item> lines,String reason,long time,long seq)throws Exception {
        run.put("failureReason",reason);save(frame,lines,"stop",time,seq);
        if(!dryOnly&&phase>=2) {
            JSONArray remaining=new JSONArray();for(Integer n:lastAssistRemaining)remaining.put(n==null?JSONObject.NULL:n);
            JSONObject state=new JSONObject(run.toString()).put("preflightPassedThisRun",true).put("phase",phase).put("step",step)
                .put("monitorBefore",monitorBefore==null?JSONObject.NULL:monitorBefore).put("lastAssistRemaining",remaining);
            try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"ura-b1-paused.png"))){frame.compress(Bitmap.CompressFormat.PNG,100,out);}
            Files.write(new File(context.getFilesDir(),"ura-b1-paused.json").toPath(),state.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        return new StagePolicy.Decision(null,reason,true);
    }
    private void save(Bitmap frame,List<StagePolicy.Item> lines,String name,long capturedAt,long sequence)throws Exception {
        File dir=new File(context.getExternalFilesDir("Download"),"ura-runtime");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Runtime log unavailable");
        String prefix=name+"-"+System.currentTimeMillis();
        try(OutputStream out=new FileOutputStream(new File(dir,prefix+".png"))){frame.compress(Bitmap.CompressFormat.PNG,100,out);}
        JSONArray ocr=new JSONArray();for(var line:lines)ocr.put(new JSONObject().put("text",line.rawText).put("x",line.x).put("y",line.y));
        JSONObject data=new JSONObject(run.toString()).put("scene",name).put("capturedAt",capturedAt).put("sequence",sequence).put("phase",phase).put("step",step).put("ocr",ocr);
        JSONArray actualRemaining=new JSONArray();for(Integer n:lastAssistRemaining)actualRemaining.put(n==null?JSONObject.NULL:n);
        data.put("lastAssistRemaining",actualRemaining).put("monitorBefore",monitorBefore==null?JSONObject.NULL:monitorBefore);
        if(vision.boardDistances!=null){JSONArray scores=new JSONArray();for(double d:vision.boardDistances)scores.put(d);data.put("normalOrbTemplateDistances",scores).put("distanceIsNotProbability",true);}
        if(boardBefore!=null){JSONArray board=new JSONArray();for(byte c:boardBefore)board.put(c);data.put("preActionBoard",board);}
        try(OutputStream out=new FileOutputStream(new File(dir,prefix+".json"))){out.write(data.toString(2).getBytes(StandardCharsets.UTF_8));}
        android.util.Log.i("PADUraBattle",data.toString());
    }
}
