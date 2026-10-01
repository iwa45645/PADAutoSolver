package com.example.padautosolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Friend-only entry validation requested by the user. Own team is never traversed. */
final class UraPreflightController {
    private final Context context;
    private final Map<String,Bitmap> references=new HashMap<>();
    private final JSONObject evidence=new JSONObject();
    private int slot,phase,misses,stable;
    private long lastSequence=-1;
    private String lastProof="";
    private StagePolicy.Decision pending;
    boolean validated;
    int helperAssistTotal;

    UraPreflightController(Context context)throws Exception {
        this.context=context;
        slot=5;
        evidence.put("helperReused",false).put("validationPolicy","USER_REQUESTED_MION_ONLY");
    }
    StagePolicy.Decision inspect(Bitmap frame,StageNavigator nav,long sequence)throws Exception {
        if(frame.getWidth()!=1220||frame.getHeight()!=2712)return stop(frame,"CALIBRATION_MISMATCH");
        if(sequence<=lastSequence)return waitFor("新しいキャプチャを待機");
        lastSequence=sequence;
        List<StagePolicy.Item> lines=phase==0?nav.readUraPreentry(frame):nav.readItems(frame);
            if(phase==0) {
                if(!UraScenePolicy.preentry(lines,2712))return retry(frame,"PREENTRY_CAPTURE_REQUIRED");
                if(slot==6){validated=true;save(frame,"helper-complete",lines);return waitFor("ミオン確認済み：実戦へ進みます");}
                return action("URA_HELPER_OPEN",1095,1700,1000,"フレンドのミオンだけ確認",()->{phase=1;reset();});
            }
            DetailIdentity helper=nav.readCandidateIdentity(frame,9411);
            if(helper==null&&DetailNumberMatch.matches(frame,reference("number-9411.png"),9411))helper=new DetailIdentity(9411,null);
            if(helper==null||helper.id!=9411)return retry(frame,"HELPER_MION_REQUIRED");
            if(pending!=null)return pending;
            if(!twice("9411"))return waitFor("ミオンの表示を再確認");
            evidence.put("helperMonsterNo",9411);save(frame,"helper",lines);
            return back("ミオン確認済み：潜入確認へ",()->{slot=6;phase=0;reset();});
    }
    static Integer turn(List<StagePolicy.Item> items,int top,int bottom) {
        Integer value=null;
        for(var item:items)if(item.y>top&&item.y<bottom) {
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("ターン[:：]?([0-9]{1,3})(?![0-9A-Z])").matcher(item.text);
            if(m.find()){int n=Integer.parseInt(m.group(1));if(value!=null&&value!=n)return null;value=n;}
        }
        return value;
    }
    private Bitmap reference(String name)throws IOException {
        Bitmap b=references.get(name);if(b==null)try(InputStream in=context.getAssets().open("ura-shura/"+name)){b=BitmapFactory.decodeStream(in);references.put(name,b);}return b;
    }
    private boolean twice(String proof){if(proof.equals(lastProof))stable++;else{lastProof=proof;stable=1;}misses=0;return stable>=2;}
    private void reset(){pending=null;stable=misses=0;lastProof="";}
    private StagePolicy.Decision action(String key,float x,float y,long hold,String text,Runnable completed) {
        StagePolicy.Decision d=new StagePolicy.Decision(new StagePolicy.Item(key,x,y),text,false);d.holdMs=hold;d.completed=completed;pending=d;return d;
    }
    private StagePolicy.Decision back(String text,Runnable next){return action("URA_DETAIL_BACK_"+slot+"_"+phase,80,590,160,text,next);}
    private StagePolicy.Decision waitFor(String text){return new StagePolicy.Decision(null,text,false);}
    private StagePolicy.Decision retry(Bitmap frame,String reason)throws Exception{return ++misses<5?waitFor(reason+"（再読込 "+misses+"/5）"):stop(frame,reason);}
    private StagePolicy.Decision stop(Bitmap frame,String reason)throws Exception{evidence.put("failureReason",reason);save(frame,"stop",Collections.emptyList());return new StagePolicy.Decision(null,reason,true);}
    private void save(Bitmap frame,String name,List<StagePolicy.Item> lines)throws Exception {
        File dir=new File(context.getExternalFilesDir("Download"),"ura-runtime");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Runtime log unavailable");
        String prefix="preflight-"+name+"-"+System.currentTimeMillis();
        try(OutputStream out=new FileOutputStream(new File(dir,prefix+".png"))){frame.compress(Bitmap.CompressFormat.PNG,100,out);}
        JSONArray ocr=new JSONArray();for(var item:lines)ocr.put(new JSONObject().put("text",item.rawText).put("x",item.x).put("y",item.y));
        JSONObject log=new JSONObject(evidence.toString()).put("slot",slot).put("phase",phase).put("ocr",ocr);
        try(OutputStream out=new FileOutputStream(new File(dir,prefix+".json"))){out.write(log.toString(2).getBytes(StandardCharsets.UTF_8));}
        android.util.Log.i("PADUraRuntime",log.toString());
    }
}
