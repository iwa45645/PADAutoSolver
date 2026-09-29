package com.example.padautosolver;

import android.content.Context;
import android.graphics.*;
import android.view.WindowManager;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Rebuild unconfirmed entries from saved, adjacent pages. Publish only after all pages align. */
final class InventoryReplay {
    static String rebuild(Context context, java.util.function.Consumer<String> progress) throws Exception {
        InventoryRepository draft = new InventoryRepository(context);
        BoxCalibration config = new BoxCalibration(context.getSharedPreferences("pad_solver",0));
        config.validate();
        int count = draft.inventory.optInt("pageCount");
        if (count < 1) throw new IOException("再集計するページがありません");
        for (int i=0;i<draft.items.length();i++)
            if(draft.items.getJSONObject(i).getJSONObject("recognition").optBoolean("userConfirmed") || draft.items.getJSONObject(i).has("detailEvidence"))
                throw new IOException("確認済み個体・詳細記録があるため自動再集計できません");
        String original=draft.inventory.toString(2), oldId=draft.inventory.getString("scanSessionId");
        File old=draft.session();
        draft.inventory.put("scanSessionId",UUID.randomUUID().toString()).put("replayedFrom",oldId);
        while(draft.items.length()>0)draft.items.remove(draft.items.length()-1);
        File crops=new File(draft.session(),"crops"),pages=new File(draft.session(),"pages");
        crops.mkdirs();pages.mkdirs();
        Point size=new Point();((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealSize(size);
        List<long[]> previous=new ArrayList<>();
        for(int page=0;page<count;page++) {
            File input=new File(new File(old,"pages"),page+".png");
            Bitmap roi=BitmapFactory.decodeFile(input.getPath());
            int top=Math.round(size.y*config.top),bottom=Math.round(size.y*config.bottom);
            if(roi==null||roi.getWidth()!=size.x||roi.getHeight()!=bottom-top)throw new IOException("保存画像の寸法が異なります: "+page);
            Bitmap full=Bitmap.createBitmap(size.x,size.y,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(full);
            canvas.drawBitmap(roi,new Rect(0,0,size.x,1),new Rect(0,top-4,size.x,top),null);
            canvas.drawBitmap(roi,0,top,null);roi.recycle();
            try {
                List<BoxGridDetector.Row> rows=BoxGridDetector.detect(full,Collections.emptyList(),config);
                List<long[]> current=new ArrayList<>();for(BoxGridDetector.Row row:rows)current.add(row.signature());
                if(current.isEmpty())throw new IOException("グリッド不明: "+page);
                int skip=previous.isEmpty()?0:InventoryDeduplicator.pageSame(previous,current)?current.size():InventoryDeduplicator.overlap(previous,current);
                for(int r=skip;r<rows.size();r++)for(int c=0;c<rows.get(r).cells.size();c++) {
                    Rect rect=rows.get(r).cells.get(c);
                    String name=String.format(Locale.ROOT,"%05d.png",draft.items.length());
                    Bitmap crop=Bitmap.createBitmap(full,rect.left,rect.top,rect.width(),rect.height());
                    try(FileOutputStream out=new FileOutputStream(new File(crops,name))){crop.compress(Bitmap.CompressFormat.PNG,100,out);}finally{crop.recycle();}
                    draft.add(page,r*config.columns+c,"scan_sessions/"+draft.inventory.getString("scanSessionId")+"/crops/"+name,rows.get(r).hashes.get(c));
                }
                previous=current;
                Files.copy(input.toPath(),new File(pages,page+".png").toPath());
                if(page%5==0)progress.accept("保存画像を再集計："+(page+1)+"/"+count+"ページ・"+draft.items.length()+"枠");
            } finally {full.recycle();}
        }
        Files.write(new File(old,"summary.json").toPath(),original.getBytes(StandardCharsets.UTF_8));
        draft.inventory.put("observedEntries",draft.items.length()).put("completeScan",false)
                .put("traversalComplete",false).put("countReconciled",false)
                .put("stopReason","保存画像の再集計完了。素材の件数・個体の同定は未確認");
        draft.save();
        return count+"ページを照合し、"+draft.items.length()+"枠を記録しました。個体番号・素材数は未確認です。";
    }
}
