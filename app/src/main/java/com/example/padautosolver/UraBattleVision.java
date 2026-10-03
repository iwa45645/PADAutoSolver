package com.example.padautosolver;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.util.*;
import org.json.*;
final class UraBattleVision {
    private final Context context;private final Map<String,Bitmap> templates=new HashMap<>();
    private final Map<String,int[]> orbReferences=new HashMap<>();
    private JSONArray normalReferences;
    double[] boardDistances;
    UraBattleVision(Context context){this.context=context;}
    private Bitmap template(String name)throws Exception {
        Bitmap b=templates.get(name);if(b==null)try(InputStream in=context.getAssets().open("ura-shura/"+name)){b=BitmapFactory.decodeStream(in);templates.put(name,b);}return b;
    }
    static int[] pixels(Bitmap b){int[] p=new int[b.getWidth()*b.getHeight()];b.getPixels(p,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return p;}
    double distance(Bitmap frame,String name,int x,int y,int w,int h)throws Exception {
        Bitmap ref=template(name),crop=Bitmap.createBitmap(frame,x,y,w,h);
        Bitmap a=Bitmap.createScaledBitmap(crop,48,32,true),b=Bitmap.createScaledBitmap(ref,48,32,true);
        try{return TeamIconMatch.distance(pixels(a),pixels(b));}finally{if(a!=crop)a.recycle();crop.recycle();if(b!=ref)b.recycle();}
    }
    boolean initialTeam(Bitmap frame)throws Exception {
        return portrait(frame,5);
    }
    boolean openingNotStarted(Bitmap frame)throws Exception{return portrait(frame,0)&&portrait(frame,3);}
    /** Exact reviewed post-transform skill heading; cooldowns still require independent literal OCR. */
    boolean sekkaPostHeading(Bitmap frame)throws Exception {
        return distance(frame,"post-sekka-skill-header.png",130,230,490,50)<.025;
    }
    boolean menuFloorOne(Bitmap frame)throws Exception {
        Bitmap ref=template("b1-floor-one.png"),crop=Bitmap.createBitmap(frame,650,975,50,70);
        int[] a=pixels(ref),b=pixels(crop);crop.recycle();int different=0,white=0;
        for(int i=0;i<a.length;i++){boolean x=white(a[i]),y=white(b[i]);if(x!=y)different++;if(y)white++;}
        return white>150&&different/(double)a.length<.04;
    }
    private static boolean white(int pixel){return ((pixel>>16)&255)>220&&((pixel>>8)&255)>220&&(pixel&255)>220;}
    /** Resume-state check only: a changed transformation in one slot cannot be diluted by the whole screen. */
    static boolean sameRunPortraits(Bitmap previous,Bitmap live) {
        // The middle of a portrait temporarily displays the remaining turns (e.g. Sekka's 35).
        // Both independent artwork bands must match; never compare the blinking digit overlay.
        for(int slot=0;slot<6;slot++)for(int y:new int[]{1380,1500}) {
            int x=60+slot*203;Bitmap before=Bitmap.createBitmap(previous,x,y,100,30);
            Bitmap scaled=Bitmap.createScaledBitmap(before,48,16,true);int[] ref=pixels(scaled);scaled.recycle();before.recycle();double best=1;
            // The six portraits bob independently by up to 20 px on this device.
            // Keep each artwork band and its distance limit; account for that observed motion.
            for(int dx=-8;dx<=8;dx+=4)for(int dy=-24;dy<=24;dy+=4){
                Bitmap crop=Bitmap.createBitmap(live,x+dx,y+dy,100,30),small=Bitmap.createScaledBitmap(crop,48,16,true);
                best=Math.min(best,TeamIconMatch.distance(ref,pixels(small)));small.recycle();crop.recycle();
            }
            if(best>.04)return false;
        }return true;
    }
    private boolean portrait(Bitmap frame,int i)throws Exception {
            double same=1,other=1;
            for(int dx=-8;dx<=8;dx+=4)for(int dy=-40;dy<=40;dy+=4) {
                same=Math.min(same,distance(frame,"b1-face-"+i+".png",60+i*203+dx,1390+dy,100,60));
                for(int j=0;j<6;j++)if(j!=i)other=Math.min(other,distance(frame,"b1-face-"+j+".png",60+i*203+dx,1390+dy,100,60));
            }
            if(same>.04||other-same<.05)return false;
        return true;
    }
    double enemyDistance(Bitmap frame)throws Exception {
        int[][] origins={{137,1010},{556,1010},{958,1010}};double worst=0;
        for(int i=0;i<3;i++) {
            double best=1;
            for(int dx=-12;dx<=12;dx+=4)for(int dy=-12;dy<=12;dy+=4)
                best=Math.min(best,distance(frame,"b1-enemy-"+i+".png",origins[i][0]+dx,origins[i][1]+dy,150,110));
            worst=Math.max(worst,best);
        }
        return worst;
    }
    boolean leonis(Bitmap frame)throws Exception {
        return leonisDistance(frame)<.055;
    }
    double leonisDistance(Bitmap frame)throws Exception {
        if(frame.getWidth()!=1220||frame.getHeight()!=2712)return 1;
        double best=1;
        for(int dx=-12;dx<=12;dx+=4)for(int dy=-12;dy<=12;dy+=4)
            best=Math.min(best,distance(frame,"b3-leonis-face.png",180+dx,640+dy,350,390));
        return best;
    }
    boolean leonisFullHealth(Bitmap frame)throws Exception {
        return distance(frame,"b3-leonis-full-hp.png",180,1220,900,20)<.035;
    }
    boolean recoveredB3Hp(Bitmap frame)throws Exception {
        return distance(frame,"b3-recovered-hp.png",770,1554,425,54)<.02;
    }
    int b3HpLowerBound(Bitmap frame) {
        // Count the contiguous actual pink fill; white digits can only shorten this bound.
        // The verified unchanged team's maximum HP is 611045. Subtract 8 px at the edge.
        int[] scan=new int[1090*3];int row=0;
        for(int y:new int[]{1566,1570,1574})frame.getPixels(scan,row++*1090,1090,90,y,1090,1);
        return UraB3Policy.hpFillLowerBound(scan);
    }
    boolean lucifer(Bitmap frame)throws Exception {
        if(frame.getWidth()!=1220||frame.getHeight()!=2712)return false;
        double best=1;
        for(int dx=-12;dx<=12;dx+=4)for(int dy=-12;dy<=12;dy+=4)
            best=Math.min(best,distance(frame,"b2-lucifer-face.png",370+dx,610+dy,390,380));
        return best<.055;
    }
    boolean poisonInstruction(Bitmap frame)throws Exception {
        return distance(frame,"b2-poison-instruction.png",210,635,800,80)<.025;
    }
    boolean poisonInstructionStrip(Bitmap strip)throws Exception {
        return strip.getWidth()==860&&strip.getHeight()==150
                &&distance(strip,"b2-poison-instruction.png",30,15,800,80)<.025;
    }
    LuciferInstruction luciferInstructionStrip(Bitmap strip)throws Exception {
        if(strip.getWidth()!=860||strip.getHeight()!=150)return null;
        boolean poison=poisonInstructionStrip(strip);
        boolean all=distance(strip,"b2-water-light-instruction.png",30,15,800,80)<.025;
        if(poison==all)return null;
        return poison?LuciferInstruction.POISON:LuciferInstruction.ALL;
    }
    boolean active(Bitmap frame,StagePolicy.Item target) {
        if(target==null||target.x<120||target.y<1500||target.x>1100||target.y>2500)return false;
        Bitmap crop=Bitmap.createBitmap(frame,Math.round(target.x)-65,Math.round(target.y)-35,130,70);
        try{return UraDialogPolicy.brightTextFraction(pixels(crop))>.08;}finally{crop.recycle();}
    }
    boolean backControl(Bitmap frame,UraDialogPolicy.Read read)throws Exception {
        if(read==null||read.back.y<1600||read.back.y>2500)return false;
        double best=1;
        for(int dy=-8;dy<=8;dy+=2)best=Math.min(best,distance(frame,"b1-back-control.png",675,Math.round(read.back.y)-34+dy,234,68));
        return best<.045;
    }
    byte[] board(Bitmap frame)throws Exception {return board(frame,false);}
    byte[] luciferBoard(Bitmap frame)throws Exception {return board(frame,true);}
    private byte[] board(Bitmap frame,boolean special)throws Exception {
        if(normalReferences==null)try(InputStream in=context.getAssets().open("ura-shura/normal-orbs.json")) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int read;
            while((read=in.read(bytes))!=-1)out.write(bytes,0,read);
            normalReferences=new JSONArray(out.toString("UTF-8"));
        }
        // Reviewed B1/B2 orb fixtures. Unrecognised artwork is UNKNOWN, never a hue-only guess.
        byte[] board=new byte[30];boardDistances=new double[30];
        for(int cell=0;cell<30;cell++) {
            float size=1220f/6,top=2712-84-5*size;
            Bitmap crop=Bitmap.createBitmap(frame,Math.round((cell%6+.5f)*size)-80,Math.round(top+(cell/6+.5f)*size)-80,160,160);
            Bitmap small=Bitmap.createScaledBitmap(crop,48,48,true);int[] live=pixels(small);small.recycle();crop.recycle();
            double[] byColor=new double[special?10:6];Arrays.fill(byColor,1);
            for(int i=0;i<normalReferences.length();i++) {
                JSONObject reference=normalReferences.getJSONObject(i);
                int refColor=reference.getInt("color");String name=reference.getString("file");
                if(refColor>=byColor.length)continue;
                int[] ref=orbReferences.get(name);
                if(ref==null){Bitmap scaled=Bitmap.createScaledBitmap(template(name),48,48,true);ref=pixels(scaled);scaled.recycle();orbReferences.put(name,ref);}
                byColor[refColor]=Math.min(byColor[refColor],TeamIconMatch.distance(live,ref));
            }
            int color=0;for(int c=1;c<byColor.length;c++)if(byColor[c]<byColor[color])color=c;
            double other=1;for(int c=0;c<byColor.length;c++)if(c!=color)other=Math.min(other,byColor[c]);
            boardDistances[cell]=byColor[color];
            if(byColor[color]>.07||other-byColor[color]<.035)return null;
            board[cell]=(byte)color;
        }
        return board;
    }
}
