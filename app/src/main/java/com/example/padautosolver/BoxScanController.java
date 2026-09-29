package com.example.padautosolver;
import android.content.Context;import android.graphics.*;import org.json.*;import java.io.*;import java.util.*;
/** Read-only traversal. Only scrolling is emitted; identity and completeness never inferred from icons. */
final class BoxScanController {
 private final Context context;private final BoxCalibration config;private InventoryRepository repo;
 private List<long[]> last=new ArrayList<>(),pending=new ArrayList<>();private int stable,unchanged,pages,scrolls;
 private StagePolicy.Decision queuedScroll;
 private int missingSceneFrames, alignmentMisses;
 private boolean restorePage; private float pendingTop=-1000, lastTop=-1000;
 private boolean awaitingScroll,finished;private long lastProgress=android.os.SystemClock.elapsedRealtime();
 BoxScanController(Context c)throws Exception{this(c,false);}
 BoxScanController(Context c,boolean resume)throws Exception{
  context=c;config=new BoxCalibration(c.getSharedPreferences("pad_solver",0));config.validate();
  if(resume){
   repo=new InventoryRepository(c);pages=repo.inventory.optInt("pageCount",0);
   if(pages<1||repo.items.length()==0||repo.inventory.optBoolean("traversalComplete"))throw new IllegalStateException("途中再開できる走査記録がありません");
   if(repo.inventory.optInt("expectedCount")!=config.expected)throw new IllegalStateException("走査時の所持数設定が変わっています");
   restorePage=true;awaitingScroll=true;
  }else repo=InventoryRepository.begin(c,config);
 }
 StagePolicy.Decision inspect(Bitmap b,List<StagePolicy.Item> items)throws Exception {
  if(finished)return stop("BOX走査終了。所持一覧の確認へ進んでください");
  if(!BoxScenePolicy.isBox(items,b.getHeight())) {
   if(++missingSceneFrames<3)return waitFor("BOX画面名を再確認中（操作待機）");
   return stop("通常のモンスターBOX（ALL）以外のため停止");
  }
  missingSceneFrames=0;
  if(queuedScroll!=null)return queuedScroll;
  if(restorePage){
   Bitmap saved=BitmapFactory.decodeFile(new File(new File(repo.session(),"pages"),(pages-1)+".png").getPath());
   if(saved==null||saved.getWidth()!=b.getWidth())return stop("前回ページの画像または画面寸法を確認できません");
   Bitmap full=Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);
   new Canvas(full).drawBitmap(saved,0,Math.round(b.getHeight()*config.top),null);saved.recycle();
   for(BoxGridDetector.Row row:BoxGridDetector.detect(full,items,config)){if(last.isEmpty())lastTop=row.y;last.add(row.signature());}full.recycle();
   if(last.isEmpty())return stop("前回ページの位置を復元できません");
   restorePage=false;
  }
  List<BoxGridDetector.Row> rows=BoxGridDetector.detect(b,items,config);List<long[]> current=new ArrayList<>();for(BoxGridDetector.Row row:rows)current.add(row.signature());
  if(rows.isEmpty())return stop("グリッドを検出できません。キャリブレーションを確認してください");
  if(Math.abs(pendingTop-rows.get(0).y)<=3 && InventoryDeduplicator.stableFrame(pending,current))stable++;else{pending=current;stable=1;}pendingTop=rows.get(0).y;
  if(stable<2){if(android.os.SystemClock.elapsedRealtime()-lastProgress>20000){saveAlignmentEvidence(b,current,"画面の静止判定が不一致");return stop("20秒間安定しないため停止");}return waitFor("BOX静止待ち");}
  int skip=0;
  if(!last.isEmpty()) {
   if(Math.abs(lastTop-rows.get(0).y)<=3 && InventoryDeduplicator.stableFrame(last,current)){
    if(!awaitingScroll)return waitFor("スクロール待ち");unchanged++;
    if(unchanged>=3){boolean covered=InventoryDeduplicator.endVerified(unchanged,repo.items.length(),config.expected,config.topConfirmed,config.filtersConfirmed);repo.inventory.put("visualEndReached",true).put("observedEntries",repo.items.length()).put("countReconciled",covered).put("traversalComplete",covered).put("scanEndEvidence","3 completed scrolls: unchanged full rows and last row; observed="+repo.items.length()+" expected="+config.expected);repo.save();finished=true;return stop(covered?"末尾までの走査候補："+repo.items.length()+"個体。人手照合と詳細確認が必要です":"一覧末尾候補："+repo.items.length()+"枠 / 所持数表示"+config.expected+"。素材・件数の照合が必要です");}
    return scroll(b);
   }
   try{skip=InventoryDeduplicator.overlap(last,current);}catch(IllegalStateException e){
    saveAlignmentEvidence(b,current,e.getMessage());
    if(++alignmentMisses<3){stable=0;return waitFor("ページの重なりを再確認中（操作待機）");}
    return stop(e.getMessage());
   }unchanged=0;alignmentMisses=0;
  }
  int additions=0;for(int row=skip;row<rows.size();row++)additions+=rows.get(row).cells.size();
  if(repo.items.length()+additions>20000)return stop("記録上限20000枠で停止。走査範囲を確認してください");
  File dir=new File(repo.session(),"crops");dir.mkdirs();
  for(int row=skip;row<rows.size();row++)for(int cell=0;cell<rows.get(row).cells.size();cell++){
   Rect rect=rows.get(row).cells.get(cell);String name=String.format(java.util.Locale.ROOT,"%05d.png",repo.items.length());File out=new File(dir,name);Bitmap crop=Bitmap.createBitmap(b,rect.left,rect.top,rect.width(),rect.height());try(FileOutputStream stream=new FileOutputStream(out)){crop.compress(Bitmap.CompressFormat.PNG,100,stream);}crop.recycle();
   repo.add(pages,row*config.columns+cell,"scan_sessions/"+repo.inventory.getString("scanSessionId")+"/crops/"+name,rows.get(row).hashes.get(cell));
  }
  File pd=new File(repo.session(),"pages");pd.mkdirs();Rect roi=new Rect(0,Math.round(b.getHeight()*config.top),b.getWidth(),Math.round(b.getHeight()*config.bottom));Bitmap crop=Bitmap.createBitmap(b,0,roi.top,roi.width(),roi.height());try(FileOutputStream stream=new FileOutputStream(new File(pd,pages+".png"))){crop.compress(Bitmap.CompressFormat.PNG,100,stream);}crop.recycle();
  pages++;repo.inventory.put("pageCount",pages);repo.save();last=current;lastTop=rows.get(0).y;lastProgress=android.os.SystemClock.elapsedRealtime();awaitingScroll=false;
  return scroll(b);
 }
 private StagePolicy.Decision scroll(Bitmap b)throws Exception{if(scrolls>=300)return stop("スクロール上限300回で停止");StagePolicy.Decision d=new StagePolicy.Decision(new StagePolicy.Item("BOX_SCROLL",b.getWidth()*.46f,b.getHeight()*.78f),"BOX_SCAN："+repo.items.length()+"枠・"+pages+"ページ（所持数表示"+config.expected+"）",false);d.endY=b.getHeight()*(.78f-config.pitchY*1.5f);d.holdMs=1000;d.completed=()->{scrolls++;awaitingScroll=true;stable=0;queuedScroll=null;};queuedScroll=d;return d;}
 private void saveAlignmentEvidence(Bitmap b,List<long[]> current,String error)throws Exception {
  File dir=new File(repo.session(),"alignment");dir.mkdirs();
  Bitmap roi=Bitmap.createBitmap(b,0,Math.round(b.getHeight()*config.top),b.getWidth(),Math.round(b.getHeight()*(config.bottom-config.top)));
  String stem=pages+"-"+alignmentMisses;
  try(FileOutputStream out=new FileOutputStream(new File(dir,stem+".png"))){roi.compress(Bitmap.CompressFormat.PNG,100,out);}finally{roi.recycle();}
  JSONObject data=new JSONObject().put("error",error).put("previous",hashRows(last)).put("current",hashRows(current));
  try(FileOutputStream out=new FileOutputStream(new File(dir,stem+".json"))){out.write(data.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 }
 private JSONArray hashRows(List<long[]> rows)throws Exception {JSONArray all=new JSONArray();for(long[] row:rows){JSONArray values=new JSONArray();for(long h:row)values.put(Long.toUnsignedString(h,16));all.put(values);}return all;}
 private StagePolicy.Decision stop(String message)throws Exception{repo.inventory.put("stopReason",message);repo.save();return new StagePolicy.Decision(null,message,true);}
 private StagePolicy.Decision waitFor(String s){return new StagePolicy.Decision(null,s,false);}
}
