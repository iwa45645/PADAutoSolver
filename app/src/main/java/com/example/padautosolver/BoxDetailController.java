package com.example.padautosolver;

import android.content.Context;
import android.graphics.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Read-only detail traversal. No sale, favorite, enhancement or skill controls are emitted. */
final class BoxDetailController {
    private final InventoryRepository repo;
    private final BoxCalibration config;
    private final long[] reference;
    private final List<Set<Long>> variants=new ArrayList<>();
    private final Map<Integer,Integer> pageOrigins=new HashMap<>();
    private long[][] prepared;
    private int preparedPages;
    private int target=-1, stable, misses, reads, visited, unchanged, previousStart=-1;
    private boolean inDetail, returning, scrolled;
    private int pendingStart=-1;
    private float pendingTop=-1000;
    private DetailIdentity pendingIdentity;
    private String pendingHeader;
    private StagePolicy.Decision queued;

    BoxDetailController(Context context)throws Exception {
        repo=new InventoryRepository(context);
        config=new BoxCalibration(context.getSharedPreferences("pad_solver",0));config.validate();
        if(repo.items.length()==0)throw new Exception("先にBOX画像を走査してください");
        reference=new long[repo.items.length()];
        for(int i=0;i<reference.length;i++) {
            JSONObject entry=repo.items.getJSONObject(i);
            reference[i]=Long.parseUnsignedLong(entry.getString("iconHash"),16);
            Set<Long> values=new HashSet<>();values.add(reference[i]);variants.add(values);
            int page=entry.getInt("sourcePage"), origin=i-entry.getInt("sourceCell");
            if(pageOrigins.containsKey(page)&&pageOrigins.get(page)!=origin)throw new Exception("保存ページの位置情報が矛盾しています");
            pageOrigins.put(page,origin);
        }
    }

