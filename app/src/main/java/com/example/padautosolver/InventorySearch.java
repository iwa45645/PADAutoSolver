package com.example.padautosolver;
import org.json.*;
import java.text.Normalizer;
import java.util.Locale;

final class InventorySearch {
 static String normalize(String text){return Normalizer.normalize(text,Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);}
 static boolean matches(String query,int slot,JSONObject item,JSONObject review){
  String q=normalize(query);if(q.isEmpty())return true;
  if(q.startsWith("#"))return q.equals("#"+slot);
  JSONObject[] sources={item,item.optJSONObject("observedIdentity"),review};
  for(JSONObject s:sources)if(s!=null){
   if(q.matches("[0-9]+")){if(!s.isNull("monsterId")&&q.equals(s.optString("monsterId")))return true;}
   else if(!s.isNull("name")&&normalize(s.optString("name")).contains(q))return true;
  }
  return false;
 }
}
