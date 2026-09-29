package com.example.padautosolver;
import java.util.*;
import org.json.*;
/** User-imported, local metadata only. No implicit network lookups or guessed identity. */
public interface MonsterCatalogProvider {
 String getVersion(); List<Integer> identify(long iconHash); JSONObject getById(int id);
 final class Local implements MonsterCatalogProvider {
  private final JSONObject root;
  public Local(JSONObject data)throws JSONException{root=data;if(data.optString("source").isEmpty()||data.optString("license").isEmpty()||data.optString("version").isEmpty())throw new JSONException("source/license/version required");}
  public String getVersion(){return root.optString("version");}
  public JSONObject getById(int id){JSONArray a=root.optJSONArray("monsters");if(a!=null)for(int i=0;i<a.length();i++)if(a.optJSONObject(i)!=null&&a.optJSONObject(i).optInt("monsterId")==id)return a.optJSONObject(i);return null;}
  public List<Integer> identify(long hash){List<Integer> out=new ArrayList<>();JSONArray a=root.optJSONArray("monsters");if(a!=null)for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);try{if(m!=null&&m.has("iconHash")&&Long.bitCount(Long.parseUnsignedLong(m.getString("iconHash"),16)^hash)<=3)out.add(m.getInt("monsterId"));}catch(Exception ignored){}}return out;}
 }
}