    StagePolicy.Decision inspect(Bitmap frame,StageNavigator navigator)throws Exception {
        if(prepared==null) {
            prepareVariants(frame);
            if(prepared==null)return waitFor("保存画像を照合準備 "+preparedPages+" / "+repo.inventory.optInt("pageCount"));
        }
        List<StagePolicy.Item> items=navigator.readItems(frame);
        if(inDetail) {
            List<StagePolicy.Item> header=navigator.readDetailHeader(frame);
            DetailIdentity identity=DetailIdentity.parse(header,frame.getHeight());
            boolean detail=identity!=null||DetailIdentity.hasHeader(header,frame.getHeight());
            StringBuilder headerKey=new StringBuilder();
            for(StagePolicy.Item line:header)headerKey.append(line.rawText).append('|');
            // Require both the numbered header and a status/skill area; a menu or BOX is never a detail.
            boolean body=false;for(StagePolicy.Item line:items)if(line.y>frame.getHeight()*.60f
                    && (line.text.contains("スキル")||line.text.contains("HP")))body=true;
            if(!detail||!body) {
                if(++misses<5)return waitFor("詳細画面を再確認中 #"+(target+1));
                return stop("詳細番号・画面種別を確認できないため停止 #"+(target+1));
            }
            misses=0;
            if(returning)return back(frame);
            reads++;
            boolean same=identity==null ? pendingIdentity==null && headerKey.toString().equals(pendingHeader) : identity.same(pendingIdentity);
            if(!same){pendingIdentity=identity;pendingHeader=headerKey.toString();stable=1;}else stable++;
            if(stable<2) {
                if(reads>=6)return stop("詳細OCRが一致しないため停止 #"+(target+1));
                return waitFor("番号・名前の再読込 #"+(target+1));
            }
            save(frame,items,header,identity==null?new DetailIdentity(0,null):identity);
            returning=true;
            return back(frame);
        }

        if(!BoxScenePolicy.isBox(items,frame.getHeight())||hasMenu(items,frame.getHeight())) {
            if(++misses<5)return waitFor("通常BOXへの復帰を確認中");
            return stop("通常のBOX一覧を確認できないため停止");
        }
        List<BoxGridDetector.Row> rows=BoxGridDetector.detect(frame,items,config);
        List<Rect> cells=new ArrayList<>();List<Long> hashes=new ArrayList<>();
        for(BoxGridDetector.Row row:rows){cells.addAll(row.cells);hashes.addAll(row.hashes);}
        long[] current=new long[hashes.size()];for(int i=0;i<current.length;i++)current[i]=hashes.get(i);
        int start=InventoryPageMatcher.locate(prepared,current);
        if(start<0) {
            queued=null;
            if(++misses<5)return waitFor("保存済みBOXとの順序を照合中");
            return stop("BOXの並び・個体が保存画像と一致しません。順序変更や追加を確認してください");
        }
        misses=0;
        float top=rows.get(0).y;
        if(start!=pendingStart||Math.abs(top-pendingTop)>3){pendingStart=start;pendingTop=top;stable=1;queued=null;return waitFor("BOXの静止と個体位置を確認中");}
        stable++;
        if(queued!=null)return queued;
        if(scrolled){if(start==previousStart)unchanged++;else unchanged=0;scrolled=false;}

        for(int cell=0;cell<cells.size();cell++) {
            int index=start+cell;
            JSONObject entry=repo.items.getJSONObject(index);
            if(((entry.optString("detailStatus").equals("identity_observed")||entry.optString("detailStatus").equals("identity_unresolved")) && entry.optJSONObject("observedIdentity")!=null && entry.getJSONObject("observedIdentity").optInt("readerVersion")==2)||entry.getJSONObject("recognition").optBoolean("userConfirmed"))continue;
            Rect r=cells.get(cell);
            StagePolicy.Decision d=action("BOX_DETAIL_OPEN",r.centerX(),r.centerY(),"個体詳細を開く #"+(index+1)+" / "+reference.length);
            d.holdMs=800;
            d.completed=()->{target=index;inDetail=true;returning=false;stable=reads=misses=0;pendingIdentity=null;pendingHeader=null;queued=null;};
            queued=d;return d;
        }
        if(start+cells.size()==reference.length) {
            repo.inventory.put("identityTraversalReachedEnd",true);repo.save();
            return stop("詳細の末尾まで記録しました。OCR結果・育成状態の照合が必要です");
        }
        if(unchanged>=3)return stop("詳細走査のスクロールが進まないため停止");
        StagePolicy.Decision d=action("BOX_DETAIL_SCROLL",frame.getWidth()*.46f,frame.getHeight()*.78f,"記録済みの行をスクロール（今回"+visited+"件）");
        d.endY=frame.getHeight()*(.78f-config.pitchY*1.5f);d.holdMs=1000;
        d.completed=()->{previousStart=start;scrolled=true;stable=0;queued=null;};queued=d;return d;
    }

    private void prepareVariants(Bitmap frame)throws Exception {
        int pageCount=repo.inventory.optInt("pageCount");
        for(int n=0;n<10&&preparedPages<pageCount;n++,preparedPages++) {
            Integer origin=pageOrigins.get(preparedPages);
            if(origin==null)continue;
            Bitmap roi=BitmapFactory.decodeFile(new File(new File(repo.session(),"pages"),preparedPages+".png").getPath());
            if(roi==null)throw new IOException("保存ページが見つかりません");
            Bitmap full=null;
            try {
                int top=Math.round(frame.getHeight()*config.top),bottom=Math.round(frame.getHeight()*config.bottom);
                if(roi.getWidth()!=frame.getWidth()||roi.getHeight()!=bottom-top)throw new IOException("保存ページの寸法が異なります");
                full=Bitmap.createBitmap(frame.getWidth(),frame.getHeight(),Bitmap.Config.ARGB_8888);
                Canvas canvas=new Canvas(full);
                canvas.drawBitmap(roi,new Rect(0,0,roi.getWidth(),1),new Rect(0,top-4,roi.getWidth(),top),null);
                canvas.drawBitmap(roi,0,top,null);
                List<BoxGridDetector.Row> rows=BoxGridDetector.detect(full,Collections.emptyList(),config);
                if(rows.isEmpty())throw new IOException("保存ページのグリッドが不明です");
                int at=origin;
                for(BoxGridDetector.Row row:rows)for(long hash:row.hashes) {
                    if(at<0||at>=variants.size())throw new IOException("保存ページの範囲が不正です");
                    variants.get(at++).add(hash);
                }
            }finally{roi.recycle();if(full!=null)full.recycle();}
        }
        if(preparedPages>=pageCount) {
            prepared=new long[reference.length][];
            for(int i=0;i<prepared.length;i++){Set<Long> values=variants.get(i);prepared[i]=new long[values.size()];int j=0;for(long h:values)prepared[i][j++]=h;}
        }
    }

