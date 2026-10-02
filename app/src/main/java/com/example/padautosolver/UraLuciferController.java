package com.example.padautosolver;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** B2 trials: observed instruction -> immutable plan -> one gesture -> fresh next instruction. */
final class UraLuciferController {
    private final Context context;
    private final UraBattleVision vision;
    private final JSONObject record=new JSONObject();
    private volatile int phase;
    private int trial,misses,stable;
    private long lastSequence=-1;
    private volatile long actionAt;
    private LuciferInstruction instruction,nextInstruction;
    private UraPuzzlePlan plan;
    private byte[] previousBoard,stableBoard;
    private boolean observationOnly;
    private boolean floorEvidenceSaved,dispatchPrepared;
    private final UraInstructionCapture instructionCapture=new UraInstructionCapture();
    void captureInstruction(Bitmap frame,long capturedAt,long seq) {
        if(phase==4&&capturedAt>=actionAt&&capturedAt-actionAt<=15000)instructionCapture.capture(frame,capturedAt,seq);
    }
    void close(){instructionCapture.close();}
    private void readCapturedInstructions(StageNavigator nav)throws Exception {
        for(var read:instructionCapture.read(context,nav,record.getString("runId"),trial,"lucifer-command")) {
            if(trial<2&&read.instruction==LuciferInstruction.values()[trial+1]) {
                nextInstruction=read.instruction;record.put("nextInstruction",nextInstruction.name()).put("nextInstructionEvidence",read.evidence);
            }
        }
    }
    private UraLuciferController(Context context)throws Exception {
        this.context=context;vision=new UraBattleVision(context);
        record.put("runId",UUID.randomUUID().toString()).put("mode","B2_LUCIFER");
    }
    static boolean isScene(Context context,Bitmap frame)throws Exception {return new UraBattleVision(context).lucifer(frame);}
    static UraLuciferController resume(Context context,Bitmap live)throws Exception {
        UraLuciferController c=new UraLuciferController(context);
        if(!c.vision.lucifer(live))return null;
        byte[] board=c.vision.luciferBoard(live);if(board==null)return null;
        File stateFile=new File(context.getFilesDir(),"ura-lucifer-state.json");
        File picture=new File(context.getFilesDir(),"ura-lucifer-state.png");
        if(stateFile.isFile()&&picture.isFile()) {
            JSONObject state=new JSONObject(new String(Files.readAllBytes(stateFile.toPath()),StandardCharsets.UTF_8));
            Bitmap old=BitmapFactory.decodeFile(picture.getPath());boolean same=false;
            try{same=old!=null&&Arrays.equals(board,c.vision.luciferBoard(old))&&UraBattleVision.sameRunPortraits(old,live);}finally{if(old!=null)old.recycle();}
            if(same&&state.optInt("trial",-1)>=0&&state.optInt("trial",-1)<=2) {
                for(Iterator<String> it=state.keys();it.hasNext();){String key=it.next();c.record.put(key,state.get(key));}
                c.record.remove("failureReason");
                c.trial=state.getInt("trial");c.instruction=LuciferInstruction.valueOf(state.getString("instruction"));
                c.observationOnly=state.optBoolean("awaitingResult");c.previousBoard=bytes(state.optJSONArray("sourceBoard"));
                if(c.trial<2&&state.optString("nextInstruction").equals(LuciferInstruction.values()[c.trial+1].name()))c.nextInstruction=LuciferInstruction.values()[c.trial+1];
                c.record.put("resumeEvidence","Same current board and six portrait bands; menu floor is recaptured");return c;
            }
        }
        // Development resumes a previously verified B2, including a stopped app from yesterday.
        // Require the exact stable board, portraits, native floor=2 record and its actual instruction pixels.
        File dir=new File(context.getExternalFilesDir("Download"),"ura-runtime");
        File[] stops=dir.listFiles((d,n)->n.startsWith("stop-")&&n.endsWith(".json"));if(stops==null)return null;
        Arrays.sort(stops,Comparator.comparingLong(File::lastModified).reversed());
        for(File stop:stops) {
            if(System.currentTimeMillis()-stop.lastModified()>48L*3600000)continue;
            JSONObject state=new JSONObject(new String(Files.readAllBytes(stop.toPath()),StandardCharsets.UTF_8));
            if(state.optInt("observedFloorAfterPuzzle")!=2||state.optInt("phase")!=19)continue;
            Bitmap old=BitmapFactory.decodeFile(new File(dir,stop.getName().replace(".json",".png")).getPath());boolean same=false;
            try{same=old!=null&&Arrays.equals(board,c.vision.luciferBoard(old))&&UraBattleVision.sameRunPortraits(old,live);}finally{if(old!=null)old.recycle();}
            if(!same)continue;
            String nativeRun=state.getString("runId");
            File[] observations=dir.listFiles((d,n)->(n.startsWith("puzzle-result-observation-")||n.startsWith("b1-lucifer-command-"))&&n.endsWith(".json"));if(observations==null)return null;
            Arrays.sort(observations,Comparator.comparingLong(File::lastModified).reversed());
            for(File file:observations) {
                JSONObject observed=new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));if(!nativeRun.equals(observed.optString("runId")))continue;
                Bitmap image=BitmapFactory.decodeFile(new File(dir,file.getName().replace(".json",".png")).getPath());boolean poison=false;
                if(file.getName().startsWith("b1-lucifer-command-")) {
                    List<StagePolicy.Item> text=new ArrayList<>();JSONArray ocr=observed.getJSONArray("ocr");
                    for(int i=0;i<ocr.length();i++){JSONObject line=ocr.getJSONObject(i);text.add(new StagePolicy.Item(line.getString("text"),(float)line.getDouble("x"),(float)line.getDouble("y")));}
                    poison=image!=null&&LuciferInstruction.read(text)==LuciferInstruction.POISON;
                    if(image!=null)image.recycle();image=null;
                }
                try{if(image!=null)poison=c.vision.poisonInstruction(image);}finally{if(image!=null)image.recycle();}
                if(poison){c.instruction=LuciferInstruction.POISON;c.record.put("sourceB1RunId",nativeRun).put("instructionEvidence",file.getName());return c;}
            }
        }return null;
    }
    StagePolicy.Decision inspect(Bitmap frame,StageNavigator nav,long capturedAt,long seq)throws Exception {
        if(frame.getWidth()!=1220||frame.getHeight()!=2712)return stop(frame,List.of(),"B2_CALIBRATION_MISMATCH",capturedAt,seq);
        if(seq<=lastSequence)return waitFor("B2：新しい画面を待機");lastSequence=seq;
        List<StagePolicy.Item> lines;
        if(phase==0||phase==5) {
            lines=nav.readUraCombat(frame);if(UraCombatText.blocked(lines))return stop(frame,lines,"B2_GAME_OVER_OR_PURCHASE",capturedAt,seq);
            StagePolicy.Item menu=UraCombatText.control(lines,"MENU",500,620);
            if(menu==null)return retry(frame,lines,"B2_MENU_REQUIRED",capturedAt,seq);
            int next=phase==0?1:6;
            return action(menu,"B2：現在の階層を確認",()->{phase=next;floorEvidenceSaved=false;misses=0;});
        }
        if(phase==1||phase==6) {
            lines=nav.readUraCombat(frame);int floor=UraCombatText.floor(lines);
            if(floor<0||!UraCombatText.joined(lines).contains("裏魔門の守護者"))return retry(frame,lines,"B2_FLOOR_CAPTURE_REQUIRED",capturedAt,seq);
            if(phase==1&&floor!=2)return stop(frame,lines,"B2_WRONG_FLOOR:"+floor,capturedAt,seq);
            if(phase==6&&floor!=3)return stop(frame,lines,"B2_CLEAR_NOT_VERIFIED:"+floor,capturedAt,seq);
            record.put("observedFloor",floor);
            if(!floorEvidenceSaved){save(frame,lines,"lucifer-floor",capturedAt,seq);floorEvidenceSaved=true;return waitFor("B2：階層の記録後に新しい画面で操作を確認");}
            StagePolicy.Item back=UraCombatText.control(lines,"戻る",2080,2220);
            if(back==null)return retry(frame,lines,"B2_MENU_BACK_REQUIRED",capturedAt,seq);
            int next=phase==6?7:observationOnly?4:2;
            return action(back,"B2：戦闘画面へ戻る",()->{phase=next;actionAt=now()-12000;misses=0;});
        }
        if(phase==7)return stop(frame,List.of(),"B2_CLEAR_VERIFIED_B3_CAPTURE_REQUIRED",capturedAt,seq);
        if(phase==4) {
            readCapturedInstructions(nav);
            lines=nav.readLuciferInstruction(frame);
            LuciferInstruction seen=observed(frame,lines);
            if(trial<2&&seen==LuciferInstruction.values()[trial+1]) {
                nextInstruction=seen;record.put("nextInstruction",seen.name());
                save(frame,lines,"lucifer-next-instruction",capturedAt,seq);
            }
            save(frame,lines,"lucifer-observation",capturedAt,seq);
            if(now()-actionAt<12000)return waitFor("B2：試練の結果と次の指示を記録中");
            List<StagePolicy.Item> combat=nav.readUraCombat(frame);
            if(UraCombatText.blocked(combat))return stop(frame,combat,"B2_GAME_OVER_OR_PURCHASE",capturedAt,seq);
            if(trial==2){phase=5;misses=0;return waitFor("B2：全消し後の実階層を確認します");}
            byte[] board=vision.luciferBoard(frame);
            if(nextInstruction!=null&&vision.lucifer(frame)&&board!=null&&previousBoard!=null&&!Arrays.equals(board,previousBoard)) {
                trial++;instruction=nextInstruction;nextInstruction=null;phase=2;stable=misses=0;stableBoard=null;
                record.remove("nextInstruction");record.put("awaitingResult",false);save(frame,lines,"lucifer-trial-verified",capturedAt,seq);
                return waitFor("B2：次の指示「"+label()+"」を実画面で確認しました");
            }
            if(now()-actionAt>45000)return stop(frame,lines,"B2_NEXT_INSTRUCTION_OR_BOARD_REQUIRED",capturedAt,seq);
            return waitFor("B2：次の指示と静止盤面を待機");
        }
        if(!vision.lucifer(frame))return retry(frame,List.of(),"B2_ENEMY_CAPTURE_REQUIRED",capturedAt,seq);
        byte[] board=vision.luciferBoard(frame);if(board==null)return retry(frame,List.of(),"B2_BOARD_UNKNOWN",capturedAt,seq);
        if(phase==2) {
            if(instruction==null)return stop(frame,List.of(),"B2_INSTRUCTION_REQUIRED",capturedAt,seq);
            if(!Arrays.equals(board,stableBoard)){stableBoard=board;stable=1;return waitFor("B2：盤面が静止するまで再確認");}
            if(++stable<2)return waitFor("B2：盤面を再確認");
            PuzzleGoal goal;
            try{goal=instruction.goal(board);}catch(IllegalArgumentException ex){return stop(frame,List.of(),"B2_INVALID_POISON_COUNT",capturedAt,seq);}
            PuzzleSolver.Result result=PuzzleSolver.solve(board,6,5,48,3000,2500,goal);
            if(instruction==LuciferInstruction.ZERO&&result.path.size()==1)
                result=new PuzzleSolver.Result(List.of(0,1,0),0,0,2,0,0,true);
            if(!result.goalSatisfied)return stop(frame,List.of(),"B2_PUZZLE_GOAL_UNSATISFIED",capturedAt,seq);
            plan=new UraPuzzlePlan(board,result,goal,now());previousBoard=board.clone();
            record.put("sourceBoard",array(board)).put("path",new JSONArray(plan.path)).put("predictedMatched",plan.stats.matched)
                .put("predictedCombos",plan.stats.combos).put("predictedPoisonMatched",plan.stats.colorMatched[7]).put("awaitingResult",false);
            save(frame,List.of(),"lucifer-dry-run",capturedAt,seq);phase=3;dispatchPrepared=false;
            // A tall overlay preview obscures Lucifer's face/instruction. The dry-run route is saved above.
            return waitFor("B2："+label()+"の経路を検証済み");
        }
        if(plan==null||!plan.current(board,now()))return stop(frame,List.of(),"B2_STALE_PUZZLE_PLAN",capturedAt,seq);
        // Durable prepared state never authorizes replay after process death/installation.
        if(!dispatchPrepared){
            record.put("awaitingResult",true);save(frame,List.of(),"lucifer-dispatch-prepared",capturedAt,seq);
            dispatchPrepared=true;return waitFor("B2：記録後の新しい画面で経路を再確認");
        }
        StagePolicy.Decision d=action(new StagePolicy.Item("URA_B2_TRIAL_"+trial,610,1700),"B2："+label()+"を1回実行",()->{
            actionAt=now();phase=4;stable=misses=0;
        });
        d.puzzlePath=plan.path;d.puzzleCols=6;d.puzzleRows=5;d.puzzleRect=BoardGeometry.calculate(1220,2712,6,5,0,84);d.puzzleDurationMs=3500;return d;
    }
    private LuciferInstruction observed(Bitmap frame,List<StagePolicy.Item> lines)throws Exception {
        LuciferInstruction read=LuciferInstruction.read(lines);if(read!=null)return read;
        return vision.poisonInstruction(frame)?LuciferInstruction.POISON:null;
    }
    private String label(){return instruction==LuciferInstruction.POISON?"毒を全て消す":instruction==LuciferInstruction.ZERO?"0コンボ":"盤面を全て消す";}
    private static byte[] bytes(JSONArray source)throws Exception {if(source==null)return null;byte[] b=new byte[source.length()];for(int i=0;i<b.length;i++)b[i]=(byte)source.getInt(i);return b;}
    private static JSONArray array(byte[] board){JSONArray a=new JSONArray();for(byte b:board)a.put(b);return a;}
    private static long now(){return android.os.SystemClock.elapsedRealtime();}
    private StagePolicy.Decision action(StagePolicy.Item item,String text,Runnable done){StagePolicy.Decision d=new StagePolicy.Decision(item,text,false);d.holdMs=160;d.completed=done;return d;}
    private StagePolicy.Decision waitFor(String text){StagePolicy.Decision d=new StagePolicy.Decision(null,text,false);d.nextFrameDelayMs=250;return d;}
    private StagePolicy.Decision retry(Bitmap frame,List<StagePolicy.Item> lines,String reason,long time,long seq)throws Exception {
        if(++misses<6)return waitFor(reason+"（再読込"+misses+"/6）");return stop(frame,lines,reason,time,seq);
    }
    private StagePolicy.Decision stop(Bitmap frame,List<StagePolicy.Item> lines,String reason,long time,long seq)throws Exception {
        record.put("failureReason",reason);save(frame,lines,"lucifer-stop",time,seq);return new StagePolicy.Decision(null,reason,true);
    }
    private void save(Bitmap frame,List<StagePolicy.Item> lines,String name,long capturedAt,long seq)throws Exception {
        File dir=new File(context.getExternalFilesDir("Download"),"ura-runtime");if(!dir.exists()&&!dir.mkdirs())throw new IOException("B2 log unavailable");
        JSONArray text=new JSONArray();for(var line:lines)text.put(new JSONObject().put("text",line.rawText).put("x",line.x).put("y",line.y));
        record.put("phase",phase).put("trial",trial).put("instruction",instruction==null?JSONObject.NULL:instruction.name())
            .put("capturedAt",capturedAt).put("sequence",seq).put("ocr",text);
        String prefix=name+"-"+System.currentTimeMillis();
        try(OutputStream out=new FileOutputStream(new File(dir,prefix+".png"))){frame.compress(Bitmap.CompressFormat.PNG,100,out);}
        Files.write(new File(dir,prefix+".json").toPath(),record.toString(2).getBytes(StandardCharsets.UTF_8));
        if(instruction!=null){
            try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"ura-lucifer-state.png"))){frame.compress(Bitmap.CompressFormat.PNG,100,out);}
            Files.write(new File(context.getFilesDir(),"ura-lucifer-state.json").toPath(),record.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        android.util.Log.i("PADLucifer",record.toString());
    }
}
