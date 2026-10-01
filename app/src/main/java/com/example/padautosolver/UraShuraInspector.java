package com.example.padautosolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** Read-only milestone inspector. It has no gesture dispatcher or entry permission. */
final class UraShuraInspector {
    private final Context context;
    private final FixedTeamProfile profile;
    private final int[][] references=new int[6][];
    UraShuraInspector(Context context) throws Exception {
        this.context=context;
        try(var in=context.getAssets().open("ura-shura/team_profile.json")) {
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
            while((count=in.read(buffer))!=-1)bytes.write(buffer,0,count);
            profile=new FixedTeamProfile(new JSONObject(bytes.toString("UTF-8")));
        }
        for(int i=0;i<6;i++)try(var in=context.getAssets().open("ura-shura/preentry-slot-"+i+".png")) {
            Bitmap b=BitmapFactory.decodeStream(in),small=Bitmap.createScaledBitmap(b,48,32,true);
            references[i]=pixels(small);if(small!=b)small.recycle();b.recycle();
        }
    }
    String inspect(Bitmap frame,StageNavigator navigator,long capturedAt,long now,long generation,boolean foreground) throws Exception {
        JSONObject log=new JSONObject().put("timestamp",System.currentTimeMillis()).put("generation",generation)
                .put("capturedAtElapsed",capturedAt).put("inspectedAtElapsed",now).put("profileId",profile.id)
                .put("gesturesSent",0).put("entryApproved",false);
        String reason;
        JSONArray ocr=new JSONArray(),matches=new JSONArray();
        if(!foreground)reason="FOREGROUND_OR_DIALOG";
        else if(now<capturedAt||now-capturedAt>1500)reason="STALE_SCREENSHOT";
        else if(frame.getWidth()!=1220||frame.getHeight()!=2712)reason="CALIBRATION_MISMATCH";
        else {
            List<StagePolicy.Item> items=navigator.readItems(frame);StringBuilder text=new StringBuilder();
            for(var item:items){ocr.put(new JSONObject().put("text",item.rawText).put("x",item.x).put("y",item.y));text.append(item.text);}
            if(!UraScenePolicy.preentry(items,frame.getHeight()))reason="PREENTRY_CAPTURE_REQUIRED";
            else {
                boolean allMatch=true;
                for(int i=0;i<6;i++) {
                    int left=(int)((.016+i*.162)*1220),right=(int)((.166+i*.162)*1220);
                    Bitmap crop=Bitmap.createBitmap(frame,left,(int)(.612*2712),right-left,(int)(.647*2712)-(int)(.612*2712));
                    Bitmap small=Bitmap.createScaledBitmap(crop,48,32,true);
                    int[] live=pixels(small);boolean matched=TeamIconMatch.matches(live,references,i);allMatch&=matched;
                    matches.put(new JSONObject().put("slot",i).put("expectedMonsterNo",profile.slots[i].monsterNo)
                            .put("templateDistance",TeamIconMatch.distance(live,references[i])).put("iconMatched",matched));
                    if(small!=crop)small.recycle();crop.recycle();
                }
                reason=allMatch?"DETAIL_AND_TEAM_INFO_CAPTURE_REQUIRED":"TeamProfileMismatch";
            }
        }
        log.put("ocr",ocr).put("slots",matches).put("failureReason",reason);
        File dir=new File(context.getExternalFilesDir("Download"),"ura-shura-replay");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new java.io.IOException("Replay directory unavailable");
        String name="inspection-"+System.currentTimeMillis();
        try(var out=new FileOutputStream(new File(dir,name+".png"))){frame.compress(Bitmap.CompressFormat.PNG,100,out);}
        Files.write(new File(dir,name+".json").toPath(),log.toString(2).getBytes(StandardCharsets.UTF_8));
        android.util.Log.i("PADUraShura",log.toString());
        String result="固定編成／読み取り専用\n";
        if("DETAIL_AND_TEAM_INFO_CAPTURE_REQUIRED".equals(reason))result+="潜入確認：6枠のアイコン一致\n未照合：今回の装備・潜在・合算スキブ\n";
        else result+=reason+"\n";
        return result+"B1予定：ルカ装備→エスペル→セッカ→ユキネ→ミオン→オーディン\n"
                +"パズル：水2セット＋コンボドロップ発動、回復優先\n"
                +"実行許可なし／スキルと盤面は操作しません\nログ："+name;
    }
    private static int[] pixels(Bitmap b){int[] p=new int[b.getWidth()*b.getHeight()];b.getPixels(p,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return p;}
}