    private boolean hasMenu(List<StagePolicy.Item> lines,int height) {
        for(StagePolicy.Item line:lines)if(line.y>height*.40f&&(line.text.contains("情報を見る")||line.text.contains("お気に入り登録")||line.text.contains("お気に入り解除")))return true;
        return false;
    }
    private StagePolicy.Decision back(Bitmap frame) {
        if(queued!=null)return queued;
        StagePolicy.Decision d=action("BOX_DETAIL_BACK",frame.getWidth()*.075f,frame.getHeight()*.217f,"詳細記録済み #"+(target+1)+"：BOXへ戻る");
        d.holdMs=120;d.completed=()->{inDetail=returning=false;stable=misses=0;pendingStart=-1;queued=null;};queued=d;return d;
    }
    private void save(Bitmap frame,List<StagePolicy.Item> items,List<StagePolicy.Item> header,DetailIdentity identity)throws Exception {
        JSONObject entry=repo.items.getJSONObject(target);
        if(entry.getJSONObject("recognition").optBoolean("userConfirmed")&&entry.optInt("monsterId")!=identity.id)throw new Exception("人手確認済み番号とOCRが不一致です");
        File dir=new File(repo.session(),"details");dir.mkdirs();String name="identity-"+target+"-"+System.currentTimeMillis()+".png";
        int top=Math.round(frame.getHeight()*.19f),bottom=Math.round(frame.getHeight()*.86f);
        Bitmap crop=Bitmap.createBitmap(frame,0,top,frame.getWidth(),bottom-top);
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){crop.compress(Bitmap.CompressFormat.PNG,100,out);}finally{crop.recycle();}
        StringBuilder text=new StringBuilder();for(StagePolicy.Item line:items)if(line.y>top)text.append(line.rawText).append('\n');
        JSONArray evidence=entry.optJSONArray("detailEvidence");if(evidence==null)evidence=new JSONArray();
        String path="scan_sessions/"+repo.inventory.getString("scanSessionId")+"/details/"+name;
        StringBuilder headerText=new StringBuilder();for(StagePolicy.Item line:header)headerText.append(line.rawText).append('\n');
        evidence.put(new JSONObject().put("image",path).put("ocr",text.toString()).put("headerOcr",headerText.toString()).put("source","live-detail").put("matchingFrames",stable));
        entry.put("detailEvidence",evidence).put("detailStatus",identity.id>0?"identity_observed":"identity_unresolved");
        entry.put("observedIdentity",new JSONObject().put("monsterId",identity.id>0?identity.id:JSONObject.NULL).put("name",identity.name==null?JSONObject.NULL:identity.name).put("readerVersion",2).put("status","ocr_consensus_unverified").put("evidence",path));
        // Keep canonical identity and userConfirmed unchanged until reviewed.
        JSONArray candidates=new JSONArray();if(identity.id>0)candidates.put(new JSONObject().put("monsterId",identity.id).put("name",identity.name==null?JSONObject.NULL:identity.name).put("source","detail_ocr_consensus"));
        entry.getJSONObject("recognition").put("candidates",candidates);
        visited++;repo.inventory.put("lastDetailIndex",target).put("detailIdentityObservations",countObserved());repo.save();
        android.util.Log.i("PADSolver","boxDetailSaved index="+target+" id="+(identity.id>0?identity.id:"unknown")+" name="+identity.name);
    }
    private int countObserved()throws Exception{int n=0;for(int i=0;i<repo.items.length();i++)if(repo.items.getJSONObject(i).has("observedIdentity"))n++;return n;}
    private StagePolicy.Decision action(String key,float x,float y,String status){return new StagePolicy.Decision(new StagePolicy.Item(key,x,y),status,false);}
    private StagePolicy.Decision waitFor(String status){return new StagePolicy.Decision(null,status,false);}
    private StagePolicy.Decision stop(String status)throws Exception{repo.inventory.put("detailStopReason",status);repo.save();return new StagePolicy.Decision(null,status,true);}
}
