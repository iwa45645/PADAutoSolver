package com.example.padautosolver;
import android.content.Context;import android.graphics.*;import org.json.*;import java.io.*;import java.util.*;
/** Read-only traversal. Only scrolling is emitted; identity and completeness never inferred from icons. */
final class BoxScanController {
 private final Context context;private final BoxCalibration config;private InventoryRepository repo;
 private List<long[]> last=new ArrayList<>(),pending=new ArrayList<>();private int stable,unchanged,pages,scrolls;
 private StagePolicy.Decision queuedScroll;
 private int missingSceneFrames;
 private boolean awaitingScroll,finished;private long lastProgress=android.os.SystemClock.elapsedRealtime();
 BoxScanController(Context c)throws Exception{context=c;config=new BoxCalibration(c.getSharedPreferences("pad_solver",0));config.validate();repo=InventoryRepository.begin(c,config);}
 StagePolicy.Decision inspect(Bitmap b,List<StagePolicy.Item> items)throws Exception {
  if(finished)return stop("BOX走査終了。所持一覧の確認へ進んでください");
  if(!BoxScenePolicy.isBox(items,b.getHeight())) {
   if(++missingSceneFrames<3)return waitFor("BOX画面名を再確認中（操作待機）");
   return stop("通常のモンスターBOX（ALL）以外のため停止");
  }
  missingSceneFrames=0;
  if(queuedScroll!=null)return queuedScroll;
  List<BoxGridDetector.Row> rows=BoxGridDetector.detect(b,items,config);List<long[]> current=new ArrayList<>();for(BoxGridDetector.Row row:rows)current.add(row.signature());
  if(rows.isEmpty())return stop("グリッドを検出できません。キャリブレーションを確認してください");
  if(InventoryDeduplicator.pageSame(pending,current))stable++;else{pending=current;stable=1;}
  if(stable<2){if(android.os.SystemClock.elapsedRealtime()-lastProgress>15000)return stop("15秒間安定しないため停止");return waitFor("BOX静止待ち");}
  int skip=0;
  if(!last.isEmpty()) {
   if(InventoryDeduplicator.pageSame(last,current)){
    if(!awaitingScroll)return waitFor("スクロール待ち");unchanged++;
    if(unchanged>=3){boolean covered=InventoryDeduplicator.endVerified(unchanged,repo.items.length(),config.expected,config.topConfirmed,config.filtersConfirmed);repo.inventory.put("traversalComplete",covered).put("scanEndEvidence","3 completed scrolls: unchanged full rows and last row; observed="+repo.items.length()+" expected="+config.expected);repo.save();finished=true;return stop(covered?"末尾までの走査候補："+repo.items.length()+"個体。人手照合と詳細確認が必要です":"末尾または停止位置：個体数不一致 "+repo.items.length()+"/"+config.expected+"。全走査済みとは扱いません");}
    return scroll(b);
   }
   try{skip=InventoryDeduplicator.overlap(last,current);}catch(IllegalStateException e){return stop(e.getMessage());}unchanged=0;
  }
  if(repo.items.length()>config.expected)return stop("所持数を超えたため停止。重複・設定を確認してください");
  File dir=new File(repo.session(),"crops");dir.mkdirs();
  for(int row=skip;row<rows.size();row++)for(int cell=0;cell<rows.get(row).cells.size();cell++){
   Rect rect=rows.get(row).cells.get(cell);String name=String.format(java.util.Locale.ROOT,"%05d.png",repo.items.length());File out=new File(dir,name);Bitmap crop=Bitmap.createBitmap(b,rect.left,rect.top,rect.width(),rect.height());try(FileOutputStream stream=new FileOutputStream(out)){crop.compress(Bitmap.CompressFormat.PNG,100,stream);}crop.recycle();
   repo.add(pages,row*config.columns+cell,"scan_sessions/"+repo.inventory.getString("scanSessionId")+"/crops/"+name,rows.get(row).hashes.get(cell));
  }
  File pd=new File(repo.session(),"pages");pd.mkdirs();Rect roi=new Rect(0,Math.round(b.getHeight()*config.top),b.getWidth(),Math.round(b.getHeight()*config.bottom));Bitmap crop=Bitmap.createBitmap(b,0,roi.top,roi.width(),roi.height());try(FileOutputStream stream=new FileOutputStream(new File(pd,pages+".png"))){crop.compress(Bitmap.CompressFormat.PNG,100,stream);}crop.recycle();
  pages++;repo.inventory.put("pageCount",pages);repo.save();last=current;lastProgress=android.os.SystemClock.elapsedRealtime();awaitingScroll=false;
  return scroll(b);
 }
 private StagePolicy.Decision scroll(Bitmap b)throws Exception{if(scrolls>=300)return stop("スクロール上限300回で停止");StagePolicy.Decision d=new StagePolicy.Decision(new StagePolicy.Item("BOX_SCROLL",b.getWidth()*.46f,b.getHeight()*.78f),"BOX_SCAN："+repo.items.length()+"/"+config.expected+"個体・"+pages+"ページ",false);d.endY=b.getHeight()*(.78f-config.pitchY*3);d.holdMs=700;d.completed=()->{scrolls++;awaitingScroll=true;stable=0;queuedScroll=null;};queuedScroll=d;return d;}
 private StagePolicy.Decision stop(String message)throws Exception{repo.inventory.put("stopReason",message);repo.save();return new StagePolicy.Decision(null,message,true);}
 private StagePolicy.Decision waitFor(String s){return new StagePolicy.Decision(null,s,false);}
}
