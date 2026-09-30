package com.example.padautosolver;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryReviewTest {
 private JSONObject inventory()throws Exception{return new JSONObject("{scanSessionId:'s',expectedCount:1,items:[{instanceId:'s:0',monsterId:null,recognition:{userConfirmed:false},detailEvidence:[{image:'scan_sessions/s/details/one.png'}]},{instanceId:'s:1',monsterId:null,recognition:{userConfirmed:false},detailEvidence:[{image:'scan_sessions/s/details/two.png'}]}]}");}
 private JSONObject bundle()throws Exception{return new JSONObject("{schemaVersion:1,kind:'inventory_review',scanSessionId:'s',items:[{instanceId:'s:0',monsterId:123,name:'テスト',evidence:'scan_sessions/s/details/one.png',source:'image review',inspectNext:true,userConfirmed:true}],countReconciliation:{stackedInstanceIds:['s:1'],displayedBoxCount:1,userConfirmed:true}}");}
 @Test public void neverConfirmsOrMutatesInventory()throws Exception{JSONObject inv=inventory();String before=inv.toString();JSONObject clean=InventoryReview.sanitized(bundle(),inv);assertEquals(before,inv.toString());assertFalse(clean.getJSONArray("items").getJSONObject(0).getBoolean("userConfirmed"));assertFalse(clean.getJSONObject("countReconciliation").getBoolean("userConfirmed"));assertTrue(clean.getJSONArray("items").getJSONObject(0).getBoolean("inspectNext"));}
 @Test public void rejectsAnotherSession()throws Exception{JSONObject b=bundle().put("scanSessionId","other");assertThrows(IllegalArgumentException.class,()->InventoryReview.validate(b,inventory()));}
 @Test public void rejectsOtherIndividualsImage()throws Exception{JSONObject b=bundle();b.getJSONArray("items").getJSONObject(0).put("evidence","scan_sessions/s/details/two.png");assertThrows(IllegalArgumentException.class,()->InventoryReview.validate(b,inventory()));}
 @Test public void rejectsDuplicateAndUnknownInstances()throws Exception{JSONObject b=bundle();b.getJSONArray("items").put(b.getJSONArray("items").getJSONObject(0));assertThrows(IllegalArgumentException.class,()->InventoryReview.validate(b,inventory()));JSONObject other=bundle();other.getJSONArray("items").getJSONObject(0).put("instanceId","s:999");assertThrows(IllegalArgumentException.class,()->InventoryReview.validate(other,inventory()));}
 @Test public void rejectsUnsafeEvidence()throws Exception{JSONObject b=bundle();b.getJSONArray("items").getJSONObject(0).put("evidence","../one.png");assertThrows(IllegalArgumentException.class,()->InventoryReview.validate(b,inventory()));}
 @Test public void rejectsCountMismatch()throws Exception{JSONObject b=bundle();b.getJSONObject("countReconciliation").put("displayedBoxCount",2);assertThrows(IllegalArgumentException.class,()->InventoryReview.validate(b,inventory()));}
 @Test public void nullableNameRemainsUnknown()throws Exception{JSONObject b=bundle();b.getJSONArray("items").getJSONObject(0).put("name",JSONObject.NULL);assertTrue(InventoryReview.sanitized(b,inventory()).getJSONArray("items").getJSONObject(0).isNull("name"));}
}
