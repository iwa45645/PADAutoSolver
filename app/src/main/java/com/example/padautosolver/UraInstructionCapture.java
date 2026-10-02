package com.example.padautosolver;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.*;

/** Read-only banner buffer, fed by MediaProjection independently of OCR and PNG compression. */
final class UraInstructionCapture {
    private static final class Frame {
        final Bitmap image;final long time,sequence;
        Frame(Bitmap image,long time,long sequence){this.image=image;this.time=time;this.sequence=sequence;}
    }
    static final class Read {
        final LuciferInstruction instruction;final String evidence;
        Read(LuciferInstruction instruction,String evidence){this.instruction=instruction;this.evidence=evidence;}
    }
    private final ArrayDeque<Frame> frames=new ArrayDeque<>();
    private volatile boolean closed;
    void capture(Bitmap image,long time,long sequence) {
        if(closed||image.getWidth()!=1220||image.getHeight()!=2712)return;
        int blue=0,white=0,total=0;
        for(int y=645;y<710;y+=4)for(int x=210;x<1010;x+=6){
            int p=image.getPixel(x,y),r=(p>>16)&255,g=(p>>8)&255,b=p&255;total++;
            if(b>r+20&&g>r+10&&b>80)blue++;
            if(r>210&&g>210&&b>210)white++;
        }
        // This only selects images to retain. Literal OCR is still required for any action.
        if(blue<total*.35||white<total*.025)return;
        Bitmap crop=Bitmap.createBitmap(image,180,620,860,150);
        synchronized(frames){
            if(closed){crop.recycle();return;}
            while(frames.size()>=24)frames.removeFirst().image.recycle();
            frames.addLast(new Frame(crop,time,sequence));
        }
    }
    List<Read> read(Context context,StageNavigator nav,String runId,int trial,String kind)throws Exception {
        List<Frame> pending=new ArrayList<>();
        synchronized(frames){while(!frames.isEmpty())pending.add(frames.removeFirst());}
        List<Read> results=new ArrayList<>();
        UraBattleVision vision=new UraBattleVision(context);
        try {
            File dir=new File(context.getExternalFilesDir("Download"),"ura-runtime");
            if(!dir.exists()&&!dir.mkdirs())throw new IOException("Instruction log unavailable");
            for(Frame frame:pending){
                List<StagePolicy.Item> lines=nav.readLuciferStrip(frame.image);
                String prefix=kind+"-"+System.currentTimeMillis()+"-"+frame.sequence;
                try(OutputStream out=new FileOutputStream(new File(dir,prefix+".png"))){frame.image.compress(Bitmap.CompressFormat.PNG,100,out);}
                JSONArray text=new JSONArray();for(var line:lines)text.put(new JSONObject().put("text",line.rawText).put("x",line.x).put("y",line.y));
                JSONObject evidence=new JSONObject().put("runId",runId).put("trial",trial).put("capturedAt",frame.time)
                    .put("sequence",frame.sequence).put("cropX",180).put("cropY",620).put("ocr",text);
                Files.write(new File(dir,prefix+".json").toPath(),evidence.toString(2).getBytes(StandardCharsets.UTF_8));
                LuciferInstruction literal=LuciferInstruction.read(lines),pixels=vision.luciferInstructionStrip(frame.image);
                results.add(new Read(literal!=null&&pixels!=null&&literal!=pixels?null:literal!=null?literal:pixels,prefix+".json"));
            }
        }finally{for(Frame frame:pending)frame.image.recycle();}
        return results;
    }
    void close(){synchronized(frames){closed=true;while(!frames.isEmpty())frames.removeFirst().image.recycle();}}
}
