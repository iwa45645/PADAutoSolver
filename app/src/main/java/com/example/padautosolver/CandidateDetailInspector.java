package com.example.padautosolver;
import android.content.Context;import android.graphics.Bitmap;import org.json.*;import java.io.*;import java.util.*;
final class CandidateDetailInspector {
 static String record(Context context,Bitmap b,List<StagePolicy.Item> items)throws Exception{
  InventoryRepository repo=new InventoryRepository(context);int index=context.getSharedPreferences("pad_solver",0).getInt("box.detailIndex",-1);if(index<0||index>=repo.items.length())throw new Exception("所持一覧から撮影対象の個体を選んでください");
  String text="";for(StagePolicy.Item item:items)text+=item.text+"\n";if(!text.contains("NO.")&&!text.contains("スキル")&&!text.contains("覚醒"))throw new Exception("モンスター詳細画面を確認できません");
  JSONObject item=repo.items.getJSONObject(index);String name="detail-"+index+"-"+System.currentTimeMillis()+".png";File dir=new File(repo.session(),"details");dir.mkdirs();Bitmap crop=Bitmap.createBitmap(b,0,Math.round(b.getHeight()*.19f),b.getWidth(),Math.round(b.getHeight()*.67f));try(FileOutputStream out=new FileOutputStream(new File(dir,name))){crop.compress(Bitmap.CompressFormat.PNG,100,out);}crop.recycle();
  JSONArray pages=item.optJSONArray("detailEvidence");if(pages==null)pages=new JSONArray();pages.put(new JSONObject().put("image","scan_sessions/"+repo.inventory.getString("scanSessionId")+"/details/"+name).put("ocr",text));item.put("detailEvidence",pages).put("detailStatus","partial");repo.save();return "詳細記録 #"+(index+1)+"：未検証のOCR・画像を保存";
 }
}
