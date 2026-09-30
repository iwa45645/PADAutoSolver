package com.example.padautosolver;

import org.json.*;
import java.util.*;

/** Untrusted offline observations, kept separate from user-confirmed inventory. */
final class InventoryReview {
    static Map<String,JSONObject> validate(JSONObject bundle, JSONObject inventory) throws JSONException {
        if(bundle.getInt("schemaVersion")!=1 || !"inventory_review".equals(bundle.getString("kind")))
            throw new IllegalArgumentException("照合データの形式が違います");
        if(!inventory.getString("scanSessionId").equals(bundle.getString("scanSessionId")))
            throw new IllegalArgumentException("別のBOX走査に対する照合データです");
        JSONArray items=inventory.getJSONArray("items"), reviews=bundle.getJSONArray("items");
        Map<String,JSONObject> owned=new HashMap<>(), result=new LinkedHashMap<>();
        for(int i=0;i<items.length();i++) {
            JSONObject item=items.getJSONObject(i);
            if(owned.put(item.getString("instanceId"),item)!=null)throw new IllegalArgumentException("所持一覧の個体IDが重複しています");
        }
        if(reviews.length()>items.length())throw new IllegalArgumentException("照合件数が所持一覧を超えています");
        for(int i=0;i<reviews.length();i++) {
            JSONObject review=reviews.getJSONObject(i);
            String instance=review.getString("instanceId");JSONObject item=owned.get(instance);
            if(item==null || result.containsKey(instance))throw new IllegalArgumentException("未所持または重複した個体IDです");
            int id=review.getInt("monsterId");if(id<1||id>999999)throw new IllegalArgumentException("番号候補が不正です");
            String evidence=review.getString("evidence");
            if(evidence.startsWith("/")||evidence.contains("\\")||evidence.contains(":")||Arrays.asList(evidence.split("/")).contains(".."))
                throw new IllegalArgumentException("証跡パスが不正です");
            JSONArray images=item.optJSONArray("detailEvidence");boolean found=false;
            if(images!=null)for(int j=0;j<images.length();j++)if(evidence.equals(images.getJSONObject(j).optString("image")))found=true;
            if(!found)throw new IllegalArgumentException("個体と詳細画像の対応が一致しません");
            String name=review.isNull("name")?null:review.optString("name",null);
            if(name!=null&&(name.trim().isEmpty()||name.length()>160))throw new IllegalArgumentException("名前候補が不正です");
            String source=review.getString("source");if(source.trim().isEmpty()||source.length()>400)throw new IllegalArgumentException("照合元が不正です");
            // Whitelist fields. Imported 'confirmed', 'roles', or similar claims confer no authority.
            JSONObject clean=new JSONObject().put("instanceId",instance).put("monsterId",id)
                    .put("name",name==null?JSONObject.NULL:name).put("evidence",evidence).put("source",source)
                    .put("inspectNext",review.optBoolean("inspectNext",false)).put("reason",review.optString("reason", ""))
                    .put("userConfirmed",false);
            if(clean.getString("reason").length()>600)throw new IllegalArgumentException("検査理由が長すぎます");
            result.put(instance,clean);
        }
        JSONObject count=bundle.optJSONObject("countReconciliation");
        if(count!=null) {
            JSONArray stacked=count.getJSONArray("stackedInstanceIds");Set<String> unique=new HashSet<>();
            for(int i=0;i<stacked.length();i++)if(!owned.containsKey(stacked.getString(i))||!unique.add(stacked.getString(i)))
                throw new IllegalArgumentException("素材内訳に未所持・重複があります");
            int displayed=count.getInt("displayedBoxCount");
            if(displayed!=inventory.getInt("expectedCount")||items.length()-unique.size()!=displayed)
                throw new IllegalArgumentException("所持数の内訳が一致しません");
        }
        return result;
    }
    static JSONObject sanitized(JSONObject bundle,JSONObject inventory)throws JSONException {
        Map<String,JSONObject> entries=validate(bundle,inventory);JSONArray rows=new JSONArray();
        for(JSONObject item:entries.values())rows.put(item);
        JSONObject clean=new JSONObject().put("schemaVersion",1).put("kind","inventory_review")
                .put("scanSessionId",bundle.getString("scanSessionId")).put("items",rows);
        JSONObject count=bundle.optJSONObject("countReconciliation");
        if(count!=null)clean.put("countReconciliation",new JSONObject()
                .put("stackedInstanceIds",new JSONArray(count.getJSONArray("stackedInstanceIds").toString()))
                .put("displayedBoxCount",count.getInt("displayedBoxCount")).put("userConfirmed",false));
        return clean;
    }
}
